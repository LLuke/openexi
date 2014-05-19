using System;
using System.Diagnostics;
using System.Collections.Generic;
using System.Numerics;

using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using EXISchemaLayout = Nagasena.Schema.EXISchemaLayout;
using XSDateTime = Nagasena.Schema.XSDateTime;

namespace Nagasena.Scomp {

  /// <summary>
  /// SchemaStruct provides functions to manipulate on the schema arrays.
  /// </summary>
  /// <exclude/>
  public abstract class EXISchemaStruct {

    // The sole purpose of SchemaStruct is to populate these arrays.
    protected internal int[] m_elems; // int array describing elements
    protected internal int[] m_attrs; // int array describing attributes
    protected internal int[] m_types; // int array describing types
    protected internal string[] m_uris; // array of interned strings representing uris
    protected internal string[] m_names; // array of interned strings
    protected internal int[][] m_localNames;
    protected internal string[] m_strings; // array of non-interned strings
    protected internal int[] m_ints; // array of int values
    protected internal long[] m_mantissas; // array of long representing mantissas
    protected internal int[] m_exponents; // array of int representing exponents
    protected internal bool[] m_signs; // array of decimal value signs
    protected internal string[] m_integralDigits; // array of decimal integral value
    protected internal string[] m_reverseFractionalDigits; // array of decimal reverse-fractional digits value
    protected internal BigInteger[] m_integers; // array of integer values
    protected internal long[] m_longs; // array of long values
    protected internal XSDateTime[] m_datetimes; // array of datetime values
    protected internal TimeSpan[] m_durations; // array of duration values
    protected internal byte[][] m_binaries; // array of binary values
    protected internal sbyte[] m_variantTypes; // array of variant types
    protected internal int[] m_variants; // array of variant values
    protected internal int[] m_grammars; // int array describing grammar structure
    protected internal int[] m_productions; // int array containing productions
    protected internal sbyte[] m_eventTypes; // byte array of event types
    protected internal int[] m_eventData; // int array of event data (i.e. node or uri)

    protected internal int m_n_elems; // number of slots used in m_elems
    protected internal int m_n_attrs; // number of slots used in m_attrs
    protected internal int m_n_types; // number of slots used in m_types
    protected internal int m_n_uris; // number of slots used in m_uris
    protected internal int m_n_names; // number of slots used in m_names
    protected internal int m_n_strings; // number of slots used in m_strings
    protected internal int m_n_ints; // number of slots used in m_ints
    protected internal int m_n_floats; // number of slots used in m_mantissas and m_exponents
    protected internal int m_n_decimals; // number of decimals used in m_decimals
    protected internal int m_n_integers; // number of decimals used in m_integers
    protected internal int m_n_longs; // number of decimals used in m_longs
    protected internal int m_n_datetimes; // number of decimals used in m_datetimes
    protected internal int m_n_durations; // number of decimals used in m_durations
    protected internal int m_n_binaries; // number of binaries used in m_binaries
    protected internal int m_n_qnames; // number of qnames used in m_qnames
    protected internal int m_n_lists; // number of lists used in m_lists
    protected internal int m_n_variants; // number of slots used in m_variants, m_variantTypes
    protected internal int m_n_grammars; // number of slots used in m_grammars
    protected internal int m_n_productions; // number of slots used in m_productions
    protected internal int m_n_events; // number of slots used in m_eventTypes and m_eventData

    protected internal int m_n_stypes; // used for counting the number of simple types

    protected internal int m_grammarCount; // the number of grammars in m_grammars

    // Event content composite to event ID 
    private readonly Dictionary<int?, int?> m_eventMap;

    private const int NODES_INITIAL = 1024;
    private const int GRAMMARS_INITIAL = 1024;
    private const int PRODUCTIONS_INITIAL = 1024;
    private const int EVENTS_INITIAL = 1024;
    private const int URIS_INITIAL = 16;
    private const int URIS_INCREMENT = 16;
    private const int NAMES_INITIAL = 1024;
    private const int NAMES_INCREMENT = 1024;
    private const int VALUES_INITIAL = 1024;
    private const int VALUES_INCREMENT = 1024;

