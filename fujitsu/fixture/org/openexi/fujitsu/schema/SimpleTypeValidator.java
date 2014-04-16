package org.openexi.fujitsu.schema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * SimpleTypeValidator validates text values against the declared
 * schema datatypes.
 */
public final class SimpleTypeValidator {

  private final EXISchema m_schema;

  private static final int LENGTH        = 0;
  private static final int MIN_LENGTH    = 1;
  private static final int MAX_LENGTH    = 2;
  public static final int MIN_INCLUSIVE = 3;
  public static final int MAX_INCLUSIVE = 4;
  public static final int MIN_EXCLUSIVE = 5;
  public static final int MAX_EXCLUSIVE = 6;

  private static final String XML_LETTER_SET =
    // BaseChar
    "\u0041\u005A\u0061\u007A\u00C0\u00D6\u00D8\u00F6\u00F8\u00FF\u0100\u0131" +
    "\u0134\u013E\u0141\u0148\u014A\u017E\u0180\u01C3\u01CD\u01F0\u01F4\u01F5" +
    "\u01FA\u0217\u0250\u02A8\u02BB\u02C1\u0386\u0386\u0388\u038A\u038C\u038C" +
    "\u038E\u03A1\u03A3\u03CE\u03D0\u03D6\u03DA\u03DA\u03DC\u03DC\u03DE\u03DE" +
    "\u03E0\u03E0\u03E2\u03F3\u0401\u040C\u040E\u044F\u0451\u045C\u045E\u0481" +
    "\u0490\u04C4\u04C7\u04C8\u04CB\u04CC\u04D0\u04EB\u04EE\u04F5\u04F8\u04F9" +
    "\u0531\u0556\u0559\u0559\u0561\u0586\u05D0\u05EA\u05F0\u05F2\u0621\u063A" +
    "\u0641\u064A\u0671\u06B7\u06BA\u06BE\u06C0\u06CE\u06D0\u06D3\u06D5\u06D5" +
    "\u06E5\u06E6\u0905\u0939\u093D\u093D\u0958\u0961\u0985\u098C\u098F\u0990" +
    "\u0993\u09A8\u09AA\u09B0\u09B2\u09B2\u09B6\u09B9\u09DC\u09DD\u09DF\u09E1" +
    "\u09F0\u09F1\u0A05\u0A0A\u0A0F\u0A10\u0A13\u0A28\u0A2A\u0A30\u0A32\u0A33" +
    "\u0A35\u0A36\u0A38\u0A39\u0A59\u0A5C\u0A5E\u0A5E\u0A72\u0A74\u0A85\u0A8B" +
    "\u0A8D\u0A8D\u0A8F\u0A91\u0A93\u0AA8\u0AAA\u0AB0\u0AB2\u0AB3\u0AB5\u0AB9" +
    "\u0ABD\u0ABD\u0AE0\u0AE0\u0B05\u0B0C\u0B0F\u0B10\u0B13\u0B28\u0B2A\u0B30" +
    "\u0B32\u0B33\u0B36\u0B39\u0B3D\u0B3D\u0B5C\u0B5D\u0B5F\u0B61\u0B85\u0B8A" +
    "\u0B8E\u0B90\u0B92\u0B95\u0B99\u0B9A\u0B9C\u0B9C\u0B9E\u0B9F\u0BA3\u0BA4" +
    "\u0BA8\u0BAA\u0BAE\u0BB5\u0BB7\u0BB9\u0C05\u0C0C\u0C0E\u0C10\u0C12\u0C28" +
    "\u0C2A\u0C33\u0C35\u0C39\u0C60\u0C61\u0C85\u0C8C\u0C8E\u0C90\u0C92\u0CA8" +
    "\u0CAA\u0CB3\u0CB5\u0CB9\u0CDE\u0CDE\u0CE0\u0CE1\u0D05\u0D0C\u0D0E\u0D10" +
    "\u0D12\u0D28\u0D2A\u0D39\u0D60\u0D61\u0E01\u0E2E\u0E30\u0E30\u0E32\u0E33" +
    "\u0E40\u0E45\u0E81\u0E82\u0E84\u0E84\u0E87\u0E88\u0E8A\u0E8A\u0E8D\u0E8D" +
    "\u0E94\u0E97\u0E99\u0E9F\u0EA1\u0EA3\u0EA5\u0EA5\u0EA7\u0EA7\u0EAA\u0EAB" +
    "\u0EAD\u0EAE\u0EB0\u0EB0\u0EB2\u0EB3\u0EBD\u0EBD\u0EC0\u0EC4\u0F40\u0F47" +
    "\u0F49\u0F69\u10A0\u10C5\u10D0\u10F6\u1100\u1100\u1102\u1103\u1105\u1107" +
    "\u1109\u1109\u110B\u110C\u110E\u1112\u113C\u113C\u113E\u113E\u1140\u1140" +
    "\u114C\u114C\u114E\u114E\u1150\u1150\u1154\u1155\u1159\u1159\u115F\u1161" +
    "\u1163\u1163\u1165\u1165\u1167\u1167\u1169\u1169\u116D\u116E\u1172\u1173" +
    "\u1175\u1175\u119E\u119E\u11A8\u11A8\u11AB\u11AB\u11AE\u11AF\u11B7\u11B8" +
    "\u11BA\u11BA\u11BC\u11C2\u11EB\u11EB\u11F0\u11F0\u11F9\u11F9\u1E00\u1E9B" +
    "\u1EA0\u1EF9\u1F00\u1F15\u1F18\u1F1D\u1F20\u1F45\u1F48\u1F4D\u1F50\u1F57" +
    "\u1F59\u1F59\u1F5B\u1F5B\u1F5D\u1F5D\u1F5F\u1F7D\u1F80\u1FB4\u1FB6\u1FBC" +
    "\u1FBE\u1FBE\u1FC2\u1FC4\u1FC6\u1FCC\u1FD0\u1FD3\u1FD6\u1FDB\u1FE0\u1FEC" +
    "\u1FF2\u1FF4\u1FF6\u1FFC\u2126\u2126\u212A\u212B\u212E\u212E\u2180\u2182" +
    "\u3041\u3094\u30A1\u30FA\u3105\u312C\uAC00\uD7A3" +
    // Ideographic
    "\u4E00\u9FA5\u3007\u3007\u3021\u3029";

  private static final String XML_DIGIT_COMBINING_CHAR_EXTENDER_SET =
    // Digit
    "\u0030\u0039\u0660\u0669\u06F0\u06F9\u0966\u096F\u09E6\u09EF\u0A66\u0A6F" +
    "\u0AE6\u0AEF\u0B66\u0B6F\u0BE7\u0BEF\u0C66\u0C6F\u0CE6\u0CEF\u0D66\u0D6F" +
    "\u0E50\u0E59\u0ED0\u0ED9\u0F20\u0F29" +
    // Combining Char
    "\u0300\u0345\u0360\u0361\u0483\u0486\u0591\u05A1\u05A3\u05B9\u05BB\u05BD" +
    "\u05BF\u05BF\u05C1\u05C2\u05C4\u05C4\u064B\u0652\u0670\u0670\u06D6\u06DC" +
    "\u06DD\u06DF\u06E0\u06E4\u06E7\u06E8\u06EA\u06ED\u0901\u0903\u093C\u093C" +
    "\u093E\u094C\u094D\u094D\u0951\u0954\u0962\u0963\u0981\u0983\u09BC\u09BC" +
    "\u09BE\u09BE\u09BF\u09BF\u09C0\u09C4\u09C7\u09C8\u09CB\u09CD\u09D7\u09D7" +
    "\u09E2\u09E3\u0A02\u0A02\u0A3C\u0A3C\u0A3E\u0A3E\u0A3F\u0A3F\u0A40\u0A42" +
    "\u0A47\u0A48\u0A4B\u0A4D\u0A70\u0A71\u0A81\u0A83\u0ABC\u0ABC\u0ABE\u0AC5" +
    "\u0AC7\u0AC9\u0ACB\u0ACD\u0B01\u0B03\u0B3C\u0B3C\u0B3E\u0B43\u0B47\u0B48" +
    "\u0B4B\u0B4D\u0B56\u0B57\u0B82\u0B83\u0BBE\u0BC2\u0BC6\u0BC8\u0BCA\u0BCD" +
    "\u0BD7\u0BD7\u0C01\u0C03\u0C3E\u0C44\u0C46\u0C48\u0C4A\u0C4D\u0C55\u0C56" +
    "\u0C82\u0C83\u0CBE\u0CC4\u0CC6\u0CC8\u0CCA\u0CCD\u0CD5\u0CD6\u0D02\u0D03" +
    "\u0D3E\u0D43\u0D46\u0D48\u0D4A\u0D4D\u0D57\u0D57\u0E31\u0E31\u0E34\u0E3A" +
    "\u0E47\u0E4E\u0EB1\u0EB1\u0EB4\u0EB9\u0EBB\u0EBC\u0EC8\u0ECD\u0F18\u0F19" +
    "\u0F35\u0F35\u0F37\u0F37\u0F39\u0F39\u0F3E\u0F3E\u0F3F\u0F3F\u0F71\u0F84" +
    "\u0F86\u0F8B\u0F90\u0F95\u0F97\u0F97\u0F99\u0FAD\u0FB1\u0FB7\u0FB9\u0FB9" +
    "\u20D0\u20DC\u20E1\u20E1\u302A\u302F\u3099\u3099\u309A\u309A" +
    // Extender
    "\u00B7\u00B7\u02D0\u02D0\u02D1\u02D1\u0387\u0387\u0640\u0640\u0E46\u0E46" +
    "\u0EC6\u0EC6\u3005\u3005\u3031\u3035\u309D\u309E\u30FC\u30FE";

  private static final String BASE64_ASCIIS =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

  private static final String HEXBIN_ASCIIS = "0123456789ABCDEF";

  private static final byte[] NAMEHEADS   = new byte[8192];
  private static final byte[] NAMECHARS   = new byte[8192];
  private static final byte[] BASE64CHARS = new byte[8192];

  private static final DatatypeFactory m_datatypeFactory;
  static {
    DatatypeFactory datatypeFactory = null;
    try {
      datatypeFactory = DatatypeFactory.newInstance();
    }
    catch(DatatypeConfigurationException dce) {
      throw new RuntimeException(dce);
    }
    finally {
      m_datatypeFactory = datatypeFactory;
    }
  }

