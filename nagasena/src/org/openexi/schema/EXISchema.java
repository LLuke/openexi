package org.openexi.schema;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StreamCorruptedException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

/**
 * EXISchema provides methods to read and write compiled EXI schemas.
 */
public final class EXISchema {

  private static final byte[] COOKIE = { 36, 51, 43, 45 }; // "$", "3", "+", "-"

  /** @y.exclude */
  public static final int NIL_NODE = -1;
  /** @y.exclude */
  public static final int NIL_VALUE = -1;
  /** @y.exclude */
  public static final int EMPTY_STRING = 0;
  // REVISIT: remove
  /** @y.exclude */
  public static final int NIL_GRAM = -1;
  
  // Event id
  /** @y.exclude */
  public static final int EVENT_AT_WILDCARD = -1;
  /** @y.exclude */
  public static final int EVENT_SE_WILDCARD = -2;
  /** @y.exclude */
  public static final int EVENT_CH_UNTYPED  = -3;
  /** @y.exclude */
  public static final int EVENT_CH_TYPED    = -4;
  /** @y.exclude */
  public static final int MIN_EVENT_ID    = EVENT_CH_TYPED;
  
  // EventType
  /** @y.exclude */
  public static final byte EVENT_TYPE_AT       = 0;
  /** @y.exclude */
  public static final byte EVENT_TYPE_SE       = 1;
  /** @y.exclude */
  public static final byte EVENT_TYPE_AT_WILDCARD_NS = 2;
  /** @y.exclude */
  public static final byte EVENT_TYPE_SE_WILDCARD_NS = 3;

  /** @y.exclude */
  public static final int TRUE_VALUE = 1;
  /** @y.exclude */
  public static final int FALSE_VALUE = 0;
  
  /** @y.exclude */
  public static final int UNBOUNDED_OCCURS = -1;

  /** @y.exclude */
  public static final int CONSTRAINT_NONE    = 0;
  /** @y.exclude */
  public static final int CONSTRAINT_DEFAULT = 1;
  /** @y.exclude */
  public static final int CONSTRAINT_FIXED   = 2;

  /** @y.exclude */
  public static final int WHITESPACE_PRESERVE = 0;
  /** @y.exclude */
  public static final int WHITESPACE_REPLACE  = 1;
  /** @y.exclude */
  public static final int WHITESPACE_COLLAPSE = 2;

  /** @y.exclude */
  public static final byte VARIANT_STRING      = 0;
  /** @y.exclude */
  public static final byte VARIANT_FLOAT       = 1;
  /** @y.exclude */
  public static final byte VARIANT_DECIMAL     = 2;
  /** @y.exclude */
  public static final byte VARIANT_INTEGER     = 3;
  /** @y.exclude */
  public static final byte VARIANT_INT         = 4;
  /** @y.exclude */
  public static final byte VARIANT_LONG        = 5;
  /** @y.exclude */
  public static final byte VARIANT_DATETIME    = 6;
  /** @y.exclude */
  public static final byte VARIANT_DURATION    = 7;
  /** @y.exclude */
  public static final byte VARIANT_BASE64      = 8;
  /** @y.exclude */
  public static final byte VARIANT_BOOLEAN     = 9;
  /** @y.exclude */
  public static final byte VARIANT_HEXBIN      = 10;

  /**
   * Default, unconstrained integer representation
   * @y.exclude
   */
  public static final int INTEGER_CODEC_DEFAULT     = 0xFF;
  
  /**
   * Non-negative integer representation
   * @y.exclude
   */
  public static final int INTEGER_CODEC_NONNEGATIVE = 0xFE;
  
  /**
   * Atomic built-in type ID (defined in EXISchemaConst) -> Ancestry ID
   */
  private static final byte[] ANCESTRY_IDS = new byte[] {
      EXISchemaConst.UNTYPED, // ANY_TYPE
      EXISchemaConst.UNTYPED, // ANY_SIMPLE_TYPE
      EXISchemaConst.STRING_TYPE, // STRING_TYPE
      EXISchemaConst.BOOLEAN_TYPE, // BOOLEAN_TYPE
      EXISchemaConst.DECIMAL_TYPE, // DECIMAL_TYPE
      EXISchemaConst.FLOAT_TYPE, // FLOAT_TYPE
      EXISchemaConst.FLOAT_TYPE, // DOUBLE_TYPE
      EXISchemaConst.DURATION_TYPE, // DURATION_TYPE
      EXISchemaConst.DATETIME_TYPE, // DATETIME_TYPE
      EXISchemaConst.TIME_TYPE, // TIME_TYPE
      EXISchemaConst.DATE_TYPE, // DATE_TYPE
      EXISchemaConst.G_YEARMONTH_TYPE, // G_YEARMONTH_TYPE
      EXISchemaConst.G_YEAR_TYPE, // G_YEAR_TYPE
      EXISchemaConst.G_MONTHDAY_TYPE, // G_MONTHDAY_TYPE
      EXISchemaConst.G_DAY_TYPE, // G_DAY_TYPE
      EXISchemaConst.G_MONTH_TYPE, // G_MONTH_TYPE
      EXISchemaConst.HEXBINARY_TYPE, // HEXBINARY_TYPE
      EXISchemaConst.BASE64BINARY_TYPE, // BASE64BINARY_TYPE
      EXISchemaConst.ANYURI_TYPE, // ANYURI_TYPE
      EXISchemaConst.UNTYPED, // QNAME_TYPE
      EXISchemaConst.UNTYPED, // NOTATION_TYPE
      EXISchemaConst.INTEGER_TYPE, // INTEGER_TYPE
  };
  
  private static final String[] ELEMENT_NAMES = new String[] {
    "",
    "",
    "StringType",
    "BooleanType",
    "DecimalType",
    "FloatType",
    "FloatType",
    "DurationType",
    "DateTimeType",
    "TimeType",
    "DateType",
    "GYearMonthType",
    "GYearType",
    "GMonthDayType",
    "GDayType",
    "GMonthType",
    "HexBinaryType",
    "Base64BinaryType",
    "AnyURIType",
    "QNameType",
    "QNameType",
    "IntegerType"
  };
  
