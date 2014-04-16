package org.openexi.fujitsu.scomp;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.datatype.DatatypeConstants; 
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaConst;
import org.openexi.fujitsu.schema.EXISchemaLayout;
import org.openexi.fujitsu.schema.XSDateTime;

/**
 * SchemaStruct provides functions to manipulate on the schema arrays.
 */
abstract class EXISchemaStruct {

  // The sole purpose of SchemaStruct is to populate these arrays.
  protected int[]        m_nodes;        // int array describing schema structure
  protected String[]     m_names;        // array of interned strings
  protected String[]     m_strings;      // array of non-interned strings
  protected int[]        m_ints;         // array of int values
  protected float[]      m_floats;       // array of float values
  protected double[]     m_doubles;      // array of double values
  protected BigDecimal[] m_decimals;     // array of decimal values
  protected BigInteger[] m_integers;     // array of integer values
  protected long[]       m_longs;        // array of long values
  protected XSDateTime[] m_datetimes;    // array of datetime values
  protected Duration[]   m_durations;    // array of duration values
  protected byte[][]     m_binaries;     // array of binary values
  protected int[]        m_qnames;       // array of qname values
  protected int[][]      m_lists;        // array of list values
  protected int[]        m_variantTypes; // array of variant types
  protected int[]        m_variants;     // array of variant values
  protected int[]        m_fragmentINodes;

  protected int      m_n_nodes;     // number of slots used in m_nodes
  protected int      m_n_names;     // number of slots used in m_names
  protected int      m_n_strings;   // number of slots used in m_strings
  protected int      m_n_ints;      // number of slots used in m_ints
  protected int      m_n_floats;    // number of slots used in m_floats
  protected int      m_n_doubles;   // number of slots used in m_doubles
  protected int      m_n_decimals;  // number of decimals used in m_decimals
  protected int      m_n_integers;  // number of decimals used in m_integers
  protected int      m_n_longs;     // number of decimals used in m_longs
  protected int      m_n_datetimes; // number of decimals used in m_datetimes
  protected int      m_n_durations; // number of decimals used in m_durations
  protected int      m_n_binaries;  // number of binaries used in m_binaries
  protected int      m_n_qnames;    // number of qnames used in m_qnames
  protected int      m_n_lists;     // number of lists used in m_lists
  protected int      m_n_variants;  // number of slots used in m_variants, m_variantTypes
  protected int      m_n_fragmentElems;
  protected int      m_n_fragmentAttrs;

  protected int m_n_elems; // used for numbering elements, counting the number of elements
  protected int m_n_attrs; // used for counting the number of attributes
  protected int m_n_types; // used for numbering types, counting the number of types
  protected int m_n_stypes; // used for counting the number of simple types
  protected int m_n_groups; // used for numbering groups, counting the number of groups

  private static final int NODES_INITIAL     = 1024;
  private static final int NAMES_INITIAL     = 1024;
  private static final int NAMES_INCREMENT   = 1024;
  private static final int VALUES_INITIAL    = 1024;
  private static final int VALUES_INCREMENT  = 1024;

  private static final String XMLSCHEMA_INSTANCE_URI = "http://www.w3.org/2001/XMLSchema-instance";
  private static final String XMLSCHEMA_INSTANCE_ALTURI = "urn:com.fujitsu.xml.xsc.XMLSchema-instance";

  /**
   * Construct a SchemaStruct.
   */
  protected EXISchemaStruct() {
  }

  /**
   * Prepares for the next run.
   */
  protected void reset() {
    clear(); // do clean-up first

    // initialize the arrays
    m_nodes        = new int[NODES_INITIAL];
    m_names        = new String[NAMES_INITIAL];
    m_strings      = new String[VALUES_INITIAL];
    m_ints         = new int[VALUES_INITIAL];
    m_floats       = new float[VALUES_INITIAL];
    m_doubles      = new double[VALUES_INITIAL];
    m_decimals     = new BigDecimal[VALUES_INITIAL];
    m_integers     = new BigInteger[VALUES_INITIAL];
    m_longs        = new long[VALUES_INITIAL];
    m_datetimes    = new XSDateTime[VALUES_INITIAL];
    m_durations    = new Duration[VALUES_INITIAL];
    m_binaries     = new byte[VALUES_INITIAL][];
    m_qnames       = new int[VALUES_INITIAL];
    m_lists       = new int[VALUES_INITIAL][];
    m_variantTypes = new int[VALUES_INITIAL];
    m_variants     = new int[VALUES_INITIAL];

    m_n_nodes   = EXISchema.THE_SCHEMA;
    m_n_names   = 0;
    m_n_strings = 0;
    m_n_ints = 0;
    m_n_floats = 0;
    m_n_doubles = 0;
    m_n_decimals = 0;
    m_n_integers = 0;
    m_n_longs = 0;
    m_n_datetimes = 0;
    m_n_durations = 0;
    m_n_binaries = 0;
    m_n_qnames = 0;
    m_n_lists = 0;
    m_n_variants = 0;

    m_n_elems = 0;
    m_n_attrs = 0;
    m_n_types = 0; // 0 represents anyType.
    m_n_stypes = 0;
    m_n_groups = 0;

    m_names[m_n_names++] = "";
    m_strings[m_n_strings++] = "";
  }

