using System;
using System.Diagnostics;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Numerics;
using System.Text;
using XmlConvert = System.Xml.XmlConvert;

namespace Nagasena.Schema {

  /// <summary>
  /// EXISchema provides methods to read and write compiled EXI schemas.
  /// </summary>
  public sealed class EXISchema {

    private static readonly sbyte[] COOKIE = new sbyte[] { 36, 51, 43, 45 }; // "$", "3", "+", "-"

    /// <summary>
    /// @y.exclude </summary>
    public const int NIL_NODE = -1;
    /// <summary>
    /// @y.exclude </summary>
    public const int NIL_VALUE = -1;
    /// <summary>
    /// @y.exclude </summary>
    public const int EMPTY_STRING = 0;
    // REVISIT: remove
    /// <summary>
    /// @y.exclude </summary>
    public const int NIL_GRAM = -1;

    // Event id
    /// <summary>
    /// @y.exclude </summary>
    public const int EVENT_AT_WILDCARD = -1;
    /// <summary>
    /// @y.exclude </summary>
    public const int EVENT_SE_WILDCARD = -2;
    /// <summary>
    /// @y.exclude </summary>
    public const int EVENT_CH_UNTYPED = -3;
    /// <summary>
    /// @y.exclude </summary>
    public const int EVENT_CH_TYPED = -4;
    /// <summary>
    /// @y.exclude </summary>
    public const int MIN_EVENT_ID = EVENT_CH_TYPED;

    // EventType
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte EVENT_TYPE_AT = 0;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte EVENT_TYPE_SE = 1;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte EVENT_TYPE_AT_WILDCARD_NS = 2;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte EVENT_TYPE_SE_WILDCARD_NS = 3;

    /// <summary>
    /// @y.exclude </summary>
    public const int TRUE_VALUE = 1;
    /// <summary>
    /// @y.exclude </summary>
    public const int FALSE_VALUE = 0;

    /// <summary>
    /// @y.exclude </summary>
    public const int UNBOUNDED_OCCURS = -1;

    /// <summary>
    /// @y.exclude </summary>
    public const int CONSTRAINT_NONE = 0;
    /// <summary>
    /// @y.exclude </summary>
    public const int CONSTRAINT_DEFAULT = 1;
    /// <summary>
    /// @y.exclude </summary>
    public const int CONSTRAINT_FIXED = 2;

    /// <summary>
    /// @y.exclude </summary>
    public const int WHITESPACE_PRESERVE = 0;
    /// <summary>
    /// @y.exclude </summary>
    public const int WHITESPACE_REPLACE = 1;
    /// <summary>
    /// @y.exclude </summary>
    public const int WHITESPACE_COLLAPSE = 2;

    /// <summary>
    /// @y.exclude </summary>
    public const sbyte VARIANT_STRING = 0;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte VARIANT_FLOAT = 1;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte VARIANT_DECIMAL = 2;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte VARIANT_INTEGER = 3;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte VARIANT_INT = 4;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte VARIANT_LONG = 5;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte VARIANT_DATETIME = 6;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte VARIANT_DURATION = 7;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte VARIANT_BASE64 = 8;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte VARIANT_BOOLEAN = 9;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte VARIANT_HEXBIN = 10;

    /// <summary>
    /// Default, unconstrained integer representation
    /// @y.exclude
    /// </summary>
    public const int INTEGER_CODEC_DEFAULT = 0xFF;

    /// <summary>
    /// Non-negative integer representation
    /// @y.exclude
    /// </summary>
    public const int INTEGER_CODEC_NONNEGATIVE = 0xFE;