  private static final boolean[] DEFAULT_TYPABLES;
  static {
    DEFAULT_TYPABLES = new boolean[] {
        true,  // anyType
        true,  // anySimpleType
        true,  // string
        false, // boolean
        true,  // decimal
        false, // float
        false, // double
        false, // duration
        false, // dateTime
        false, // time
        false, // date
        false, // gYearMonth
        false, // gYear
        false, // gMonthDay
        false, // gDay
        false, // gMonth
        false, // hexBinary
        false, // base64Binary
        false, // anyURI
        false, // QName
        false, // NOTATION
        true,  // integer
        true,  // nonNegativeInteger
        true,  // unsignedLong
        false, // positiveInteger
        true,  // nonPositiveInteger
        false, // negativeInteger
        true,  // int
        true,  // short
        false, // byte
        true,  // unsignedShort
        false, // unsignedByte
        true,  // long
        true,  // unsignedInt
        true,  // normalizedString
        true,  // token
        false, // language
        true,  // Name
        true,  // NCName
        false, // NMTOKEN
        false, // ENTITY
        false, // IDREF
        false, // ID
        false, // ENTITIES
        false, // IDREFS
        false  // NMTOKENS
    };
    assert DEFAULT_TYPABLES.length == EXISchemaConst.N_BUILTIN_TYPES;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Public value enumerations
  ///////////////////////////////////////////////////////////////////////////

  /** @y.exclude */
  public static final byte UR_SIMPLE_TYPE     = 0;
  /** @y.exclude */
  public static final byte ATOMIC_SIMPLE_TYPE = 1;
  /** @y.exclude */
  public static final byte LIST_SIMPLE_TYPE   = 2;
  /** @y.exclude */
  public static final byte UNION_SIMPLE_TYPE  = 3;

  private int[]   m_elems; // int array describing elements
  private int[]   m_attrs; // int array describing attributes
  private int[]   m_types; // int array describing types
  /** @y.exclude */
  public String[]  uris; // array of interned strings representing uris
  /** @y.exclude */
  public String[][] localNames;
  private int[][]  m_localNames;
  String[]         m_names; // array of interned strings
  String[]         m_strings; // array of non-interned strings
  int[]            m_ints; // array of int values
  long[]           m_mantissas; // array of long representing mantissas
  int[]            m_exponents; // array of int representing exponents
  boolean[]        m_signs; // array of decimal value signs
  String[]         m_integralDigits; // array of decimal integral value
  String[]         m_reverseFractionalDigits; // array of decimal reverse-fractional digits value
  BigInteger[]     m_integers; // array of integer values
  long[]           m_longs; // array of long values
  XSDateTime[]     m_datetimes; // array of datetime values
  Duration[]       m_durations; // array of duration values
  byte[][]         m_binaries; // array of binary values
  byte[]           m_variantTypes; // array of variant types
  int[]            m_variants; // array of variant values
  
  private transient XSDateTime[] m_computedDatetimes; // array of datetime values for enumeration matching
  private transient Characters[] m_variantCharacters; // array of variant's Characters
  
  private int  m_n_stypes; // grand total number of simple type nodes (both global and local)
  public transient byte[] ancestryIds; // [0 ... m_n_stypes] where non-atomic entries are filled with EXISchemaConst.UNTYPED.
  private transient int m_stypes_end; // tp is a simple type if tp < m_stypes_end except for xsd:anyType
  
  private int[]  m_grammars;     // int array describing grammar structure
  private int[]  m_productions;  // int array containing productions
  private byte[] m_eventTypes;   // byte array of event types
  private int[]  m_eventData;    // int array of event data (i.e. node or uri)
  
  private int m_grammarCount; // the number of grammars in m_grammars;

  private transient int[] m_fragmentINodes;
  private transient int m_n_fragmentElems;
  
  private transient HashMap<String,int[]> m_globalElementsDirectory;
  private transient HashMap<String,int[]> m_globalAttributesDirectory;
  private transient HashMap<String,int[]> m_globalTypesDirectory;
  private transient int[] m_buitinTypes;
  private transient int[] m_globalElems;
  private transient int[] m_globalAttrs;

  /**
   * @y.exclude
   */
  public static final DatatypeFactory datatypeFactory;
  static {
    DatatypeFactory _datatypeFactory = null;
    try {
      _datatypeFactory = DatatypeFactory.newInstance();
    }
    catch(DatatypeConfigurationException dce) {
      throw new RuntimeException(dce);
    }
    finally {
      datatypeFactory = _datatypeFactory;
    }
  }
  
  private EXISchema() {
  }
  
  /**
   * @y.exclude
   */
  public EXISchema(int[] nodes, int n_nodes,
               int[] attrs, int n_attrs,
               int[] types, int n_types,
               String[] uris, int n_uris, 
               String[] names, int n_names, int[][] localNames, 
               String[] strings, int n_strings, int[] ints, int n_ints,
               long[] mantissas, int[] exponents, int n_floats,
               boolean[] signs, String[] integralDigits, String[] reverseFractionalDigits, int n_decimals, 
               BigInteger[] integers, int n_integers, 
               long[] longs, int n_longs,
               XSDateTime[] datetimes, int n_datetimes,
               Duration[] durations, int n_durations,
               byte[][] binaries, int n_binaries,
               byte[] variantTypes, int[] variants, int n_variants,
               int[] grammars, int n_grammars, int grammarCount, 
               int[] productions, int n_productions,
               byte[] eventTypes, int[] eventData, int n_events,
               int n_stypes) {

    m_elems = new int[n_nodes];
    System.arraycopy(nodes, 0, m_elems, 0, n_nodes);

    m_attrs = new int[n_attrs];
    System.arraycopy(attrs, 0, m_attrs, 0, n_attrs);
    
    m_types = new int[n_types];
    System.arraycopy(types, 0, m_types, 0, n_types);
    
    this.uris = new String[n_uris];
    System.arraycopy(uris, 0, this.uris, 0, n_uris);

    m_localNames = new int[localNames.length][];
    for (int i = 0; i < localNames.length; i++) {
      m_localNames[i] = new int[localNames[i].length];
      System.arraycopy(localNames[i], 0, m_localNames[i], 0, localNames[i].length);
    }
    
    m_names = new String[n_names];
    System.arraycopy(names, 0, m_names, 0, n_names);

    m_strings = new String[n_strings];
    System.arraycopy(strings, 0, m_strings, 0, n_strings);

    m_ints = new int[n_ints];
    System.arraycopy(ints, 0, m_ints, 0, n_ints);

    m_mantissas = new long[n_floats];
    System.arraycopy(mantissas, 0, m_mantissas, 0, n_floats);

    m_exponents = new int[n_floats];
    System.arraycopy(exponents, 0, m_exponents, 0, n_floats);
    
    m_signs = new boolean[n_decimals];
    System.arraycopy(signs, 0, m_signs, 0, n_decimals);
    
    m_integralDigits = new String[n_decimals];
    System.arraycopy(integralDigits, 0, m_integralDigits, 0, n_decimals);
    
    m_reverseFractionalDigits = new String[n_decimals];
    System.arraycopy(reverseFractionalDigits, 0, m_reverseFractionalDigits, 0, n_decimals);

    m_integers = new BigInteger[n_integers];
    System.arraycopy(integers, 0, m_integers, 0, n_integers);

    m_longs = new long[n_longs];
    System.arraycopy(longs, 0, m_longs, 0, n_longs);

    m_datetimes = new XSDateTime[n_datetimes];
    System.arraycopy(datetimes, 0, m_datetimes, 0, n_datetimes);

    m_durations = new Duration[n_durations];
    System.arraycopy(durations, 0, m_durations, 0, n_durations);

    m_binaries = new byte[n_binaries][];
    System.arraycopy(binaries, 0, m_binaries, 0, n_binaries);

    m_variants = new int[n_variants];
    System.arraycopy(variants, 0, m_variants, 0, n_variants);

    m_variantTypes = new byte[n_variants];
    System.arraycopy(variantTypes, 0, m_variantTypes, 0, n_variants);

    m_grammars = new int[n_grammars];
    System.arraycopy(grammars, 0, m_grammars, 0, n_grammars);

    m_productions = new int[n_productions];
    System.arraycopy(productions, 0, m_productions, 0, n_productions);

    m_eventTypes = new byte[n_events];
    System.arraycopy(eventTypes, 0, m_eventTypes, 0, n_events);
    
    m_eventData = new int[n_events];
    System.arraycopy(eventData, 0, m_eventData, 0, n_events);
    
    m_n_stypes = n_stypes;
    
    m_grammarCount = grammarCount;

    setUp();
  }
  
  private void setUp() {
    computeAncestryIds();
    populateLocalNames();
    computeGlobalDirectory();
    computeVariantCharacters();
    computeDateTimes();
    buildFragmentsArray();
  }

  private void computeAncestryIds() {
    ancestryIds = new byte[m_n_stypes + 1]; // the extra entry is for xsd:anyType albeit unused 
    ancestryIds[0] = EXISchemaConst.UNTYPED; // 0 is xsd:anyType (unused. not an atomic simple type.)
    int tp = EXISchemaLayout.SZ_COMPLEX_TYPE;
    for (int n_stypes = 0; n_stypes < m_n_stypes; tp += EXISchema._getTypeSize(tp, m_types, ancestryIds)) {
      final int serial = getSerialOfType(tp);
      assert 0 < serial && serial <= m_n_stypes && isSimpleType(tp);
      ++n_stypes;
      final byte ancestryId;
      if (getVarietyOfSimpleType(tp) == ATOMIC_SIMPLE_TYPE) {
        int _tp = tp;
        int _serial;
        while ((_serial = getSerialOfType(_tp)) >= EXISchemaConst.N_PRIMITIVE_TYPES_PLUS_INTEGER) {
          _tp = getBaseTypeOfSimpleType(_tp);
        }
        assert _serial >= 2; // 0 is xsd:anyType, and 1 is xsd:anySimpleType
        ancestryId = ANCESTRY_IDS[_serial];  
      }
      else { // not atomic
        ancestryId = EXISchemaConst.UNTYPED;
      }
      ancestryIds[serial] = ancestryId;
    }
    m_stypes_end = tp;
  }

  private void populateLocalNames() {
    localNames = new String[m_localNames.length][];
    for (int i = 0; i < m_localNames.length; i++) {
      final String[] _localNames = new String[m_localNames[i].length];
      for (int j = 0; j < _localNames.length; j++) {
        _localNames[j] = m_names[m_localNames[i][j]];
      }
      localNames[i] = _localNames;
    }
  }

  private void buildFragmentsArray() {
    int pos;
    int currentUri, currentName;
    int currentNode;
    boolean isSpecific;
    
    final List<Integer> elemDecls = new ArrayList<Integer>();
    currentUri = currentName = -1;
    currentNode = Integer.MIN_VALUE; // an arbitrary value (i.e. any value is ok)
    isSpecific = true;
    for (pos = 0; pos < m_elems.length; pos += EXISchemaLayout.SZ_ELEM) {
      final int elem = pos; 
      final int uri = m_elems[elem + EXISchemaLayout.INODE_URI];
      final int name = m_elems[elem + EXISchemaLayout.INODE_NAME];
      if (currentUri != uri || currentName != name) {
        if (pos != 0)
          elemDecls.add(isSpecific ? currentNode : (0 - currentNode) - 1);
        currentNode = elem;
        isSpecific = true;
        currentUri = uri;
        currentName = name;
        continue;
      }
      assert currentNode != elem && uri == currentUri && name == currentName;
      if (getTypeOfElem(elem) != getTypeOfElem(currentNode) || isNillableElement(elem) != isNillableElement(currentNode)) {
        isSpecific = false;
      }
    }
    if (pos != 0)
      elemDecls.add(isSpecific ? currentNode : (0 - currentNode) - 1);

    final List<Integer> attrDecls = new ArrayList<Integer>();
    currentUri = currentName = -1;
    currentNode = Integer.MIN_VALUE; // an arbitrary value (i.e. any value is ok)
    isSpecific = true;
    for (pos = 0; pos < m_attrs.length; pos += EXISchemaLayout.SZ_ATTR) {
      final int attr = pos;
      final int uri = m_attrs[pos + EXISchemaLayout.INODE_URI];
      final int name = m_attrs[pos + EXISchemaLayout.INODE_NAME];
      if (currentUri != uri || currentName != name) {
        if (pos != 0)
          attrDecls.add(isSpecific ? currentNode : (0 - currentNode) - 1);
        currentNode = attr;
        isSpecific = true;
        currentUri = uri;
        currentName = name;
        continue;
      }
      assert currentNode != attr && uri == currentUri && name == currentName;
      if (getTypeOfAttr(attr) != getTypeOfAttr(currentNode))
        isSpecific = false;
    }
    if (pos != 0)
      attrDecls.add(isSpecific ? currentNode : (0 - currentNode) - 1);
    
    int i = 0;
    Iterator<Integer> iterInteger;
    m_n_fragmentElems = elemDecls.size();
    m_fragmentINodes = new int[m_n_fragmentElems + attrDecls.size()];
    iterInteger = elemDecls.iterator();
    while (iterInteger.hasNext())
      m_fragmentINodes[i++] = iterInteger.next().intValue();
    assert i == m_n_fragmentElems;
    iterInteger = attrDecls.iterator();
    while (iterInteger.hasNext())
      m_fragmentINodes[i++] = iterInteger.next().intValue();
  }

  private void computeGlobalDirectory() {
    m_globalElementsDirectory = new HashMap<String,int[]>();
    m_globalAttributesDirectory = new HashMap<String,int[]>();
    m_globalTypesDirectory = new HashMap<String,int[]>();

    final List<Integer> globalElements = new ArrayList<Integer>();
    // Build up global elements directory
    for (int elem = 0; elem < m_elems.length; elem += EXISchemaLayout.SZ_ELEM) {
      if (isGlobalElem(elem)) {
        String name = getNameOfElem(elem);
        int[] nodes;
        if ((nodes = m_globalElementsDirectory.get(name)) != null) {
          final int[] _nodes = new int[nodes.length + 1];
          System.arraycopy(nodes, 0, _nodes, 0, nodes.length);
          _nodes[nodes.length] = elem;
          nodes = _nodes;
        }
        else {
          nodes = new int[1];
          nodes[0] = elem;
        }
        m_globalElementsDirectory.put(name, nodes);
        globalElements.add(elem);
      }
    }
    
    // Build global elements array
    m_globalElems = new int[globalElements.size()];
    assert m_globalElems.length == getGlobalElemCountOfSchema();
    for (int i = 0; i < m_globalElems.length; i++)
      m_globalElems[i] = globalElements.get(i).intValue();

    final List<Integer> globalAttributes = new ArrayList<Integer>();
    // Build up global attributes directory
    for (int attr = 0; attr < m_attrs.length; attr += EXISchemaLayout.SZ_ATTR) {
      if (isGlobalAttr(attr)) {
        final int localName = m_attrs[attr + EXISchemaLayout.INODE_NAME];
        final int uri = m_attrs[attr + EXISchemaLayout.INODE_URI];
        String name = m_names[m_localNames[uri][localName]];
        int[] nodes;
        if ((nodes = m_globalAttributesDirectory.get(name)) != null) {
          final int[] _nodes = new int[nodes.length + 1];
          System.arraycopy(nodes, 0, _nodes, 0, nodes.length);
          _nodes[nodes.length] = attr;
          nodes = _nodes;
        }
        else {
          nodes = new int[1];
          nodes[0] = attr;
        }
        m_globalAttributesDirectory.put(name, nodes);
        globalAttributes.add(attr);
      }
    }

    // Build global attributes array
    m_globalAttrs = new int[globalAttributes.size()];
    for (int i = 0; i < m_globalAttrs.length; i++) {
      m_globalAttrs[i] = globalAttributes.get(i).intValue();
    }

    m_buitinTypes = new int[EXISchemaConst.N_BUILTIN_TYPES];
    int tp, i;
    for (tp = 0, i = 0; tp < m_types.length; tp += EXISchema._getTypeSize(tp, m_types, ancestryIds), i++) {
      final String tname = getNameOfType(tp);
      if (i < EXISchemaConst.N_BUILTIN_TYPES) {
        assert getUriOfType(tp) == 3 && tname.length() != 0;
        m_buitinTypes[i] = tp;
      }
      if (!"".equals(tname)) {
        int[] nodes;
        if ((nodes = m_globalTypesDirectory.get(tname)) != null) {
          int[] _nodes = new int[nodes.length + 1];
          System.arraycopy(nodes, 0, _nodes, 0, nodes.length);
          _nodes[nodes.length] = tp;
          nodes = _nodes;
        }
        else {
          nodes = new int[1];
          nodes[0] = tp;
        }
        m_globalTypesDirectory.put(tname, nodes);
      }
    }
  }
  
  private void computeVariantCharacters() {
    final int n_variants = m_variants.length;
    final Characters[] variantCharacters = new Characters[n_variants];
    for (int i = 0; i < n_variants; i++) {
      final String stringValue = computeVariantCharacters(i);
      variantCharacters[i] = new Characters(stringValue.toCharArray(), 0, stringValue.length(), false);
    }
    m_variantCharacters = variantCharacters;
  }
  
  private String computeVariantCharacters(int variant) {
    String stringValue = "";
    final byte[] binaryValue;
    switch (m_variantTypes[variant]) {
      case VARIANT_STRING:
        stringValue = getStringValueOfVariant(variant);
        break;
      case VARIANT_FLOAT:
        final int ind = m_variants[variant];
        final long mantissa = m_mantissas[ind];
        final int exponent;
        if ((exponent = m_exponents[ind]) != 0) {
          if (exponent == -16384)
            stringValue = mantissa == 1L ? "INF" : mantissa == -1 ? "-INF" : "NaN";
          else
            stringValue = Long.toString(mantissa) + "E" + Integer.toString(exponent);
        }
        else
          stringValue = Long.toString(mantissa);
        break;
      case VARIANT_DECIMAL:
        final boolean sign = getSignOfDecimalVariant(variant);
        final String integralDigits = getIntegralDigitsOfDecimalVariant(variant);
        final String fractionalDigits = new StringBuilder(getReverseFractionalDigitsOfDecimalVariant(variant)).reverse().toString();
        final boolean zeroFractionalDigits;
        if ((zeroFractionalDigits = "0".equals(fractionalDigits)) && "0".equals(integralDigits)) {
          stringValue = "0";
        }
        else {
          if (sign)
            stringValue += '-';
          stringValue += integralDigits;
          if (!zeroFractionalDigits) {
            stringValue += '.' ;
            stringValue += fractionalDigits;
          }
        }
        break;
      case VARIANT_INTEGER:
        final BigInteger bigInteger = getIntegerValueOfVariant(variant);
        stringValue = bigInteger.toString(); 
        break;
      case VARIANT_INT:
        final int intValue = getIntValueOfVariant(variant);
        stringValue = Integer.toString(intValue); 
        break;
      case VARIANT_LONG:
        final long longValue = getLongValueOfVariant(variant);
        stringValue = Long.toString(longValue);
        break;
      case VARIANT_DATETIME:
        final XSDateTime dateTime = getDateTimeValueOfVariant(variant);
        stringValue = dateTime.toString();
        break;
      case VARIANT_DURATION:
        stringValue = getDurationValueOfVariant(variant).toString();
        break;
      case VARIANT_BASE64:
        binaryValue = getBinaryValueOfVariant(variant);
        int maxChars = (binaryValue.length / 3) << 2;
        if (binaryValue.length % 3 != 0)
          maxChars += 4;
        maxChars += maxChars / 76;
        final char[] characters = new char[maxChars]; 
        final int n_chars = Base64.encode(binaryValue, 0, binaryValue.length, characters, 0);
        stringValue = new String(characters, 0, n_chars);
        break;
      case VARIANT_BOOLEAN:
        assert false;
        return null;
      case VARIANT_HEXBIN:
        binaryValue = getBinaryValueOfVariant(variant);
        final StringBuffer stringBuffer = new StringBuffer();
        HexBin.encode(binaryValue, binaryValue.length, stringBuffer);
        stringValue = stringBuffer.toString();
        break;
      default:
        assert false;
        break;
    }
    return stringValue;
  }

  /**
   * Populate m_computedDatetimes.
   */
  private void computeDateTimes() {
    final int n_dateTimes = m_datetimes.length;
    final XSDateTime[] processedDatetimes = new XSDateTime[n_dateTimes];
    for (int i = 0; i < n_dateTimes; i++) {
      final XSDateTime dateTime;
      processedDatetimes[i] = dateTime = new XSDateTime(m_datetimes[i]);
      dateTime.normalize(true);
    }
    m_computedDatetimes = processedDatetimes;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Corpus methods
  ///////////////////////////////////////////////////////////////////////////

  /** @y.exclude */
  public int[] getElems() {
    return m_elems;
  }

  int[] getAttrs() {
    return m_attrs;
  }

  /** @y.exclude */
  public int[] getTypes() {
    return m_types;
  }

  /** @y.exclude */
  public int[][] getLocalNames() {
    return m_localNames;
  }

  /** @y.exclude */
  public int[] getGrammars() {
    return m_grammars;
  }
  
  /** @y.exclude */
  public int[] getInts() {
    return m_ints;
  }

  /** @y.exclude */
  public long[] getLongs() {
    return m_longs;
  }

  /** @y.exclude */
  public int[] getVariants() {
    return m_variants;
  }

  /** @y.exclude */
  public int[] getFragmentINodes() {
    return m_fragmentINodes;
  }
  
  /** @y.exclude */
  public int getFragmentElemCount() {
    return this.m_n_fragmentElems;
  }
  
  /**
   * Returns the total number of simple types (both global and local) available in the schema.
   * @param schema schema node
   * @return total number of simple types available in the schema.
   * @y.exclude
   */

  public int getTotalSimpleTypeCount() {
    return m_n_stypes;
  }

  /**
   * Returns the total number of grammars contained in the corpus.
   * @return total number of grammars
   * @y.exclude
   */
  public int getTotalGrammarCount() {
    return m_grammarCount;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Generic methods
  ///////////////////////////////////////////////////////////////////////////

  public boolean isSimpleType(int tp) {
    return (m_types[tp + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.TYPE_TYPE_OFFSET_MASK) != 0;
  }
  
  public static boolean _isSimpleType(int tp, int[] types) {
    return (types[tp + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.TYPE_TYPE_OFFSET_MASK) != 0;
  }
  
  /**
   * Returns the size of a type.
   * @param tp a type
   * @return size of the type if it is a type, otherwise returns NIL_VALUE
   * @y.exclude
   */
  public static int _getTypeSize(int tp, int[] types, byte[] ancestryIds) {
    assert tp != NIL_NODE;
    if ((types[tp + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.TYPE_TYPE_OFFSET_MASK) != 0)
      return _getSizeOfSimpleType(tp, types, ancestryIds);
    else // i.e. is a complex type
      return EXISchemaLayout.SZ_COMPLEX_TYPE;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Directory methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * @y.exclude
   */
  public int getGlobalElemOfSchema(String uri, String name) {
    final int[] nodes;
    if ((nodes = m_globalElementsDirectory.get(name)) != null) {
      final int len = nodes.length;
      for (int i = 0; i < len; i++) {
        final int elem = nodes[i];
        final int uriId = getUriOfElem(elem); 
        if (uri.equals(uris[uriId])) {
          return elem;
        }
      }
    }
    return NIL_NODE;
  }

  /**
   * @y.exclude
   */
  public int getGlobalAttrOfSchema(String uri, String name) {
    final int[] attrs;
    if ((attrs = m_globalAttributesDirectory.get(name)) != null) {
      final int len = attrs.length;
      for (int i = 0; i < len; i++) {
        final int attr = attrs[i];
        final int uriId = getUriOfAttr(attr); 
        if (uri.equals(uris[uriId])) {
          return attr;
        }
      }
    }
    return NIL_NODE;
  }

  /**
   * @y.exclude
   */
  public int getTypeOfSchema(String uri, String name) {
    final int[] nodes;
    if ((nodes = m_globalTypesDirectory.get(name)) != null) {
      final int len = nodes.length;
      for (int i = 0; i < len; i++) {
        final int tp = nodes[i];
        final int uriId;
        if ((uriId = getUriOfType(tp)) != -1 && uri.equals(uris[uriId])) {
          return tp;
        }
      }
    }
    return NIL_NODE;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Schema methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the total number of global elements available in the schema.
   * @return total number of global elements available in the schema.
   * @y.exclude
   */
  public int getGlobalElemCountOfSchema() {
    return m_globalElems.length;
  }
  
  /**
   * Returns the i-th global element of the schema.
   * @param i index of the element
   * @return i-th element
   * @y.exclude
   */
  public int getGlobalElemOfSchema(int i) {
    assert 0 <= i && i < m_globalElems.length; 
    return m_globalElems[i];
  }

  /**
   * Returns the total number of global attributes available in the schema.
   * @return total number of global attributes available in the schema.
   * @y.exclude
   */
  public int getGlobalAttrCountOfSchema() {
    return m_globalAttrs.length;
  }
  
  /**
   * @y.exclude
   */
  public int getGlobalAttrOfSchema(int i) {
    assert 0 <= i && i < m_globalAttrs.length;
    return m_globalAttrs[i];
  }

  /**
   * Returns the i-th built-in type of the schema.
   * @param i index of the type
   * @return i-th built-in type
   * @throws EXISchemaRuntimeException If the index is out of array bounds
   * @y.exclude
   */
  public int getBuiltinTypeOfSchema(int i)
      throws EXISchemaRuntimeException {
    if (i < 0 || EXISchemaConst.N_BUILTIN_TYPES - 1 < i)
      throw new EXISchemaRuntimeException(
        EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
        new String[] { String.valueOf(i), String.valueOf(0), String.valueOf(EXISchemaConst.N_BUILTIN_TYPES - 1) });
    return m_buitinTypes[i];
  }

  ///////////////////////////////////////////////////////////////////////////
  // Element methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the total number of elements. 
   * @y.exclude
   **/
  public int getElemCountOfSchema() {
    return m_elems.length / EXISchemaLayout.SZ_ELEM;
  }

  /**
   * Returns name of an element.
   * @param elem element node
   * @return name of an element node.
   * @y.exclude
   */
  public String getNameOfElem(int elem) {
    assert 0 <= elem;
    final int localName = m_elems[elem + EXISchemaLayout.INODE_NAME];
    if (localName != -1) {
      final int uri = m_elems[elem + EXISchemaLayout.INODE_URI];
      return m_names[m_localNames[uri][localName]];
    }
    return "";
  }

  /**
   * Returns localName ID of an elem.
   * @param elem element node
   * @return localName ID
   * @y.exclude
   */
  public int getLocalNameOfElem(int elem) {
    assert 0 <= elem;
    return m_elems[elem + EXISchemaLayout.INODE_NAME];
  }
  
  /**
   * Returns uri of an element.
   * @param elem an element
   * @return uri
   * @y.exclude
   */
  public int getUriOfElem(int elem) {
    assert 0 <= elem;
    return m_elems[elem + EXISchemaLayout.INODE_URI];
  }

  /** @y.exclude */
  public int getTypeOfElem(int elem) {
    assert 0 <= elem;
    final int tp = m_elems[elem + EXISchemaLayout.INODE_TYPE];
    /**
     * NOTE: Simply reverting negative to positive would have been done by
     * return (stype & 0x80000000) != 0 ? ~stype + 1 : stype;
     */
    return (tp & 0x80000000) != 0 ? ~tp : tp; 
  }

  /** @y.exclude */
  public boolean isGlobalElem(int elem) {
    final int tp = m_elems[elem + EXISchemaLayout.INODE_TYPE];
    return (tp & 0x80000000) != 0;
  }
  
  /** @y.exclude */
  public boolean isNillableElement(int elem) {
    assert 0 <= elem;
    return m_elems[elem + EXISchemaLayout.ELEM_NILLABLE] != 0;
  }

  /** @y.exclude */
  public static boolean _isNillableElement(int elem, int[] elems) {
    assert 0 <= elem;
    return elems[elem + EXISchemaLayout.ELEM_NILLABLE] != 0;
  }

  /**
   * Returns serial number of an element.
   * @param elem element node
   * @return serial number
   * @y.exclude
   */
  public int getSerialOfElem(int elem) {
    assert 0 <= elem && elem % EXISchemaLayout.SZ_ELEM == 0;
    return elem / EXISchemaLayout.SZ_ELEM;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Type methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns name of a type.
   * @param tp type node
   * @return name of a type node.
   * @y.exclude
   */
  public String getNameOfType(int tp) {
    assert 0 <= tp;
    final int localName = m_types[tp + EXISchemaLayout.TYPE_NAME];
    if (localName != -1) {
      final int uri = m_types[tp + EXISchemaLayout.TYPE_URI];
      return m_names[m_localNames[uri][localName]];
    }
    return "";
  }
  
  /**
   * Returns localName ID of an type.
   * @param tp type node
   * @return localName ID
   * @y.exclude
   */
  public int getLocalNameOfType(int tp) {
    assert 0 <= tp;
    return m_types[tp + EXISchemaLayout.TYPE_NAME];
  }

  /**
   * Returns target namespace name of type.
   * @param tp type node
   * @return target namespace name
   * @y.exclude
   */
  public int getUriOfType(int tp) {
    assert 0 <= tp;
    return m_types[tp + EXISchemaLayout.TYPE_URI];
  }

  /**
   * Returns serial number of a type. Those serial numbers of built-in
   * primitive types are static and do not change.
   * @param tp type node
   * @return serial number
   * @y.exclude
   */
  public int getSerialOfType(int tp) {
    assert 0 <= tp;
    return m_types[tp + EXISchemaLayout.TYPE_NUMBER];
  }
  
  /** @y.exclude */
  public int getGrammarOfType(int tp) {
    assert 0 <= tp;
    return m_types[tp + EXISchemaLayout.TYPE_GRAMMAR];
  }
  
  /** @y.exclude */
  public static int _getGrammarOfType(int tp, int[] types) {
    assert 0 <= tp;
    return types[tp + EXISchemaLayout.TYPE_GRAMMAR];
  }
  
  /** @y.exclude */
  public boolean isTypableType(int tp) {
    assert 0 <= tp;
    return m_types[tp + EXISchemaLayout.TYPE_TYPABLE] != 0;
  }

  ///////////////////////////////////////////////////////////////////////////
  // SimpleType methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the size of a simple type.
   * @param stype simple type node
   * @return size of the simple type
   */
  private static int _getSizeOfSimpleType(int stype, int[] types, byte[] ancestryIds) {
    int size = EXISchemaLayout.SZ_SIMPLE_TYPE;
    final byte variety = _getVarietyOfSimpleType(stype, types);
    if (variety == ATOMIC_SIMPLE_TYPE) {
      if (_isEnumeratedAtomicSimpleType(stype, types))
        size += 1 + _getEnumerationFacetCountOfAtomicSimpleType(stype, types, ancestryIds); 
      if (ancestryIds[types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.STRING_TYPE)
        size += _getRestrictedCharacterCountOfStringSimpleType(stype, types, ancestryIds);
    }
    return size; 
  }

  /**
   * Returns base type of a simple type.
   * @param stype simple type node
   * @return base type
   * @y.exclude
   */
  public int getBaseTypeOfSimpleType(int stype) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0;
    return m_types[stype + (EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_BASE_TYPE)];
  }

  /**
   * Returns variety property of a simple type.
   * @param stype simple type node
   * @return one of UR_SIMPLE_TYPE, ATOMIC_SIMPLE_TYPE, LIST_SIMPLE_TYPE or UNION_SIMPLE_TYPE
   * @y.exclude
   */
  public byte getVarietyOfSimpleType(int stype) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0;
    return _getVarietyOfSimpleType(stype, m_types);
  }
  /**
   * @y.exclude
   */
  public static byte _getVarietyOfSimpleType(int stype, int[] types) {
    return (byte)(types[stype + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_VARIETY_MASK);
  }

  /**
   * Returns item type of a list simple type.
   * @param stype list simple type
   * @return item type
   * @y.exclude
   */
  public int getItemTypeOfListSimpleType(int stype) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0;
    return m_types[stype + (EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_ITEM_TYPE)];
  }

  /**
   * Determines if a simple type is primitive.
   * @param stype simple type
   * @return true if the simple type is primitive
   * @y.exclude
   */
  public boolean isPrimitiveSimpleType(int stype) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0;
    final int serial = getSerialOfType(stype);
    // primitive simple types' serial is [2...20]
    return EXISchemaConst.ANY_SIMPLE_TYPE < serial && 
      serial < EXISchemaConst.STRING_TYPE + EXISchemaConst.N_PRIMITIVE_TYPES;
  }

  /**
   * Determines if a simple type represents integral numbers.
   * @param stype simple type
   * @return true if the simple type represents integral numbers
   * @y.exclude
   */
  public boolean isIntegralSimpleType(int stype) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0;
    return ancestryIds[m_types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.INTEGER_TYPE;
  }

  /**
   * Returns the representation of an integral simple type.
   * @param stype integral simple type
   * @return INTEGER_CODEC_NONNEGATIVE for a non-negative 
   * integer representation, INTEGER_CODEC_DEFAULT for a default 
   * integer representation, otherwise width of n-bits integer 
   * representation.
   * @y.exclude
   */
  public int getWidthOfIntegralSimpleType(int stype) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0;
    return _getWidthOfIntegralSimpleType(stype, m_types);
  }

  /** @y.exclude */
  public static int _getWidthOfIntegralSimpleType(int stype, int[] types)
    throws EXISchemaRuntimeException {
    return (types[stype + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_MASK) >> 
      EXISchemaLayout.SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_OFFSET;
  }
  
  /**
   * Returns whiteSpace facet value of a string simple type.
   * @param stype simple type
   * @return one of WHITESPACE_* enumerated values
   * @y.exclude
   */
  public int getWhitespaceFacetValueOfStringSimpleType(int stype) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0 && 
        ancestryIds[m_types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.STRING_TYPE;
    return (m_types[stype + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_MASK) >> 
      EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_OFFSET;   
  }
  
 /** @y.exclude */
  public boolean isPatternedBooleanSimpleType(int stype) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0 && 
        ancestryIds[m_types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.BOOLEAN_TYPE;
    return (m_types[stype + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_ATOMIC_BOOLEAN_HAS_PATTERN_MASK) != 0; 
  }

  /**
   * Returns minInclusive facet variant of an integer simple type.
   * @param stype simple type
   * @return minInclusive facet variant if available, otherwise NIL_VALUE
   * @y.exclude
   */
  public int getMinInclusiveFacetOfIntegerSimpleType(int stype) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0 && 
        ancestryIds[m_types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.INTEGER_TYPE;
    return m_types[stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_FACET_MININCLUSIVE];
  }

  /**
   * Returns the number of characters in restricted characters set associated with a string simple type.
   * @param stype simple type
   * @return number of characters in restricted character set
   * @y.exclude
   */
  public int getRestrictedCharacterCountOfStringSimpleType(int stype) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0 && 
        ancestryIds[m_types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.STRING_TYPE;
    return _getRestrictedCharacterCountOfStringSimpleType(stype, m_types, ancestryIds);
  }

  private static int _getRestrictedCharacterCountOfStringSimpleType(int stype, int[] types, byte[] ancestryIds) {
    final int n_restrictedCharacters = (types[stype + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_MASK) >> 
      EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_OFFSET;
    assert 0 <= n_restrictedCharacters && n_restrictedCharacters < 256; 
    return n_restrictedCharacters;
  }

  /**
   * Returns the start index of the restricted charset. 
   * @param stype simple type
   * @y.exclude
   */
  public int getRestrictedCharacterOfSimpleType(int stype) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0;
    return stype + EXISchemaLayout.SZ_SIMPLE_TYPE;
  }
  
  private boolean isEnumeratedAtomicSimpleType(int stype) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0 && 
        getVarietyOfSimpleType(stype) == ATOMIC_SIMPLE_TYPE;
    return _isEnumeratedAtomicSimpleType(stype, m_types);
  }

  private static boolean _isEnumeratedAtomicSimpleType(int stype, int[] types) {
    return (types[stype + EXISchemaLayout.TYPE_AUX] & 
        EXISchemaLayout.SIMPLE_TYPE_HAS_ENUMERATED_VALUES_MASK) != 0;
  }
  
  /**
   * Returns the number of enumeration facets associated with an atomic simple type.
   * @param stype simple type
   * @return number of enumeration facets
   * @y.exclude
   */
  public int getEnumerationFacetCountOfAtomicSimpleType(int stype) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0 && 
        getVarietyOfSimpleType(stype) == ATOMIC_SIMPLE_TYPE;
    return _getEnumerationFacetCountOfAtomicSimpleType(stype, m_types, ancestryIds);
  }

  private static int _getEnumerationFacetCountOfAtomicSimpleType(int stype, int[] types, byte[] ancestryIds) {
    if (_isEnumeratedAtomicSimpleType(stype, types)) {
      final int n_patterns = ancestryIds[types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.STRING_TYPE ?
          _getRestrictedCharacterCountOfStringSimpleType(stype, types, ancestryIds) : 0;    
      return types[stype + EXISchemaLayout.SZ_SIMPLE_TYPE + n_patterns];
    }
    return 0;
  }
  
  /**
   * Returns the i-th enumeration facet variant of an atomic simple type.
   * @param stype simple type
   * @param i index of the enumeration facet
   * @return ith enumeration facet variant
   * @y.exclude
   */
  public int getEnumerationFacetOfAtomicSimpleType(int stype, int i) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0 && 
        getVarietyOfSimpleType(stype) == ATOMIC_SIMPLE_TYPE;
    final int n = _getEnumerationFacetCountOfAtomicSimpleType(stype, m_types, ancestryIds);
    if (i < 0 || i >= n) {
      throw new EXISchemaRuntimeException(
        EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
        new String[] { String.valueOf(i), String.valueOf(0), String.valueOf(n - 1) });
    }
    final int n_patterns = ancestryIds[m_types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.STRING_TYPE ?
        _getRestrictedCharacterCountOfStringSimpleType(stype, m_types, ancestryIds) : 0;
    return m_types[stype + EXISchemaLayout.SZ_SIMPLE_TYPE + n_patterns + 1 + i];
  }

  /** @y.exclude */
  public int getNextSimpleType(int stype) {
    assert 0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0;
    final int _stype;
    if ((_stype = stype + _getSizeOfSimpleType(stype, m_types, ancestryIds)) != m_stypes_end) {
      return _stype;
    }
    return NIL_NODE;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // ComplexType methods
  ///////////////////////////////////////////////////////////////////////////

  /** @y.exclude */
  public int getContentDatatypeOfComplexType(int ctype) {
    assert 0 <= ctype && m_types[ctype + EXISchemaLayout.TYPE_AUX] >= 0;
    final int tp;
    // if tp is 0, return NIL_NODE. Otherwise, return tp.
    return (tp = m_types[ctype + EXISchemaLayout.TYPE_AUX]) != 0 ? tp : NIL_NODE;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Grammar methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the size of a grammar.
   * @param gram a grammar
   * @return size of the grammar
   * @y.exclude
   */
  public static int getSizeOfGrammar(int gram, int[] grammars) {
    int sz = EXISchemaLayout.SZ_GRAMMAR;
    if (_hasContentGrammar(gram, grammars)) {
      ++sz;
      if (_hasEmptyGrammar(gram, grammars))
        ++sz;
    }
    return sz + _getProductionCountOfGrammar(gram, grammars);
  }
  
  /** @y.exclude  */
  public int getSerialOfGrammar(int gram) {
    return m_grammars[gram + EXISchemaLayout.GRAMMAR_NUMBER];
  }
  
  /** @y.exclude  */
  public boolean hasEndElement(int gram) {
    return (m_grammars[gram + EXISchemaLayout.GRAMMAR_N_PRODUCTION] & EXISchemaLayout.GRAMMAR_HAS_END_ELEMENT_MASK) != 0;
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */  
  public boolean hasEmptyGrammar(int gram) {
    return _hasEmptyGrammar(gram, m_grammars);
  }
  /**
   * Not for public use.
   * @y.exclude
   */
  public static boolean _hasEmptyGrammar(int gram, int[] grammars) {
    final boolean res;
    res = (grammars[gram + EXISchemaLayout.GRAMMAR_N_PRODUCTION] & EXISchemaLayout.GRAMMAR_HAS_EMPTY_GRAMMAR_MASK) != 0; 
    assert !res || _hasContentGrammar(gram, grammars);
    return res; 
  }
  /**
   * Not for public use.
   * @y.exclude
   */
  public boolean hasContentGrammar(int gram) {
    return _hasContentGrammar(gram, m_grammars);
  }
  /**
   * Not for public use.
   * @y.exclude
   */  
  public static boolean _hasContentGrammar(int gram, int[] grammars) {
    return (grammars[gram + EXISchemaLayout.GRAMMAR_N_PRODUCTION] & EXISchemaLayout.GRAMMAR_HAS_CONTENT_GRAMMAR_MASK) != 0;
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */
  public int getProductionCountOfGrammar(int gram) {
    return _getProductionCountOfGrammar(gram, m_grammars);
  }
  /**
   * Not for public use.
   * @y.exclude
   */
  public static int _getProductionCountOfGrammar(int gram, int[] grammars) {
    return grammars[gram + EXISchemaLayout.GRAMMAR_N_PRODUCTION] & EXISchemaLayout.GRAMMAR_N_PRODUCTION_MASK;
  }
  
  /** @y.exclude */
  public int getProductionOfGrammar(int gram, int i) {
    final int n = getProductionCountOfGrammar(gram);
    if (i < 0 || i >= n) {
      throw new EXISchemaRuntimeException(
        EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
        new String[] { String.valueOf(i), String.valueOf(0), String.valueOf(n - 1) });
    }
    return _getProductionOfGrammar(gram, i, m_grammars);
  }
  
  /** @y.exclude */
  public static int _getProductionOfGrammar(int gram, int i, int[] grammars) {
    int prods;
    prods = gram + EXISchemaLayout.SZ_GRAMMAR;
    if (_hasContentGrammar(gram, grammars)) {
      ++prods;
      if (_hasEmptyGrammar(gram, grammars)) {
        ++prods;
      }
    }
    return grammars[prods + i];
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */
  public int getContentGrammarOfGrammar(int gram) {
    return _getContentGrammarOfGrammar(gram, m_grammars);
  }
  
  /** @y.exclude */
  public static int _getContentGrammarOfGrammar(int gram, int[] grammars) {
    if (_hasContentGrammar(gram, grammars)) {
      return grammars[gram + (EXISchemaLayout.SZ_GRAMMAR + EXISchemaLayout.GRAMMAR_EXT_CONTENT_GRAMMAR)];
    }
    return EXISchema.NIL_GRAM;
  }
  
  /** @y.exclude */
  public int getTypeEmptyGrammarOfGrammar(int gram) {
    return _getTypeEmptyGrammarOfGrammar(gram, m_grammars);
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */
  public static int _getTypeEmptyGrammarOfGrammar(int gram, int[] grammars) {
    if (_hasEmptyGrammar(gram, grammars)) {
      return grammars[gram + (EXISchemaLayout.SZ_GRAMMAR + EXISchemaLayout.GRAMMAR_EXT_EMPTY_GRAMMAR)];
    }
    return EXISchema.NIL_GRAM;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Production methods
  ///////////////////////////////////////////////////////////////////////////
  
  /**
   * Not for public use.
   * @y.exclude
   */
  public int getEventOfProduction(int prod) {
    return m_productions[prod + EXISchemaLayout.PRODUCTION_EVENT];
  }
  /**
   * Not for public use.
   * @y.exclude
   */
  public int getGrammarOfProduction(int prod) {
    return m_productions[prod + EXISchemaLayout.PRODUCTION_GRAMMAR];
  }

  ///////////////////////////////////////////////////////////////////////////
  // Event methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns event type of the event.
   * @param event
   * @return one of EVENT_TYPE_AT, EVENT_TYPE_SE,  
   * EVENT_TYPE_AT_WILDCARD_NS or EVENT_TYPE_SE_WILDCARD_NS
   * Not for public use.
   * @y.exclude
   */
  public byte getEventType(int event) {
    return m_eventTypes[event];
  }
  /**
   * Not for public use.
   * @y.exclude
   */  
  public int getNodeOfEventType(int event) {
    assert m_eventTypes[event] == EVENT_TYPE_AT || m_eventTypes[event] == EVENT_TYPE_SE;
    return m_eventData[event];
  }
  /**
   * Not for public use.
   * @y.exclude
   */
  public int getUriOfEventType(int event) {
    assert m_eventTypes[event] == EVENT_TYPE_AT_WILDCARD_NS || m_eventTypes[event] == EVENT_TYPE_SE_WILDCARD_NS;
    return m_eventData[event];
  }

  ///////////////////////////////////////////////////////////////////////////
  // Attribute methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns localName ID of an attribute.
   * @param attr attribute node
   * @return localName ID
   * @y.exclude
   */
  public int getLocalNameOfAttr(int attr) {
    assert 0 <= attr;
    return m_attrs[attr + EXISchemaLayout.INODE_NAME];
  }

  /**
   * Returns target namespace name of an attribute.
   * @param attr attribute node
   * @return target namespace name
   * @y.exclude
   */
  public int getUriOfAttr(int attr) {
    assert 0 <= attr;
    return m_attrs[attr + EXISchemaLayout.INODE_URI];
  }
  
  /**
   * Returns the type of an attribute.
   * @param attr attribute node
   * @return type of the attribute
   * @y.exclude
   */
  public int getTypeOfAttr(int attr) {
    assert 0 <= attr;
    return _getTypeOfAttr(attr, m_attrs);
  }
  
  /** @y.exclude */
  public static int _getTypeOfAttr(int attr, int[] attrs) {
    final int stype = attrs[attr + EXISchemaLayout.INODE_TYPE];
    /**
     * NOTE: Simply reverting negative to positive would have been done by
     * return (stype & 0x80000000) != 0 ? ~stype + 1 : stype;
     */
    return (stype & 0x80000000) != 0 ? ~stype : stype; 
  }
  
  /** @y.exclude */
  public boolean isGlobalAttr(int attr) {
    final int stype = m_attrs[attr + EXISchemaLayout.INODE_TYPE];
    return (stype & 0x80000000) != 0;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Variant methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the type of a variant.
   * @param variant a variant
   * @return one of VARIANT_* enumerated values
   * @y.exclude
   */
  public int getTypeOfVariant(int variant) {
    return m_variantTypes[variant];
  }

  /**
   * Returns Characters value of a variant. 
   * @param variant
   * @return Characters value
   * @y.exclude
   */
  public Characters getVariantCharacters(int variant) {
    return m_variantCharacters[variant];
  }
  
  /**
   * Returns String value of a variant.
   * @param variant a variant of type VARIANT_STRING
   * @return String value
   * @y.exclude
   */
  public String getStringValueOfVariant(int variant) {
    assert 0 <= variant && m_variantTypes[variant] == VARIANT_STRING;
    return m_strings[m_variants[variant]];
  }

  /**
   * Returns boolean value of a variant.
   * @param variant a variant of type VARIANT_BOOLEAN
   * @return boolean value
   * @y.exclude
   */
  public boolean getBooleanValueOfVariant(int variant) {
    assert 0 <= variant && m_variantTypes[variant] == VARIANT_BOOLEAN;
    assert m_variants[variant] == 0 || m_variants[variant] ==1;
    return m_variants[variant] == 1;
  }

  /**
   * Returns mantissa of a float variant.
   * @param variant a variant of type VARIANT_FLOAT
   * @return mantissa
   * @y.exclude
   */
  public long getMantissaOfFloatVariant(int variant) {
    assert 0 <= variant && m_variantTypes[variant] == VARIANT_FLOAT;
    return m_mantissas[m_variants[variant]];
  }

  /**
   * Returns exponent of a float variant.
   * @param variant a variant of type VARIANT_FLOAT
   * @return exponent
   * @y.exclude
   */
  public int getExponentOfFloatVariant(int variant) {
    assert 0 <= variant && m_variantTypes[variant] == VARIANT_FLOAT;
    return m_exponents[m_variants[variant]];
  }
  
  /**
   * Returns sign of a decimal variant.
   * @param variant a variant of type VARIANT_DECIMAL
   * @return sign
   * @y.exclude
   */
  public boolean getSignOfDecimalVariant(int variant) {
    assert 0 <= variant && m_variantTypes[variant] == VARIANT_DECIMAL;
    return m_signs[m_variants[variant]];
  }

  /**
   * Returns integral digits of a decimal variant.
   * @param variant a variant of type VARIANT_DECIMAL
   * @return integral digits
   * @y.exclude
   */
  public String getIntegralDigitsOfDecimalVariant(int variant) {
    assert 0 <= variant && m_variantTypes[variant] == VARIANT_DECIMAL;
    return m_integralDigits[m_variants[variant]];
  }

  /**
   * Returns reverse-fractional digits of a decimal variant.
   * @param variant a variant of type VARIANT_DECIMAL
   * @return reverse-fractional digits
   * @y.exclude
   */
  public String getReverseFractionalDigitsOfDecimalVariant(int variant) {
    assert 0 <= variant && m_variantTypes[variant] == VARIANT_DECIMAL;
    return m_reverseFractionalDigits[m_variants[variant]];
  }

  /**
   * Returns int value of a variant.
   * @param variant a variant of type VARIANT_INT
   * @return int value
   * @y.exclude
   */
  public int getIntValueOfVariant(int variant) {
    assert 0 <= variant && m_variantTypes[variant] == VARIANT_INT;
    return m_ints[m_variants[variant]];
  }

  /**
   * Returns long value of a variant.
   * @param variant a variant of type VARIANT_LONG
   * @return long value
   * @y.exclude
   */
  public long getLongValueOfVariant(int variant) {
    assert 0 <= variant && m_variantTypes[variant] == VARIANT_LONG;
    return m_longs[m_variants[variant]];
  }

  /**
   * Returns BigInteger value of a variant.
   * @param variant a variant of type VARIANT_INTEGER
   * @return BigInteger value
   * @y.exclude
   */
  public BigInteger getIntegerValueOfVariant(int variant) {
    assert 0 <= variant && m_variantTypes[variant] == VARIANT_INTEGER;
    return m_integers[m_variants[variant]];
  }

  /**
   * Returns datetime value of a variant.
   * @param variant a variant of type VARIANT_DATETIME
   * @return datetime value as XSDateTime
   * @y.exclude
   */
  public XSDateTime getDateTimeValueOfVariant(int variant) {
    assert 0 <= variant && m_variantTypes[variant] == VARIANT_DATETIME;
    return m_datetimes[m_variants[variant]];
  }

  /**
   * Returns a computed datetime value of a variant. (for enumeration handling)
   * @param variant a variant of type VARIANT_DATETIME
   * @return datetime value as XSDateTime
   * @y.exclude
   */
  public XSDateTime getComputedDateTimeValueOfVariant(int variant) {
    assert 0 <= variant && m_variantTypes[variant] == VARIANT_DATETIME;
    return m_computedDatetimes[m_variants[variant]];
  }

  /**
   * Returns duration value of a variant.
   * @param variant a variant of type VARIANT_DURATION
   * @return duration value as XSDuration
   * @y.exclude
   */
  public Duration getDurationValueOfVariant(int variant) {
    assert 0 <= variant && m_variantTypes[variant] == VARIANT_DURATION;
    return m_durations[m_variants[variant]];
  }

  /**
   * Returns binary value of a variant.
   * @param variant a variant of type VARIANT_BINARY
   * @return duration value as byte array
   * @y.exclude
   */
  public byte[] getBinaryValueOfVariant(int variant) {
    final int variantType = m_variantTypes[variant];
    assert 0 <= variant && (variantType == VARIANT_BASE64 || variantType == VARIANT_HEXBIN);
    return m_binaries[m_variants[variant]];
  }

  ///////////////////////////////////////////////////////////////////////////
  // Serialization/Deserialization
  ///////////////////////////////////////////////////////////////////////////

 /**
  * Reads an EXI Schema from a DataInputStream.
  * @param in DataInputStream containing a serialized EXISchema
  * @return {@link org.openexi.schema.EXISchema}
  * @throws IOException, ClassNotFoundException
  */
  public static EXISchema readIn(DataInputStream in) throws IOException, ClassNotFoundException {
    int i, len;

    for (i = 0; i < COOKIE.length; i++) {
      if ((byte)in.read() != COOKIE[i]) {
        throw new StreamCorruptedException("The stream starts with a wrong magic cookie.");
      }
    }

    len = in.readInt();
    int[] nodes = new int[len];
    for (i = 0; i < len; i++)
      nodes[i] = in.readInt();

    len = in.readInt();
    int[] attrs = new int[len];
    for (i = 0; i < len; i++)
      attrs[i] = in.readInt();

    len = in.readInt();
    int[] types = new int[len];
    for (i = 0; i < len; i++)
      types[i] = in.readInt();

    len = in.readInt();
    String[] uris = new String[len + 4];
    uris[0] = "";
    uris[1] = "http://www.w3.org/XML/1998/namespace";
    uris[2] = "http://www.w3.org/2001/XMLSchema-instance";
    uris[3] = "http://www.w3.org/2001/XMLSchema";
    for (i = 4; i < len + 4; i++)
      uris[i] = readString(in); 
    
    len = in.readInt();
    String[] names = new String[EXISchemaConst.N_BUILTIN_LOCAL_NAMES + len];
    i = 0;
    names[i++] = "";
    for (int n = 0; n < EXISchemaConst.XML_LOCALNAMES.length; n++) {
      names[i++] = EXISchemaConst.XML_LOCALNAMES[n];
    }
    for (int n = 0; n < EXISchemaConst.XSI_LOCALNAMES.length; n++) {
      names[i++] = EXISchemaConst.XSI_LOCALNAMES[n];
    }
    for (int n = 0; n < EXISchemaConst.XSD_LOCALNAMES.length; n++) {
      names[i++] = EXISchemaConst.XSD_LOCALNAMES[n];
    }
    assert i == EXISchemaConst.N_BUILTIN_LOCAL_NAMES;
    for (; i < names.length; i++)
      names[i] = readString(in);

    len = in.readInt();
    int[][] localNames = new int[len][];
    for (i = 0; i < len; i++) {
      localNames[i] = new int[in.readInt()];
      for (int j = 0; j < localNames[i].length; j++) {
        localNames[i][j] = in.readInt();
      }
    }
    
    len = in.readInt();
    String[] strings = new String[len];
    for (i = 0; i < len; i++)
      strings[i] = readString(in);

    len = in.readInt();
    int[] ints = new int[len];
    for (i = 0; i < len; i++)
      ints[i] = in.readInt();

    len = in.readInt();
    long[] mantissas = new long[len];
    int[] exponents = new int[len];
    for (i = 0; i < len; i++) {
      mantissas[i] = in.readLong();
      exponents[i] = in.readInt();
    }

    len = in.readInt();
    boolean[] signs = new boolean[len];
    String[] integralDigits = new String[len];
    String[] reverseFractionalDigits = new String[len];
    for (i = 0; i < len; i++) {
      signs[i] = in.readBoolean();
      integralDigits[i] = readString(in);
      reverseFractionalDigits[i] = readString(in);
    }
    
    len = in.readInt();
    BigInteger[] integers = new BigInteger[len];
    for (i = 0; i < len; i++)
      integers[i] = new BigInteger(readString(in));
    
    len = in.readInt();
    long[] longs = new long[len];
    for (i = 0; i < len; i++)
      longs[i] = in.readLong(); 

    len = in.readInt();
    XSDateTime[] datetimes = new XSDateTime[len];
    for (i = 0; i < len; i++)
      datetimes[i] = XSDateTime.readIn(in);

    len = in.readInt();
    Duration[] durations = new Duration[len];
    for (i = 0; i < len; i++)
      durations[i] = datatypeFactory.newDuration(readString(in));

    len = in.readInt();
    byte[][] binaries = new byte[len][];
    for (i = 0; i < len; i++) {
      int n, n_bytes;
      binaries[i] = new byte[in.readInt()];
      for (n = 0; n < binaries[i].length; n += n_bytes) {
        if ((n_bytes = in.read(binaries[i], n, binaries[i].length - n)) < 0)
          break;
      }
    }

    len = in.readInt();
    byte[] variantTypes = new byte[len];
    for (i = 0; i < len; i++)
      variantTypes[i] = (byte)in.read(); 

    len = in.readInt();
    int[] variants = new int[len];
    for (i = 0; i < len; i++)
      variants[i] = in.readInt();

    int n_stypes = in.readInt();
    int grammarCount = in.readInt();

    len = in.readInt();
    int[] grammars = new int[len];
    for (i = 0; i < len; i++)
      grammars[i] = in.readInt();

    len = in.readInt();
    int[] productions = new int[len];
    for (i = 0; i < len; i++)
      productions[i] = in.readInt();

    len = in.readInt();
    byte[] eventTypes = new byte[len];
    
    int n, n_bytes;
    for (n = 0; n < len; n += n_bytes) {
      if ((n_bytes = in.read(eventTypes, n, eventTypes.length - n)) < 0)
        break;
    }
    int[] eventData = new int[len];
    for (i = 0; i < len; i++)
      eventData[i] = in.readInt();
    
    final EXISchema schema = new EXISchema();
    schema.m_elems = nodes;
    schema.m_attrs = attrs;
    schema.m_types = types;
    schema.uris = uris;
    schema.m_names = names;
    schema.m_localNames = localNames;
    schema.m_strings = strings;
    schema.m_ints = ints;
    schema.m_mantissas = mantissas;
    schema.m_exponents = exponents;
    schema.m_signs = signs;
    schema.m_integralDigits = integralDigits; 
    schema.m_reverseFractionalDigits = reverseFractionalDigits;
    schema.m_integers = integers;
    schema.m_longs = longs;
    schema.m_datetimes = datetimes;
    schema.m_durations = durations;
    schema.m_binaries = binaries;
    schema.m_variantTypes = variantTypes;
    schema.m_variants = variants;
    schema.m_grammars = grammars;
    schema.m_grammarCount = grammarCount;
    schema.m_productions = productions;
    schema.m_eventTypes = eventTypes;
    schema.m_eventData = eventData;
    schema.m_n_stypes = n_stypes;
  
    schema.setUp();
    return schema;
  }
  
  /**
   * Writes out a serialized EXISchema.
   * @param out DataOutputStream to receive the serialized EXISchema
   * @throws IOException
   */
  public void writeOut(DataOutputStream out) throws IOException {
    out.write(COOKIE);

    int i, len;

    len = m_elems.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeInt(m_elems[i]);
    
    len = m_attrs.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeInt(m_attrs[i]);
          
    len = m_types.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeInt(m_types[i]);

    len = uris.length;
    assert len >= 4;
    out.writeInt(len - 4);
    for (i = 4; i < len; i++)
      writeString(uris[i], out);
    
    len = m_names.length;
    assert len >= EXISchemaConst.N_BUILTIN_LOCAL_NAMES;
    out.writeInt(len - EXISchemaConst.N_BUILTIN_LOCAL_NAMES);
    for (i = EXISchemaConst.N_BUILTIN_LOCAL_NAMES; i < len; i++)
      writeString(m_names[i], out);

    len = m_localNames.length;
    out.writeInt(len);
    for (i = 0; i < len; i++) {
      out.writeInt(m_localNames[i].length);
      for (int j = 0; j < m_localNames[i].length; j++) {
        out.writeInt(m_localNames[i][j]);
      }
    }
    
    len = m_strings.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      writeString(m_strings[i], out);

    len = m_ints.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeInt(m_ints[i]);
    
    assert m_mantissas.length == m_exponents.length;
    len = m_mantissas.length;
    out.writeInt(len);
    for (i = 0; i < len; i++) {
      out.writeLong(m_mantissas[i]);
      out.writeInt(m_exponents[i]);
    }
    
    assert m_signs.length == m_integralDigits.length && m_integralDigits.length == m_reverseFractionalDigits.length;
    len = m_signs.length;
    out.writeInt(len);
    for (i = 0; i < len; i++) {
      out.writeBoolean(m_signs[i]);
      writeString(m_integralDigits[i], out);
      writeString(m_reverseFractionalDigits[i], out);
    }
    
    len = m_integers.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      writeString(m_integers[i].toString(), out);

    len = m_longs.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeLong(m_longs[i]);

    len = m_datetimes.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      m_datetimes[i].writeOut(out);

    len = m_durations.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      writeString(m_durations[i].toString(), out);

    len = m_binaries.length;
    out.writeInt(len);
    for (i = 0; i < len; i++) {
      out.writeInt(m_binaries[i].length);
      out.write(m_binaries[i]);
    }

    len = m_variantTypes.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.write(m_variantTypes[i]);

    len = m_variants.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeInt(m_variants[i]);

    out.writeInt(m_n_stypes);
    out.writeInt(m_grammarCount);

    len = m_grammars.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeInt(m_grammars[i]);
    
    len = m_productions.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeInt(m_productions[i]);

    len = m_eventTypes.length;
    out.writeInt(len);
    out.write(m_eventTypes);
    for (i = 0; i < len; i++)
      out.writeInt(m_eventData[i]);
  }
  
  static void writeString(String s, DataOutputStream out) throws IOException {
    final int len = s.length();
    out.writeShort(len);
    for (int i = 0; i < len; i++) {
      out.writeChar(s.charAt(i));
    }
  }
  
  static String readString(DataInputStream in) throws IOException {
    final int len = in.readShort();
    StringBuilder stringBuider = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      stringBuider.append(in.readChar());
    }
    return stringBuider.toString();
  }
  
  public void writeXml(OutputStream out, boolean whole) throws IOException {
    OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
    writer.write("<EXIGrammar xmlns=\"urn:publicid:nagasena\">");
    writeOutStringTable(writer);
    writeTypes(writer, whole);
    writeElems(writer);
    writeAttrs(writer);
    writeGrammars(writer);
    writer.write("</EXIGrammar>");
    writer.flush();
  }
  
  private void writeOutStringTable(OutputStreamWriter writer) throws IOException {
    writer.write("<StringTable>");
    int i = 0;
    assert 4 <= uris.length;
    String[] names;
    names = localNames[i++]; // NoNamespace
    if (names.length != 0) {
      writer.write("<NoNamespace>");
      for (int j = 0; j < names.length; j++) {
        writer.write("<Name>" + names[j] + "</Name>");
      }
      writer.write("</NoNamespace>");
    }
    names = localNames[i++]; // xml namespace
    if (names.length > 4) {
      writer.write("<XmlNamespace>");
      for (int j = 0; j < names.length; j++) {
        writer.write("<Name>" + names[j] + "</Name>");
      }
      writer.write("</XmlNamespace>");
    }
    names = localNames[i++]; // xsi namespace
    if (names.length > 2) {
      writer.write("<XsiNamespace>");
      for (int j = 0; j < names.length; j++) {
        writer.write("<Name>" + names[j] + "</Name>");
      }
      writer.write("</XsiNamespace>");
    }
    names = localNames[i++]; // xsd namespace
    if (names.length > 46) {
      writer.write("<XsdNamespace>");
      for (int j = 0; j < names.length; j++) {
        writer.write("<Name>" + names[j] + "</Name>");
      }
      writer.write("</XsdNamespace>");
    }
    assert i == 4;
    for (; i < uris.length; i++) {
      names = localNames[i];
      writer.write("<Namespace>");
      writer.write("<Uri>" + uris[i] + "</Uri>");
      for (int j = 0; j < names.length; j++) {
        writer.write("<Name>" + names[j] + "</Name>");
      }
      writer.write("</Namespace>");
    }
    writer.write("</StringTable>");
  }
  
  private void writeTypes(OutputStreamWriter writer, boolean whole) throws IOException {
    writer.write("<Types>");
    if (whole)
      writeAnyType(writer);
    int tp = EXISchemaLayout.SZ_COMPLEX_TYPE;
    int serial = EXISchemaConst.ANY_SIMPLE_TYPE;
    for (; tp < m_types.length; tp += _getTypeSize(tp, m_types, ancestryIds), ++serial) {
      if (!whole && serial < EXISchemaConst.N_BUILTIN_TYPES) {
        if (!DEFAULT_TYPABLES[serial] && isTypableType(tp)) {
          writer.write("<MakeTypable>" + serial + "</MakeTypable>");
        }
        continue;
      }
      if (isSimpleType(tp))
        writeSimpleType(tp, writer);
      else
        writeComplexType(tp, writer);
    }
    writer.write("</Types>");
  }

  private void writeAnyType(OutputStreamWriter writer) throws IOException {
    writer.write("<AnyType>");
    writer.write("<Typable/>");
    final int gram = getGrammarOfType(0); 
    writer.write("<Grammar>" + getSerialOfGrammar(gram) + "</Grammar>");
    writer.write("</AnyType>");
  }
  
  private void writeTypeCommon(int tp, OutputStreamWriter writer) throws IOException {
    final String localName;
    if ((localName = getNameOfType(tp)).length() != 0) {
      writer.write("<Uri>" + uris[getUriOfType(tp)] + "</Uri>");
      writer.write("<Name>" + localName + "</Name>");
    }
    if (isTypableType(tp)) writer.write("<Typable/>");
    final int gram = getGrammarOfType(tp); 
    writer.write("<Grammar>" + getSerialOfGrammar(gram) + "</Grammar>");
  }

  private void writeComplexType(int tp, OutputStreamWriter writer) throws IOException {
    if (tp != 0) writer.write("<ComplexType>");
    writeTypeCommon(tp, writer);
    // The rest is the part specific to complex types
    final int contentDatatype = getContentDatatypeOfComplexType(tp);
    if (contentDatatype != NIL_NODE)
      writer.write("<ContentDatatype>" + getSerialOfType(contentDatatype) +  "</ContentDatatype>");
    if (tp != 0) writer.write("</ComplexType>");
  }

  private void writeSimpleType(int tp, OutputStreamWriter writer) throws IOException {
    final byte variety = getVarietyOfSimpleType(tp);
    if (variety == UR_SIMPLE_TYPE) {
      writer.write("<AnySimpleType>");
      writer.write("<Typable/>");
      final int gram = getGrammarOfType(tp); 
      writer.write("<Grammar>" + getSerialOfGrammar(gram) + "</Grammar>");
      writer.write("</AnySimpleType>");
      return;
    }
    int serial = -1;
    final String elementName;
    if (variety == LIST_SIMPLE_TYPE) {
      elementName = "ListType";
    }
    else if (variety == UNION_SIMPLE_TYPE) {
      elementName = "UnionType";
    }
    else {
      assert variety == ATOMIC_SIMPLE_TYPE;
      int _tp = tp;
      while ((serial = getSerialOfType(_tp)) >= EXISchemaConst.N_PRIMITIVE_TYPES_PLUS_INTEGER) {
        _tp = getBaseTypeOfSimpleType(_tp);
      }
      assert serial >= 2 && serial <= EXISchemaConst.INTEGER_TYPE; // 0 is xsd:anyType, and 1 is xsd:anySimpleType
      elementName = ELEMENT_NAMES[serial];
    }
    writer.write("<" + elementName + ">");
    writeTypeCommon(tp, writer);
    // The rest is the part specific to simple types
    writer.write("<BaseType>" + getSerialOfType(getBaseTypeOfSimpleType(tp)) + "</BaseType>");
    if (variety == LIST_SIMPLE_TYPE) {
      final int itemType = getItemTypeOfListSimpleType(tp);
      writer.write("<ItemType>" + getSerialOfType(itemType) + "</ItemType>");
    }
    else if (variety == UNION_SIMPLE_TYPE) {
    }
    else if (variety == UR_SIMPLE_TYPE) {
    }
    else {
      assert variety == ATOMIC_SIMPLE_TYPE;
      if (serial == EXISchemaConst.STRING_TYPE) {
        final int whiteSpace = getWhitespaceFacetValueOfStringSimpleType(tp);
        if (whiteSpace == WHITESPACE_REPLACE) {
          writer.write("<Replace/>");
        }
        else if (whiteSpace == WHITESPACE_COLLAPSE) {
          writer.write("<Collapse/>");
        }
        final int rcsCount = getRestrictedCharacterCountOfStringSimpleType(tp);
        if (rcsCount != 0) {
          writer.write("<RestrictedCharset>");
          final int rcs = getRestrictedCharacterOfSimpleType(tp);
          int prevChar = m_types[rcs + 0];
          int startChar = prevChar;
          for (int i = 1; i < rcsCount; i++) {
            final int ch = m_types[rcs + i];
            if (prevChar + 1 == ch) {
              prevChar = ch;
            }
            else {
              assert prevChar + 1 < ch;
              if (startChar == prevChar)
                writer.write("<Char>" + prevChar + "</Char>");
              else {
                assert startChar < prevChar;
                writer.write("<StartChar>" + startChar + "</StartChar>");
                writer.write("<EndChar>" + prevChar + "</EndChar>");
              }
              startChar = prevChar = ch;
            }
          }
          if (startChar == prevChar)
            writer.write("<Char>" + prevChar + "</Char>");
          else {
            assert startChar < prevChar;
            writer.write("<StartChar>" + startChar + "</StartChar>");
            writer.write("<EndChar>" + prevChar + "</EndChar>");
          }
          writer.write("</RestrictedCharset>");
        }
        writeEnumerations(tp, "String", writer);
      }
      else if (serial == EXISchemaConst.BOOLEAN_TYPE) {
        if (isPatternedBooleanSimpleType(tp)) {
          writer.write("<Patterned/>");
        }
      }
      else if (serial == EXISchemaConst.DECIMAL_TYPE) {
        writeEnumerations(tp, "Decimal", writer);
      }
      else if (serial == EXISchemaConst.FLOAT_TYPE || serial == EXISchemaConst.DOUBLE_TYPE) {
        writeEnumerations(tp, "Float", writer);
      }
      else if (serial == EXISchemaConst.DURATION_TYPE) {
        writeEnumerations(tp, "Duration", writer);
      }
      else if (serial == EXISchemaConst.DATETIME_TYPE) {
        writeEnumerations(tp, "DateTime", writer);
      }
      else if (serial == EXISchemaConst.TIME_TYPE) {
        writeEnumerations(tp, "Time", writer);
      }
      else if (serial == EXISchemaConst.DATE_TYPE) {
        writeEnumerations(tp, "Date", writer);
      }
      else if (serial == EXISchemaConst.G_YEARMONTH_TYPE) {
        writeEnumerations(tp, "GYearMonth", writer);
      }
      else if (serial == EXISchemaConst.G_YEAR_TYPE) {
        writeEnumerations(tp, "GYear", writer);
      }
      else if (serial == EXISchemaConst.G_MONTHDAY_TYPE) {
        writeEnumerations(tp, "GMonthDay", writer);
      }
      else if (serial == EXISchemaConst.G_DAY_TYPE) {
        writeEnumerations(tp, "GDay", writer);
      }
      else if (serial == EXISchemaConst.G_MONTH_TYPE) {
        writeEnumerations(tp, "GMonth", writer);
      }
      else if (serial == EXISchemaConst.HEXBINARY_TYPE) {
        writeEnumerations(tp, "HexBinary", writer);
      }
      else if (serial == EXISchemaConst.BASE64BINARY_TYPE) {
        writeEnumerations(tp, "Base64Binary", writer);
      }
      else if (serial == EXISchemaConst.ANYURI_TYPE) {
        writeEnumerations(tp, "String", writer);
      }
      else if (serial == EXISchemaConst.QNAME_TYPE) {
      }
      else if (serial == EXISchemaConst.NOTATION_TYPE) {
      }
      else if (serial == EXISchemaConst.INTEGER_TYPE) {
        final int width = getWidthOfIntegralSimpleType(tp);
        if (width == INTEGER_CODEC_NONNEGATIVE) {
          writer.write("<NonNegative/>");
        }
        else if (width != INTEGER_CODEC_DEFAULT) {
          assert 0 <= width && width <= 12;
          writer.write("<NBit>" + width + "</NBit>");
          final int variant = getMinInclusiveFacetOfIntegerSimpleType(tp);
          writer.write("<MinInteger>" + getVariantCharacters(variant).makeString() + "</MinInteger>");
        }
        writeEnumerations(tp, "Integer", writer);
      }
      else {
        assert false;
      }
    }
    writer.write("</" + elementName + ">");
  }
  
  private void writeEnumerations(int tp, String valueTagName, OutputStreamWriter writer) throws IOException {
    if (isEnumeratedAtomicSimpleType(tp)) {
      int n_enums = 0;
      final int enumCount = getEnumerationFacetCountOfAtomicSimpleType(tp);
      for (int i = 0; i < enumCount; i++) {
        final int variant;
        if ((variant = getEnumerationFacetOfAtomicSimpleType(tp, i)) != EXISchema.NIL_VALUE) {
          if (n_enums++ == 0)
            writer.write("<Enumeration>");
          writer.write("<" + valueTagName + ">");
          String stringValue = getVariantCharacters(variant).makeString();
          if ("String".equals(valueTagName)) {
            final StringBuilder stringBuilder = new StringBuilder();
            final int len = stringValue.length(); 
            for (int j = 0; j < len; j++) {
              final char c;
              switch (c = stringValue.charAt(j)) {
                case 0x0A:
                  stringBuilder.append("&#xA;");
                  break;
                case 0x0D:
                  stringBuilder.append("&#xD;");
                  break;
                case 0x9:
                  stringBuilder.append("&#x9;");
                  break;
                default:
                  stringBuilder.append(c);
                  break;
              }
            }
            stringValue = stringBuilder.toString();
          }
          writer.write(stringValue);
          writer.write("</" + valueTagName + ">");
        }
      }
      if (n_enums != 0)
        writer.write("</Enumeration>");
    }
  }
  
  private void writeElems(OutputStreamWriter writer) throws IOException {
    writer.write("<Elements>");
    int lastUri, lastName;
    lastUri = lastName = -1;
    for (int elem = 0; elem < m_elems.length; elem += EXISchemaLayout.SZ_ELEM) {
      final int uri = getUriOfElem(elem);
      final int name = getLocalNameOfElem(elem);
      if (uri != lastUri || name != lastName) {
        writer.write("<Uri>" + uris[uri] + "</Uri>");
        writer.write("<Name>" + m_names[m_localNames[uri][name]] + "</Name>");
        lastUri = uri;
        lastName = name;
      }
      final boolean isGlobal = isGlobalElem(elem); 
      if (isGlobal)
        writer.write("<GlobalElement>");
      else
        writer.write("<LocalElement>");
      writer.write("<Type>" + getSerialOfType(getTypeOfElem(elem)) + "</Type>");
      if (isNillableElement(elem)) {
        writer.write("<Nillable/>");
      }
      if (isGlobal)
        writer.write("</GlobalElement>");
      else
        writer.write("</LocalElement>");
    }
    writer.write("</Elements>");
  }
  
  private void writeAttrs(OutputStreamWriter writer) throws IOException {
    writer.write("<Attributes>");
    int lastUri, lastName;
    lastUri = lastName = -1;
    for (int attr = 0; attr < m_attrs.length; attr += EXISchemaLayout.SZ_ATTR) {
      final int uri = getUriOfAttr(attr);
      final int name = getLocalNameOfAttr(attr);
      if (uri != lastUri || name != lastName) {
        writer.write("<Uri>" + uris[uri] + "</Uri>");
        writer.write("<Name>" + m_names[m_localNames[uri][name]] + "</Name>");
        lastUri = uri;
        lastName = name;
      }
      final boolean isGlobal = isGlobalAttr(attr); 
      if (isGlobal)
        writer.write("<GlobalAttribute>");
      else
        writer.write("<LocalAttribute>");
      writer.write("<Type>" + getSerialOfType(getTypeOfAttr(attr)) + "</Type>");
      if (isGlobal)
        writer.write("</GlobalAttribute>");
      else
        writer.write("</LocalAttribute>");
    }
    writer.write("</Attributes>");
  }
  
  private void writeGrammars(OutputStreamWriter writer) throws IOException {
    writer.write("<Grammars>");
    int gram, serial;
    for (gram = 0, serial = 0; gram < m_grammars.length; gram += getSizeOfGrammar(gram, m_grammars), ++serial) {
      if (serial < 7) // Grammars at indices 0 through 6 are fixtures.
        continue;
      writer.write("<!-- Grammar# " + serial + " -->");
      writer.write("<Grammar>");
      writer.write("<Productions>");
      boolean needEndElem = hasEndElement(gram);
      final int n_productions = getProductionCountOfGrammar(gram);
      for (int i = 0; i < n_productions; i++) {
        final int prod = getProductionOfGrammar(gram, i);
        final int event = getEventOfProduction(prod);
        switch (event) {
          case EXISchema.EVENT_AT_WILDCARD:
            writer.write("<AttributeWildcard/>");
            break;
          case EXISchema.EVENT_SE_WILDCARD:
            writer.write("<ElementWildcard/>");
            break;
          case EXISchema.EVENT_CH_UNTYPED:
          case EXISchema.EVENT_CH_TYPED:
            if (hasEndElement(gram)) {
              writer.write("<EndElement/>");
              needEndElem = false;
            }
            if (event == EXISchema.EVENT_CH_UNTYPED)
              writer.write("<CharactersMixed/>");
            else
              writer.write("<CharactersTyped/>");
            break;
          default:
            switch (getEventType(event)) {
              case EVENT_TYPE_AT:
                final int attr = getNodeOfEventType(event);
                writer.write("<Attribute>" + attr/EXISchemaLayout.SZ_ATTR + "</Attribute>");
                break;
              case EVENT_TYPE_SE:
                final int elem = getNodeOfEventType(event);
                writer.write("<Element>" + getSerialOfElem(elem) + "</Element>");
                break;
              case EVENT_TYPE_AT_WILDCARD_NS:
                writer.write("<AttributeWildcardNS>" + uris[getUriOfEventType(event)] + "</AttributeWildcardNS>");
                break;
              case EVENT_TYPE_SE_WILDCARD_NS:
                writer.write("<ElementWildcardNS>" + uris[getUriOfEventType(event)] + "</ElementWildcardNS>");
                break;
              default:
                assert false;
                break;
            }
            break;
        }
        writer.write("<Grammar>" + getSerialOfGrammar(getGrammarOfProduction(prod)) + "</Grammar>");
      }
      if (needEndElem)
        writer.write("<EndElement/>");
      writer.write("</Productions>");
      if (hasContentGrammar(gram))
        writer.write("<ContentGrammar>" + getSerialOfGrammar(getContentGrammarOfGrammar(gram)) + "</ContentGrammar>");  
      if (hasEmptyGrammar(gram))
        writer.write("<EmptyGrammar>" + getSerialOfGrammar(getTypeEmptyGrammarOfGrammar(gram)) + "</EmptyGrammar>");  
      writer.write("</Grammar>");
    }
    writer.write("</Grammars>");
  }

}