  static {
    // Letter
    int i, len;
    for (i = 0, len = XML_LETTER_SET.length(); i < len; i += 2) {
      char c, ed;
      for (c = XML_LETTER_SET.charAt(i), ed = XML_LETTER_SET.charAt(i + 1);
           c <= ed; c++) {
        NAMEHEADS[c / 8] |= 1 << (7 - c % 8);
        NAMECHARS[c / 8] |= 1 << (7 - c % 8);
      }
    }
    // Digit, Combining Char, Extender
    for (i = 0, len = XML_DIGIT_COMBINING_CHAR_EXTENDER_SET.length(); i < len; i += 2) {
      char c, ed;
      for (c = XML_DIGIT_COMBINING_CHAR_EXTENDER_SET.charAt(i),
           ed = XML_DIGIT_COMBINING_CHAR_EXTENDER_SET.charAt(i + 1);
           c <= ed; c++)
        NAMECHARS[c / 8] |= 1 << (7 - c % 8);
    }
    // '_'
    NAMEHEADS['_' / 8] |= 1 << (7 - '_' % 8);
    NAMECHARS['_' / 8] |= 1 << (7 - '_' % 8);
    // ':'
    NAMEHEADS[':' / 8] |= 1 << (7 - ':' % 8);
    NAMECHARS[':' / 8] |= 1 << (7 - ':' % 8);
    // '.'
    NAMECHARS['.' / 8] |= 1 << (7 - '.' % 8);
    // '-'
    NAMECHARS['-' / 8] |= 1 << (7 - '-' % 8);

    // Base64 asciis
    for (i = 0, len = BASE64_ASCIIS.length(); i < len; i++) {
      char c = BASE64_ASCIIS.charAt(i);
      BASE64CHARS[c / 8] |= 1 << (7 - c % 8);
    }
  }

  static boolean isNameHeadChar(char c) {
    return (NAMEHEADS[c / 8] & (1 << (7 - c % 8))) != 0;
  }

  static boolean isNameChar(char c) {
    return (NAMECHARS[c / 8] & (1 << (7 - c % 8))) != 0;
  }

  private static boolean isBase64Char(char c) {
    return (BASE64CHARS[c / 8] & (1 << (7 - c % 8))) != 0;
  }

  /**
   * Determines if it is a sequence of XML Chars per definition:
   * Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
   * any Unicode character, excluding the surrogate blocks, FFFE, and FFFF.
   */
  static boolean isXMLCharString(String text) {
    final int len;
    int i;
    for (i = 0, len = text.length(); i < len;) {
      final char c = text.charAt(i);
      if (0xD7FF < c && c < 0xDC00) { // high surrogate
        if (++i < len) {
          final char low = text.charAt(i);
          if (0xDBFF < low && low < 0xE000) { // low surrogate
            ++i;
            continue;
          }
        }
        // invalid surrogate pair
        return false;
      }
      else {
        switch (c) {
          case '\t':
          case '\n':
          case '\r':
            break;
          default:
            if (!(('\u0020' <= c && c <= '\uD7FF') || ('\uE000' <= c && c <= '\uFFFD'))) {
              return false;
            }
            break;
        }
        ++i;
      }
    }
    return true;
  }
  
  public static int toUTF16(int ucs4, char[] utf16) {
    if (ucs4 < 0 || ucs4 > 0x10FFFF) {
      return -1; // not a valid ucs4 value.
    }
    if (ucs4 < 0x10000) { // BMP
      utf16[0] = (char)ucs4;
      return 1;
    }
    final int value = ucs4 - 0x10000;
    final int highSurrogate = (0xD800 + (value >>> 10));
    final int lowSurrogate = (0xDC00 + (value & 0x3FF));
    assert highSurrogate < 0xDC00 && lowSurrogate < 0xE000;
    utf16[0] = (char)highSurrogate;
    utf16[1] = (char)lowSurrogate;
    return 2;
  }
  
  public SimpleTypeValidator(EXISchema schemaCorpus) {
    m_schema = schemaCorpus;
  }

  EXISchema getEXISchema() {
    return m_schema;
  }

  /**
   * Validates a text against a simple type.
   * @param text text to be validated
   * @param stype simple type or complex type of simple content
   */
  public String validate(String text, int stype)
      throws SchemaValidatorException {
    return validate(text, stype, new PrefixUriBindings());
  }

  public String validate(String text, int stype, PrefixUriBindings npm)
    throws SchemaValidatorException {
    return validate(text, stype, (SimpleTypeValidationInfo)null, npm);
  }

  /**
   * Validates a text against a simple type.
   * @param text text to be validated
   * @param stype simple type or complex type of simple content
   * @param valInfo nullable reference to the area to store post-validation typed object.
   * @return text normalized per whitespace facet value
   */
  public String validate(String text, int stype,
                         SimpleTypeValidationInfo valInfo)
    throws SchemaValidatorException {
    return validate(text, stype, valInfo, new PrefixUriBindings());
  }
    
  /**
   * Validates a text against a simple type.
   * @param text text to be validated
   * @param stype simple type or complex type of simple content
   * @param valInfo nullable reference to the area to store post-validation typed object.
   * @param npm namespace-prefix map (nullable)
   * @return text normalized per whitespace facet value
   */
  public String validate(String text, int stype,
                         SimpleTypeValidationInfo valInfo,
                         PrefixUriBindings npm)
      throws SchemaValidatorException {
    if (m_schema.getNodeType(stype) == EXISchema.COMPLEX_TYPE_NODE) {
      if (m_schema.getContentClassOfComplexType(stype) == EXISchema.CONTENT_SIMPLE) {
        while (m_schema.getNodeType(stype) != EXISchema.SIMPLE_TYPE_NODE)
          stype = m_schema.getBaseTypeOfType(stype);
      }
      else {
        throw new SchemaValidatorException(
            SchemaValidatorException.COMPLEX_TYPE_NOT_OF_SIMPLE_CONTENT,
            new String[] { m_schema.getNameOfType(stype),
            m_schema.getTargetNamespaceNameOfType(stype) },
            m_schema, stype);
      }
    }
    if (valInfo == null) {
      valInfo = new SimpleTypeValidationInfo();
    }
    ListTypedValue listValue = new ListTypedValue(m_schema);
    valInfo.setListTypedValue(listValue);
    return validate(text, stype, listValue, npm);
  }

  private String validate(String text, int stype, ListTypedValue listValue, PrefixUriBindings npm)
      throws SchemaValidatorException {
    if (m_schema.getNodeType(stype) == EXISchema.COMPLEX_TYPE_NODE) {
      if (m_schema.getContentClassOfComplexType(stype) == EXISchema.CONTENT_SIMPLE) {
        while (m_schema.getNodeType(stype) != EXISchema.SIMPLE_TYPE_NODE)
          stype = m_schema.getBaseTypeOfType(stype);
      }
      else {
        throw new SchemaValidatorException(
            SchemaValidatorException.COMPLEX_TYPE_NOT_OF_SIMPLE_CONTENT,
            new String[] { m_schema.getNameOfType(stype),
            m_schema.getTargetNamespaceNameOfType(stype) },
            m_schema, stype);
      }
    }
    String norm = text;
    int whiteSpace = m_schema.getWhitespaceFacetValueOfSimpleType(stype);
    if (whiteSpace != EXISchema.WHITESPACE_ABSENT) {
      norm = normalize(text, whiteSpace);
    }
    int i;
    AtomicTypedValue atomicValue = null;
    int variety = m_schema.getVarietyOfSimpleType(stype);
    int primTypeId;
    switch (variety) {
      case EXISchema.ATOMIC_SIMPLE_TYPE:
        int primType = m_schema.getPrimitiveTypeOfAtomicSimpleType(stype);
        primTypeId = m_schema.getSerialOfType(primType);
        Object data = parseText(norm, stype, primTypeId, npm);
        atomicValue = new AtomicTypedValue(m_schema, primTypeId);
        listValue.appendAtomicValue(atomicValue);
        // comes before validateAtomic to make the parsed value available
        atomicValue.setTypedValue(data);
        validateAtomic(norm, data, stype, primTypeId);
        atomicValue.setType(stype); // This comes after validateAtomic call
        break;
      case EXISchema.UNION_SIMPLE_TYPE:
	int n_memberTypes, n_enums;
	for (i = 0, n_memberTypes = m_schema.getMemberTypesCountOfSimpleType(stype);
	     i < n_memberTypes; i++) {
          boolean doCont = true;
	  try {
            norm = validate(text, m_schema.getMemberTypeOfSimpleType(stype, i), listValue, npm);
            if ((n_enums = m_schema.getEnumerationFacetCountOfSimpleType(stype)) > 0) {
              try {
                doCont = false;
//                primTypeId = lv.isList() ? EXISchema.NIL_NODE :
//                    lv.getLastAtomicValue().getPrimTypeId();
                atomicValue = listValue.getLastAtomicValue();
                validateValueEnumeration(norm, atomicValue.getValue(),
                                         stype, atomicValue.getPrimTypeId(),
                                         n_enums);
              }
              catch (SchemaValidatorException sve) {
                throw sve;
              }
            }
            return norm;
	  }
	  catch (SchemaValidatorException sve) {
            if (doCont)
              continue;
            else
              throw sve;
	  }
	}
	String typeName = m_schema.getNameOfType(stype);
	typeName = typeName != null ? "'" + typeName + "'" : "anonymous";
        throw new SchemaValidatorException(
            SchemaValidatorException.INVALID_UNION, new String[] { text , typeName },
            m_schema, stype);
      case EXISchema.LIST_SIMPLE_TYPE:
        if (listValue.getType() == EXISchema.NIL_NODE) {
          listValue.setType(stype);
        }
        int st, len, n_items;
        int itemType = m_schema.getItemTypeOfListSimpleType(stype);
        for (i = st = 0, n_items = 0, len = norm.length(); i < len; i++) {
          char c;
          if ((c = norm.charAt(i)) == ' ' || i == len - 1) { // end-of-token
            ++n_items;
            int ed = c != ' ' ? i + 1 : i;
            validate(norm.substring(st, ed), itemType, listValue, npm);
            st = ed + 1;
          }
        }
        int expected;
        if ((expected = m_schema.getLengthFacetValueOfSimpleType(stype)) >= 0) {
          if (n_items != expected) {
            throw new SchemaValidatorException(
                SchemaValidatorException.LENGTH_INVALID,
                new String[] { Integer.toString(n_items), Integer.toString(expected)},
                m_schema, stype);
          }
        }
        if ((expected = m_schema.getMinLengthFacetValueOfSimpleType(stype)) >= 0) {
          if (n_items < expected) {
            throw new SchemaValidatorException(
                SchemaValidatorException.MIN_LENGTH_INVALID,
                new String[] { Integer.toString(n_items), Integer.toString(expected)},
                m_schema, stype);
          }
        }
        if ((expected = m_schema.getMaxLengthFacetValueOfSimpleType(stype)) >= 0) {
          if (expected < n_items) {
            throw new SchemaValidatorException(
                SchemaValidatorException.MAX_LENGTH_INVALID,
                new String[] { Integer.toString(n_items), Integer.toString(expected)},
                m_schema, stype);
          }
        }
        break;
      default: // i.e. EXISchema.UR_SIMPLE_TYPE
        primTypeId = EXISchemaConst.ANY_SIMPLE_TYPE;
        atomicValue = new AtomicTypedValue(m_schema, primTypeId);
        atomicValue.setTypedValue(parseText(norm, stype, primTypeId, npm));
        atomicValue.setType(stype); // This comes after validateAtomic call
        listValue.appendAtomicValue(atomicValue);
        break;
    }
    return norm;
  }

  /**
   * Validate an element value against an element definition.
   * Specifying a null or empty value causes the method to return default
   * (or fixed) value if any otherwise null without performing validation.
   * @param text an element value
   * @param elem element node
   * @param valInfo validation result
   * @return normalized element value
   * @throws SchemaValidatorException if there are any validation errors
   * @throws EXISchemaRuntimeException if elem is not a simple content element
   */
  public String validateElemValue(String text, int elem,
                                  SimpleTypeValidationInfo valInfo)
      throws SchemaValidatorException, EXISchemaRuntimeException {
    return validateElemValue(text, elem, valInfo, null);
  }
    