  /**
   * Releases resources that was allocated in the previous run.
   */
  protected void clear() {
    // delete the arrays
    m_nodes        = null;
    m_names        = null;
    m_strings      = null;
    m_ints         = null;
    m_floats       = null;
    m_doubles      = null;
    m_decimals     = null;
    m_integers     = null;
    m_longs        = null;
    m_datetimes    = null;
    m_durations    = null;
    m_binaries     = null;
    m_qnames       = null;
    m_lists        = null;
    m_variantTypes = null;
    m_variants     = null;
  }

  /////////////////////////////////////////////////////////////////////////
  // House-keeping procedures
  /////////////////////////////////////////////////////////////////////////

  protected final void ensureNodes(int size) {
    while (m_n_nodes + size > m_nodes.length) {
      int[] nodes = new int[2 * m_nodes.length];
      System.arraycopy(m_nodes, 0, nodes, 0, m_nodes.length);
      m_nodes = nodes;
    }
  }

  /**
   * Allocate space for an opaque lot of a certain size.
   * Note that the size of an opaque node is that of the opaque lot plus
   * SZ_OPAQUE. This method not only ensures the array size but also does
   * space allocation, which means m_n_nodes is added up in this method.
   * @param size size of the opaque lot
   * @return start position of the opaque lot
   */
  protected final int allocOpaque(int size) {
    final int nodeSize = size + EXISchemaLayout.SZ_OPAQUE;
    ensureNodes(nodeSize);
    final int nd = m_n_nodes;
    m_nodes[nd] = EXISchema.OPAQUE_NODE;
    m_nodes[nd + 1] = nodeSize;
    m_n_nodes += nodeSize;
    return nd + EXISchemaLayout.SZ_OPAQUE;
  }

  protected final void ensureVariants() {
    ensureVariants(1);
  }

  protected final void ensureVariants(int size) {
    while (m_n_variants + size > m_variants.length) {
      int[] variantTypes = new int[m_variantTypes.length + VALUES_INCREMENT];
      int[] variants     = new int[m_variants.length + VALUES_INCREMENT];
      System.arraycopy(m_variantTypes, 0, variantTypes, 0,
                       m_variantTypes.length);
      System.arraycopy(m_variants, 0, variants, 0, m_variants.length);
      m_variantTypes = variantTypes;
      m_variants     = variants;
    }
  }