    protected internal static string[] INITIAL_URI_ENTRIES = new string[] { 
      "",
      "http://www.w3.org/XML/1998/namespace", 
      "http://www.w3.org/2001/XMLSchema-instance", 
      "http://www.w3.org/2001/XMLSchema" 
    };

    private static readonly BigInteger INT_MIN_VALUE, LONG_MIN_VALUE;
    static EXISchemaStruct() {
      INT_MIN_VALUE = new BigInteger((long)int.MinValue);
      LONG_MIN_VALUE = new BigInteger((long)long.MinValue);
    }

    /// <summary>
    /// Construct a SchemaStruct.
    /// </summary>
    protected internal EXISchemaStruct() {
      m_eventMap = new Dictionary<int?, int?>();
    }

    /// <summary>
    /// Prepares for the next run.
    /// </summary>
    protected internal virtual void reset() {
      clear(); // do clean-up first

      // initialize the arrays
      m_elems = new int[NODES_INITIAL];
      m_attrs = new int[NODES_INITIAL];
      m_types = new int[NODES_INITIAL];
      m_grammars = new int[GRAMMARS_INITIAL];
      m_productions = new int[PRODUCTIONS_INITIAL];
      m_eventTypes = new sbyte[EVENTS_INITIAL];
      m_eventData = new int[EVENTS_INITIAL];
      m_uris = new string[URIS_INITIAL];
      m_names = new string[NAMES_INITIAL];
      m_strings = new string[VALUES_INITIAL];
      m_ints = new int[VALUES_INITIAL];
      m_mantissas = new long[VALUES_INITIAL];
      m_exponents = new int[VALUES_INITIAL];
      m_signs = new bool[VALUES_INITIAL];
      m_integralDigits = new string[VALUES_INITIAL];
      m_reverseFractionalDigits = new string[VALUES_INITIAL];
      m_integers = new BigInteger[VALUES_INITIAL];
      m_longs = new long[VALUES_INITIAL];
      m_datetimes = new XSDateTime[VALUES_INITIAL];
      m_durations = new TimeSpan[VALUES_INITIAL];
      m_binaries = new byte[VALUES_INITIAL][];
      m_variantTypes = new sbyte[VALUES_INITIAL];
      m_variants = new int[VALUES_INITIAL];

      m_n_elems = 0;
      m_n_attrs = 0;
      m_n_types = 0;
      m_n_grammars = 0;
      m_n_productions = 0;
      m_n_events = 0;
      m_n_uris = 0;
      m_n_names = 0;
      m_n_strings = 0;
      m_n_ints = 0;
      m_n_floats = 0;
      m_n_decimals = 0;
      m_n_integers = 0;
      m_n_longs = 0;
      m_n_datetimes = 0;
      m_n_durations = 0;
      m_n_binaries = 0;
      m_n_qnames = 0;
      m_n_lists = 0;
      m_n_variants = 0;

      m_n_stypes = 0;

      m_grammarCount = 0;

      int i;
      for (i = 0; i < INITIAL_URI_ENTRIES.Length; i++) {
        m_uris[m_n_uris++] = INITIAL_URI_ENTRIES[i];
      }

      m_names[m_n_names++] = "";
      for (i = 0; i < EXISchemaConst.XML_LOCALNAMES.Length; i++) {
        m_names[m_n_names++] = EXISchemaConst.XML_LOCALNAMES[i];
      }
      for (i = 0; i < EXISchemaConst.XSI_LOCALNAMES.Length; i++) {
        m_names[m_n_names++] = EXISchemaConst.XSI_LOCALNAMES[i];
      }
      for (i = 0; i < EXISchemaConst.XSD_LOCALNAMES.Length; i++) {
        m_names[m_n_names++] = EXISchemaConst.XSD_LOCALNAMES[i];
      }

      Debug.Assert(m_n_strings == 0);
      m_strings[EXISchema.EMPTY_STRING] = "";
      m_n_strings++;
    }

