package org.openexi.scomp;

import java.math.BigInteger;
import java.util.HashMap;

import javax.xml.datatype.Duration;

import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.EXISchemaLayout;
import org.openexi.schema.XSDateTime;

/**
 * SchemaStruct provides functions to manipulate on the schema arrays.
 */
abstract class EXISchemaStruct {

  // The sole purpose of SchemaStruct is to populate these arrays.
  protected int[]        m_elems;        // int array describing elements
  protected int[]        m_attrs;        // int array describing attributes
  protected int[]        m_types;        // int array describing types
  protected String[]     m_uris;         // array of interned strings representing uris
  protected String[]     m_names;        // array of interned strings
  protected int[][]      m_localNames;
  protected String[]     m_strings;      // array of non-interned strings
  protected int[]        m_ints;         // array of int values
  protected long[]       m_mantissas;    // array of long representing mantissas
  protected int[]        m_exponents;    // array of int representing exponents
  protected boolean[]    m_signs;        // array of decimal value signs        
  protected String[]     m_integralDigits; // array of decimal integral value
  protected String[]     m_reverseFractionalDigits; // array of decimal reverse-fractional digits value
  protected BigInteger[] m_integers;     // array of integer values
  protected long[]       m_longs;        // array of long values
  protected XSDateTime[] m_datetimes;    // array of datetime values
  protected Duration[]   m_durations;    // array of duration values
  protected byte[][]     m_binaries;     // array of binary values
  protected byte[]       m_variantTypes; // array of variant types
  protected int[]        m_variants;     // array of variant values
  protected int[]        m_grammars;     // int array describing grammar structure
  protected int[]        m_productions;  // int array containing productions
  protected byte[]       m_eventTypes;   // byte array of event types
  protected int[]        m_eventData;    // int array of event data (i.e. node or uri)

  protected int      m_n_elems;     // number of slots used in m_elems
  protected int      m_n_attrs;     // number of slots used in m_attrs
  protected int      m_n_types;     // number of slots used in m_types
  protected int      m_n_uris;      // number of slots used in m_uris
  protected int      m_n_names;     // number of slots used in m_names
  protected int      m_n_strings;   // number of slots used in m_strings
  protected int      m_n_ints;      // number of slots used in m_ints
  protected int      m_n_floats;    // number of slots used in m_mantissas and m_exponents
  protected int      m_n_decimals;  // number of decimals used in m_decimals
  protected int      m_n_integers;  // number of decimals used in m_integers
  protected int      m_n_longs;     // number of decimals used in m_longs
  protected int      m_n_datetimes; // number of decimals used in m_datetimes
  protected int      m_n_durations; // number of decimals used in m_durations
  protected int      m_n_binaries;  // number of binaries used in m_binaries
  protected int      m_n_qnames;    // number of qnames used in m_qnames
  protected int      m_n_lists;     // number of lists used in m_lists
  protected int      m_n_variants;  // number of slots used in m_variants, m_variantTypes
  protected int      m_n_grammars; // number of slots used in m_grammars
  protected int      m_n_productions; // number of slots used in m_productions
  protected int      m_n_events; // number of slots used in m_eventTypes and m_eventData

  protected int m_n_stypes; // used for counting the number of simple types

  protected int m_grammarCount; // the number of grammars in m_grammars
  
  // Event content composite to event ID 
  private final HashMap<Integer,Integer> m_eventMap;
  
  private static final int NODES_INITIAL       = 1024;
  private static final int GRAMMARS_INITIAL    = 1024;
  private static final int PRODUCTIONS_INITIAL = 1024;
  private static final int EVENTS_INITIAL      = 1024;
  private static final int URIS_INITIAL        = 16;
  private static final int URIS_INCREMENT      = 16;
  private static final int NAMES_INITIAL       = 1024;
  private static final int NAMES_INCREMENT     = 1024;
  private static final int VALUES_INITIAL      = 1024;
  private static final int VALUES_INCREMENT    = 1024;

  protected static String[] INITIAL_URI_ENTRIES = {
    "",
    "http://www.w3.org/XML/1998/namespace",
    "http://www.w3.org/2001/XMLSchema-instance",
    "http://www.w3.org/2001/XMLSchema"
  };

  private static final BigInteger INT_MIN_VALUE, LONG_MIN_VALUE;
  static {
    INT_MIN_VALUE = BigInteger.valueOf((long)Integer.MIN_VALUE);
    LONG_MIN_VALUE = BigInteger.valueOf((long)Long.MIN_VALUE);
  }