    /// <summary>
    /// Atomic built-in type ID (defined in EXISchemaConst) -> Ancestry ID
    /// </summary>
    private static readonly sbyte[] ANCESTRY_IDS = new sbyte[] { 
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

    private static readonly string[] ELEMENT_NAMES = new string[] { 
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

    private static readonly bool[] DEFAULT_TYPABLES;
    static EXISchema() {
      DEFAULT_TYPABLES = new bool[] { 
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
      Debug.Assert(DEFAULT_TYPABLES.Length == EXISchemaConst.N_BUILTIN_TYPES);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Public value enumerations
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// @y.exclude </summary>
    public const sbyte UR_SIMPLE_TYPE = 0;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte ATOMIC_SIMPLE_TYPE = 1;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte LIST_SIMPLE_TYPE = 2;
    /// <summary>
    /// @y.exclude </summary>
    public const sbyte UNION_SIMPLE_TYPE = 3;

    private int[] m_elems; // int array describing elements
    private int[] m_attrs; // int array describing attributes
    private int[] m_types; // int array describing types
    /// <summary>
    /// @y.exclude </summary>
    public string[] uris; // array of interned strings representing uris
    /// <summary>
    /// @y.exclude </summary>
    public string[][] localNames;
    private int[][] m_localNames;
    internal string[] m_names; // array of interned strings
    internal string[] m_strings; // array of non-interned strings
    internal int[] m_ints; // array of int values
    internal long[] m_mantissas; // array of long representing mantissas
    internal int[] m_exponents; // array of int representing exponents
    internal bool[] m_signs; // array of decimal value signs
    internal string[] m_integralDigits; // array of decimal integral value
    internal string[] m_reverseFractionalDigits; // array of decimal reverse-fractional digits value
    internal BigInteger[] m_integers; // array of integer values
    internal long[] m_longs; // array of long values
    internal XSDateTime[] m_datetimes; // array of datetime values
    internal TimeSpan[] m_durations; // array of duration values
    internal byte[][] m_binaries; // array of binary values
    internal sbyte[] m_variantTypes; // array of variant types
    internal int[] m_variants; // array of variant values

    [NonSerialized]
    private XSDateTime[] m_computedDatetimes; // array of datetime values for enumeration matching
    [NonSerialized]
    private Characters[] m_variantCharacters; // array of variant's Characters

    private int m_n_stypes; // grand total number of simple type nodes (both global and local)
    [NonSerialized]
    public sbyte[] ancestryIds; // [0 ... m_n_stypes] where non-atomic entries are filled with EXISchemaConst.UNTYPED.
    [NonSerialized]
    private int m_stypes_end; // tp is a simple type if tp < m_stypes_end except for xsd:anyType

    private int[] m_grammars; // int array describing grammar structure
    private int[] m_productions; // int array containing productions
    private sbyte[] m_eventTypes; // byte array of event types
    private int[] m_eventData; // int array of event data (i.e. node or uri)

    private int m_grammarCount; // the number of grammars in m_grammars;

    [NonSerialized]
    private int[] m_fragmentINodes;
    [NonSerialized]
    private int m_n_fragmentElems;

    [NonSerialized]
    private Dictionary<string, int[]> m_globalElementsDirectory;
    [NonSerialized]
    private Dictionary<string, int[]> m_globalAttributesDirectory;
    [NonSerialized]
    private Dictionary<string, int[]> m_globalTypesDirectory;
    [NonSerialized]
    private int[] m_buitinTypes;
    [NonSerialized]
    private int[] m_globalElems;
    [NonSerialized]
    private int[] m_globalAttrs;

    private EXISchema() {
    }

    /// <summary>
    /// @y.exclude
    /// </summary>
    internal EXISchema(int[] nodes, int n_nodes, int[] attrs, int n_attrs, int[] types, int n_types, 
      string[] uris, int n_uris, string[] names, int n_names, int[][] localNames, string[] strings, int n_strings, 
      int[] ints, int n_ints, long[] mantissas, int[] exponents, int n_floats, 
      bool[] signs, string[] integralDigits, string[] reverseFractionalDigits, 
      int n_decimals, BigInteger[] integers, int n_integers, long[] longs, int n_longs,
      XSDateTime[] datetimes, int n_datetimes, TimeSpan[] durations, int n_durations, 
      byte[][] binaries, int n_binaries, sbyte[] variantTypes, int[] variants, int n_variants, 
      int[] grammars, int n_grammars, int grammarCount, int[] productions, int n_productions, 
      sbyte[] eventTypes, int[] eventData, int n_events, int n_stypes) {

      m_elems = new int[n_nodes];
      Array.Copy(nodes, 0, m_elems, 0, n_nodes);

      m_attrs = new int[n_attrs];
      Array.Copy(attrs, 0, m_attrs, 0, n_attrs);

      m_types = new int[n_types];
      Array.Copy(types, 0, m_types, 0, n_types);

      this.uris = new string[n_uris];
      Array.Copy(uris, 0, this.uris, 0, n_uris);

      m_localNames = new int[localNames.Length][];
      for (int i = 0; i < localNames.Length; i++) {
        m_localNames[i] = new int[localNames[i].Length];
        Array.Copy(localNames[i], 0, m_localNames[i], 0, localNames[i].Length);
      }

      m_names = new string[n_names];
      Array.Copy(names, 0, m_names, 0, n_names);

      m_strings = new string[n_strings];
      Array.Copy(strings, 0, m_strings, 0, n_strings);

      m_ints = new int[n_ints];
      Array.Copy(ints, 0, m_ints, 0, n_ints);

      m_mantissas = new long[n_floats];
      Array.Copy(mantissas, 0, m_mantissas, 0, n_floats);

      m_exponents = new int[n_floats];
      Array.Copy(exponents, 0, m_exponents, 0, n_floats);

      m_signs = new bool[n_decimals];
      Array.Copy(signs, 0, m_signs, 0, n_decimals);

      m_integralDigits = new string[n_decimals];
      Array.Copy(integralDigits, 0, m_integralDigits, 0, n_decimals);

      m_reverseFractionalDigits = new string[n_decimals];
      Array.Copy(reverseFractionalDigits, 0, m_reverseFractionalDigits, 0, n_decimals);

      m_integers = new BigInteger[n_integers];
      Array.Copy(integers, 0, m_integers, 0, n_integers);

      m_longs = new long[n_longs];
      Array.Copy(longs, 0, m_longs, 0, n_longs);

      m_datetimes = new XSDateTime[n_datetimes];
      Array.Copy(datetimes, 0, m_datetimes, 0, n_datetimes);

      m_durations = new TimeSpan[n_durations];
      Array.Copy(durations, 0, m_durations, 0, n_durations);

      m_binaries = new byte[n_binaries][];
      Array.Copy(binaries, 0, m_binaries, 0, n_binaries);

      m_variants = new int[n_variants];
      Array.Copy(variants, 0, m_variants, 0, n_variants);

      m_variantTypes = new sbyte[n_variants];
      Array.Copy(variantTypes, 0, m_variantTypes, 0, n_variants);

      m_grammars = new int[n_grammars];
      Array.Copy(grammars, 0, m_grammars, 0, n_grammars);

      m_productions = new int[n_productions];
      Array.Copy(productions, 0, m_productions, 0, n_productions);

      m_eventTypes = new sbyte[n_events];
      Array.Copy(eventTypes, 0, m_eventTypes, 0, n_events);

      m_eventData = new int[n_events];
      Array.Copy(eventData, 0, m_eventData, 0, n_events);

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
      ancestryIds = new sbyte[m_n_stypes + 1]; // the extra entry is for xsd:anyType albeit unused
      ancestryIds[0] = EXISchemaConst.UNTYPED; // 0 is xsd:anyType (unused. not an atomic simple type.)
      int tp = EXISchemaLayout.SZ_COMPLEX_TYPE;
      for (int n_stypes = 0; n_stypes < m_n_stypes; tp += EXISchema._getTypeSize(tp, m_types, ancestryIds)) {
        int serial = getSerialOfType(tp);
        Debug.Assert(0 < serial && serial <= m_n_stypes && isSimpleType(tp));
        ++n_stypes;
        sbyte ancestryId;
        if (getVarietyOfSimpleType(tp) == ATOMIC_SIMPLE_TYPE) {
          int _tp = tp;
          int _serial;
          while ((_serial = getSerialOfType(_tp)) >= EXISchemaConst.N_PRIMITIVE_TYPES_PLUS_INTEGER) {
            _tp = getBaseTypeOfSimpleType(_tp);
          }
          Debug.Assert(_serial >= 2); // 0 is xsd:anyType, and 1 is xsd:anySimpleType
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
      localNames = new string[m_localNames.Length][];
      for (int i = 0; i < m_localNames.Length; i++) {
        string[] _localNames = new string[m_localNames[i].Length];
        for (int j = 0; j < _localNames.Length; j++) {
          _localNames[j] = m_names[m_localNames[i][j]];
        }
        localNames[i] = _localNames;
      }
    }

    private void buildFragmentsArray() {
      int pos;
      int currentUri, currentName;
      int currentNode;
      bool isSpecific;

      IList<int?> elemDecls = new List<int?>();
      currentUri = currentName = -1;
      currentNode = int.MinValue; // an arbitrary value (i.e. any value is ok)
      isSpecific = true;
      for (pos = 0; pos < m_elems.Length; pos += EXISchemaLayout.SZ_ELEM) {
        int elem = pos;
        int uri = m_elems[elem + EXISchemaLayout.INODE_URI];
        int name = m_elems[elem + EXISchemaLayout.INODE_NAME];
        if (currentUri != uri || currentName != name) {
          if (pos != 0) {
            elemDecls.Add(isSpecific ? currentNode : (0 - currentNode) - 1);
          }
          currentNode = elem;
          isSpecific = true;
          currentUri = uri;
          currentName = name;
          continue;
        }
        Debug.Assert(currentNode != elem && uri == currentUri && name == currentName);
        if (getTypeOfElem(elem) != getTypeOfElem(currentNode) || isNillableElement(elem) != isNillableElement(currentNode)) {
          isSpecific = false;
        }
      }
      if (pos != 0) {
        elemDecls.Add(isSpecific ? currentNode : (0 - currentNode) - 1);
      }

      IList<int?> attrDecls = new List<int?>();
      currentUri = currentName = -1;
      currentNode = int.MinValue; // an arbitrary value (i.e. any value is ok)
      isSpecific = true;
      for (pos = 0; pos < m_attrs.Length; pos += EXISchemaLayout.SZ_ATTR) {
        int attr = pos;
        int uri = m_attrs[pos + EXISchemaLayout.INODE_URI];
        int name = m_attrs[pos + EXISchemaLayout.INODE_NAME];
        if (currentUri != uri || currentName != name) {
          if (pos != 0) {
            attrDecls.Add(isSpecific ? currentNode : (0 - currentNode) - 1);
          }
          currentNode = attr;
          isSpecific = true;
          currentUri = uri;
          currentName = name;
          continue;
        }
        Debug.Assert(currentNode != attr && uri == currentUri && name == currentName);
        if (getTypeOfAttr(attr) != getTypeOfAttr(currentNode)) {
          isSpecific = false;
        }
      }
      if (pos != 0) {
        attrDecls.Add(isSpecific ? currentNode : (0 - currentNode) - 1);
      }

      int i = 0;
      IEnumerator<int?> iterInteger;
      m_n_fragmentElems = elemDecls.Count;
      m_fragmentINodes = new int[m_n_fragmentElems + attrDecls.Count];
      iterInteger = elemDecls.GetEnumerator();
      while (iterInteger.MoveNext()) {
        m_fragmentINodes[i++] = (int)iterInteger.Current;
      }
      Debug.Assert(i == m_n_fragmentElems);
      iterInteger = attrDecls.GetEnumerator();
      while (iterInteger.MoveNext()) {
        m_fragmentINodes[i++] = (int)iterInteger.Current;
      }
    }

    private void computeGlobalDirectory() {
      m_globalElementsDirectory = new Dictionary<string, int[]>();
      m_globalAttributesDirectory = new Dictionary<string, int[]>();
      m_globalTypesDirectory = new Dictionary<string, int[]>();

      IList<int?> globalElements = new List<int?>();
      // Build up global elements directory
      for (int elem = 0; elem < m_elems.Length; elem += EXISchemaLayout.SZ_ELEM) {
        if (isGlobalElem(elem)) {
          string name = getNameOfElem(elem);
          int[] nodes;
          if (m_globalElementsDirectory.TryGetValue(name, out nodes)) {
            int[] _nodes = new int[nodes.Length + 1];
            Array.Copy(nodes, 0, _nodes, 0, nodes.Length);
            _nodes[nodes.Length] = elem;
            nodes = _nodes;
          }
          else {
            nodes = new int[1];
            nodes[0] = elem;
          }
          m_globalElementsDirectory[name] = nodes;
          globalElements.Add(elem);
        }
      }

      int i;
      // Build global elements array
      m_globalElems = new int[globalElements.Count];
      Debug.Assert(m_globalElems.Length == GlobalElemCountOfSchema);
      for (i = 0; i < m_globalElems.Length; i++) {
        m_globalElems[i] = (int)globalElements[i];
      }

      IList<int?> globalAttributes = new List<int?>();
      // Build up global attributes directory
      for (int attr = 0; attr < m_attrs.Length; attr += EXISchemaLayout.SZ_ATTR) {
        if (isGlobalAttr(attr)) {
          int localName = m_attrs[attr + EXISchemaLayout.INODE_NAME];
          int uri = m_attrs[attr + EXISchemaLayout.INODE_URI];
          string name = m_names[m_localNames[uri][localName]];
          int[] nodes;
          if (m_globalAttributesDirectory.TryGetValue(name, out nodes)) {
            int[] _nodes = new int[nodes.Length + 1];
            Array.Copy(nodes, 0, _nodes, 0, nodes.Length);
            _nodes[nodes.Length] = attr;
            nodes = _nodes;
          }
          else {
            nodes = new int[1];
            nodes[0] = attr;
          }
          m_globalAttributesDirectory[name] = nodes;
          globalAttributes.Add(attr);
        }
      }

      // Build global attributes array
      m_globalAttrs = new int[globalAttributes.Count];
      for (i = 0; i < m_globalAttrs.Length; i++) {
        m_globalAttrs[i] = (int)globalAttributes[i];
      }

      m_buitinTypes = new int[EXISchemaConst.N_BUILTIN_TYPES];
      int tp;
      for (tp = 0, i = 0; tp < m_types.Length; tp += EXISchema._getTypeSize(tp, m_types, ancestryIds), i++) {
        string tname = getNameOfType(tp);
        if (i < EXISchemaConst.N_BUILTIN_TYPES) {
          Debug.Assert(getUriOfType(tp) == 3 && tname.Length != 0);
          m_buitinTypes[i] = tp;
        }
        if (!"".Equals(tname)) {
          int[] nodes;
          if (m_globalTypesDirectory.TryGetValue(tname, out nodes)) {
            int[] _nodes = new int[nodes.Length + 1];
            Array.Copy(nodes, 0, _nodes, 0, nodes.Length);
            _nodes[nodes.Length] = tp;
            nodes = _nodes;
          }
          else {
            nodes = new int[1];
            nodes[0] = tp;
          }
          m_globalTypesDirectory[tname] = nodes;
        }
      }
    }

    private void computeVariantCharacters() {
      int n_variants = m_variants.Length;
      Characters[] variantCharacters = new Characters[n_variants];
      for (int i = 0; i < n_variants; i++) {
        string stringValue = computeVariantCharacters(i);
        variantCharacters[i] = new Characters(stringValue.ToCharArray(), 0, stringValue.Length, false);
      }
      m_variantCharacters = variantCharacters;
    }

    private string computeVariantCharacters(int variant) {
      string stringValue = "";
      byte[] binaryValue;
      switch (m_variantTypes[variant]) {
        case VARIANT_STRING:
          stringValue = getStringValueOfVariant(variant);
          break;
        case VARIANT_FLOAT:
          int ind = m_variants[variant];
          long mantissa = m_mantissas[ind];
          int exponent;
          if ((exponent = m_exponents[ind]) != 0) {
            if (exponent == -16384) {
              stringValue = mantissa == 1L ? "INF" : mantissa == -1 ? "-INF" : "NaN";
            }
            else {
              stringValue = Convert.ToString(mantissa, NumberFormatInfo.InvariantInfo) + "E" + Convert.ToString(exponent, NumberFormatInfo.InvariantInfo);
            }
          }
          else {
            stringValue = Convert.ToString(mantissa, NumberFormatInfo.InvariantInfo);
          }
          break;
        case VARIANT_DECIMAL:
          bool sign = getSignOfDecimalVariant(variant);
          string integralDigits = getIntegralDigitsOfDecimalVariant(variant);
          char[] charArray = getReverseFractionalDigitsOfDecimalVariant(variant).ToCharArray();
          Array.Reverse(charArray);
          string fractionalDigits = new String(charArray);
          bool zeroFractionalDigits;
          if ((zeroFractionalDigits = "0".Equals(fractionalDigits)) && "0".Equals(integralDigits)) {
            stringValue = "0";
          }
          else {
            if (sign) {
              stringValue += '-';
            }
            stringValue += integralDigits;
            if (!zeroFractionalDigits) {
              stringValue += '.';
              stringValue += fractionalDigits;
            }
          }
          break;
        case VARIANT_INTEGER:
          BigInteger bigInteger = getIntegerValueOfVariant(variant);
          stringValue = bigInteger.ToString(NumberFormatInfo.InvariantInfo);
          break;
        case VARIANT_INT:
          int intValue = getIntValueOfVariant(variant);
          stringValue = Convert.ToString(intValue, NumberFormatInfo.InvariantInfo);
          break;
        case VARIANT_LONG:
          long longValue = getLongValueOfVariant(variant);
          stringValue = Convert.ToString(longValue, NumberFormatInfo.InvariantInfo);
          break;
        case VARIANT_DATETIME:
          XSDateTime dateTime = getDateTimeValueOfVariant(variant);
          stringValue = dateTime.ToString(/**/);
          break;
        case VARIANT_DURATION:
          stringValue = XmlConvert.ToString(getDurationValueOfVariant(variant));
          break;
        case VARIANT_BASE64:
          binaryValue = getBinaryValueOfVariant(variant);
          int maxChars = (binaryValue.Length / 3) << 2;
          if (binaryValue.Length % 3 != 0) {
            maxChars += 4;
          }
          maxChars += maxChars / 76;
          char[] characters = new char[maxChars];
          int n_chars = Base64.encode(binaryValue, 0, binaryValue.Length, characters, 0);
          stringValue = new string(characters, 0, n_chars);
          break;
        case VARIANT_BOOLEAN:
          Debug.Assert(false);
          return null;
        case VARIANT_HEXBIN:
          binaryValue = getBinaryValueOfVariant(variant);
          StringBuilder stringBuffer = new StringBuilder();
          HexBin.encode(binaryValue, binaryValue.Length, stringBuffer);
          stringValue = stringBuffer.ToString(/**/);
          break;
        default:
          Debug.Assert(false);
          break;
      }
      return stringValue;
    }

    /// <summary>
    /// Populate m_computedDatetimes.
    /// </summary>
    private void computeDateTimes() {
      int n_dateTimes = m_datetimes.Length;
      XSDateTime[] processedDatetimes = new XSDateTime[n_dateTimes];
      for (int i = 0; i < n_dateTimes; i++) {
        XSDateTime dateTime;
        processedDatetimes[i] = dateTime = new XSDateTime(m_datetimes[i]);
        dateTime.normalize();
      }
      m_computedDatetimes = processedDatetimes;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Corpus methods
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// @y.exclude </summary>
    public int[] Elems {
      get {
        return m_elems;
      }
    }

    internal int[] Attrs {
      get {
        return m_attrs;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public int[] Types {
      get {
        return m_types;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public int[][] LocalNames {
      get {
        return m_localNames;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public int[] Grammars {
      get {
        return m_grammars;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public int[] Ints {
      get {
        return m_ints;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public long[] Longs {
      get {
        return m_longs;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public int[] Variants {
      get {
        return m_variants;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public int[] FragmentINodes {
      get {
        return m_fragmentINodes;
      }
    }

    /// <summary>
    /// @y.exclude </summary>
    public int FragmentElemCount {
      get {
        return this.m_n_fragmentElems;
      }
    }

    /// <summary>
    /// Returns the total number of simple types (both global and local) available in the schema. </summary>
    /// <param name="schema"> schema node </param>
    /// <returns> total number of simple types available in the schema.
    /// @y.exclude </returns>

    public int TotalSimpleTypeCount {
      get {
        return m_n_stypes;
      }
    }

    /// <summary>
    /// Returns the total number of grammars contained in the corpus. </summary>
    /// <returns> total number of grammars
    /// @y.exclude </returns>
    public int TotalGrammarCount {
      get {
        return m_grammarCount;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Generic methods
    ///////////////////////////////////////////////////////////////////////////

    public bool isSimpleType(int tp) {
      return (m_types[tp + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.TYPE_TYPE_OFFSET_MASK) != 0;
    }

    public static bool _isSimpleType(int tp, int[] types) {
      return (types[tp + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.TYPE_TYPE_OFFSET_MASK) != 0;
    }

    /// <summary>
    /// Returns the size of a type. </summary>
    /// <param name="tp"> a type </param>
    /// <returns> size of the type if it is a type, otherwise returns NIL_VALUE
    /// @y.exclude </returns>
    public static int _getTypeSize(int tp, int[] types, sbyte[] ancestryIds) {
      Debug.Assert(tp != NIL_NODE);
      if ((types[tp + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.TYPE_TYPE_OFFSET_MASK) != 0) {
        return _getSizeOfSimpleType(tp, types, ancestryIds);
      }
      else { // i.e. is a complex type
        return EXISchemaLayout.SZ_COMPLEX_TYPE;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Directory methods
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// @y.exclude
    /// </summary>
    public int getGlobalElemOfSchema(string uri, string name) {
      int[] nodes;
      if (m_globalElementsDirectory.TryGetValue(name, out nodes)) {
        int len = nodes.Length;
        for (int i = 0; i < len; i++) {
          int elem = nodes[i];
          int uriId = getUriOfElem(elem);
          if (uri.Equals(uris[uriId])) {
            return elem;
          }
        }
      }
      return NIL_NODE;
    }

    /// <summary>
    /// @y.exclude
    /// </summary>
    public int getGlobalAttrOfSchema(string uri, string name) {
      int[] attrs;
      if (m_globalAttributesDirectory.TryGetValue(name, out attrs)) {
        int len = attrs.Length;
        for (int i = 0; i < len; i++) {
          int attr = attrs[i];
          int uriId = getUriOfAttr(attr);
          if (uri.Equals(uris[uriId])) {
            return attr;
          }
        }
      }
      return NIL_NODE;
    }

    /// <summary>
    /// @y.exclude
    /// </summary>
    public int getTypeOfSchema(string uri, string name) {
      int[] nodes;
      if (m_globalTypesDirectory.TryGetValue(name, out nodes)) {
        int len = nodes.Length;
        for (int i = 0; i < len; i++) {
          int tp = nodes[i];
          int uriId;
          if ((uriId = getUriOfType(tp)) != -1 && uri.Equals(uris[uriId])) {
            return tp;
          }
        }
      }
      return NIL_NODE;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Schema methods
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Returns the total number of global elements available in the schema. </summary>
    /// <returns> total number of global elements available in the schema.
    /// @y.exclude </returns>
    public int GlobalElemCountOfSchema {
      get {
        return m_globalElems.Length;
      }
    }

    /// <summary>
    /// Returns the i-th global element of the schema. </summary>
    /// <param name="i"> index of the element </param>
    /// <returns> i-th element
    /// @y.exclude </returns>
    public int getGlobalElemOfSchema(int i) {
      Debug.Assert(0 <= i && i < m_globalElems.Length);
      return m_globalElems[i];
    }

    /// <summary>
    /// Returns the total number of global attributes available in the schema. </summary>
    /// <returns> total number of global attributes available in the schema.
    /// @y.exclude </returns>
    public int GlobalAttrCountOfSchema {
      get {
        return m_globalAttrs.Length;
      }
    }

    /// <summary>
    /// @y.exclude
    /// </summary>
    public int getGlobalAttrOfSchema(int i) {
      Debug.Assert(0 <= i && i < m_globalAttrs.Length);
      return m_globalAttrs[i];
    }

    /// <summary>
    /// Returns the i-th built-in type of the schema. </summary>
    /// <param name="i"> index of the type </param>
    /// <returns> i-th built-in type </returns>
    /// <exception cref="EXISchemaRuntimeException"> If the index is out of array bounds
    /// @y.exclude </exception>
    public int getBuiltinTypeOfSchema(int i) {
      if (i < 0 || EXISchemaConst.N_BUILTIN_TYPES - 1 < i) {
        throw new EXISchemaRuntimeException(EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
          new string[] { 
            Convert.ToString(i, NumberFormatInfo.InvariantInfo), 
            Convert.ToString(0, NumberFormatInfo.InvariantInfo), 
            Convert.ToString(EXISchemaConst.N_BUILTIN_TYPES - 1, NumberFormatInfo.InvariantInfo) });
      }
      return m_buitinTypes[i];
    }

    ///////////////////////////////////////////////////////////////////////////
    // Element methods
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Returns the total number of elements. 
    /// @y.exclude
    /// 
    /// </summary>
    public int ElemCountOfSchema {
      get {
        return m_elems.Length / EXISchemaLayout.SZ_ELEM;
      }
    }

    /// <summary>
    /// Returns name of an element. </summary>
    /// <param name="elem"> element node </param>
    /// <returns> name of an element node.
    /// @y.exclude </returns>
    public string getNameOfElem(int elem) {
      Debug.Assert(0 <= elem);
      int localName = m_elems[elem + EXISchemaLayout.INODE_NAME];
      if (localName != -1) {
        int uri = m_elems[elem + EXISchemaLayout.INODE_URI];
        return m_names[m_localNames[uri][localName]];
      }
      return "";
    }

    /// <summary>
    /// Returns localName ID of an elem. </summary>
    /// <param name="elem"> element node </param>
    /// <returns> localName ID
    /// @y.exclude </returns>
    public int getLocalNameOfElem(int elem) {
      Debug.Assert(0 <= elem);
      return m_elems[elem + EXISchemaLayout.INODE_NAME];
    }

    /// <summary>
    /// Returns uri of an element. </summary>
    /// <param name="elem"> an element </param>
    /// <returns> uri
    /// @y.exclude </returns>
    public int getUriOfElem(int elem) {
      Debug.Assert(0 <= elem);
      return m_elems[elem + EXISchemaLayout.INODE_URI];
    }

    /// <summary>
    /// @y.exclude </summary>
    public int getTypeOfElem(int elem) {
      Debug.Assert(0 <= elem);
      int tp = m_elems[elem + EXISchemaLayout.INODE_TYPE];
      /// NOTE: Simply reverting negative to positive would have been done by
      /// return (stype & 0x80000000) != 0 ? ~stype + 1 : stype;
      return (tp & 0x80000000) != 0 ?~tp : tp;
    }

    /// <summary>
    /// @y.exclude </summary>
    public bool isGlobalElem(int elem) {
      int tp = m_elems[elem + EXISchemaLayout.INODE_TYPE];
      return (tp & 0x80000000) != 0;
    }

    /// <summary>
    /// @y.exclude </summary>
    public bool isNillableElement(int elem) {
      Debug.Assert(0 <= elem);
      return m_elems[elem + EXISchemaLayout.ELEM_NILLABLE] != 0;
    }

    /// <summary>
    /// @y.exclude </summary>
    public static bool _isNillableElement(int elem, int[] elems) {
      Debug.Assert(0 <= elem);
      return elems[elem + EXISchemaLayout.ELEM_NILLABLE] != 0;
    }

    /// <summary>
    /// Returns serial number of an element. </summary>
    /// <param name="elem"> element node </param>
    /// <returns> serial number
    /// @y.exclude </returns>
    public int getSerialOfElem(int elem) {
      Debug.Assert(0 <= elem && elem % EXISchemaLayout.SZ_ELEM == 0);
      return elem / EXISchemaLayout.SZ_ELEM;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Type methods
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Returns name of a type. </summary>
    /// <param name="tp"> type node </param>
    /// <returns> name of a type node.
    /// @y.exclude </returns>
    public string getNameOfType(int tp) {
      Debug.Assert(0 <= tp);
      int localName = m_types[tp + EXISchemaLayout.TYPE_NAME];
      if (localName != -1) {
        int uri = m_types[tp + EXISchemaLayout.TYPE_URI];
        return m_names[m_localNames[uri][localName]];
      }
      return "";
    }

    /// <summary>
    /// Returns localName ID of an type. </summary>
    /// <param name="tp"> type node </param>
    /// <returns> localName ID
    /// @y.exclude </returns>
    public int getLocalNameOfType(int tp) {
      Debug.Assert(0 <= tp);
      return m_types[tp + EXISchemaLayout.TYPE_NAME];
    }

    /// <summary>
    /// Returns target namespace name of type. </summary>
    /// <param name="tp"> type node </param>
    /// <returns> target namespace name
    /// @y.exclude </returns>
    public int getUriOfType(int tp) {
      Debug.Assert(0 <= tp);
      return m_types[tp + EXISchemaLayout.TYPE_URI];
    }

    /// <summary>
    /// Returns serial number of a type. Those serial numbers of built-in
    /// primitive types are static and do not change. </summary>
    /// <param name="tp"> type node </param>
    /// <returns> serial number
    /// @y.exclude </returns>
    public int getSerialOfType(int tp) {
      Debug.Assert(0 <= tp);
      return m_types[tp + EXISchemaLayout.TYPE_NUMBER];
    }

    /// <summary>
    /// @y.exclude </summary>
    public int getGrammarOfType(int tp) {
      Debug.Assert(0 <= tp);
      return m_types[tp + EXISchemaLayout.TYPE_GRAMMAR];
    }

    /// <summary>
    /// @y.exclude </summary>
    public static int _getGrammarOfType(int tp, int[] types) {
      Debug.Assert(0 <= tp);
      return types[tp + EXISchemaLayout.TYPE_GRAMMAR];
    }

    /// <summary>
    /// @y.exclude </summary>
    public bool isTypableType(int tp) {
      Debug.Assert(0 <= tp);
      return m_types[tp + EXISchemaLayout.TYPE_TYPABLE] != 0;
    }

    ///////////////////////////////////////////////////////////////////////////
    // SimpleType methods
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Returns the size of a simple type. </summary>
    /// <param name="stype"> simple type node </param>
    /// <returns> size of the simple type </returns>
    private static int _getSizeOfSimpleType(int stype, int[] types, sbyte[] ancestryIds) {
      int size = EXISchemaLayout.SZ_SIMPLE_TYPE;
      sbyte variety = _getVarietyOfSimpleType(stype, types);
      if (variety == ATOMIC_SIMPLE_TYPE) {
        if (_isEnumeratedAtomicSimpleType(stype, types)) {
          size += 1 + _getEnumerationFacetCountOfAtomicSimpleType(stype, types, ancestryIds);
        }
        if (ancestryIds[types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.STRING_TYPE) {
          size += _getRestrictedCharacterCountOfStringSimpleType(stype, types, ancestryIds);
        }
      }
      return size;
    }

    /// <summary>
    /// Returns base type of a simple type. </summary>
    /// <param name="stype"> simple type node </param>
    /// <returns> base type
    /// @y.exclude </returns>
    public int getBaseTypeOfSimpleType(int stype) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0);
      return m_types[stype + (EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_BASE_TYPE)];
    }

    /// <summary>
    /// Returns variety property of a simple type. </summary>
    /// <param name="stype"> simple type node </param>
    /// <returns> one of UR_SIMPLE_TYPE, ATOMIC_SIMPLE_TYPE, LIST_SIMPLE_TYPE or UNION_SIMPLE_TYPE
    /// @y.exclude </returns>
    public sbyte getVarietyOfSimpleType(int stype) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0);
      return _getVarietyOfSimpleType(stype, m_types);
    }
    /// <summary>
    /// @y.exclude
    /// </summary>
    public static sbyte _getVarietyOfSimpleType(int stype, int[] types) {
      return (sbyte)(types[stype + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_VARIETY_MASK);
    }

    /// <summary>
    /// Returns item type of a list simple type. </summary>
    /// <param name="stype"> list simple type </param>
    /// <returns> item type
    /// @y.exclude </returns>
    public int getItemTypeOfListSimpleType(int stype) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0);
      return m_types[stype + (EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_ITEM_TYPE)];
    }

    /// <summary>
    /// Determines if a simple type is primitive. </summary>
    /// <param name="stype"> simple type </param>
    /// <returns> true if the simple type is primitive
    /// @y.exclude </returns>
    public bool isPrimitiveSimpleType(int stype) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0);
      int serial = getSerialOfType(stype);
      // primitive simple types' serial is [2...20]
      return EXISchemaConst.ANY_SIMPLE_TYPE < serial && serial < EXISchemaConst.STRING_TYPE + EXISchemaConst.N_PRIMITIVE_TYPES;
    }

    /// <summary>
    /// Determines if a simple type represents integral numbers. </summary>
    /// <param name="stype"> simple type </param>
    /// <returns> true if the simple type represents integral numbers
    /// @y.exclude </returns>
    public bool isIntegralSimpleType(int stype) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0);
      return ancestryIds[m_types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.INTEGER_TYPE;
    }

    /// <summary>
    /// Returns the representation of an integral simple type. </summary>
    /// <param name="stype"> integral simple type </param>
    /// <returns> INTEGER_CODEC_NONNEGATIVE for a non-negative 
    /// integer representation, INTEGER_CODEC_DEFAULT for a default 
    /// integer representation, otherwise width of n-bits integer 
    /// representation.
    /// @y.exclude </returns>
    public int getWidthOfIntegralSimpleType(int stype) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0);
      return _getWidthOfIntegralSimpleType(stype, m_types);
    }

    /// <summary>
    /// @y.exclude </summary>
    public static int _getWidthOfIntegralSimpleType(int stype, int[] types) {
      return (types[stype + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_MASK) >> EXISchemaLayout.SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_OFFSET;
    }

    /// <summary>
    /// Returns whiteSpace facet value of a string simple type. </summary>
    /// <param name="stype"> simple type </param>
    /// <returns> one of WHITESPACE_* enumerated values
    /// @y.exclude </returns>
    public int getWhitespaceFacetValueOfStringSimpleType(int stype) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0 && ancestryIds[m_types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.STRING_TYPE);
      return (m_types[stype + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_MASK) >> EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_OFFSET;
    }

   /// <summary>
   /// @y.exclude </summary>
    public bool isPatternedBooleanSimpleType(int stype) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0 && ancestryIds[m_types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.BOOLEAN_TYPE);
      return (m_types[stype + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_ATOMIC_BOOLEAN_HAS_PATTERN_MASK) != 0;
    }

    /// <summary>
    /// Returns minInclusive facet variant of an integer simple type. </summary>
    /// <param name="stype"> simple type </param>
    /// <returns> minInclusive facet variant if available, otherwise NIL_VALUE
    /// @y.exclude </returns>
    public int getMinInclusiveFacetOfIntegerSimpleType(int stype) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0 && ancestryIds[m_types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.INTEGER_TYPE);
      return m_types[stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_FACET_MININCLUSIVE];
    }

    /// <summary>
    /// Returns the number of characters in restricted characters set associated with a string simple type. </summary>
    /// <param name="stype"> simple type </param>
    /// <returns> number of characters in restricted character set
    /// @y.exclude </returns>
    public int getRestrictedCharacterCountOfStringSimpleType(int stype) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0 && ancestryIds[m_types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.STRING_TYPE);
      return _getRestrictedCharacterCountOfStringSimpleType(stype, m_types, ancestryIds);
    }

    private static int _getRestrictedCharacterCountOfStringSimpleType(int stype, int[] types, sbyte[] ancestryIds) {
      int n_restrictedCharacters = (types[stype + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_MASK) >> EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_OFFSET;
      Debug.Assert(0 <= n_restrictedCharacters && n_restrictedCharacters < 256);
      return n_restrictedCharacters;
    }

    /// <summary>
    /// Returns the start index of the restricted charset. </summary>
    /// <param name="stype"> simple type
    /// @y.exclude </param>
    public int getRestrictedCharacterOfSimpleType(int stype) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0);
      return stype + EXISchemaLayout.SZ_SIMPLE_TYPE;
    }

    private bool isEnumeratedAtomicSimpleType(int stype) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0 && getVarietyOfSimpleType(stype) == ATOMIC_SIMPLE_TYPE);
      return _isEnumeratedAtomicSimpleType(stype, m_types);
    }

    private static bool _isEnumeratedAtomicSimpleType(int stype, int[] types) {
      return (types[stype + EXISchemaLayout.TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_HAS_ENUMERATED_VALUES_MASK) != 0;
    }

    /// <summary>
    /// Returns the number of enumeration facets associated with an atomic simple type. </summary>
    /// <param name="stype"> simple type </param>
    /// <returns> number of enumeration facets
    /// @y.exclude </returns>
    public int getEnumerationFacetCountOfAtomicSimpleType(int stype) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0 && getVarietyOfSimpleType(stype) == ATOMIC_SIMPLE_TYPE);
      return _getEnumerationFacetCountOfAtomicSimpleType(stype, m_types, ancestryIds);
    }

    private static int _getEnumerationFacetCountOfAtomicSimpleType(int stype, int[] types, sbyte[] ancestryIds) {
      if (_isEnumeratedAtomicSimpleType(stype, types)) {
        int n_patterns = ancestryIds[types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.STRING_TYPE ? _getRestrictedCharacterCountOfStringSimpleType(stype, types, ancestryIds) : 0;
        return types[stype + EXISchemaLayout.SZ_SIMPLE_TYPE + n_patterns];
      }
      return 0;
    }

    /// <summary>
    /// Returns the i-th enumeration facet variant of an atomic simple type. </summary>
    /// <param name="stype"> simple type </param>
    /// <param name="i"> index of the enumeration facet </param>
    /// <returns> ith enumeration facet variant
    /// @y.exclude </returns>
    public int getEnumerationFacetOfAtomicSimpleType(int stype, int i) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0 && getVarietyOfSimpleType(stype) == ATOMIC_SIMPLE_TYPE);
      int n = _getEnumerationFacetCountOfAtomicSimpleType(stype, m_types, ancestryIds);
      if (i < 0 || i >= n) {
        throw new EXISchemaRuntimeException(EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
          new string[] { Convert.ToString(i, NumberFormatInfo.InvariantInfo), 
            Convert.ToString(0, NumberFormatInfo.InvariantInfo), Convert.ToString(n - 1, NumberFormatInfo.InvariantInfo) });
      }
      int n_patterns = ancestryIds[m_types[stype + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.STRING_TYPE ? _getRestrictedCharacterCountOfStringSimpleType(stype, m_types, ancestryIds) : 0;
      return m_types[stype + EXISchemaLayout.SZ_SIMPLE_TYPE + n_patterns + 1 + i];
    }

    /// <summary>
    /// @y.exclude </summary>
    public int getNextSimpleType(int stype) {
      Debug.Assert(0 <= stype && m_types[stype + EXISchemaLayout.TYPE_AUX] < 0);
      int _stype;
      if ((_stype = stype + _getSizeOfSimpleType(stype, m_types, ancestryIds)) != m_stypes_end) {
        return _stype;
      }
      return NIL_NODE;
    }

    ///////////////////////////////////////////////////////////////////////////
    // ComplexType methods
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// @y.exclude </summary>
    public int getContentDatatypeOfComplexType(int ctype) {
      Debug.Assert(0 <= ctype && m_types[ctype + EXISchemaLayout.TYPE_AUX] >= 0);
      int tp;
      // if tp is 0, return NIL_NODE. Otherwise, return tp.
      return (tp = m_types[ctype + EXISchemaLayout.TYPE_AUX]) != 0 ? tp : NIL_NODE;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Grammar methods
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Returns the size of a grammar. </summary>
    /// <param name="gram"> a grammar </param>
    /// <returns> size of the grammar
    /// @y.exclude </returns>
    public static int getSizeOfGrammar(int gram, int[] grammars) {
      int sz = EXISchemaLayout.SZ_GRAMMAR;
      if (_hasContentGrammar(gram, grammars)) {
        ++sz;
        if (_hasEmptyGrammar(gram, grammars)) {
          ++sz;
        }
      }
      return sz + _getProductionCountOfGrammar(gram, grammars);
    }

    /// <summary>
    /// @y.exclude </summary>
    public int getSerialOfGrammar(int gram) {
      return m_grammars[gram + EXISchemaLayout.GRAMMAR_NUMBER];
    }

    /// <summary>
    /// @y.exclude </summary>
    public bool hasEndElement(int gram) {
      return (m_grammars[gram + EXISchemaLayout.GRAMMAR_N_PRODUCTION] & EXISchemaLayout.GRAMMAR_HAS_END_ELEMENT_MASK) != 0;
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public bool hasEmptyGrammar(int gram) {
      return _hasEmptyGrammar(gram, m_grammars);
    }
    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public static bool _hasEmptyGrammar(int gram, int[] grammars) {
      bool res;
      res = (grammars[gram + EXISchemaLayout.GRAMMAR_N_PRODUCTION] & EXISchemaLayout.GRAMMAR_HAS_EMPTY_GRAMMAR_MASK) != 0;
      Debug.Assert(!res || _hasContentGrammar(gram, grammars));
      return res;
    }
    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public bool hasContentGrammar(int gram) {
      return _hasContentGrammar(gram, m_grammars);
    }
    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public static bool _hasContentGrammar(int gram, int[] grammars) {
      return (grammars[gram + EXISchemaLayout.GRAMMAR_N_PRODUCTION] & EXISchemaLayout.GRAMMAR_HAS_CONTENT_GRAMMAR_MASK) != 0;
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public int getProductionCountOfGrammar(int gram) {
      return _getProductionCountOfGrammar(gram, m_grammars);
    }
    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public static int _getProductionCountOfGrammar(int gram, int[] grammars) {
      return grammars[gram + EXISchemaLayout.GRAMMAR_N_PRODUCTION] & EXISchemaLayout.GRAMMAR_N_PRODUCTION_MASK;
    }

    /// <summary>
    /// @y.exclude </summary>
    public int getProductionOfGrammar(int gram, int i) {
      int n = getProductionCountOfGrammar(gram);
      if (i < 0 || i >= n) {
        throw new EXISchemaRuntimeException(EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
          new string[] { Convert.ToString(i, NumberFormatInfo.InvariantInfo), 
            Convert.ToString(0, NumberFormatInfo.InvariantInfo), 
            Convert.ToString(n - 1, NumberFormatInfo.InvariantInfo) });
      }
      return _getProductionOfGrammar(gram, i, m_grammars);
    }

    /// <summary>
    /// @y.exclude </summary>
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

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public int getContentGrammarOfGrammar(int gram) {
      return _getContentGrammarOfGrammar(gram, m_grammars);
    }

    /// <summary>
    /// @y.exclude </summary>
    public static int _getContentGrammarOfGrammar(int gram, int[] grammars) {
      if (_hasContentGrammar(gram, grammars)) {
        return grammars[gram + (EXISchemaLayout.SZ_GRAMMAR + EXISchemaLayout.GRAMMAR_EXT_CONTENT_GRAMMAR)];
      }
      return EXISchema.NIL_GRAM;
    }

    /// <summary>
    /// @y.exclude </summary>
    public int getTypeEmptyGrammarOfGrammar(int gram) {
      return _getTypeEmptyGrammarOfGrammar(gram, m_grammars);
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public static int _getTypeEmptyGrammarOfGrammar(int gram, int[] grammars) {
      if (_hasEmptyGrammar(gram, grammars)) {
        return grammars[gram + (EXISchemaLayout.SZ_GRAMMAR + EXISchemaLayout.GRAMMAR_EXT_EMPTY_GRAMMAR)];
      }
      return EXISchema.NIL_GRAM;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Production methods
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public int getEventOfProduction(int prod) {
      return m_productions[prod + EXISchemaLayout.PRODUCTION_EVENT];
    }
    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public int getGrammarOfProduction(int prod) {
      return m_productions[prod + EXISchemaLayout.PRODUCTION_GRAMMAR];
    }

    ///////////////////////////////////////////////////////////////////////////
    // Event methods
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Returns event type of the event. </summary>
    /// <param name="event"> </param>
    /// <returns> one of EVENT_TYPE_AT, EVENT_TYPE_SE,  
    /// EVENT_TYPE_AT_WILDCARD_NS or EVENT_TYPE_SE_WILDCARD_NS
    /// Not for public use.
    /// @y.exclude </returns>
    public sbyte getEventType(int @event) {
      return m_eventTypes[@event];
    }
    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public int getNodeOfEventType(int @event) {
      Debug.Assert(m_eventTypes[@event] == EVENT_TYPE_AT || m_eventTypes[@event] == EVENT_TYPE_SE);
      return m_eventData[@event];
    }
    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public int getUriOfEventType(int @event) {
      Debug.Assert(m_eventTypes[@event] == EVENT_TYPE_AT_WILDCARD_NS || m_eventTypes[@event] == EVENT_TYPE_SE_WILDCARD_NS);
      return m_eventData[@event];
    }

    ///////////////////////////////////////////////////////////////////////////
    // Attribute methods
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Returns localName ID of an attribute. </summary>
    /// <param name="attr"> attribute node </param>
    /// <returns> localName ID
    /// @y.exclude </returns>
    public int getLocalNameOfAttr(int attr) {
      Debug.Assert(0 <= attr);
      return m_attrs[attr + EXISchemaLayout.INODE_NAME];
    }

    /// <summary>
    /// Returns target namespace name of an attribute. </summary>
    /// <param name="attr"> attribute node </param>
    /// <returns> target namespace name
    /// @y.exclude </returns>
    public int getUriOfAttr(int attr) {
      Debug.Assert(0 <= attr);
      return m_attrs[attr + EXISchemaLayout.INODE_URI];
    }

    /// <summary>
    /// Returns the type of an attribute. </summary>
    /// <param name="attr"> attribute node </param>
    /// <returns> type of the attribute
    /// @y.exclude </returns>
    public int getTypeOfAttr(int attr) {
      Debug.Assert(0 <= attr);
      return _getTypeOfAttr(attr, m_attrs);
    }

    /// <summary>
    /// @y.exclude </summary>
    public static int _getTypeOfAttr(int attr, int[] attrs) {
      int stype = attrs[attr + EXISchemaLayout.INODE_TYPE];
      /// NOTE: Simply reverting negative to positive would have been done by
      /// return (stype & 0x80000000) != 0 ? ~stype + 1 : stype;
      return (stype & 0x80000000) != 0 ?~stype : stype;
    }

    /// <summary>
    /// @y.exclude </summary>
    public bool isGlobalAttr(int attr) {
      int stype = m_attrs[attr + EXISchemaLayout.INODE_TYPE];
      return (stype & 0x80000000) != 0;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Variant methods
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Returns the type of a variant. </summary>
    /// <param name="variant"> a variant </param>
    /// <returns> one of VARIANT_* enumerated values
    /// @y.exclude </returns>
    public int getTypeOfVariant(int variant) {
      return m_variantTypes[variant];
    }

    /// <summary>
    /// Returns Characters value of a variant. </summary>
    /// <param name="variant"> </param>
    /// <returns> Characters value
    /// @y.exclude </returns>
    public Characters getVariantCharacters(int variant) {
      return m_variantCharacters[variant];
    }

    /// <summary>
    /// Returns String value of a variant. </summary>
    /// <param name="variant"> a variant of type VARIANT_STRING </param>
    /// <returns> String value
    /// @y.exclude </returns>
    public string getStringValueOfVariant(int variant) {
      Debug.Assert(0 <= variant && m_variantTypes[variant] == VARIANT_STRING);
      return m_strings[m_variants[variant]];
    }

    /// <summary>
    /// Returns boolean value of a variant. </summary>
    /// <param name="variant"> a variant of type VARIANT_BOOLEAN </param>
    /// <returns> boolean value
    /// @y.exclude </returns>
    public bool getBooleanValueOfVariant(int variant) {
      Debug.Assert(0 <= variant && m_variantTypes[variant] == VARIANT_BOOLEAN);
      Debug.Assert(m_variants[variant] == 0 || m_variants[variant] == 1);
      return m_variants[variant] == 1;
    }

    /// <summary>
    /// Returns mantissa of a float variant. </summary>
    /// <param name="variant"> a variant of type VARIANT_FLOAT </param>
    /// <returns> mantissa
    /// @y.exclude </returns>
    public long getMantissaOfFloatVariant(int variant) {
      Debug.Assert(0 <= variant && m_variantTypes[variant] == VARIANT_FLOAT);
      return m_mantissas[m_variants[variant]];
    }

    /// <summary>
    /// Returns exponent of a float variant. </summary>
    /// <param name="variant"> a variant of type VARIANT_FLOAT </param>
    /// <returns> exponent
    /// @y.exclude </returns>
    public int getExponentOfFloatVariant(int variant) {
      Debug.Assert(0 <= variant && m_variantTypes[variant] == VARIANT_FLOAT);
      return m_exponents[m_variants[variant]];
    }

    /// <summary>
    /// Returns sign of a decimal variant. </summary>
    /// <param name="variant"> a variant of type VARIANT_DECIMAL </param>
    /// <returns> sign
    /// @y.exclude </returns>
    public bool getSignOfDecimalVariant(int variant) {
      Debug.Assert(0 <= variant && m_variantTypes[variant] == VARIANT_DECIMAL);
      return m_signs[m_variants[variant]];
    }

    /// <summary>
    /// Returns integral digits of a decimal variant. </summary>
    /// <param name="variant"> a variant of type VARIANT_DECIMAL </param>
    /// <returns> integral digits
    /// @y.exclude </returns>
    public string getIntegralDigitsOfDecimalVariant(int variant) {
      Debug.Assert(0 <= variant && m_variantTypes[variant] == VARIANT_DECIMAL);
      return m_integralDigits[m_variants[variant]];
    }

    /// <summary>
    /// Returns reverse-fractional digits of a decimal variant. </summary>
    /// <param name="variant"> a variant of type VARIANT_DECIMAL </param>
    /// <returns> reverse-fractional digits
    /// @y.exclude </returns>
    public string getReverseFractionalDigitsOfDecimalVariant(int variant) {
      Debug.Assert(0 <= variant && m_variantTypes[variant] == VARIANT_DECIMAL);
      return m_reverseFractionalDigits[m_variants[variant]];
    }

    /// <summary>
    /// Returns int value of a variant. </summary>
    /// <param name="variant"> a variant of type VARIANT_INT </param>
    /// <returns> int value
    /// @y.exclude </returns>
    public int getIntValueOfVariant(int variant) {
      Debug.Assert(0 <= variant && m_variantTypes[variant] == VARIANT_INT);
      return m_ints[m_variants[variant]];
    }

    /// <summary>
    /// Returns long value of a variant. </summary>
    /// <param name="variant"> a variant of type VARIANT_LONG </param>
    /// <returns> long value
    /// @y.exclude </returns>
    public long getLongValueOfVariant(int variant) {
      Debug.Assert(0 <= variant && m_variantTypes[variant] == VARIANT_LONG);
      return m_longs[m_variants[variant]];
    }

    /// <summary>
    /// Returns BigInteger value of a variant. </summary>
    /// <param name="variant"> a variant of type VARIANT_INTEGER </param>
    /// <returns> BigInteger value
    /// @y.exclude </returns>
    public BigInteger getIntegerValueOfVariant(int variant) {
      Debug.Assert(0 <= variant && m_variantTypes[variant] == VARIANT_INTEGER);
      return m_integers[m_variants[variant]];
    }

    /// <summary>
    /// Returns datetime value of a variant. </summary>
    /// <param name="variant"> a variant of type VARIANT_DATETIME </param>
    /// <returns> datetime value as XSDateTime
    /// @y.exclude </returns>
    public XSDateTime getDateTimeValueOfVariant(int variant) {
      Debug.Assert(0 <= variant && m_variantTypes[variant] == VARIANT_DATETIME);
      return m_datetimes[m_variants[variant]];
    }

    /// <summary>
    /// Returns a computed datetime value of a variant. (for enumeration handling) </summary>
    /// <param name="variant"> a variant of type VARIANT_DATETIME </param>
    /// <returns> datetime value as XSDateTime
    /// @y.exclude </returns>
    public XSDateTime getComputedDateTimeValueOfVariant(int variant) {
      Debug.Assert(0 <= variant && m_variantTypes[variant] == VARIANT_DATETIME);
      return m_computedDatetimes[m_variants[variant]];
    }

    /// <summary>
    /// Returns duration value of a variant. </summary>
    /// <param name="variant"> a variant of type VARIANT_DURATION </param>
    /// <returns> duration value as XSDuration
    /// @y.exclude </returns>
    public TimeSpan getDurationValueOfVariant(int variant) {
      Debug.Assert(0 <= variant && m_variantTypes[variant] == VARIANT_DURATION);
      return m_durations[m_variants[variant]];
    }

    /// <summary>
    /// Returns binary value of a variant. </summary>
    /// <param name="variant"> a variant of type VARIANT_BINARY </param>
    /// <returns> duration value as byte array
    /// @y.exclude </returns>
    public byte[] getBinaryValueOfVariant(int variant) {
      int variantType = m_variantTypes[variant];
      Debug.Assert(0 <= variant && (variantType == VARIANT_BASE64 || variantType == VARIANT_HEXBIN));
      return m_binaries[m_variants[variant]];
    }

    ///////////////////////////////////////////////////////////////////////////
    // Serialization/Deserialization
    ///////////////////////////////////////////////////////////////////////////

   /// <summary>
   /// Reads an EXI Schema from a DataInputStream. </summary>
   /// <param name="in"> DataInputStream containing a serialized EXISchema </param>
   /// <returns> <seealso cref="org.openexi.schema.EXISchema"/> </returns>
   /// <exception cref="IOException">, ClassNotFoundException </exception>
    public static EXISchema readIn(Stream @in) {
      int i, len, n;

      for (i = 0; i < COOKIE.Length; i++) {
        if ((sbyte)@in.ReadByte() != COOKIE[i]) {
          throw new InvalidDataException("The stream starts with a wrong magic cookie.");
        }
      }

      len = ReadInt(@in);
      int[] nodes = new int[len];
      for (i = 0; i < len; i++) {
        nodes[i] = ReadInt(@in);
      }

      len = ReadInt(@in);
      int[] attrs = new int[len];
      for (i = 0; i < len; i++) {
        attrs[i] = ReadInt(@in);
      }

      len = ReadInt(@in);
      int[] types = new int[len];
      for (i = 0; i < len; i++) {
        types[i] = ReadInt(@in);
      }

      len = ReadInt(@in);
      string[] uris = new string[len + 4];
      uris[0] = "";
      uris[1] = "http://www.w3.org/XML/1998/namespace";
      uris[2] = "http://www.w3.org/2001/XMLSchema-instance";
      uris[3] = "http://www.w3.org/2001/XMLSchema";
      for (i = 4; i < len + 4; i++) {
        uris[i] = readString(@in);
      }

      len = ReadInt(@in);
      string[] names = new string[EXISchemaConst.N_BUILTIN_LOCAL_NAMES + len];
      i = 0;
      names[i++] = "";
      for (n = 0; n < EXISchemaConst.XML_LOCALNAMES.Length; n++) {
        names[i++] = EXISchemaConst.XML_LOCALNAMES[n];
      }
      for (n = 0; n < EXISchemaConst.XSI_LOCALNAMES.Length; n++) {
        names[i++] = EXISchemaConst.XSI_LOCALNAMES[n];
      }
      for (n = 0; n < EXISchemaConst.XSD_LOCALNAMES.Length; n++) {
        names[i++] = EXISchemaConst.XSD_LOCALNAMES[n];
      }
      Debug.Assert(i == EXISchemaConst.N_BUILTIN_LOCAL_NAMES);
      for (; i < names.Length; i++) {
        names[i] = readString(@in);
      }

      len = ReadInt(@in);
      int[][] localNames = new int[len][];
      for (i = 0; i < len; i++) {
        localNames[i] = new int[ReadInt(@in)];
        for (int j = 0; j < localNames[i].Length; j++) {
          localNames[i][j] = ReadInt(@in);
        }
      }

      len = ReadInt(@in);
      string[] strings = new string[len];
      for (i = 0; i < len; i++) {
        strings[i] = readString(@in);
      }

      len = ReadInt(@in);
      int[] ints = new int[len];
      for (i = 0; i < len; i++) {
        ints[i] = ReadInt(@in);
      }

      len = ReadInt(@in);
      long[] mantissas = new long[len];
      int[] exponents = new int[len];
      for (i = 0; i < len; i++) {
        mantissas[i] = ReadLong(@in);
        exponents[i] = ReadInt(@in);
      }

      len = ReadInt(@in);
      bool[] signs = new bool[len];
      string[] integralDigits = new string[len];
      string[] reverseFractionalDigits = new string[len];
      for (i = 0; i < len; i++) {
        signs[i] = ReadBoolean(@in);
        integralDigits[i] = readString(@in);
        reverseFractionalDigits[i] = readString(@in);
      }

      len = ReadInt(@in);
      BigInteger[] integers = new BigInteger[len];
      for (i = 0; i < len; i++) {
        integers[i] = BigInteger.Parse(readString(@in), NumberFormatInfo.InvariantInfo);
      }

      len = ReadInt(@in);
      long[] longs = new long[len];
      for (i = 0; i < len; i++) {
        longs[i] = ReadLong(@in);
      }

      len = ReadInt(@in);
      XSDateTime[] datetimes = new XSDateTime[len];
      for (i = 0; i < len; i++) {
        datetimes[i] = XSDateTime.readIn(@in);
      }

      len = ReadInt(@in);
      TimeSpan[] durations = new TimeSpan[len];
      for (i = 0; i < len; i++) {
        durations[i] = XmlConvert.ToTimeSpan(readString(@in));
      }

      len = ReadInt(@in);
      byte[][] binaries = new byte[len][];
      for (i = 0; i < len; i++) {
        binaries[i] = new byte[ReadInt(@in)];
        for (n = 0; n < binaries[i].Length; n++) {
          binaries[i][n] = ReadByte(@in);
        }
      }

      len = ReadInt(@in);
      sbyte[] variantTypes = new sbyte[len];
      for (i = 0; i < len; i++) {
        variantTypes[i] = ReadSByte(@in);
      }

      len = ReadInt(@in);
      int[] variants = new int[len];
      for (i = 0; i < len; i++) {
        variants[i] = ReadInt(@in);
      }

      int n_stypes = ReadInt(@in);
      int grammarCount = ReadInt(@in);

      len = ReadInt(@in);
      int[] grammars = new int[len];
      for (i = 0; i < len; i++) {
        grammars[i] = ReadInt(@in);
      }

      len = ReadInt(@in);
      int[] productions = new int[len];
      for (i = 0; i < len; i++) {
        productions[i] = ReadInt(@in);
      }

      len = ReadInt(@in);
      sbyte[] eventTypes = new sbyte[len];

      for (n = 0; n < len; n++) {
        eventTypes[n] = ReadSByte(@in);
      }
      int[] eventData = new int[len];
      for (i = 0; i < len; i++) {
        eventData[i] = ReadInt(@in);
      }

      EXISchema schema = new EXISchema();
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

    /// <summary>
    /// Writes out a serialized EXISchema. </summary>
    /// <param name="out"> DataOutputStream to receive the serialized EXISchema </param>
    /// <exception cref="IOException"> </exception>
    public void writeOut(Stream @out) {
      int i, len;

      len = COOKIE.Length;
      for (i = 0; i < len; i++) {
        @out.WriteByte((byte)COOKIE[i]);
      }

      len = m_elems.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        WriteInt(m_elems[i], @out);
      }

      len = m_attrs.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        WriteInt(m_attrs[i], @out);
      }

      len = m_types.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        WriteInt(m_types[i], @out);
      }

      len = uris.Length;
      Debug.Assert(len >= 4);
      WriteInt(len - 4, @out);
      for (i = 4; i < len; i++) {
        writeString(uris[i], @out);
      }

      len = m_names.Length;
      Debug.Assert(len >= EXISchemaConst.N_BUILTIN_LOCAL_NAMES);
      WriteInt(len - EXISchemaConst.N_BUILTIN_LOCAL_NAMES, @out);
      for (i = EXISchemaConst.N_BUILTIN_LOCAL_NAMES; i < len; i++) {
        writeString(m_names[i], @out);
      }

      len = m_localNames.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        WriteInt(m_localNames[i].Length, @out);
        for (int j = 0; j < m_localNames[i].Length; j++) {
          WriteInt(m_localNames[i][j], @out);
        }
      }

      len = m_strings.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        writeString(m_strings[i], @out);
      }

      len = m_ints.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        WriteInt(m_ints[i], @out);
      }

      Debug.Assert(m_mantissas.Length == m_exponents.Length);
      len = m_mantissas.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        WriteLong(m_mantissas[i], @out);
        WriteInt(m_exponents[i], @out);
      }

      Debug.Assert(m_signs.Length == m_integralDigits.Length && m_integralDigits.Length == m_reverseFractionalDigits.Length);
      len = m_signs.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        @out.WriteByte(m_signs[i] ? (byte)1 : (byte)0);
        writeString(m_integralDigits[i], @out);
        writeString(m_reverseFractionalDigits[i], @out);
      }

      len = m_integers.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        writeString(m_integers[i].ToString(NumberFormatInfo.InvariantInfo), @out);
      }

      len = m_longs.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        WriteLong(m_longs[i], @out);
      }

      len = m_datetimes.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        m_datetimes[i].writeOut(@out);
      }

      len = m_durations.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        writeString(XmlConvert.ToString(m_durations[i]), @out);
      }

      len = m_binaries.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        WriteInt(m_binaries[i].Length, @out);
        for (int j = 0; j < m_binaries[i].Length; j++) {
          @out.WriteByte((byte)m_binaries[i][j]);
        }
      }

      len = m_variantTypes.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        @out.WriteByte((byte)m_variantTypes[i]);
      }

      len = m_variants.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        WriteInt(m_variants[i], @out);
      }

      WriteInt(m_n_stypes, @out);
      WriteInt(m_grammarCount, @out);

      len = m_grammars.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        WriteInt(m_grammars[i], @out);
      }

      len = m_productions.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        WriteInt(m_productions[i], @out);
      }

      len = m_eventTypes.Length;
      WriteInt(len, @out);
      for (i = 0; i < len; i++) {
        @out.WriteByte((byte)m_eventTypes[i]);
      }
      for (i = 0; i < len; i++) {
        WriteInt(m_eventData[i], @out);
      }
    }

    internal static void writeString(string s, Stream @out) {
      int len = s.Length;
      WriteShort((short)len, @out);
      for (int i = 0; i < len; i++) {
        WriteChar(s[i], @out);
      }
    }

    internal static string readString(Stream @in) {
      int len = ReadShort(@in);
      StringBuilder stringBuider = new StringBuilder(len);
      for (int i = 0; i < len; i++) {
        stringBuider.Append(ReadChar(@in));
      }
      return stringBuider.ToString(/**/);
    }

    public void writeXml(Stream @out, bool whole) {
      StreamWriter writer = new StreamWriter(@out, Encoding.UTF8);
      writer.Write("<EXIGrammar xmlns=\"urn:publicid:nagasena\">");
      writeOutStringTable(writer);
      writeTypes(writer, whole);
      writeElems(writer);
      writeAttrs(writer);
      writeGrammars(writer);
      writer.Write("</EXIGrammar>");
      writer.Flush();
    }

    private void writeOutStringTable(StreamWriter writer) {
      writer.Write("<StringTable>");
      int i = 0;
      Debug.Assert(4 <= uris.Length);
      string[] names;
      names = localNames[i++]; // NoNamespace
      if (names.Length != 0) {
        writer.Write("<NoNamespace>");
        for (int j = 0; j < names.Length; j++) {
          writer.Write("<Name>" + names[j] + "</Name>");
        }
        writer.Write("</NoNamespace>");
      }
      names = localNames[i++]; // xml namespace
      if (names.Length > 4) {
        writer.Write("<XmlNamespace>");
        for (int j = 0; j < names.Length; j++) {
          writer.Write("<Name>" + names[j] + "</Name>");
        }
        writer.Write("</XmlNamespace>");
      }
      names = localNames[i++]; // xsi namespace
      if (names.Length > 2) {
        writer.Write("<XsiNamespace>");
        for (int j = 0; j < names.Length; j++) {
          writer.Write("<Name>" + names[j] + "</Name>");
        }
        writer.Write("</XsiNamespace>");
      }
      names = localNames[i++]; // xsd namespace
      if (names.Length > 46) {
        writer.Write("<XsdNamespace>");
        for (int j = 0; j < names.Length; j++) {
          writer.Write("<Name>" + names[j] + "</Name>");
        }
        writer.Write("</XsdNamespace>");
      }
      Debug.Assert(i == 4);
      for (; i < uris.Length; i++) {
        names = localNames[i];
        writer.Write("<Namespace>");
        writer.Write("<Uri>" + uris[i] + "</Uri>");
        for (int j = 0; j < names.Length; j++) {
          writer.Write("<Name>" + names[j] + "</Name>");
        }
        writer.Write("</Namespace>");
      }
      writer.Write("</StringTable>");
    }

    private void writeTypes(StreamWriter writer, bool whole) {
      writer.Write("<Types>");
      if (whole) {
        writeAnyType(writer);
      }
      int tp = EXISchemaLayout.SZ_COMPLEX_TYPE;
      int serial = EXISchemaConst.ANY_SIMPLE_TYPE;
      for (; tp < m_types.Length; tp += _getTypeSize(tp, m_types, ancestryIds), ++serial) {
        if (!whole && serial < EXISchemaConst.N_BUILTIN_TYPES) {
          if (!DEFAULT_TYPABLES[serial] && isTypableType(tp)) {
            writer.Write("<MakeTypable>" + serial + "</MakeTypable>");
          }
          continue;
        }
        if (isSimpleType(tp)) {
          writeSimpleType(tp, writer);
        }
        else {
          writeComplexType(tp, writer);
        }
      }
      writer.Write("</Types>");
    }

    private void writeAnyType(StreamWriter writer) {
      writer.Write("<AnyType>");
      writer.Write("<Typable/>");
      int gram = getGrammarOfType(0);
      writer.Write("<Grammar>" + getSerialOfGrammar(gram) + "</Grammar>");
      writer.Write("</AnyType>");
    }

    private void writeTypeCommon(int tp, StreamWriter writer) {
      string localName;
      if ((localName = getNameOfType(tp)).Length != 0) {
        writer.Write("<Uri>" + uris[getUriOfType(tp)] + "</Uri>");
        writer.Write("<Name>" + localName + "</Name>");
      }
      if (isTypableType(tp)) {
        writer.Write("<Typable/>");
      }
      int gram = getGrammarOfType(tp);
      writer.Write("<Grammar>" + getSerialOfGrammar(gram) + "</Grammar>");
    }

    private void writeComplexType(int tp, StreamWriter writer) {
      if (tp != 0) {
        writer.Write("<ComplexType>");
      }
      writeTypeCommon(tp, writer);
      // The rest is the part specific to complex types
      int contentDatatype = getContentDatatypeOfComplexType(tp);
      if (contentDatatype != NIL_NODE) {
        writer.Write("<ContentDatatype>" + getSerialOfType(contentDatatype) + "</ContentDatatype>");
      }
      if (tp != 0) {
        writer.Write("</ComplexType>");
      }
    }

    private void writeSimpleType(int tp, StreamWriter writer) {
      sbyte variety = getVarietyOfSimpleType(tp);
      if (variety == UR_SIMPLE_TYPE) {
        writer.Write("<AnySimpleType>");
        writer.Write("<Typable/>");
        int gram = getGrammarOfType(tp);
        writer.Write("<Grammar>" + getSerialOfGrammar(gram) + "</Grammar>");
        writer.Write("</AnySimpleType>");
        return;
      }
      int serial = -1;
      string elementName;
      if (variety == LIST_SIMPLE_TYPE) {
        elementName = "ListType";
      }
      else if (variety == UNION_SIMPLE_TYPE) {
        elementName = "UnionType";
      }
      else {
        Debug.Assert(variety == ATOMIC_SIMPLE_TYPE);
        int _tp = tp;
        while ((serial = getSerialOfType(_tp)) >= EXISchemaConst.N_PRIMITIVE_TYPES_PLUS_INTEGER) {
          _tp = getBaseTypeOfSimpleType(_tp);
        }
        Debug.Assert(serial >= 2 && serial <= EXISchemaConst.INTEGER_TYPE); // 0 is xsd:anyType, and 1 is xsd:anySimpleType
        elementName = ELEMENT_NAMES[serial];
      }
      writer.Write("<" + elementName + ">");
      writeTypeCommon(tp, writer);
      // The rest is the part specific to simple types
      writer.Write("<BaseType>" + getSerialOfType(getBaseTypeOfSimpleType(tp)) + "</BaseType>");
      if (variety == LIST_SIMPLE_TYPE) {
        int itemType = getItemTypeOfListSimpleType(tp);
        writer.Write("<ItemType>" + getSerialOfType(itemType) + "</ItemType>");
      }
      else if (variety == UNION_SIMPLE_TYPE) {
      }
      else if (variety == UR_SIMPLE_TYPE) {
      }
      else {
        Debug.Assert(variety == ATOMIC_SIMPLE_TYPE);
        if (serial == EXISchemaConst.STRING_TYPE) {
          int whiteSpace = getWhitespaceFacetValueOfStringSimpleType(tp);
          if (whiteSpace == WHITESPACE_REPLACE) {
            writer.Write("<Replace/>");
          }
          else if (whiteSpace == WHITESPACE_COLLAPSE) {
            writer.Write("<Collapse/>");
          }
          int rcsCount = getRestrictedCharacterCountOfStringSimpleType(tp);
          if (rcsCount != 0) {
            writer.Write("<RestrictedCharset>");
            int rcs = getRestrictedCharacterOfSimpleType(tp);
            int prevChar = m_types[rcs + 0];
            int startChar = prevChar;
            for (int i = 1; i < rcsCount; i++) {
              int ch = m_types[rcs + i];
              if (prevChar + 1 == ch) {
                prevChar = ch;
              }
              else {
                Debug.Assert(prevChar + 1 < ch);
                if (startChar == prevChar) {
                  writer.Write("<Char>" + prevChar + "</Char>");
                }
                else {
                  Debug.Assert(startChar < prevChar);
                  writer.Write("<StartChar>" + startChar + "</StartChar>");
                  writer.Write("<EndChar>" + prevChar + "</EndChar>");
                }
                startChar = prevChar = ch;
              }
            }
            if (startChar == prevChar) {
              writer.Write("<Char>" + prevChar + "</Char>");
            }
            else {
              Debug.Assert(startChar < prevChar);
              writer.Write("<StartChar>" + startChar + "</StartChar>");
              writer.Write("<EndChar>" + prevChar + "</EndChar>");
            }
            writer.Write("</RestrictedCharset>");
          }
          writeEnumerations(tp, "String", writer);
        }
        else if (serial == EXISchemaConst.BOOLEAN_TYPE) {
          if (isPatternedBooleanSimpleType(tp)) {
            writer.Write("<Patterned/>");
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
          int width = getWidthOfIntegralSimpleType(tp);
          if (width == INTEGER_CODEC_NONNEGATIVE) {
            writer.Write("<NonNegative/>");
          }
          else if (width != INTEGER_CODEC_DEFAULT) {
            Debug.Assert(0 <= width && width <= 12);
            writer.Write("<NBit>" + width + "</NBit>");
            int variant = getMinInclusiveFacetOfIntegerSimpleType(tp);
            writer.Write("<MinInteger>" + getVariantCharacters(variant).makeString() + "</MinInteger>");
          }
          writeEnumerations(tp, "Integer", writer);
        }
        else {
          Debug.Assert(false);
        }
      }
      writer.Write("</" + elementName + ">");
    }

    private void writeEnumerations(int tp, string valueTagName, StreamWriter writer) {
      if (isEnumeratedAtomicSimpleType(tp)) {
        int n_enums = 0;
        int enumCount = getEnumerationFacetCountOfAtomicSimpleType(tp);
        for (int i = 0; i < enumCount; i++) {
          int variant;
          if ((variant = getEnumerationFacetOfAtomicSimpleType(tp, i)) != EXISchema.NIL_VALUE) {
            if (n_enums++ == 0) {
              writer.Write("<Enumeration>");
            }
            writer.Write("<" + valueTagName + ">");
            string stringValue = getVariantCharacters(variant).makeString();
            if ("String".Equals(valueTagName)) {
              StringBuilder stringBuilder = new StringBuilder();
              int len = stringValue.Length;
              for (int j = 0; j < len; j++) {
                char c;
                switch (c = stringValue[j]) {
                  case '\u000A':
                    stringBuilder.Append("&#xA;");
                    break;
                  case '\u000D':
                    stringBuilder.Append("&#xD;");
                    break;
                  case '\u0009':
                    stringBuilder.Append("&#x9;");
                    break;
                  default:
                    stringBuilder.Append(c);
                    break;
                }
              }
              stringValue = stringBuilder.ToString(/**/);
            }
            writer.Write(stringValue);
            writer.Write("</" + valueTagName + ">");
          }
        }
        if (n_enums != 0) {
          writer.Write("</Enumeration>");
        }
      }
    }

    private void writeElems(StreamWriter writer) {
      writer.Write("<Elements>");
      int lastUri, lastName;
      lastUri = lastName = -1;
      for (int elem = 0; elem < m_elems.Length; elem += EXISchemaLayout.SZ_ELEM) {
        int uri = getUriOfElem(elem);
        int name = getLocalNameOfElem(elem);
        if (uri != lastUri || name != lastName) {
          writer.Write("<Uri>" + uris[uri] + "</Uri>");
          writer.Write("<Name>" + m_names[m_localNames[uri][name]] + "</Name>");
          lastUri = uri;
          lastName = name;
        }
        bool isGlobal = isGlobalElem(elem);
        if (isGlobal) {
          writer.Write("<GlobalElement>");
        }
        else {
          writer.Write("<LocalElement>");
        }
        writer.Write("<Type>" + getSerialOfType(getTypeOfElem(elem)) + "</Type>");
        if (isNillableElement(elem)) {
          writer.Write("<Nillable/>");
        }
        if (isGlobal) {
          writer.Write("</GlobalElement>");
        }
        else {
          writer.Write("</LocalElement>");
        }
      }
      writer.Write("</Elements>");
    }

    private void writeAttrs(StreamWriter writer) {
      writer.Write("<Attributes>");
      int lastUri, lastName;
      lastUri = lastName = -1;
      for (int attr = 0; attr < m_attrs.Length; attr += EXISchemaLayout.SZ_ATTR) {
        int uri = getUriOfAttr(attr);
        int name = getLocalNameOfAttr(attr);
        if (uri != lastUri || name != lastName) {
          writer.Write("<Uri>" + uris[uri] + "</Uri>");
          writer.Write("<Name>" + m_names[m_localNames[uri][name]] + "</Name>");
          lastUri = uri;
          lastName = name;
        }
        bool isGlobal = isGlobalAttr(attr);
        if (isGlobal) {
          writer.Write("<GlobalAttribute>");
        }
        else {
          writer.Write("<LocalAttribute>");
        }
        writer.Write("<Type>" + getSerialOfType(getTypeOfAttr(attr)) + "</Type>");
        if (isGlobal) {
          writer.Write("</GlobalAttribute>");
        }
        else {
          writer.Write("</LocalAttribute>");
        }
      }
      writer.Write("</Attributes>");
    }

    private void writeGrammars(StreamWriter writer) {
      writer.Write("<Grammars>");
      int gram, serial;
      for (gram = 0, serial = 0; gram < m_grammars.Length; gram += getSizeOfGrammar(gram, m_grammars), ++serial) {
        if (serial < 7) { // Grammars at indices 0 through 6 are fixtures.
          continue;
        }
        writer.Write("<Grammar>");
        writer.Write("<Productions>");
        bool needEndElem = hasEndElement(gram);
        int n_productions = getProductionCountOfGrammar(gram);
        for (int i = 0; i < n_productions; i++) {
          int prod = getProductionOfGrammar(gram, i);
          int @event = getEventOfProduction(prod);
          switch (@event) {
            case EXISchema.EVENT_AT_WILDCARD:
              writer.Write("<AttributeWildcard/>");
              break;
            case EXISchema.EVENT_SE_WILDCARD:
              writer.Write("<ElementWildcard/>");
              break;
            case EXISchema.EVENT_CH_UNTYPED:
            case EXISchema.EVENT_CH_TYPED:
              if (hasEndElement(gram)) {
                writer.Write("<EndElement/>");
                needEndElem = false;
              }
              if (@event == EXISchema.EVENT_CH_UNTYPED) {
                writer.Write("<CharactersMixed/>");
              }
              else {
                writer.Write("<CharactersTyped/>");
              }
              break;
            default:
              switch (getEventType(@event)) {
                case EVENT_TYPE_AT:
                  int attr = getNodeOfEventType(@event);
                  writer.Write("<Attribute>" + attr / EXISchemaLayout.SZ_ATTR + "</Attribute>");
                  break;
                case EVENT_TYPE_SE:
                  int elem = getNodeOfEventType(@event);
                  writer.Write("<Element>" + getSerialOfElem(elem) + "</Element>");
                  break;
                case EVENT_TYPE_AT_WILDCARD_NS:
                  writer.Write("<AttributeWildcardNS>" + uris[getUriOfEventType(@event)] + "</AttributeWildcardNS>");
                  break;
                case EVENT_TYPE_SE_WILDCARD_NS:
                  writer.Write("<ElementWildcardNS>" + uris[getUriOfEventType(@event)] + "</ElementWildcardNS>");
                  break;
                default:
                  Debug.Assert(false);
                  break;
              }
              break;
          }
          writer.Write("<Grammar>" + getSerialOfGrammar(getGrammarOfProduction(prod)) + "</Grammar>");
        }
        if (needEndElem) {
          writer.Write("<EndElement/>");
        }
        writer.Write("</Productions>");
        if (hasContentGrammar(gram)) {
          writer.Write("<ContentGrammar>" + getSerialOfGrammar(getContentGrammarOfGrammar(gram)) + "</ContentGrammar>");
        }
        if (hasEmptyGrammar(gram)) {
          writer.Write("<EmptyGrammar>" + getSerialOfGrammar(getTypeEmptyGrammarOfGrammar(gram)) + "</EmptyGrammar>");
        }
        writer.Write("</Grammar>");
      }
      writer.Write("</Grammars>");
    }

    internal static int ReadInt(Stream @in) {
      byte[] byteArray = new byte[4];
      int iBytesRead = @in.Read(byteArray, 0, 4);
      if (iBytesRead != 4)
        throw new EndOfStreamException();
      Array.Reverse(byteArray);                        
      return BitConverter.ToInt32(byteArray, 0);
      //int b0, b1, b2, b3;
      //if ((b0 = @in.ReadByte()) == -1)
      //  throw new EndOfStreamException();
      //if ((b1 = @in.ReadByte()) == -1)
      //  throw new EndOfStreamException();
      //if ((b2 = @in.ReadByte()) == -1)
      //  throw new EndOfStreamException();
      //if ((b3 = @in.ReadByte()) == -1)
      //  throw new EndOfStreamException();
      //return (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
    }

    private static short ReadShort(Stream @in) {
      byte[] byteArray = new byte[2];
      int iBytesRead = @in.Read(byteArray, 0, 2);
      if (iBytesRead != 2)
        throw new EndOfStreamException();
      Array.Reverse(byteArray);
      return BitConverter.ToInt16(byteArray, 0);
    }

    private static char ReadChar(Stream @in) {
      byte[] byteArray = new byte[2];
      int iBytesRead = @in.Read(byteArray, 0, 2);
      if (iBytesRead != 2)
        throw new EndOfStreamException();
      Array.Reverse(byteArray);
      return BitConverter.ToChar(byteArray, 0);
    }

    internal static long ReadLong(Stream @in) {
      byte[] byteArray = new byte[8];
      int iBytesRead = @in.Read(byteArray, 0, 8);
      if (iBytesRead != 8)
        throw new EndOfStreamException();
      Array.Reverse(byteArray);
      return BitConverter.ToInt64(byteArray, 0);
    }

    internal static bool ReadBoolean(Stream @in) {
      int bt = @in.ReadByte();
      if (bt == -1)
        throw new EndOfStreamException();
      return bt != 0;
    }

    internal static sbyte ReadSByte(Stream @in) {
      int bt = @in.ReadByte();
      if (bt == -1)
        throw new EndOfStreamException();
      return (sbyte)bt;
    }

    internal static byte ReadByte(Stream @in) {
      int bt = @in.ReadByte();
      if (bt == -1)
        throw new EndOfStreamException();
      return (byte)bt;
    }

    internal static void WriteShort(short value, Stream @out) {
      byte[] bytes = BitConverter.GetBytes(value);
      Debug.Assert(bytes.Length == 2);
      Array.Reverse(bytes, 0, 2);
      @out.Write(bytes, 0, 2);
    }

    internal static void WriteChar(char value, Stream @out) {
      byte[] bytes = BitConverter.GetBytes(value);
      Debug.Assert(bytes.Length == 2);
      Array.Reverse(bytes, 0, 2);
      @out.Write(bytes, 0, 2);
    }

    internal static void WriteInt(int value, Stream @out) {
      byte[] bytes = BitConverter.GetBytes(value);
      Debug.Assert(bytes.Length == 4);
      Array.Reverse(bytes, 0, 4);
      @out.Write(bytes, 0, 4);
    }

    internal static void WriteLong(long value, Stream @out) {
      byte[] bytes = BitConverter.GetBytes(value);
      Debug.Assert(bytes.Length == 8);
      Array.Reverse(bytes, 0, 8);
      @out.Write(bytes, 0, 8);
    }

  }

}