    /// <summary>
    /// Releases resources that was allocated in the previous run.
    /// </summary>
    protected internal virtual void clear() {
      // delete the arrays
      m_elems = null;
      m_attrs = null;
      m_types = null;
      m_grammars = null;
      m_productions = null;
      m_eventTypes = null;
      m_eventData = null;
      m_uris = null;
      m_names = null;
      m_localNames = null;
      m_strings = null;
      m_ints = null;
      m_mantissas = null;
      m_exponents = null;
      m_signs = null;
      m_integralDigits = null;
      m_reverseFractionalDigits = null;
      m_integers = null;
      m_longs = null;
      m_datetimes = null;
      m_durations = null;
      m_binaries = null;
      m_variantTypes = null;
      m_variants = null;

      m_eventMap.Clear();
    }

    /////////////////////////////////////////////////////////////////////////
    // House-keeping procedures
    /////////////////////////////////////////////////////////////////////////

    protected internal void ensureElems(int size) {
      while (m_n_elems + size > m_elems.Length) {
        int[] nodes = new int[2 * m_elems.Length];
        Array.Copy(m_elems, 0, nodes, 0, m_elems.Length);
        m_elems = nodes;
      }
    }

    protected internal void ensureAttrs(int size) {
      while (m_n_attrs + size > m_attrs.Length) {
        int[] attrs = new int[2 * m_attrs.Length];
        Array.Copy(m_attrs, 0, attrs, 0, m_attrs.Length);
        m_attrs = attrs;
      }
    }

    protected internal void ensureTypes(int size) {
      while (m_n_types + size > m_types.Length) {
        int[] types = new int[2 * m_types.Length];
        Array.Copy(m_types, 0, types, 0, m_types.Length);
        m_types = types;
      }
    }

    protected internal void ensureVariants() {
      ensureVariants(1);
    }

    protected internal void ensureVariants(int size) {
      while (m_n_variants + size > m_variants.Length) {
        sbyte[] variantTypes = new sbyte[m_variantTypes.Length + VALUES_INCREMENT];
        int[] variants = new int[m_variants.Length + VALUES_INCREMENT];
        Array.Copy(m_variantTypes, 0, variantTypes, 0, m_variantTypes.Length);
        Array.Copy(m_variants, 0, variants, 0, m_variants.Length);
        m_variantTypes = variantTypes;
        m_variants = variants;
      }
    }

    protected internal int indexOfUri(string uri) {
      for (int i = 0; i < m_n_uris; i++) {
        if (uri.Equals(m_uris[i])) {
          return i;
        }
      }
      return -1;
    }

    /// <summary>
    /// Intern a uri. </summary>
    /// <returns> id of the uri </returns>
    protected internal int internUri(string uri) {
      int index;
      if ((index = indexOfUri(uri)) != -1) {
        return index;
      }
      // Ensure array size
      if (m_n_uris == m_uris.Length) {
        string[] uris = new string[m_uris.Length + URIS_INCREMENT];
        Array.Copy(m_uris, 0, uris, 0, m_uris.Length);
        m_uris = uris;
      }
      m_uris[m_n_uris] = uri;
      return m_n_uris++;
    }

    protected internal int indexOfLocalName(string name, int uri) {
      int[] localNames = m_localNames[uri];
      for (int i = 0; i < localNames.Length; i++) {
        if (m_names[localNames[i]].Equals(name)) {
          return i;
        }
      }
      return -1;
    }

    protected internal int indexOfName(string name) {
      for (int i = 0; i < m_n_names; i++) {
        if (name.Equals(m_names[i])) {
          return i;
        }
      }
      return -1;
    }

    /// <summary>
    /// Intern a name. </summary>
    /// <returns> id of the name </returns>
    protected internal int internName(string name) {
      int index;
      if ((index = indexOfName(name)) != -1) {
        return index;
      }
      // Ensure array size
      if (m_n_names >= m_names.Length) {
        string[] names = new string[m_names.Length + NAMES_INCREMENT];
        Array.Copy(m_names, 0, names, 0, m_names.Length);
        m_names = names;
      }
      m_names[m_n_names] = name;
      return m_n_names++;
    }