  /**
   * Construct a SchemaStruct.
   */
  protected EXISchemaStruct() {
    m_eventMap = new HashMap<Integer,Integer>();
  }

  /**
   * Prepares for the next run.
   */
  protected void reset() {
    clear(); // do clean-up first

    // initialize the arrays
    m_elems        = new int[NODES_INITIAL];
    m_attrs        = new int[NODES_INITIAL];
    m_types        = new int[NODES_INITIAL];
    m_grammars     = new int[GRAMMARS_INITIAL];
    m_productions  = new int[PRODUCTIONS_INITIAL];
    m_eventTypes   = new byte[EVENTS_INITIAL];
    m_eventData    = new int[EVENTS_INITIAL];
    m_uris         = new String[URIS_INITIAL];
    m_names        = new String[NAMES_INITIAL];
    m_strings      = new String[VALUES_INITIAL];
    m_ints         = new int[VALUES_INITIAL];
    m_mantissas    = new long[VALUES_INITIAL];
    m_exponents    = new int[VALUES_INITIAL];
    m_signs        = new boolean[VALUES_INITIAL];
    m_integralDigits = new String[VALUES_INITIAL];
    m_reverseFractionalDigits = new String[VALUES_INITIAL];
    m_integers     = new BigInteger[VALUES_INITIAL];
    m_longs        = new long[VALUES_INITIAL];
    m_datetimes    = new XSDateTime[VALUES_INITIAL];
    m_durations    = new Duration[VALUES_INITIAL];
    m_binaries     = new byte[VALUES_INITIAL][];
    m_variantTypes = new byte[VALUES_INITIAL];
    m_variants     = new int[VALUES_INITIAL];

    m_n_elems  = 0;
    m_n_attrs  = 0;
    m_n_types = 0;
    m_n_grammars = 0;
    m_n_productions = 0;
    m_n_events = 0;
    m_n_uris    = 0;
    m_n_names   = 0;
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
    for (i = 0; i < INITIAL_URI_ENTRIES.length; i++)
      m_uris[m_n_uris++] = INITIAL_URI_ENTRIES[i];
    
    m_names[m_n_names++] = "";
    for (i = 0; i < EXISchemaConst.XML_LOCALNAMES.length; i++)
      m_names[m_n_names++] = EXISchemaConst.XML_LOCALNAMES[i];
    for (i = 0; i < EXISchemaConst.XSI_LOCALNAMES.length; i++)
      m_names[m_n_names++] = EXISchemaConst.XSI_LOCALNAMES[i];
    for (i = 0; i < EXISchemaConst.XSD_LOCALNAMES.length; i++)
      m_names[m_n_names++] = EXISchemaConst.XSD_LOCALNAMES[i];
    
    assert m_n_strings == 0;
    m_strings[EXISchema.EMPTY_STRING] = "";
    m_n_strings++;
  }

  /**
   * Releases resources that was allocated in the previous run.
   */
  protected void clear() {
    // delete the arrays
    m_elems        = null;
    m_attrs        = null;
    m_types        = null;
    m_grammars     = null;
    m_productions  = null;
    m_eventTypes   = null;
    m_eventData    = null;
    m_uris         = null;
    m_names        = null;
    m_localNames   = null;
    m_strings      = null;
    m_ints         = null;
    m_mantissas    = null;
    m_exponents    = null;
    m_signs        = null;
    m_integralDigits = null;
    m_reverseFractionalDigits = null;
    m_integers     = null;
    m_longs        = null;
    m_datetimes    = null;
    m_durations    = null;
    m_binaries     = null;
    m_variantTypes = null;
    m_variants     = null;
    
    m_eventMap.clear();
  }

  /////////////////////////////////////////////////////////////////////////
  // House-keeping procedures
  /////////////////////////////////////////////////////////////////////////

  protected final void ensureElems(int size) {
    while (m_n_elems + size > m_elems.length) {
      final int[] nodes = new int[2 * m_elems.length];
      System.arraycopy(m_elems, 0, nodes, 0, m_elems.length);
      m_elems = nodes;
    }
  }

  protected final void ensureAttrs(int size) {
    while (m_n_attrs + size > m_attrs.length) {
      final int[] attrs = new int[2 * m_attrs.length];
      System.arraycopy(m_attrs, 0, attrs, 0, m_attrs.length);
      m_attrs = attrs;
    }
  }

  protected final void ensureTypes(int size) {
    while (m_n_types + size > m_types.length) {
      final int[] types = new int[2 * m_types.length];
      System.arraycopy(m_types, 0, types, 0, m_types.length);
      m_types = types;
    }
  }

  protected final void ensureVariants() {
    ensureVariants(1);
  }