  /**
   * Intern a name.
   * @return id of the name
   */
  protected final int internName(String name) {
    // Replace XMLSCHEMA_INSTANCE_ALTURI with XMLSCHEMA_INSTANCE_URI
    if (name.equals(XMLSCHEMA_INSTANCE_ALTURI))
      name = XMLSCHEMA_INSTANCE_URI;
    for (int i = 0; i < m_n_names; i++) {
      if (name.equals(m_names[i]))
        return i;
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

  protected final int addStringValue(String val, boolean shareEmpty) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      if (shareEmpty && val.length() == 0) {
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

  protected final int addFloatValue(float val) {
    int pos = EXISchema.NIL_VALUE;
    // Ensure array size
    if (m_n_floats >= m_floats.length) {
      float[] vals = new float[m_floats.length + VALUES_INCREMENT];
      System.arraycopy(m_floats, 0, vals, 0, m_floats.length);
      m_floats = vals;
    }
    // Append the value
    m_floats[m_n_floats] = val;
    pos = m_n_floats++;
    return pos;
  }

  protected final int addDoubleValue(double val) {
    int pos = EXISchema.NIL_VALUE;
    // Ensure array size
    if (m_n_doubles >= m_doubles.length) {
      double[] vals = new double[m_doubles.length + VALUES_INCREMENT];
      System.arraycopy(m_doubles, 0, vals, 0, m_doubles.length);
      m_doubles = vals;
    }
    // Append the value
    m_doubles[m_n_doubles] = val;
    pos = m_n_doubles++;
    return pos;
  }

  protected final int addDecimalValue(BigDecimal val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      // Ensure array size
      if (m_n_decimals >= m_decimals.length) {
        BigDecimal[] vals = new BigDecimal[m_decimals.length + VALUES_INCREMENT];
        System.arraycopy(m_decimals, 0, vals, 0, m_decimals.length);
        m_decimals = vals;
      }
      // Append the value
      m_decimals[m_n_decimals] = val;
      pos = m_n_decimals++;
    }
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

  protected final int addQNameValue(String uri, String name) {
    int pos = EXISchema.NIL_VALUE;
    if (name != null) {
      // Ensure array size
      if (m_n_qnames >= m_qnames.length - 1) {
        int[] qnames = new int[m_qnames.length + VALUES_INCREMENT];
        System.arraycopy(m_qnames, 0, qnames, 0, m_qnames.length);
        m_qnames = qnames;
      }
      if (name.indexOf(':') == -1 && uri == null) {
        uri = "";
      }
      // Append the value
      m_qnames[m_n_qnames] = internName(uri);
      m_qnames[m_n_qnames + 1] = internName(name);
      pos = m_n_qnames;
      m_n_qnames += 2;
    }
    return pos;
  }

  protected final int addListValue(int[] val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      // Ensure array size
      if (m_n_lists >= m_lists.length) {
        int[][] vals = new int[m_lists.length + VALUES_INCREMENT][];
        System.arraycopy(m_lists, 0, vals, 0, m_lists.length);
        m_lists = vals;
      }
      // Append the value
      m_lists[m_n_lists] = val;
      pos = m_n_lists++;
    }
    return pos;
  }
  
  protected final int addVariantName(String name) {
    int pos = EXISchema.NIL_VALUE;
    if (name != null) {
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_NAME;
      m_variants[m_n_variants]     = internName(name);
      pos = m_n_variants++;
    }
    return pos;
  }

  protected final int addVariantStringValue(String val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_STRING;
      m_variants[m_n_variants]     = addStringValue(val, false);
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

  protected final int addVariantFloatValue(float val) {
    int pos = EXISchema.NIL_VALUE;
    ensureVariants(); // Ensure variant array size
    // Append the value
    m_variantTypes[m_n_variants] = EXISchema.VARIANT_FLOAT;
    m_variants[m_n_variants]     = addFloatValue(val);
    pos = m_n_variants++;
    return pos;
  }

  protected final int addVariantDoubleValue(double val) {
    int pos = EXISchema.NIL_VALUE;
    ensureVariants(); // Ensure variant array size
    // Append the value
    m_variantTypes[m_n_variants] = EXISchema.VARIANT_DOUBLE;
    m_variants[m_n_variants]     = addDoubleValue(val);
    pos = m_n_variants++;
    return pos;
  }

  protected final int addVariantDecimalValue(BigDecimal val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_DECIMAL;
      m_variants[m_n_variants]     = addDecimalValue(val);
      pos = m_n_variants++;
    }
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

  protected final int addVariantDateTimeValue(XMLGregorianCalendar val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      int typeSerial = EXISchemaConst.UNTYPED;
      final QName qname = val.getXMLSchemaType();
      if (qname == DatatypeConstants.DATETIME)
        typeSerial = EXISchemaConst.DATETIME_TYPE;
      else if (qname == DatatypeConstants.DATE)
        typeSerial = EXISchemaConst.DATE_TYPE;
      else if (qname == DatatypeConstants.TIME)
        typeSerial = EXISchemaConst.TIME_TYPE;
      else if (qname == DatatypeConstants.GYEARMONTH)
        typeSerial = EXISchemaConst.G_YEARMONTH_TYPE;
      else if (qname == DatatypeConstants.GMONTHDAY)
        typeSerial = EXISchemaConst.G_MONTHDAY_TYPE;
      else if (qname == DatatypeConstants.GYEAR)
        typeSerial = EXISchemaConst.G_YEAR_TYPE;
      else if (qname == DatatypeConstants.GMONTH)
        typeSerial = EXISchemaConst.G_MONTH_TYPE;
      else if (qname == DatatypeConstants.GDAY)
        typeSerial = EXISchemaConst.G_DAY_TYPE;
      else
        return EXISchema.NIL_VALUE;

      final XSDateTime dateTime;
      dateTime = new XSDateTime(
          val.getYear(), val.getMonth(), val.getDay(), val.getHour(), val.getMinute(),
          val.getSecond(), val.getMillisecond(), val.getTimezone(), typeSerial);
      
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_DATETIME;
      m_variants[m_n_variants]     = addDateTimeValue(dateTime);
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

  protected final int addVariantBinaryValue(byte[] val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_BINARY;
      m_variants[m_n_variants]     = addBinaryValue(val);
      pos = m_n_variants++;
    }
    return pos;
  }

  protected final int addVariantQNameValue(String uri, String name) {
    int pos = EXISchema.NIL_VALUE;
    if (name != null) {
      ensureVariants(); // Ensure variant array size
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_QNAME;
      m_variants[m_n_variants] = addQNameValue(uri, name);
      pos = m_n_variants++;
    }
    return pos;
  }

  protected final int addVariantListValue(int[] val) {
    int pos = EXISchema.NIL_VALUE;
    if (val != null) {
      ensureVariants(); // Ensure variant array size
      // Append the value
      m_variantTypes[m_n_variants] = EXISchema.VARIANT_LIST;
      m_variants[m_n_variants]     = addListValue(val);
      pos = m_n_variants++;
    }
    return pos;
  }
  
}