    protected internal int addStringValue(string val) {
      int pos = EXISchema.NIL_VALUE;
      if (val != null) {
        if (val.Length == 0) {
          return EXISchema.EMPTY_STRING;
        }
        // Ensure array size
        if (m_n_strings >= m_strings.Length) {
          string[] vals = new string[m_strings.Length + VALUES_INCREMENT];
          Array.Copy(m_strings, 0, vals, 0, m_strings.Length);
          m_strings = vals;
        }
        // Append the value
        m_strings[m_n_strings] = val;
        pos = m_n_strings++;
      }
      return pos;
    }

    protected internal int addIntValue(int val) {
      int pos = EXISchema.NIL_VALUE;
      // Ensure array size
      if (m_n_ints >= m_ints.Length) {
        int[] vals = new int[m_ints.Length + VALUES_INCREMENT];
        Array.Copy(m_ints, 0, vals, 0, m_ints.Length);
        m_ints = vals;
      }
      // Append the value
      m_ints[m_n_ints] = val;
      pos = m_n_ints++;
      return pos;
    }

    protected internal int addFloatValue(long mantissa, int exponent) {
      int pos = EXISchema.NIL_VALUE;
      // Ensure array size
      if (m_n_floats == m_mantissas.Length) {
        int newLength = m_n_floats + VALUES_INCREMENT;
        long[] _mantissas = new long[newLength];
        Array.Copy(m_mantissas, 0, _mantissas, 0, m_n_floats);
        m_mantissas = _mantissas;
        int[] _exponents = new int[newLength];
        Array.Copy(m_exponents, 0, _exponents, 0, m_n_floats);
        m_exponents = _exponents;
      }
      m_mantissas[m_n_floats] = mantissa;
      m_exponents[m_n_floats] = exponent;
      pos = m_n_floats++;
      return pos;
    }

    protected internal int addDecimalValue(bool sign, string integralDigits, string reverseFractionalDigits) {
      int pos = EXISchema.NIL_VALUE;
      // Ensure array size
      if (m_n_decimals >= m_signs.Length) {
        int newLength = m_n_decimals + VALUES_INCREMENT;
        bool[] _signs = new bool[newLength];
        Array.Copy(m_signs, 0, _signs, 0, m_n_decimals);
        m_signs = _signs;
        string[] _integralDigits = new string[newLength];
        Array.Copy(m_integralDigits, 0, _integralDigits, 0, m_n_decimals);
        m_integralDigits = _integralDigits;
        string[] _reverseFractionalDigits = new string[newLength];
        Array.Copy(m_reverseFractionalDigits, 0, _reverseFractionalDigits, 0, m_n_decimals);
        m_reverseFractionalDigits = _reverseFractionalDigits;
      }
      m_signs[m_n_decimals] = sign;
      m_integralDigits[m_n_decimals] = integralDigits;
      m_reverseFractionalDigits[m_n_decimals] = reverseFractionalDigits;
      pos = m_n_decimals++;
      return pos;
    }

    protected internal int addIntegerValue(BigInteger val) {
      int pos = EXISchema.NIL_VALUE;
      if (val != null) {
        // Ensure array size
        if (m_n_integers >= m_integers.Length) {
          BigInteger[] vals = new BigInteger[m_integers.Length + VALUES_INCREMENT];
          Array.Copy(m_integers, 0, vals, 0, m_integers.Length);
          m_integers = vals;
        }
        // Append the value
        m_integers[m_n_integers] = val;
        pos = m_n_integers++;
      }
      return pos;
    }

    protected internal int addLongValue(long val) {
      int pos = EXISchema.NIL_VALUE;
      // Ensure array size
      if (m_n_longs >= m_longs.Length) {
        long[] vals = new long[m_longs.Length + VALUES_INCREMENT];
        Array.Copy(m_longs, 0, vals, 0, m_longs.Length);
        m_longs = vals;
      }
      // Append the value
      m_longs[m_n_longs] = val;
      pos = m_n_longs++;
      return pos;
    }