  protected final void ensureVariants(int size) {
    while (m_n_variants + size > m_variants.length) {
      byte[] variantTypes = new byte[m_variantTypes.length + VALUES_INCREMENT];
      int[] variants      = new int[m_variants.length + VALUES_INCREMENT];
      System.arraycopy(m_variantTypes, 0, variantTypes, 0,
                       m_variantTypes.length);
      System.arraycopy(m_variants, 0, variants, 0, m_variants.length);
      m_variantTypes = variantTypes;
      m_variants     = variants;
    }
  }

  protected final int indexOfUri(String uri) {
    for (int i = 0; i < m_n_uris; i++) {
      if (uri.equals(m_uris[i]))
        return i;
    }
    return -1;
  }
  
  /**
   * Intern a uri.
   * @return id of the uri
   */
  protected final int internUri(String uri) {
    final int index;
    if ((index = indexOfUri(uri)) != -1) {
      return index;
    }
    // Ensure array size
    if (m_n_uris == m_uris.length) {
      final String[] uris = new String[m_uris.length + URIS_INCREMENT];
      System.arraycopy(m_uris, 0, uris, 0, m_uris.length);
      m_uris = uris;
    }
    m_uris[m_n_uris] = uri;
    return m_n_uris++;
  }
  
  protected final int indexOfLocalName(String name, int uri) {
    final int localNames[] = m_localNames[uri];
    for (int i = 0; i < localNames.length; i++) {
      if (m_names[localNames[i]].equals(name))
        return i;
    }
    return -1;
  }

  protected final int indexOfName(String name) {
    for (int i = 0; i < m_n_names; i++) {
      if (name.equals(m_names[i]))
        return i;
    }
    return -1;
  }
  
  /**
   * Intern a name.
   * @return id of the name
   */
  protected final int internName(String name) {
    final int index;
    if ((index = indexOfName(name)) != -1) {
      return index;
    }
    // Ensure array size
    if (m_n_names >= m_names.length) {
      final String[] names = new String[m_names.length + NAMES_INCREMENT];
      System.arraycopy(m_names, 0, names, 0, m_names.length);
      m_names = names;
    }
    m_names[m_n_names] = name;
    return m_n_names++;
  }