  /**
   * Validate an element value against an element definition.
   * Specifying a null or empty value causes the method to return default
   * (or fixed) value if any otherwise null without performing validation.
   * @param text an element value
   * @param elem element node
   * @param valInfo validation result
   * @param npm namespace-prefix mapping (nullable)
   * @return normalized element value
   * @throws SchemaValidatorException if there are any validation errors
   * @throws EXISchemaRuntimeException if elem is not a simple content element
   */
  public String validateElemValue(String text, int elem,
                                  SimpleTypeValidationInfo valInfo,
                                  PrefixUriBindings npm)
      throws SchemaValidatorException, EXISchemaRuntimeException {

    if (m_schema.getNodeType(elem) != EXISchema.ELEMENT_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_ELEMENT,
          new String[] { String.valueOf(elem) });
    }
    else if (m_schema.getContentClassOfElem(elem) != EXISchema.CONTENT_SIMPLE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.ELEMENT_CONTENT_NOT_SIMPLE,
          new String[] { String.valueOf(elem) });
    }

    final int stype = m_schema.getSimpleTypeOfElem(elem);
    final int constraint = m_schema.getConstraintOfElem(elem);
    int cvalue = EXISchema.NIL_VALUE;
    if (constraint != EXISchema.CONSTRAINT_NONE)
      cvalue = m_schema.getConstraintValueOfElem(elem);

    String norm = null;

    // substitute with default value if any for null or empty content
    if ((text == null || text.length() == 0) && cvalue != EXISchema.NIL_VALUE &&
        constraint != EXISchema.CONSTRAINT_NONE)
      text = variantToString(cvalue, stype, npm); 

    if (text != null) {
      TypedValue typedValue = null;
      if (stype != EXISchema.NIL_NODE) {
        if (valInfo == null)
          valInfo = new SimpleTypeValidationInfo();
        norm = validate(text, stype, valInfo, npm);
        typedValue = valInfo.getTypedValue();
      }

      if (constraint == EXISchema.CONSTRAINT_FIXED && cvalue != EXISchema.NIL_VALUE) {
        boolean matched;
        if (typedValue.isList()) {
          try {
            final SimpleTypeValidationInfo valInfoFixed = new SimpleTypeValidationInfo();
            validate(variantToString(cvalue, stype, npm), stype, valInfoFixed, npm);
            matched = typedValue.equals(valInfoFixed.getTypedValue());
          }
          catch (SchemaValidatorException sve) {
            matched = false;
          }
        }
        else {
          AtomicTypedValue atomicValue = (AtomicTypedValue)typedValue;
          matched = validateFixedValue(norm, atomicValue.getValue(), cvalue, atomicValue.getPrimTypeId());
        }
        if (!matched) {
          throw new SchemaValidatorException(
              SchemaValidatorException.ELEMENT_INVALID_PER_FIXED,
              new String[] { norm, variantToString(cvalue, stype, npm), m_schema.getNameOfElem(elem),
              m_schema.getTargetNamespaceNameOfElem(elem) }, m_schema, elem);
        }
      }
    }
    return norm;
  }

  /**
   * Validate an attribute value against an attribute definition or
   * an attribute use. Specifying a null value causes the method to
   * return default (or fixed) value if any otherwise null without
   * performing validation.
   * @param text an attribute value
   * @param attr either an attribute definition or an attribute use
   * @param valInfo validation result
   * @return normalized attribute value
   * @throws SchemaValidatorException if there are any validation errors
   * @throws EXISchemaRuntimeException if attr is neither an attribute nor an attribute use
   */
  public String validateAttrValue(String text, int attr,
                                  SimpleTypeValidationInfo valInfo)
    throws SchemaValidatorException, EXISchemaRuntimeException {
    return validateAttrValue(text, attr, valInfo, null);
  }
  
  /**
   * Validate an attribute value against an attribute definition or
   * an attribute use. Specifying a null value causes the method to
   * return default (or fixed) value if any otherwise null without
   * performing validation.
   * @param text an attribute value
   * @param attr either an attribute definition or an attribute use
   * @param valInfo validation result
   * @param npm namespace-prefix map (nullable)
   * @return normalized attribute value
   * @throws SchemaValidatorException if there are any validation errors
   * @throws EXISchemaRuntimeException if attr is neither an attribute nor an attribute use
   */
  public String validateAttrValue(String text, int attr,
                                  SimpleTypeValidationInfo valInfo,
                                  PrefixUriBindings npm)
      throws SchemaValidatorException, EXISchemaRuntimeException {
    int stype = EXISchema.NIL_NODE;
    int constraint = EXISchema.CONSTRAINT_NONE;
    int cvalue = EXISchema.NIL_VALUE;
    int nodeType;
    switch (nodeType = m_schema.getNodeType(attr)) {
      case EXISchema.ATTRIBUTE_NODE:
        stype = m_schema.getTypeOfAttr(attr);
        constraint = m_schema.getConstraintOfAttr(attr);
        cvalue = m_schema.getConstraintValueOfAttr(attr);
        break;
      case EXISchema.ATTRIBUTE_USE_NODE:
        stype = m_schema.getAttrOfAttrUse(attr);
        if (stype != EXISchema.NIL_NODE)
          stype = m_schema.getTypeOfAttr(stype);
        constraint = m_schema.getConstraintOfAttrUse(attr);
        cvalue = m_schema.getConstraintValueOfAttrUse(attr);
        break;
      default:
        throw new EXISchemaRuntimeException(
            EXISchemaRuntimeException.NOT_ATTRIBUTE_NOR_ATTRIBUTE_USE,
            new String[] { String.valueOf(attr) });
    }

    String norm = null;

    if (text == null && constraint != EXISchema.CONSTRAINT_NONE) {
      text = variantToString(cvalue, stype, npm);
    }

    if (valInfo == null) {
      valInfo = new SimpleTypeValidationInfo();
    }
    
    if (text != null) {
      TypedValue typedValue = null;
      if (stype != EXISchema.NIL_NODE) {
        norm = validate(text, stype, valInfo, npm);
        typedValue = valInfo.getTypedValue();
      }

      if (constraint == EXISchema.CONSTRAINT_FIXED && cvalue != EXISchema.NIL_VALUE) {
        boolean matched = false;
        if (typedValue.isList()) {
          ListTypedValue listTypedValue = (ListTypedValue)typedValue;
          assert m_schema.getTypeOfVariant(cvalue) == EXISchema.VARIANT_LIST;
          int[] variants = m_schema.getListValueOfVariant(cvalue);
          if (listTypedValue.getAtomicValueCount() == variants.length) {
            for (int i = 0; i < variants.length; i++) {
              Object value = listTypedValue.getAtomicValue(i).getValue();
              switch (m_schema.getTypeOfVariant(variants[i])) {
                case EXISchema.VARIANT_STRING:
                  matched = value.equals(m_schema.getStringValueOfVariant(variants[i]));
                  break;
                case EXISchema.VARIANT_QNAME:
                  QName qni = (QName)value;
                  int qns = m_schema.getQNameValueOfVariant(variants[i]);
                  if (qni.localName.equals(m_schema.getNameOfQName(qns))) {
                    String nsname = qni.namespaceName;
                    if (nsname != null && nsname.equals(m_schema.getNamespaceNameOfQName(qns)) ||
                        nsname == null && m_schema.getNamespaceNameOfQName(qns) == null)
                      matched = true;
                  }
                  break;
                case EXISchema.VARIANT_DECIMAL:
                  matched = ((XSDecimal)value).getValue().compareTo(
                      m_schema.getDecimalValueOfVariant(variants[i])) == 0;
                  break;
                case EXISchema.VARIANT_FLOAT:
                  matched = ((Float)value).compareTo(m_schema.getFloatValueOfVariant(variants[i])) == 0;
                  break;
                case EXISchema.VARIANT_DOUBLE:
                  matched = ((Double)value).compareTo(m_schema.getDoubleValueOfVariant(variants[i])) == 0;
                  break;
                case EXISchema.VARIANT_DATETIME:
                  matched = ((XSDateTime)value).equals(m_schema.getDateTimeValueOfVariant(variants[i]));
                  break;
                case EXISchema.VARIANT_DURATION:
                  matched = ((Duration)value).equals(m_schema.getDurationValueOfVariant(variants[i]));
                  break;
                case EXISchema.VARIANT_BINARY:
                  byte[] vi = (byte[])value;
                  byte[] vs = m_schema.getBinaryValueOfVariant(variants[i]);
                  if (vi.length == vs.length) {
                    int j;
                    for (j = 0; j < vi.length && vi[j] == vs[j]; j++);
                    matched = j == vi.length;
                  }
                  break;
                case EXISchema.VARIANT_BOOLEAN:
                  matched = ((Boolean)value).booleanValue() == m_schema.getBooleanValueOfVariant(variants[i]);
                  break;
                default:
                  break;
              }
            }
          }
        }
        else {
          AtomicTypedValue atomicValue = (AtomicTypedValue)typedValue;
          matched = validateFixedValue(norm, atomicValue.getValue(), cvalue, atomicValue.getPrimTypeId());
        }
        if (!matched) {
          int attrNode = attr;
          if (nodeType == EXISchema.ATTRIBUTE_USE_NODE)
            attrNode = m_schema.getAttrOfAttrUse(attrNode);
          throw new SchemaValidatorException(
              SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED,
              new String[] { norm, variantToString(cvalue, stype, npm), m_schema.getNameOfAttr(attrNode),
              m_schema.getTargetNamespaceNameOfAttr(attrNode) }, m_schema, attr);
        }
      }
    }
    return norm;
  }

  /**
   * Convert a variant into a string. 
   * @param variant variant value being converted
   * @param stype type associated with the variant if any, or NIL_NODE
   */
  private String variantToString(int variant, int stype, PrefixUriBindings npm)
    throws SchemaValidatorException {
    if (variant != EXISchema.NIL_VALUE) {
      int primTypeId = EXISchemaConst.UNTYPED;
      if (stype != EXISchema.NIL_NODE &&
          m_schema.getVarietyOfSimpleType(stype) == EXISchema.ATOMIC_SIMPLE_TYPE) {
        primTypeId = m_schema.getSerialOfType(m_schema.getPrimitiveTypeOfAtomicSimpleType(stype));
      }
      switch (m_schema.getTypeOfVariant(variant)) {
        case EXISchema.VARIANT_STRING:
          try {
            return m_schema.getStringValueOfVariant(variant);
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchema.VARIANT_QNAME:
          // REVISIT: implement QName variant to String conversion using NamespacePrefixMap
          break;
        case EXISchema.VARIANT_DECIMAL:
          try {
            BigDecimal decimal = m_schema.getDecimalValueOfVariant(variant);
            return decimal.toString();
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchema.VARIANT_FLOAT:
          try {
            return Float.toString(m_schema.getFloatValueOfVariant(variant));
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchema.VARIANT_DOUBLE:
          try {
            return Double.toString(m_schema.getDoubleValueOfVariant(variant));
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchema.VARIANT_DATETIME:
          try {
            XSDateTime dateTime = m_schema.getDateTimeValueOfVariant(variant);
            return dateTime.getXMLGregorianCalendar().toXMLFormat();
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchema.VARIANT_DURATION:
          try {
            Duration duration = m_schema.getDurationValueOfVariant(variant);
            return duration.toString();
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchema.VARIANT_BINARY:
          try {
            byte[] binary = m_schema.getBinaryValueOfVariant(variant);
            StringBuffer encodingResult = new StringBuffer();
            switch (primTypeId) {
              case EXISchemaConst.BASE64BINARY_TYPE:
                Base64.encode(binary, encodingResult);
                break;
              case EXISchemaConst.HEXBINARY_TYPE:
                HexBin.encode(binary, encodingResult);
                break;
              default:
                assert false;
                break;
            }
            return encodingResult.toString();
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchema.VARIANT_BOOLEAN:
          try {
            boolean bval = m_schema.getBooleanValueOfVariant(variant);
            return Boolean.toString(bval);
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchema.VARIANT_LIST:
          
          break;
        default:
          break;
      }
    }
    return null;
  }

  /**
   * Validates a value against a simple type.
   * @param text textual representation of the value
   * @param validatableObject the value to be validated
   * @param atomicType atomic simple
   * @return AtomicTypedValue
   * @throws SchemaValidatorException if there are any validation errors
   */
  public AtomicTypedValue validateAtomicValue(String text, Object validatableObject, int atomicType)
    throws SchemaValidatorException {
    if (m_schema.getNodeType(atomicType) != EXISchema.SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(atomicType) });
    }
    if (m_schema.getVarietyOfSimpleType(atomicType) != EXISchema.ATOMIC_SIMPLE_TYPE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.SIMPLE_TYPE_NOT_ATOMIC,
          new String[] { String.valueOf(atomicType) });
    }
    String norm = text;
    int whiteSpace = m_schema.getWhitespaceFacetValueOfSimpleType(atomicType);
    if (whiteSpace != EXISchema.WHITESPACE_ABSENT) {
      norm = normalize(text, whiteSpace);
    }
    final int primType = m_schema.getPrimitiveTypeOfAtomicSimpleType(atomicType);
    final int primTypeId = m_schema.getSerialOfType(primType);
    validateAtomic(norm, validatableObject, atomicType, primTypeId);
    
    AtomicTypedValue atomicTypedValue;
    atomicTypedValue = new AtomicTypedValue(m_schema, primTypeId);
    atomicTypedValue.setType(atomicType);
    atomicTypedValue.setTypedValue(validatableObject);
    return atomicTypedValue;
  }
  
  /**
   * Returns objects of one of the following types.
   * 
   * java.lang.String
   * com.fujitsu.xml.xsc.QName
   * com.fujitsu.xml.xsc.XSDecimal
   * java.lang.Float
   * java.lang.Double
   * java.lang.Boolean
   * com.fujitsu.xml.xbrl.xwand.xmlschema.util.XSDateTime
   * com.fujitsu.xml.xbrl.xwand.xmlschema.util.XSDuration;
   * byte[]
   */
  private Object parseText(String norm, int stype, int primTypeId, PrefixUriBindings npm)
      throws SchemaValidatorException {
    switch (primTypeId) {
      // xs:anySimpleType and xdt:anyAtomicType are semi-primitive.
      case EXISchemaConst.ANY_SIMPLE_TYPE:
      case EXISchemaConst.STRING_TYPE:
        return parseString(norm, stype); 
      case EXISchemaConst.ANYURI_TYPE:
        return norm;
      case EXISchemaConst.NOTATION_TYPE:
      case EXISchemaConst.QNAME_TYPE:
        return validateQName(norm, stype, npm);
      case EXISchemaConst.DECIMAL_TYPE:
        final XSDecimal dec = XSDecimal.parse(norm);
        // lexical validation for integer types.
        final int builtinType = m_schema.getBuiltinTypeOfAtomicSimpleType(stype);
        if (m_schema.getSerialOfType(builtinType) != EXISchemaConst.DECIMAL_TYPE &&
            dec.getTrailingZeros() != 0) {
          throw new SchemaValidatorException(
              SchemaValidatorException.INVALID_INTEGER, new String[] { norm }, m_schema, stype);
        }
        return dec;
      case EXISchemaConst.FLOAT_TYPE:
        return parseFloatOrDouble(norm, true, stype);
      case EXISchemaConst.DOUBLE_TYPE:
        return parseFloatOrDouble(norm, false, stype);
      case EXISchemaConst.BOOLEAN_TYPE:
        return parseBoolean(norm, stype);
      case EXISchemaConst.DATETIME_TYPE:
        return parseDateTime(norm, m_schema, stype);
      case EXISchemaConst.DATE_TYPE:
        return parseDate(norm, m_schema, stype);
      case EXISchemaConst.TIME_TYPE:
        return parseTime(norm, m_schema, stype);
      case EXISchemaConst.G_YEARMONTH_TYPE:
        return parseGYearMonth(norm, stype);
      case EXISchemaConst.G_MONTHDAY_TYPE:
        return parseGMonthDay(norm, stype);
      case EXISchemaConst.G_YEAR_TYPE:
        return parseGYear(norm, stype);
      case EXISchemaConst.G_MONTH_TYPE:
        return parseGMonth(norm, stype);
      case EXISchemaConst.G_DAY_TYPE:
        return parseGDay(norm, stype);
      case EXISchemaConst.DURATION_TYPE:
        return parseDuration(norm, stype);
      case EXISchemaConst.BASE64BINARY_TYPE:
      case EXISchemaConst.HEXBINARY_TYPE:
        return parseBinary(norm, stype, primTypeId);
      default:
        return null;
    }
  }

  private byte[] parseBinary(String norm, int stype, int primTypeId)
    throws SchemaValidatorException {

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

    Exception nested_exc = null;

    byte[] ret = null;
    try {
      ret = isBase64 ? Base64.decode(norm) : HexBin.decode(norm);
    }
    catch (RuntimeException e) {
      // REVISIT: Remove this section of code when xerces removal is complete.
      nested_exc = e;
    }
    catch (SchemaValidatorException sve) {
      sve.setEXISchema(m_schema);
      sve.setSchemaNode(stype);
      throw sve;
    }

    if (ret == null) {
      SchemaValidatorException sve = new SchemaValidatorException(
          isBase64 ? SchemaValidatorException.INVALID_BASE64_BINARY :
          SchemaValidatorException.INVALID_HEX_BINARY,
          new String[] {norm}, m_schema, stype);
      sve.setException(nested_exc);
      throw sve;
    }

    return ret;
  }

//  private static byte[] parseHexBinary(String norm)
//    throws SchemaValidatorException {
//    String ret;
//    if ((ret = HexBin.decode(norm)) == null)
//      throw new SchemaValidatorException(
//          SchemaValidatorException.INVALID_HEX_BINARY, new String[] {norm});
//    return ret;
//  }

  static XSDateTime parseDateTime(String norm, EXISchema corpus, int stype)
    throws SchemaValidatorException {
    XMLGregorianCalendar dateTime = null;
    javax.xml.namespace.QName qname = null;
    try {
      dateTime = m_datatypeFactory.newXMLGregorianCalendar(norm);
      qname = dateTime.getXMLSchemaType();
    }
    catch (RuntimeException re) {
    }
    if (dateTime == null || qname == null || DatatypeConstants.DATETIME != qname)  {
      throw new SchemaValidatorException(
          SchemaValidatorException.INVALID_DATETIME, new String[] {norm},
          corpus, stype);
    }
    return new XSDateTime(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), 
        dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), 
        dateTime.getTimezone(), EXISchemaConst.DATETIME_TYPE);
  }
  
  static XSDateTime parseDate(String norm, EXISchema corpus, int stype)
    throws SchemaValidatorException {
    XMLGregorianCalendar dateTime = null;
    javax.xml.namespace.QName qname = null;
    try {
      dateTime = m_datatypeFactory.newXMLGregorianCalendar(norm);
      qname = dateTime.getXMLSchemaType();
    }
    catch (RuntimeException re) {
    }
    if (dateTime == null || qname == null || DatatypeConstants.DATE != qname)  {
      throw new SchemaValidatorException(
          SchemaValidatorException.INVALID_DATE, new String[] {norm},
          corpus, stype);
    }
    return new XSDateTime(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), 
        dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), 
        dateTime.getTimezone(), EXISchemaConst.DATE_TYPE);
  }
  
  static XSDateTime parseTime(String norm, EXISchema corpus, int stype)
    throws SchemaValidatorException {
    XMLGregorianCalendar dateTime = null;
    javax.xml.namespace.QName qname = null;
    try {
      dateTime = m_datatypeFactory.newXMLGregorianCalendar(norm);
      qname = dateTime.getXMLSchemaType();
    }
    catch (RuntimeException re) {
    }
    if (dateTime == null || qname == null || DatatypeConstants.TIME != qname)  {
      throw new SchemaValidatorException(
          SchemaValidatorException.INVALID_TIME, new String[] {norm},
          corpus, stype);
    }
    return new XSDateTime(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), 
        dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), 
        dateTime.getTimezone(), EXISchemaConst.TIME_TYPE);
  }
  
  private XSDateTime parseGYearMonth(String norm, int stype)
    throws SchemaValidatorException {
    XMLGregorianCalendar dateTime = null;
    javax.xml.namespace.QName qname = null;
    try {
      dateTime = m_datatypeFactory.newXMLGregorianCalendar(norm);
      qname = dateTime.getXMLSchemaType();
    }
    catch (RuntimeException re) {
    }
    if (dateTime == null || qname == null || DatatypeConstants.GYEARMONTH != qname)  {
      throw new SchemaValidatorException(
          SchemaValidatorException.INVALID_GYEARMONTH, new String[] {norm},
          m_schema, stype);
    }
    return new XSDateTime(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), 
        dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), 
        dateTime.getTimezone(), EXISchemaConst.G_YEARMONTH_TYPE);
  }
  
  private XSDateTime parseGMonthDay(String norm, int stype)
    throws SchemaValidatorException {
    XMLGregorianCalendar dateTime = null;
    javax.xml.namespace.QName qname = null;
    try {
      dateTime = m_datatypeFactory.newXMLGregorianCalendar(norm);
      qname = dateTime.getXMLSchemaType();
    }
    catch (RuntimeException re) {
    }
    if (dateTime == null || qname == null || DatatypeConstants.GMONTHDAY != qname)  {
      throw new SchemaValidatorException(
          SchemaValidatorException.INVALID_GMONTHDAY, new String[] {norm},
          m_schema, stype);
    }
    return new XSDateTime(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), 
        dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), 
        dateTime.getTimezone(), EXISchemaConst.G_MONTHDAY_TYPE);
  }
  
  private XSDateTime parseGYear(String norm, int stype)
    throws SchemaValidatorException {
    XMLGregorianCalendar dateTime = null;
    javax.xml.namespace.QName qname = null;
    try {
      dateTime = m_datatypeFactory.newXMLGregorianCalendar(norm);
      qname = dateTime.getXMLSchemaType();
    }
    catch (RuntimeException re) {
    }
    if (dateTime == null || qname == null || DatatypeConstants.GYEAR != qname)  {
      throw new SchemaValidatorException(
          SchemaValidatorException.INVALID_GYEAR, new String[] {norm},
          m_schema, stype);
    }
    return new XSDateTime(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), 
        dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), 
        dateTime.getTimezone(), EXISchemaConst.G_YEAR_TYPE);
  }
  
  private XSDateTime parseGMonth(String norm, int stype)
    throws SchemaValidatorException {
    XMLGregorianCalendar dateTime = null;
    javax.xml.namespace.QName qname = null;
    try {
      dateTime = m_datatypeFactory.newXMLGregorianCalendar(norm);
      qname = dateTime.getXMLSchemaType();
    }
    catch (RuntimeException re) {
    }
    if (dateTime == null || qname == null || DatatypeConstants.GMONTH != qname)  {
      throw new SchemaValidatorException(
          SchemaValidatorException.INVALID_GMONTH, new String[] {norm},
          m_schema, stype);
    }
    return new XSDateTime(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), 
        dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), 
        dateTime.getTimezone(), EXISchemaConst.G_MONTH_TYPE);
  }
  
  private XSDateTime parseGDay(String norm, int stype)
    throws SchemaValidatorException {
    XMLGregorianCalendar dateTime = null;
    javax.xml.namespace.QName qname = null;
    try {
      dateTime = m_datatypeFactory.newXMLGregorianCalendar(norm);
      qname = dateTime.getXMLSchemaType();
    }
    catch (RuntimeException re) {
    }
    if (dateTime == null || qname == null || DatatypeConstants.GDAY != qname)  {
      throw new SchemaValidatorException(
          SchemaValidatorException.INVALID_GDAY, new String[] {norm},
          m_schema, stype);
    }
    return new XSDateTime(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), 
        dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), dateTime.getMillisecond(), 
        dateTime.getTimezone(), EXISchemaConst.G_DAY_TYPE);
  }
  
  private Duration parseDuration(String norm, int stype)
      throws SchemaValidatorException {
    Duration duration = null;
    if (norm.length() != 0) {
      try {
        duration = m_datatypeFactory.newDuration(norm);
      }
      catch (IllegalArgumentException iae) {
      }
    }
    if (duration == null) {
      throw new SchemaValidatorException(
          SchemaValidatorException.INVALID_DURATION, new String[] { norm },
          m_schema, stype);
    }
    return duration;
  }

  private String parseString(String norm, int stype)
      throws SchemaValidatorException {
    int builtinType;
    for (builtinType = stype; !m_schema.isBuiltinSimpleType(builtinType);
         builtinType = m_schema.getBaseTypeOfType(builtinType));
    int builtinTypeId = m_schema.getSerialOfType(builtinType);
    switch (builtinTypeId) {
      case EXISchemaConst.LANGUAGE_TYPE:
        validateLanguage(norm, stype); // apply RFC3066 rule
        break;
      case EXISchemaConst.TOKEN_TYPE:
      case EXISchemaConst.NORMALIZED_STRING_TYPE:
        break;
      case EXISchemaConst.NAME_TYPE:
        validateName(norm, stype);
        break;
      case EXISchemaConst.ID_TYPE:
      case EXISchemaConst.IDREF_TYPE:
      case EXISchemaConst.ENTITY_TYPE:
      case EXISchemaConst.NCNAME_TYPE:
        validateNCName(norm, stype);
        break;
      case EXISchemaConst.NMTOKEN_TYPE:
        validateNMTOKEN(norm, stype);
        break;
      default:
        break;
    }
    return norm;
  }

  private static void validateNormalizedString(String norm)
      throws SchemaValidatorException {
    /**
     * No checking necessary because there is supposed to be no TAB, LF or
     * CR chars after normalization with whiteSpace facet value 'replace'.
     *
    int i, len;
    for (i = 0, len = norm.length(); i < len; i++) {
      switch (norm.charAt(i)) {
        case '\011': // tab
        case '\012': // lf
        case '\015': // cr
          throw new SchemaValidatorException(
              SchemaValidatorException.INVALID_NORMALIZED_STRING,
              new String[] {norm});
        default:
          break;
      }
    }
    */
  }

  private static void validateToken(String norm)
    throws SchemaValidatorException {
    validateNormalizedString(norm);
    /**
     * No checking necessary because there is supposed to be no leading/trailing
     * spaces nor sequences of spaces after normalization with whiteSpace
     * facet value 'collapse'.
     *
    int i, len;
    if ((len = text.length()) > 0) {
      // check leading or trailing spaces
      if (text.charAt(0) == '\040' || text.charAt(len - 1) == '\040')
        throw new SchemaValidatorException(
            SchemaValidatorException.INVALID_TOKEN, new String[] {text});
      // check internal sequences of two or more spaces.
      boolean was_space, is_space;
      for (i = 0, was_space = false, is_space = false;
           i < len; i++, was_space = is_space) {
        is_space = ' ' == text.charAt(i);
        if (was_space && is_space)
          break;
      }
      if (i < len)
        throw new SchemaValidatorException(
            SchemaValidatorException.INVALID_TOKEN, new String[] {text});
    }
    */
  }

  private void validateLanguage(String norm, int stype)
      throws SchemaValidatorException {
    try {
      validateToken(norm);

      StringBuffer tag = new StringBuffer();
      boolean validTag = true;
      int pos, len, n_tags;
      for (pos = 0, len = norm.length(), n_tags = 0;
           pos <= len; pos++) {
        char c = (char) 0;
        if (pos == len || (c = norm.charAt(pos)) == '-') {
          ++n_tags;
          int n_chars = tag.length();
          if (validTag) {
            if (n_chars < 1 || n_chars > 8)
              validTag = false;
            else {
              if (n_tags == 1) { // primary tag
                /**
                 * 2 letters are ISO 639, 3 letters are ISO 639 part2.
                 * Single letter tag has to be either "i" or "x".
                 * Tags of more than 3 letters are not permitted.
                 */
                if (n_chars == 1) {
                  char t = tag.charAt(0);
                  if (t != 'i' && t != 'x')
                    validTag = false;
                }
                else if (n_chars > 3)
                  validTag = false;
              }
              else if (n_tags == 2) { // secondary tag
                /**
                 * 2 letters are ISO 3166, 3-8 letters are IANA.
                 * Single letter tag is not permitted.
                 */
                if (n_chars == 1)
                  validTag = false;
              }
            }
          }
          try {
            if (!validTag) {
              int code = n_tags == 1 ?
                  SchemaValidatorException.INVALID_LANGUAGE_PRIMARY_TAG :
                  SchemaValidatorException.INVALID_LANGUAGE_SUBSEQUENT_TAG;
              throw new SchemaValidatorException(
                  code, new String[] {tag.toString()}, m_schema, stype);
            }
          }
          finally {
            tag.delete(0, tag.length());
            validTag = true;
          }
        }
        else {
          boolean isAlpha = 'A' <= c && c <= 'Z' || 'a' <= c && c <= 'z';
          boolean isDigit = isAlpha ? false : '0' <= c && c <= '9';
          if (n_tags > 0) { // subsequent tag
            if (!isAlpha && !isDigit)
              validTag = false;
          }
          else { // primary tag
            if (!isAlpha)
              validTag = false;
          }
          tag.append(c);
        }
      }
    }
    catch (SchemaValidatorException cause) {
      SchemaValidatorException sve = new SchemaValidatorException(
          SchemaValidatorException.INVALID_LANGUAGE,
          new String[] { norm }, m_schema, stype);
      sve.setException(cause);
      throw sve;
    }
  }

  private void validateName(String norm, int stype)
    throws SchemaValidatorException {

    int i, len;
    // Name length is at least 1. Check the first char.
    if ((len = norm.length()) == 0 || !isNameHeadChar(norm.charAt(0)))
      throw new SchemaValidatorException(
          SchemaValidatorException.INVALID_NAME, new String[] {norm},
          m_schema, stype);

    // Check the rest
    for (i = 1; i < len; i++) {
      if (!isNameChar(norm.charAt(i)))
      throw new SchemaValidatorException(
          SchemaValidatorException.INVALID_NAME, new String[] {norm},
          m_schema, stype);
    }
  }

  private void validateNCName(String norm, int stype)
    throws SchemaValidatorException {
    validateName(norm, stype);
    int i, len;
    for (i = 0, len = norm.length(); i < len; i++) {
      if (':' == norm.charAt(i))
        throw new SchemaValidatorException(
            SchemaValidatorException.INVALID_NCNAME, new String[] {norm},
            m_schema, stype);
    }
  }

  private void validateNMTOKEN(String norm, int stype)
    throws SchemaValidatorException {

    int i, len;
    // Name length is at least 1.
    if ((len = norm.length()) == 0)
      throw new SchemaValidatorException(
          SchemaValidatorException.INVALID_NMTOKEN, new String[] {norm},
          m_schema, stype);

    for (i = 0; i < len; i++) {
      if (!isNameChar(norm.charAt(i)))
        throw new SchemaValidatorException(
          SchemaValidatorException.INVALID_NMTOKEN, new String[] {norm},
          m_schema, stype);
    }
  }

  public QName validateQName(String norm, int stype, PrefixUriBindings npm)
      throws SchemaValidatorException {
    int ind;
    try {
      String namespaceName = null;
      if ((ind = norm.indexOf(':')) > 0 && ind < norm.length() - 1) {
        String pfx = norm.substring(0, ind); 
        validateNCName(pfx, stype);
        validateNCName(norm.substring(ind + 1), stype);
        if ((namespaceName = npm.getUri(pfx)) == null) {
          throw new SchemaValidatorException(
              SchemaValidatorException.INVALID_QNAME, new String[] { norm },
              m_schema, stype);
        }
      }
      else {
        validateNCName(norm, stype);
        namespaceName = npm.getDefaultUri();
      }
      return new QName(norm, namespaceName);
    }
    catch (SchemaValidatorException sve) {
      throw new SchemaValidatorException(
          SchemaValidatorException.INVALID_QNAME, new String[] {norm},
          m_schema, stype);
    }
  }

  private Boolean parseBoolean(String norm, int stype)
    throws SchemaValidatorException {
    return Boolean.valueOf(_parseBoolean(norm, stype, m_schema));
  }

  static boolean _parseBoolean(String norm, int stype, EXISchema corpus)
    throws SchemaValidatorException {
    char c;

    switch (norm.length()) {
      case 1:
        if ((c = norm.charAt(0)) == '1')
          return true;
        else if (c == '0')
          return false;
        else
          throw new SchemaValidatorException(
              SchemaValidatorException.INVALID_BOOLEAN,
              new String[] { norm }, corpus, stype);
      case 4:
        if ((c = norm.charAt(0)) == 't' && "true".equals(norm))
          return true;
          throw new SchemaValidatorException(
              SchemaValidatorException.INVALID_BOOLEAN,
              new String[] { norm }, corpus, stype);
      case 5:
        if ((c = norm.charAt(0)) == 'f' && "false".equals(norm))
          return false;
          throw new SchemaValidatorException(
              SchemaValidatorException.INVALID_BOOLEAN,
              new String[] { norm }, corpus, stype);
      default:
        throw new SchemaValidatorException(
            SchemaValidatorException.INVALID_BOOLEAN,
            new String[] { norm }, corpus, stype);
    }
  }

  /**
   * Parses a string into a float value.
   * @param text a string of which whitespaces have been collapsed
   * @return a float value
   * @throws SchemaValidatorException thrown if the string could not parse as a float value
   */
  public static float parseFloat(String text) throws SchemaValidatorException {
    final int len;
    if ((len = text.length()) > 0) {
      switch (text.charAt(len - 1)) {
        case 'F':
          if (len == 3 && "INF".equals(text))
            return Float.POSITIVE_INFINITY;
          else if (len == 4 && "-INF".equals(text))
            return Float.NEGATIVE_INFINITY;
          break;
        case 'N':
          if (len == 3 && "NaN".equals(text))
            return Float.NaN;
          break;
        default:
          try {
            return Float.parseFloat(text);
          }
          catch (NumberFormatException nfe) {
            break;
          }
      }
    }
    throw new SchemaValidatorException(
        SchemaValidatorException.INVALID_FLOAT, new String[] { text }, (EXISchema)null, EXISchema.NIL_NODE);
  }

  /**
   * Parses a string into a double value.
   * @param text a string of which whitespaces have been collapsed
   * @return a double value
   * @throws SchemaValidatorException thrown if the string could not parse as a double value
   */
  public static double parseDouble(String text) throws SchemaValidatorException {
    final int len;
    if ((len = text.length()) > 0) {
      switch (text.charAt(len - 1)) {
        case 'F':
          if (len == 3 && "INF".equals(text))
            return Double.POSITIVE_INFINITY;
          else if (len == 4 && "-INF".equals(text))
            return Double.NEGATIVE_INFINITY;
          break;
        case 'N':
          if (len == 3 && "NaN".equals(text))
            return Double.NaN;
          break;
        default:
          try {
            return Double.parseDouble(text);
          }
          catch (NumberFormatException nfe) {
            break;
          }
      }
    }
    throw new SchemaValidatorException(
        SchemaValidatorException.INVALID_DOUBLE, new String[] { text }, (EXISchema)null, EXISchema.NIL_NODE);
  }

  private Object parseFloatOrDouble(String norm, boolean isFloat, int stype)
    throws SchemaValidatorException {
    try {
      if (isFloat)
        return new Float(parseFloat(norm));
      else
        return new Double(parseDouble(norm));
    }
    catch (SchemaValidatorException sve) {
      sve.setEXISchema(m_schema);
      sve.setSchemaNode(stype);
      throw sve;
    }
  }

  private void validateAtomic(String norm, Object validatableObject, int stype, int primType)
    throws SchemaValidatorException {

    if (primType == EXISchemaConst.NOTATION_TYPE && m_schema.getSerialOfType(stype) == primType) {
      // NOTATION cannot be used directly.
      throw new SchemaValidatorException(
          SchemaValidatorException.DIRECT_USE_OF_NOTATION,
          new String[] { norm }, m_schema, stype);
    }
    int length, minLength, maxLength, totalDigits, fractionDigits,
        minInclusiveFacet, maxInclusiveFacet, minExclusiveFacet,
        maxExclusiveFacet, n_enums;
    if ((length = m_schema.getLengthFacetValueOfSimpleType(stype)) >= 0) {
      validateAtomicLength(norm, validatableObject, length, stype, primType, LENGTH);
    }
    if ((minLength = m_schema.getMinLengthFacetValueOfSimpleType(stype)) >= 0) {
      validateAtomicLength(norm, validatableObject, minLength, stype, primType, MIN_LENGTH);
    }
    if ((maxLength = m_schema.getMaxLengthFacetValueOfSimpleType(stype)) >= 0) {
      validateAtomicLength(norm, validatableObject, maxLength, stype, primType, MAX_LENGTH);
    }
    if ((totalDigits = m_schema.getTotalDigitsFacetValueOfSimpleType(stype)) >= 0) {
      XSDecimal dec = (XSDecimal)validatableObject;
      int n = dec.getFractionDigits();
      BigInteger valueRange = BigInteger.valueOf(10).pow(totalDigits);
      BigInteger bint = dec.getIntegralValue();
      if (n > totalDigits || bint.compareTo(valueRange) >= 0)
        throw new SchemaValidatorException(
            SchemaValidatorException.TOTAL_DIGITS_INVALID,
            new String[] { norm, Integer.toString(totalDigits)},
            m_schema, stype);
    }
    if ((fractionDigits = m_schema.getFractionDigitsFacetValueOfSimpleType(stype)) >= 0) {
      XSDecimal dec = (XSDecimal)validatableObject;
      int actual;
      if ((actual = dec.getFractionDigits()) > fractionDigits)
        throw new SchemaValidatorException(
            SchemaValidatorException.FRACTION_DIGITS_INVALID,
            new String[] { Integer.toString(actual), norm, Integer.toString(fractionDigits)},
            m_schema, stype);
    }
    if ((minInclusiveFacet = m_schema.getMinInclusiveFacetOfSimpleType(stype)) != EXISchema.NIL_VALUE) {
      validateValueRange(validatableObject, minInclusiveFacet, stype, primType, MIN_INCLUSIVE);
    }
    if ((maxInclusiveFacet = m_schema.getMaxInclusiveFacetOfSimpleType(stype)) != EXISchema.NIL_VALUE) {
      validateValueRange(validatableObject, maxInclusiveFacet, stype, primType, MAX_INCLUSIVE);
    }
    if ((minExclusiveFacet = m_schema.getMinExclusiveFacetOfSimpleType(stype)) != EXISchema.NIL_VALUE) {
      validateValueRange(validatableObject, minExclusiveFacet, stype, primType, MIN_EXCLUSIVE);
    }
    if ((maxExclusiveFacet = m_schema.getMaxExclusiveFacetOfSimpleType(stype)) != EXISchema.NIL_VALUE) {
      validateValueRange(validatableObject, maxExclusiveFacet, stype, primType, MAX_EXCLUSIVE);
    }
    /**
     * REVISIT: consider pattern-based validation
     * if ((n_patterns = m_schema.getRestrictedCharacterCountOfSimpleType(stype)) > 0) {
     *   validateValuePatterns(norm, stype, n_patterns);
     * }
     */
    if ((n_enums = m_schema.getEnumerationFacetCountOfSimpleType(stype)) > 0) {
      validateValueEnumeration(norm, validatableObject, stype, primType, n_enums);
    }
  }

  private boolean validateFixedValue(String norm, Object data, int variant, int primType) {
    if (variant != EXISchema.NIL_VALUE) {
      switch (primType) {
        case EXISchemaConst.ANY_SIMPLE_TYPE:
        case EXISchemaConst.ANYURI_TYPE:
        case EXISchemaConst.STRING_TYPE:
          try {
            String str = m_schema.getStringValueOfVariant(variant);
            if (norm.equals(str))
              return true;
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchemaConst.NOTATION_TYPE:
        case EXISchemaConst.QNAME_TYPE:
          try {
            final int qname = m_schema.getQNameValueOfVariant(variant);
            final String nsuri = m_schema.getNamespaceNameOfQName(qname);
            final String name = m_schema.getNameOfQName(qname);
            // NOTE: null namespace name indicates missing namespace binding
            //       and will not be taken into account for a match.
            if (name.equals(((QName)data).localName) &&
                nsuri != null && nsuri.equals(((QName)data).namespaceName)) {
              return true;
            }
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchemaConst.DECIMAL_TYPE:
          try {
            BigDecimal decimal = m_schema.getDecimalValueOfVariant(variant);
            if ((((XSDecimal)data).getValue()).compareTo(decimal) == 0)
              return true;
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchemaConst.FLOAT_TYPE:
          try {
            Float flt = new Float(m_schema.getFloatValueOfVariant(variant));
            if (((Float)data).compareTo(flt) == 0)
              return true;
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchemaConst.DOUBLE_TYPE:
          try {
            Double dbl = new Double(m_schema.getDoubleValueOfVariant(variant));
            if (((Double)data).compareTo(dbl) == 0)
              return true;
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchemaConst.G_YEAR_TYPE:
        case EXISchemaConst.G_YEARMONTH_TYPE:
        case EXISchemaConst.G_MONTHDAY_TYPE:
        case EXISchemaConst.G_MONTH_TYPE:
        case EXISchemaConst.G_DAY_TYPE:
        case EXISchemaConst.TIME_TYPE:
        case EXISchemaConst.DATE_TYPE:
        case EXISchemaConst.DATETIME_TYPE:
          try {
            XSDateTime dateTime = m_schema.getDateTimeValueOfVariant(variant);
            if (((XSDateTime)data).equals(dateTime))
              return true;
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchemaConst.DURATION_TYPE:
          try {
            Duration duration = m_schema.getDurationValueOfVariant(variant);
            if (((Duration)data).equals(duration))
              return true;
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchemaConst.HEXBINARY_TYPE:
        case EXISchemaConst.BASE64BINARY_TYPE:
          try {
            byte[] binary = m_schema.getBinaryValueOfVariant(variant);
            byte[] ibin = (byte[]) data; // instance binary data
            if (ibin.length == binary.length) {
              int j;
              for (j = 0; j < ibin.length && ibin[j] == binary[j]; j++);
              if (j == ibin.length)
              return true;
            }
            StringBuffer encodingResult = new StringBuffer();
            switch (primType) {
              case EXISchemaConst.BASE64BINARY_TYPE:
                Base64.encode(binary, encodingResult);
                break;
              case EXISchemaConst.HEXBINARY_TYPE:
                HexBin.encode(binary, encodingResult);
                break;
              default:
                assert false;
                break;
            }
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        case EXISchemaConst.BOOLEAN_TYPE:
          try {
            boolean bval = m_schema.getBooleanValueOfVariant(variant);
            if (((Boolean)data).booleanValue() == bval)
              return true;
          }
          catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
          break;
        default:
          return true;
      }
    }
    return false;
  }
  
  private void validateValueEnumeration(String norm, Object data,
                                        int stype, int primType, int n_enums)
      throws SchemaValidatorException {
    StringBuffer allValues = new StringBuffer();
    int i, j, facet;
    for (i = 0; i < n_enums; i++) {
      if ((facet = m_schema.getEnumerationFacetOfSimpleType(stype, i)) !=
          EXISchema.NIL_VALUE) {
        Object enumValue = null;
        switch (primType) {
          case EXISchemaConst.ANY_SIMPLE_TYPE:
          case EXISchemaConst.ANYURI_TYPE:
          case EXISchemaConst.STRING_TYPE:
            try {
              String str = m_schema.getStringValueOfVariant(facet);
              if (norm.equals(str))
                return;
              enumValue = str;
            }
            catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
            break;
          case EXISchemaConst.NOTATION_TYPE:
          case EXISchemaConst.QNAME_TYPE:
            try {
              final int qname = m_schema.getQNameValueOfVariant(facet);
              final String valueUri = m_schema.getNamespaceNameOfQName(qname);
              final String valueName = m_schema.getNameOfQName(qname);
              if (valueName.equals(((QName)data).localName)) {
                final String nsuri = ((QName)data).namespaceName;
                if (nsuri == valueUri || nsuri != null && nsuri.equals(valueUri))
                return;
              }
              enumValue = valueUri != null ? "{" + valueUri + "}" + valueName : valueName;
            }
            catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
            break;
          case EXISchemaConst.DECIMAL_TYPE:
            try {
              BigDecimal decimal = m_schema.getDecimalValueOfVariant(facet);
              if ((((XSDecimal)data).getValue()).compareTo(decimal) == 0)
                return;
              enumValue = decimal;
            }
            catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
            break;
          case EXISchemaConst.FLOAT_TYPE:
            try {
              Float flt = new Float(m_schema.getFloatValueOfVariant(facet));
              if (((Float)data).compareTo(flt) == 0)
                return;
              enumValue = flt;
            }
            catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
            break;
          case EXISchemaConst.DOUBLE_TYPE:
            try {
              Double dbl = new Double(m_schema.getDoubleValueOfVariant(facet));
              if (((Double)data).compareTo(dbl) == 0)
                return;
              enumValue = dbl;
            }
            catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
            break;
          case EXISchemaConst.G_YEAR_TYPE:
          case EXISchemaConst.G_YEARMONTH_TYPE:
          case EXISchemaConst.G_MONTHDAY_TYPE:
          case EXISchemaConst.G_MONTH_TYPE:
          case EXISchemaConst.G_DAY_TYPE:
          case EXISchemaConst.TIME_TYPE:
          case EXISchemaConst.DATE_TYPE:
          case EXISchemaConst.DATETIME_TYPE:
            try {
              XSDateTime dateTime = m_schema.getDateTimeValueOfVariant(facet);
              if (((XSDateTime)data).equals(dateTime))
                return;
              enumValue = dateTime;
            }
            catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
            break;
          case EXISchemaConst.DURATION_TYPE:
            try {
              Duration duration = m_schema.getDurationValueOfVariant(facet);
              if (((Duration)data).equals(duration))
                return;
              enumValue = duration;
            }
            catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
            break;
          case EXISchemaConst.HEXBINARY_TYPE:
          case EXISchemaConst.BASE64BINARY_TYPE:
            try {
              byte[] binary = m_schema.getBinaryValueOfVariant(facet);
              byte[] ibin = (byte[]) data; // instance binary data
              if (ibin.length == binary.length) {
                for (j = 0; j < ibin.length && ibin[j] == binary[j]; j++)
                  ;
                if (j == ibin.length)
                  return;
              }
              StringBuffer encodingResult = new StringBuffer();
              switch (primType) {
                case EXISchemaConst.BASE64BINARY_TYPE:
                  Base64.encode(binary, encodingResult);
                  break;
                case EXISchemaConst.HEXBINARY_TYPE:
                  HexBin.encode(binary, encodingResult);
                  break;
                default:
                  assert false;
                  break;
              }
              enumValue = encodingResult;
            }
            catch (EXISchemaRuntimeException sce) {} // happens for unioned enum
            break;
          case EXISchemaConst.BOOLEAN_TYPE: // Enum facet does not apply to boolean
          default:
            return;
        }
        if (enumValue != null) {
          if (allValues.length() > 0)
            allValues.append(", ");
          allValues.append("'" + enumValue.toString() + "'");
        }
      }
    }
    throw new SchemaValidatorException(
        SchemaValidatorException.INVALID_ENUMERATION,
        new String[] { norm, allValues.toString() }, m_schema, stype);
  }

  /**
   * @param behaviour one of MIN_INCLUSIVE, MAX_INCLUSIVE, MIN_EXCLUSIVE or MAX_EXCLUSIVE
   * @throws SchemaValidatorException
   */
  private void validateValueRange(Object data, int minmaxFacet,
                                 int stype, int primType, int behaviour)
      throws SchemaValidatorException {

    switch (primType) {
      case EXISchemaConst.DECIMAL_TYPE:
        validateDecimalValueRange(((XSDecimal)data).getValue(),
                                  m_schema.getDecimalValueOfVariant(minmaxFacet),
                                  stype, behaviour);
        break;
      case EXISchemaConst.FLOAT_TYPE:
        validateDoubleValueRange(((Float)data).floatValue(),
                                 m_schema.getFloatValueOfVariant(minmaxFacet),
                                 stype, behaviour);
        break;
      case EXISchemaConst.DOUBLE_TYPE:
        validateDoubleValueRange(((Double)data).doubleValue(),
                                 m_schema.getDoubleValueOfVariant(minmaxFacet),
                                 stype, behaviour);
        break;
      case EXISchemaConst.G_DAY_TYPE:
      case EXISchemaConst.G_MONTH_TYPE:
      case EXISchemaConst.G_YEAR_TYPE:
      case EXISchemaConst.G_MONTHDAY_TYPE:
      case EXISchemaConst.G_YEARMONTH_TYPE:
      case EXISchemaConst.TIME_TYPE:
      case EXISchemaConst.DATE_TYPE:
      case EXISchemaConst.DATETIME_TYPE:
        /**
         * validateDateTimeValueRange((XSDateTime)data,
         *                            m_schema.getDateTimeValueOfVariant(minmaxFacet),
         *                            stype, behaviour);
         */
        break;
      case EXISchemaConst.DURATION_TYPE:
        /**
         * validateDurationValueRange((XSDuration)data,
         *                            m_schema.getDurationValueOfVariant(minmaxFacet),
         *                            stype, behaviour);
         */
        break;
    }
  }

  /**
   * Validates to see if a value is either larger or smaller than the
   * specified boundary value.
   * @param behaviour one of MIN_INCLUSIVE, MAX_INCLUSIVE, MIN_EXCLUSIVE or MAX_EXCLUSIVE
   * @throws SchemaValidatorException Thrown if the condition was violated.
   */
  public void validateValueRange(Object data, Object minmaxValue,
                                 int stype, int behaviour)
      throws SchemaValidatorException {

    if (m_schema.getNodeType(stype) != EXISchema.SIMPLE_TYPE_NODE)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });

    int primType;
    for (primType = stype; !m_schema.isPrimitiveSimpleType(primType);
         primType = m_schema.getPrimitiveTypeOfAtomicSimpleType(primType));
    int primTypeId = m_schema.getSerialOfType(primType);

    switch (primTypeId) {
      case EXISchemaConst.DECIMAL_TYPE:
        validateDecimalValueRange(((XSDecimal)data).getValue(),
                                  ((XSDecimal)minmaxValue).getValue(),
                                  stype, behaviour);
        break;
      case EXISchemaConst.FLOAT_TYPE:
        validateDoubleValueRange(((Float)data).floatValue(),
                                 ((Float)minmaxValue).floatValue(),
                                 stype, behaviour);
        break;
      case EXISchemaConst.DOUBLE_TYPE:
        validateDoubleValueRange(((Double)data).doubleValue(),
                                 ((Double)minmaxValue).doubleValue(),
                                 stype, behaviour);
        break;
      case EXISchemaConst.G_DAY_TYPE:
      case EXISchemaConst.G_MONTH_TYPE:
      case EXISchemaConst.G_YEAR_TYPE:
      case EXISchemaConst.G_MONTHDAY_TYPE:
      case EXISchemaConst.G_YEARMONTH_TYPE:
      case EXISchemaConst.TIME_TYPE:
      case EXISchemaConst.DATE_TYPE:
      case EXISchemaConst.DATETIME_TYPE:
        // TODO: implement.
        break;
      case EXISchemaConst.DURATION_TYPE:
        // TODO: implement.
        break;
    }
  }
  
  private void validateDoubleValueRange(double actual, double expected,
                                        int stype, int behaviour)
      throws SchemaValidatorException {
    switch (behaviour) {
      case MIN_INCLUSIVE:
        if (actual < expected)
          throw new SchemaValidatorException(
              SchemaValidatorException.MIN_INCLUSIVE_INVALID,
              new String[] { String.valueOf(actual), String.valueOf(expected) },
              m_schema, stype);
        break;
      case MAX_INCLUSIVE:
        if (expected < actual)
          throw new SchemaValidatorException(
              SchemaValidatorException.MAX_INCLUSIVE_INVALID,
              new String[] { String.valueOf(actual), String.valueOf(expected) },
              m_schema, stype);
        break;
      case MIN_EXCLUSIVE:
        if (actual <= expected)
          throw new SchemaValidatorException(
              SchemaValidatorException.MIN_EXCLUSIVE_INVALID,
              new String[] { String.valueOf(actual), String.valueOf(expected) },
              m_schema, stype);
        break;
      case MAX_EXCLUSIVE:
        if (expected <= actual)
          throw new SchemaValidatorException(
              SchemaValidatorException.MAX_EXCLUSIVE_INVALID,
              new String[] { String.valueOf(actual), String.valueOf(expected) },
              m_schema, stype);
        break;
      default:
        throw new UnsupportedOperationException(
            "Unsupported behaviour '" + behaviour + "' specified.");
    }
  }

  private void validateDecimalValueRange(BigDecimal actual,
                                         BigDecimal expected,
                                         int stype, int behaviour)
      throws SchemaValidatorException {

    switch (behaviour) {
      case MIN_INCLUSIVE:
        if (actual.compareTo(expected) < 0)
          throw new SchemaValidatorException(
              SchemaValidatorException.MIN_INCLUSIVE_INVALID,
              new String[] { actual.toString(), expected.toString() },
              m_schema, stype);
        break;
      case MAX_INCLUSIVE:
        if (expected.compareTo(actual) < 0)
          throw new SchemaValidatorException(
              SchemaValidatorException.MAX_INCLUSIVE_INVALID,
              new String[] { actual.toString(), expected.toString() },
              m_schema, stype);
        break;
      case MIN_EXCLUSIVE:
        if (actual.compareTo(expected) <= 0)
          throw new SchemaValidatorException(
              SchemaValidatorException.MIN_EXCLUSIVE_INVALID,
              new String[] { actual.toString(), expected.toString() },
              m_schema, stype);
        break;
      case MAX_EXCLUSIVE:
        if (expected.compareTo(actual) <= 0)
          throw new SchemaValidatorException(
              SchemaValidatorException.MAX_EXCLUSIVE_INVALID,
              new String[] { actual.toString(), expected.toString() },
              m_schema, stype);
        break;
      default:
        break;
    }
  }

  private void validateAtomicLength(String norm, Object data, int expected,
                                    int stype, int primTypeId, int behaviour)
      throws SchemaValidatorException {
    int actual = expected;
    switch (primTypeId) {
      case EXISchemaConst.STRING_TYPE:
      case EXISchemaConst.ANYURI_TYPE:
      case EXISchemaConst.QNAME_TYPE:
      case EXISchemaConst.NOTATION_TYPE:
        int i;
        final int len;
        for (i = 0, actual = 0, len = norm.length(); i < len; actual++) {
          final char ith = norm.charAt(i);
          if (0xD7FF < ith && ith < 0xDC00) { // high surrogate
            if (++i < len) {
              final char low = norm.charAt(i);
              if (0xDBFF < low && low < 0xE000) { // low surrogate
                ++i;
                continue;
              }
            }
            // invalid surrogate pair
            throw new SchemaValidatorException(
                SchemaValidatorException.INVALID_SURROGATE_PAIR,
                new String[] { norm }, m_schema, stype);
          }
          ++i;
        }
        break;
      case EXISchemaConst.HEXBINARY_TYPE:
      case EXISchemaConst.BASE64BINARY_TYPE:
        actual = ((byte[])data).length;
        break;
      default:
        break;
    }
    switch (behaviour) {
      case LENGTH:
        if (actual != expected)
          throw new SchemaValidatorException(
              SchemaValidatorException.LENGTH_INVALID,
              new String[] { Integer.toString(actual), Integer.toString(expected)},
              m_schema, stype);
        break;
      case MIN_LENGTH:
        if (actual < expected)
          throw new SchemaValidatorException(
              SchemaValidatorException.MIN_LENGTH_INVALID,
              new String[] { Integer.toString(actual), Integer.toString(expected)},
              m_schema, stype);
        break;
      case MAX_LENGTH:
        if (actual > expected)
          throw new SchemaValidatorException(
              SchemaValidatorException.MAX_LENGTH_INVALID,
              new String[] { Integer.toString(actual), Integer.toString(expected)},
              m_schema, stype);
        break;
      default:
        break;
    }
  }

  static final String normalize(final String text, final int whiteSpace) {
    
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
  
  /**
   * Encode octets into a base64-encoded ascii equivalent.
   * @param octets binary data
   * @return ascii text
   */
  public static String encodeBinaryByBase64(byte[] octets) {
    StringBuffer encodingResult = new StringBuffer();
    Base64.encode(octets, encodingResult);
    return encodingResult.toString();
  }

  /**
   * Encode octets into a hex-encoded ascii equivalent.
   * @param octets binary data
   * @return ascii text
   */
  public static String encodeBinaryByHexBin(byte[] octets) {
    StringBuffer encodingResult = new StringBuffer();
    HexBin.encode(octets, encodingResult);
    return encodingResult.toString();
  }
  
  public static class HexBin {
    public static byte[] decode(String norm)
        throws SchemaValidatorException {
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
                  throw new SchemaValidatorException(
                      SchemaValidatorException.INVALID_HEX_BINARY,
                      new String[] { norm }, null, EXISchema.NIL_NODE);
                }
                else // between 'a' and 'f'
                  dec |= (10 + (c - 'a')) << (4 * (1 - nc++));
              }
              else if (c < '\u003a') { // c <= '9'
                if (c < '\u0030') { // c < '0'
                  throw new SchemaValidatorException(
                      SchemaValidatorException.INVALID_HEX_BINARY,
                      new String[] { norm }, null, EXISchema.NIL_NODE);
                }
                else // between '0' and '9'
                  dec |= (c - '0') << (4 * (1 - nc++));
              }
              else if ('\u0040' < c && c < '\u0047') { // between 'A' and 'F'
                dec |= (10 + (c - 'A')) << (4 * (1 - nc++));
              }
              else { // off the range.
                throw new SchemaValidatorException(
                    SchemaValidatorException.INVALID_HEX_BINARY,
                    new String[] { norm }, null, EXISchema.NIL_NODE);
              }
            }
            if (nc < 2) {
              throw new SchemaValidatorException(
                  SchemaValidatorException.INVALID_HEX_BINARY,
                  new String[] { norm }, null, EXISchema.NIL_NODE);
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

    public static void encode(byte[] octets, StringBuffer encodingResult) {
      if (octets != null && encodingResult != null) {
        int i, len;
        for (i = 0, len = octets.length; i < len; i++) {
          int dec0, dec1;
          if ((dec0 = (octets[i] >> 4)) < 0)
            dec0 &= 0x000F;
          dec1 = octets[i] & 0x000F;
          encodingResult.append(HEXBIN_ASCIIS.charAt(dec0));
          encodingResult.append(HEXBIN_ASCIIS.charAt(dec1));
        }
      }
    }
  }

  public static class Base64 {

    static final private byte[] m_octets; // Base64 ASCII -> byte (6 bits)

    static {
      m_octets = new byte[256];

      for (int i = 'Z'; i >= 'A'; i--)
        m_octets[i] = (byte)(i - 'A');
      for (int i = 'z'; i >= 'a'; i--)
        m_octets[i] = (byte)(i - 'a' + 26);
      for (int i = '9'; i >= '0'; i--)
        m_octets[i] = (byte)(i - '0' + 52);

      m_octets['+']  = 62;
      m_octets['/']  = 63;
    }

    /**
     * NOTE: This method has to be in sync with the other decode method.
     */
    static byte[] decode(char[] ch, int start, int len, byte[] result, int[] returnedInt)
      throws SchemaValidatorException {
      int n_result = 0;
      if (len > 0) {
        final char[] enc = new char[4];
        int pos;
        for (pos = 0; pos < len;) {
          int nc;
          for (nc = 0; nc < 4 && pos < len; pos++) {
            final char c = ch[start + pos]; 
            if (isBase64Char(c))
              enc[nc++] = c;
            else if (!Character.isWhitespace(c)) {
              throw new SchemaValidatorException(
                  SchemaValidatorException.INVALID_BASE64_BINARY,
                  new String[] { new String(ch, start, len) }, null, EXISchema.NIL_NODE);
            }
          }
          if (nc == 4) {
            if (enc[0] == '=' || enc[1] == '=') { // invalid
              throw new SchemaValidatorException(
                  SchemaValidatorException.INVALID_BASE64_BINARY,
                  new String[] { new String(ch, start, len) }, null, EXISchema.NIL_NODE);
            }
            final byte b0, b1, b2, b3;
            b0 = m_octets[enc[0]];
            b1 = m_octets[enc[1]];
            if (n_result >= result.length) {
              byte[] _result = new byte[2 * result.length];
              System.arraycopy(result, 0, _result, 0, n_result);
              result = _result;
            }
            result[n_result++] = (byte)(b0 << 2 | b1 >> 4);
            if (enc[2] == '=') { // it is the end
              if (enc[3] != '=') {
                throw new SchemaValidatorException(
                    SchemaValidatorException.INVALID_BASE64_BINARY,
                    new String[] { new String(ch, start, len) }, null, EXISchema.NIL_NODE);
              }
              break;
            }
            b2 = m_octets[enc[2]];
            if (n_result >= result.length) {
              byte[] _result = new byte[2 * result.length];
              System.arraycopy(result, 0, _result, 0, n_result);
              result = _result;
            }
            result[n_result++] = (byte)(((b1 & 0x0F) << 4) | ((b2 >> 2) & 0x0F));
            if (enc[3] == '=') // it is the end
              break;
            b3 = m_octets[enc[3]];
            if (n_result >= result.length) {
              byte[] _result = new byte[2 * result.length];
              System.arraycopy(result, 0, _result, 0, n_result);
              result = _result;
            }
            result[n_result++] = (byte)(b2 << 6 | b3);
          }
          else if (nc > 0) { // not multiple of four
            throw new SchemaValidatorException(
                SchemaValidatorException.INVALID_BASE64_BINARY,
                new String[] { new String(ch, start, len) }, null, EXISchema.NIL_NODE);
          }
        }
        for (; pos < len; pos++) { // Check if there are any extra chars
          if (!Character.isWhitespace(ch[start + pos])) { 
            throw new SchemaValidatorException(
                SchemaValidatorException.INVALID_BASE64_BINARY,
                new String[] { new String(ch, start, len) }, null, EXISchema.NIL_NODE);
          }
        }
      }
      returnedInt[0] = n_result;
      return result;
    }
    
    /**
     * NOTE: This method has to be in sync with the other decode method.
     */
    public static byte[] decode(String norm)
        throws SchemaValidatorException {
      byte[] octets = null;
      if (norm != null) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
          int len;
          if ((len = norm.length()) > 0) {
            final char[] enc = new char[4];
            int pos;
            for (pos = 0; pos < len;) {
              int nc;
              for (nc = 0; nc < 4 && pos < len; pos++) {
                final char c = norm.charAt(pos);
                if (isBase64Char(c))
                  enc[nc++] = c;
                else if (!Character.isWhitespace(c)) {
                  throw new SchemaValidatorException(
                      SchemaValidatorException.INVALID_BASE64_BINARY,
                      new String[] { norm }, null, EXISchema.NIL_NODE);
                }
              }
              if (nc == 4) {
                if (enc[0] == '=' || enc[1] == '=') { // invalid
                  throw new SchemaValidatorException(
                      SchemaValidatorException.INVALID_BASE64_BINARY,
                      new String[] { norm }, null, EXISchema.NIL_NODE);
                }
                final byte b0, b1, b2, b3;
                b0 = m_octets[enc[0]];
                b1 = m_octets[enc[1]];
                baos.write(b0 << 2 | b1 >> 4);
                if (enc[2] == '=') { // it is the end
                  if (enc[3] != '=') {
                    throw new SchemaValidatorException(
                        SchemaValidatorException.INVALID_BASE64_BINARY,
                        new String[] { norm }, null, EXISchema.NIL_NODE);
                  }
                  break;
                }
                b2 = m_octets[enc[2]];
                baos.write((byte)(((b1 & 0x0F) << 4) | ((b2 >> 2) & 0x0F)));
                if (enc[3] == '=') // it is the end
                  break;
                b3 = m_octets[enc[3]];
                baos.write((byte)(b2 << 6 | b3));
              }
              else if (nc > 0) { // not multiple of four
                throw new SchemaValidatorException(
                    SchemaValidatorException.INVALID_BASE64_BINARY,
                    new String[] { norm }, null, EXISchema.NIL_NODE);
              }
            }
            for (; pos < len; pos++) { // Check if there are any extra chars
              if (!Character.isWhitespace(norm.charAt(pos))) {
                throw new SchemaValidatorException(
                    SchemaValidatorException.INVALID_BASE64_BINARY,
                    new String[] { norm }, null, EXISchema.NIL_NODE);
              }
            }
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

    public static void encode(byte[] octets, StringBuffer encodingResult) {
      if (octets != null && encodingResult != null) {
        int len;
        if ( (len = octets.length) > 0) {
          int pos, mod;
          for (pos = 0, mod = 0; pos < len; mod++) {
            int n, st;
            for (n = 0, st = pos; n < 3 && pos < len; pos++, n++);
            assert n == 1 || n == 2 || n == 3;
            byte b0, b1;
            byte b2 = 64, b3 = 64;
            if ( (b0 = (byte) (octets[st] >> 2)) < 0)
              b0 = (byte) (b0 ^ 0xC0);
            if (n > 1) {
              if ( (b1 = (byte) (octets[st + 1] >> 4)) < 0)
                b1 = (byte) (b1 ^ 0xF0);
              b1 = (byte) ( (octets[st] & 0x03) << 4 | b1);
              if (n > 2) { // n == 3
                if ( (b2 = (byte) (octets[st + 2] >> 6)) < 0)
                  b2 = (byte) (b2 ^ 0xFC);
                b2 = (byte) ( (octets[st + 1] & 0x0F) << 2 | b2);
                b3 = (byte) (octets[st + 2] & 0x3F);
              }
              else { // n == 2
                b2 = (byte) ( (octets[st + 1] & 0x0F) << 2);
              }
            }
            else { // n == 1
              b1 = (byte) ( (octets[st] & 0x03) << 4);
            }
            encodingResult.append(BASE64_ASCIIS.charAt(b0));
            encodingResult.append(BASE64_ASCIIS.charAt(b1));
            encodingResult.append(BASE64_ASCIIS.charAt(b2));
            encodingResult.append(BASE64_ASCIIS.charAt(b3));
            if (mod % 19 == 18)
              encodingResult.append('\n');
          }
        }
      }
    }
  }

}