    protected internal int addDateTimeValue(XSDateTime val) {
      int pos = EXISchema.NIL_VALUE;
      if (val != null) {
        // Ensure array size
        if (m_n_datetimes >= m_datetimes.Length) {
          XSDateTime[] vals = new XSDateTime[m_datetimes.Length + VALUES_INCREMENT];
          Array.Copy(m_datetimes, 0, vals, 0, m_datetimes.Length);
          m_datetimes = vals;
        }
        // Append the value
        m_datetimes[m_n_datetimes] = val;
        pos = m_n_datetimes++;
      }
      return pos;
    }

    protected internal int addDurationValue(TimeSpan val) {
      int pos = EXISchema.NIL_VALUE;
      if (val != null) {
        // Ensure array size
        if (m_n_durations >= m_durations.Length) {
          TimeSpan[] vals = new TimeSpan[m_durations.Length + VALUES_INCREMENT];
          Array.Copy(m_durations, 0, vals, 0, m_durations.Length);
          m_durations = vals;
        }
        // Append the value
        m_durations[m_n_durations] = val;
        pos = m_n_durations++;
      }
      return pos;
    }

    protected internal int addBinaryValue(byte[] val) {
      int pos = EXISchema.NIL_VALUE;
      if (val != null) {
        // Ensure array size
        if (m_n_binaries >= m_binaries.Length) {
          byte[][] vals = new byte[m_binaries.Length + VALUES_INCREMENT][];
          Array.Copy(m_binaries, 0, vals, 0, m_binaries.Length);
          m_binaries = vals;
        }
        // Append the value
        m_binaries[m_n_binaries] = val;
        pos = m_n_binaries++;
      }
      return pos;
    }

    protected internal int addVariantStringValue(string val) {
      int pos = EXISchema.NIL_VALUE;
      if (val != null) {
        ensureVariants(); // Ensure variant array size
        // Append the value
        m_variantTypes[m_n_variants] = EXISchema.VARIANT_STRING;
        m_variants[m_n_variants] = addStringValue(val);
        pos = m_n_variants++;
      }
      return pos;
    }

    protected internal int addVariantBooleanValue(bool val) {
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_BOOLEAN;
      m_variants[m_n_variants] = val ? EXISchema.TRUE_VALUE : EXISchema.FALSE_VALUE;
      return m_n_variants++;
    }

    protected internal int addVariantFloatValue(long mantissa, int exponent) {
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_FLOAT;
      m_variants[m_n_variants] = addFloatValue(mantissa, exponent);
      return m_n_variants++;
    }

    protected internal int addVariantDecimalValue(bool sign, string integralDigits, string reverseFractionalDigits) {
      int pos = EXISchema.NIL_VALUE;
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_DECIMAL;
      m_variants[m_n_variants] = addDecimalValue(sign, integralDigits, reverseFractionalDigits);
      pos = m_n_variants++;
      return pos;
    }

    protected internal int addVariantIntegerValue(BigInteger val) {
      int pos = EXISchema.NIL_VALUE;
      if (val != null) {
        ensureVariants(); // Ensure variant array size
        // Append the value
        m_variantTypes[m_n_variants] = EXISchema.VARIANT_INTEGER;
        m_variants[m_n_variants] = addIntegerValue(val);
        pos = m_n_variants++;
      }
      return pos;
    }

    protected internal int addVariantIntValue(int val) {
      int pos = EXISchema.NIL_VALUE;
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_INT;
      m_variants[m_n_variants] = addIntValue(val);
      pos = m_n_variants++;
      return pos;
    }

    protected internal int addVariantLongValue(long val) {
      int pos = EXISchema.NIL_VALUE;
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_LONG;
      m_variants[m_n_variants] = addLongValue(val);
      pos = m_n_variants++;
      return pos;
    }

    protected internal int addVariantDateTimeValue(XSDateTime val) {
      int pos = EXISchema.NIL_VALUE;
      if (val != null) {
        ensureVariants(); // Ensure variant array size
        // Append the value
        m_variantTypes[m_n_variants] = EXISchema.VARIANT_DATETIME;
        m_variants[m_n_variants] = addDateTimeValue(val);
        pos = m_n_variants++;
      }
      return pos;
    }