  protected final int addStringValue(String val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      if (val.length() == 0) {
        return EXISchema.EMPTY_STRING;
      }
      // Ensure array size
      if (m_n_strings >= m_strings.length) {
        String[] vals = new String[m_strings.length + VALUES_INCREMENT];
        System.arraycopy(m_strings, 0, vals, 0, m_strings.length);
        m_strings = vals;
      }
      // Append the value
      m_strings[m_n_strings] = val;
      pos = m_n_strings++;
    }
    return pos;
  }

  protected final int addIntValue(int val) {
    int pos = EXISchema.NIL_VALUE;
    // Ensure array size
    if (m_n_ints >= m_ints.length) {
      int[] vals = new int[m_ints.length + VALUES_INCREMENT];
      System.arraycopy(m_ints, 0, vals, 0, m_ints.length);
      m_ints = vals;
    }
    // Append the value
    m_ints[m_n_ints] = val;
    pos = m_n_ints++;
    return pos;
  }

  protected final int addFloatValue(long mantissa, int exponent) {
    int pos = EXISchema.NIL_VALUE;
    // Ensure array size
    if (m_n_floats == m_mantissas.length) {
      final int newLength = m_n_floats + VALUES_INCREMENT;
      final long[] _mantissas = new long[newLength];
      System.arraycopy(m_mantissas, 0, _mantissas, 0, m_n_floats);
      m_mantissas = _mantissas;
      final int[] _exponents = new int[newLength];
      System.arraycopy(m_exponents, 0, _exponents, 0, m_n_floats);
      m_exponents = _exponents;
    }
    m_mantissas[m_n_floats] = mantissa;
    m_exponents[m_n_floats] = exponent;
    pos = m_n_floats++;
    return pos;
  }
  
  protected final int addDecimalValue(boolean sign, String integralDigits, String reverseFractionalDigits) {
    int pos = EXISchema.NIL_VALUE;
    // Ensure array size
    if (m_n_decimals >= m_signs.length) {
      final int newLength = m_n_decimals + VALUES_INCREMENT;
      boolean[] _signs = new boolean[newLength];
      System.arraycopy(m_signs, 0, _signs, 0, m_n_decimals);
      m_signs = _signs;
      String[] _integralDigits = new String[newLength];
      System.arraycopy(m_integralDigits, 0, _integralDigits, 0, m_n_decimals);
      m_integralDigits = _integralDigits;
      String[] _reverseFractionalDigits = new String[newLength];
      System.arraycopy(m_reverseFractionalDigits, 0, _reverseFractionalDigits, 0, m_n_decimals);
      m_reverseFractionalDigits = _reverseFractionalDigits;
    }
    m_signs[m_n_decimals] = sign;
    m_integralDigits[m_n_decimals] = integralDigits;
    m_reverseFractionalDigits[m_n_decimals] = reverseFractionalDigits;
    pos = m_n_decimals++;
    return pos;
  }

  protected final int addIntegerValue(BigInteger val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      // Ensure array size
      if (m_n_integers >= m_integers.length) {
        BigInteger[] vals = new BigInteger[m_integers.length + VALUES_INCREMENT];
        System.arraycopy(m_integers, 0, vals, 0, m_integers.length);
        m_integers = vals;
      }
      // Append the value
      m_integers[m_n_integers] = val;
      pos = m_n_integers++;
    }
    return pos;
  }

  protected final int addLongValue(long val) {
    int pos = EXISchema.NIL_VALUE;
    // Ensure array size
    if (m_n_longs >= m_longs.length) {
      long[] vals = new long[m_longs.length + VALUES_INCREMENT];
      System.arraycopy(m_longs, 0, vals, 0, m_longs.length);
      m_longs = vals;
    }
    // Append the value
    m_longs[m_n_longs] = val;
    pos = m_n_longs++;
    return pos;
  }

  protected final int addDateTimeValue(XSDateTime val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      // Ensure array size
      if (m_n_datetimes >= m_datetimes.length) {
        XSDateTime[] vals = new XSDateTime[m_datetimes.length + VALUES_INCREMENT];
        System.arraycopy(m_datetimes, 0, vals, 0, m_datetimes.length);
        m_datetimes = vals;
      }
      // Append the value
      m_datetimes[m_n_datetimes] = val;
      pos = m_n_datetimes++;
    }
    return pos;
  }

  protected final int addDurationValue(Duration val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      // Ensure array size
      if (m_n_durations >= m_durations.length) {
        Duration[] vals = new Duration[m_durations.length + VALUES_INCREMENT];
        System.arraycopy(m_durations, 0, vals, 0, m_durations.length);
        m_durations = vals;
      }
      // Append the value
      m_durations[m_n_durations] = val;
      pos = m_n_durations++;
    }
    return pos;
  }

  protected final int addBinaryValue(byte[] val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      // Ensure array size
      if (m_n_binaries >= m_binaries.length) {
        byte[][] vals = new byte[m_binaries.length + VALUES_INCREMENT][];
        System.arraycopy(m_binaries, 0, vals, 0, m_binaries.length);
        m_binaries = vals;
      }
      // Append the value
      m_binaries[m_n_binaries] = val;
      pos = m_n_binaries++;
    }
    return pos;
  }

  protected final int addVariantStringValue(String val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_STRING;
      m_variants[m_n_variants]     = addStringValue(val);
      pos = m_n_variants++;
    }
    return pos;
  }

  protected final int addVariantBooleanValue(boolean val) {
    ensureVariants(); // Ensure variant array size
    // Append the value
    m_variantTypes[m_n_variants] = EXISchema.VARIANT_BOOLEAN;
    m_variants[m_n_variants]     = val ? EXISchema.TRUE_VALUE : EXISchema.FALSE_VALUE ;
    return m_n_variants++;
  }
  
  protected final int addVariantFloatValue(long mantissa, int exponent) {
    ensureVariants(); // Ensure variant array size
    // Append the value
    m_variantTypes[m_n_variants] = EXISchema.VARIANT_FLOAT;
    m_variants[m_n_variants]     = addFloatValue(mantissa, exponent);
    return m_n_variants++;
  }

  protected final int addVariantDecimalValue(boolean sign, String integralDigits, String reverseFractionalDigits) {
    int pos = EXISchema.NIL_VALUE;
    ensureVariants(); // Ensure variant array size
    // Append the value
    m_variantTypes[m_n_variants] = EXISchema.VARIANT_DECIMAL;
    m_variants[m_n_variants]     = addDecimalValue(sign, integralDigits, reverseFractionalDigits);
    pos = m_n_variants++;
    return pos;
  }

  protected final int addVariantIntegerValue(BigInteger val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_INTEGER;
      m_variants[m_n_variants]     = addIntegerValue(val);
      pos = m_n_variants++;
    }
    return pos;
  }

  protected final int addVariantIntValue(int val) {
    int pos = EXISchema.NIL_VALUE;
    ensureVariants(); // Ensure variant array size
    // Append the value
    m_variantTypes[m_n_variants] = EXISchema.VARIANT_INT;
    m_variants[m_n_variants]     = addIntValue(val);
    pos = m_n_variants++;
    return pos;
  }

  protected final int addVariantLongValue(long val) {
    int pos = EXISchema.NIL_VALUE;
    ensureVariants(); // Ensure variant array size
    // Append the value
    m_variantTypes[m_n_variants] = EXISchema.VARIANT_LONG;
    m_variants[m_n_variants]     = addLongValue(val);
    pos = m_n_variants++;
    return pos;
  }

  protected final int addVariantDateTimeValue(XSDateTime val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_DATETIME; 
      m_variants[m_n_variants]     = addDateTimeValue(val);
      pos = m_n_variants++;
    }
    return pos;
  }

  protected final int addVariantDurationValue(Duration val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_DURATION;
      m_variants[m_n_variants]     = addDurationValue(val);
      pos = m_n_variants++;
    }
    return pos;
  }

  protected final int addVariantBinaryValue(byte[] val, byte variantType) {
    assert variantType == EXISchema.VARIANT_BASE64 || variantType == EXISchema.VARIANT_HEXBIN; 
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = variantType;
      m_variants[m_n_variants]     = addBinaryValue(val);
      pos = m_n_variants++;
    }
    return pos;
  }
  
  protected int doIntegralVariantValue(BigInteger integer, int marginWidth) {
    final int margin = (1 << marginWidth) - 1;
    final BigInteger limitInt = BigInteger.valueOf((long)Integer.MAX_VALUE - margin);
    final BigInteger limitLong = BigInteger.valueOf(Long.MAX_VALUE - margin);
    final int variant;
    if (INT_MIN_VALUE.compareTo(integer) <= 0 && integer.compareTo(limitInt) <= 0)
      variant = addVariantIntValue(integer.intValue());
    else if (LONG_MIN_VALUE.compareTo(integer) <= 0 && integer.compareTo(limitLong) <= 0)
      variant = addVariantLongValue(integer.longValue());
    else
      variant = addVariantIntegerValue(integer);
    return variant;
  }

  protected final void ensureGrammar(int size) {
    while (m_n_grammars + size > m_grammars.length) {
      int[] grammars = new int[2 * m_grammars.length];
      System.arraycopy(m_grammars, 0, grammars, 0, m_grammars.length);
      m_grammars = grammars;
    }
  }
  
  protected final void ensureProduction() {
    if (m_n_productions + EXISchemaLayout.SZ_PRODUCTION > m_productions.length) {
      int[] productions = new int[2 * m_productions.length];
      System.arraycopy(m_productions, 0, productions, 0, m_productions.length);
      m_productions = productions;
    }
  }
  
  private void ensureEvent() {
    if (m_n_events + 1 > m_eventTypes.length) {
      byte[] eventTypes = new byte[2 * m_eventTypes.length];
      System.arraycopy(m_eventTypes, 0, eventTypes, 0, m_eventTypes.length);
      m_eventTypes = eventTypes;
      int[] eventData = new int[2 * m_eventData.length];
      System.arraycopy(m_eventData, 0, eventData, 0, m_eventData.length);
      m_eventData = eventData;
    }
  }
  
  protected final int addEvent(byte eventType, int nd) {
    assert (eventType == EXISchema.EVENT_TYPE_AT || eventType == EXISchema.EVENT_TYPE_SE) && nd != EXISchema.NIL_NODE;
    final int eventComposite = (nd << 2) | eventType;
    final Integer _event;
    if ((_event = m_eventMap.get(eventComposite)) != null) {
      return _event.intValue();
    }
    ensureEvent();
    int event = m_n_events++;
    m_eventMap.put(eventComposite, event); 
    m_eventTypes[event] = eventType;
    m_eventData[event] = nd;
    return event;
  }
  
  protected final int addEvent(byte eventType, String uri) {
    assert eventType == EXISchema.EVENT_TYPE_AT_WILDCARD_NS || eventType == EXISchema.EVENT_TYPE_SE_WILDCARD_NS;
    final int index = indexOfUri(uri);
    assert 0 <= index;
    final int eventComposite = (index << 2) | eventType;
    final Integer _event;
    if ((_event = m_eventMap.get(eventComposite)) != null) {
      return _event.intValue();
    }
    ensureEvent();
    final int event = m_n_events++;
    m_eventMap.put(eventComposite, event); 
    m_eventTypes[event] = eventType;
    m_eventData[event] = index;
    return event;
  }

}