    protected internal int addVariantDurationValue(TimeSpan val) {
      int pos = EXISchema.NIL_VALUE;
      if (val != null) {
        ensureVariants(); // Ensure variant array size
        // Append the value
        m_variantTypes[m_n_variants] = EXISchema.VARIANT_DURATION;
        m_variants[m_n_variants] = addDurationValue(val);
        pos = m_n_variants++;
      }
      return pos;
    }

    protected internal int addVariantBinaryValue(byte[] val, sbyte variantType) {
      Debug.Assert(variantType == EXISchema.VARIANT_BASE64 || variantType == EXISchema.VARIANT_HEXBIN);
      int pos = EXISchema.NIL_VALUE;
      if (val != null) {
        ensureVariants(); // Ensure variant array size
        // Append the value
        m_variantTypes[m_n_variants] = variantType;
        m_variants[m_n_variants] = addBinaryValue(val);
        pos = m_n_variants++;
      }
      return pos;
    }

    protected internal virtual int doIntegralVariantValue(BigInteger integer, int marginWidth) {
      int margin = (1 << marginWidth) - 1;
      BigInteger limitInt = new BigInteger((long)int.MaxValue - margin);
      BigInteger limitLong = new BigInteger(long.MaxValue - margin);
      int variant;
      if (INT_MIN_VALUE <= integer && integer <= limitInt) {
        variant = addVariantIntValue((int)integer);
      }
      else if (LONG_MIN_VALUE <= integer && integer <= limitLong) {
        variant = addVariantLongValue((long)integer);
      }
      else {
        variant = addVariantIntegerValue(integer);
      }
      return variant;
    }

    protected internal void ensureGrammar(int size) {
      while (m_n_grammars + size > m_grammars.Length) {
        int[] grammars = new int[2 * m_grammars.Length];
        Array.Copy(m_grammars, 0, grammars, 0, m_grammars.Length);
        m_grammars = grammars;
      }
    }

    protected internal void ensureProduction() {
      if (m_n_productions + EXISchemaLayout.SZ_PRODUCTION > m_productions.Length) {
        int[] productions = new int[2 * m_productions.Length];
        Array.Copy(m_productions, 0, productions, 0, m_productions.Length);
        m_productions = productions;
      }
    }

    private void ensureEvent() {
      if (m_n_events + 1 > m_eventTypes.Length) {
        sbyte[] eventTypes = new sbyte[2 * m_eventTypes.Length];
        Array.Copy(m_eventTypes, 0, eventTypes, 0, m_eventTypes.Length);
        m_eventTypes = eventTypes;
        int[] eventData = new int[2 * m_eventData.Length];
        Array.Copy(m_eventData, 0, eventData, 0, m_eventData.Length);
        m_eventData = eventData;
      }
    }

    protected internal int addEvent(sbyte eventType, int nd) {
      Debug.Assert((eventType == EXISchema.EVENT_TYPE_AT || eventType == EXISchema.EVENT_TYPE_SE) && nd != EXISchema.NIL_NODE);
      int eventComposite = (nd << 2) | (byte)eventType;
      int? _event;
      if (m_eventMap.TryGetValue(eventComposite, out _event)) {
        return (int)_event;
      }
      ensureEvent();
      int @event = m_n_events++;
      m_eventMap[eventComposite] = @event;
      m_eventTypes[@event] = eventType;
      m_eventData[@event] = nd;
      return @event;
    }

    protected internal int addEvent(sbyte eventType, string uri) {
      Debug.Assert(eventType == EXISchema.EVENT_TYPE_AT_WILDCARD_NS || eventType == EXISchema.EVENT_TYPE_SE_WILDCARD_NS);
      int index = indexOfUri(uri);
      Debug.Assert(0 <= index);
      int eventComposite = (index << 2) | (byte)eventType;
      int? _event;
      if (m_eventMap.TryGetValue(eventComposite, out _event)) {
        return (int)_event;
      }
      ensureEvent();
      int @event = m_n_events++;
      m_eventMap[eventComposite] = @event;
      m_eventTypes[@event] = eventType;
      m_eventData[@event] = index;
      return @event;
    }

  }

}