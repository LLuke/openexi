package org.openexi.schema;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import javax.xml.datatype.Duration;

/**
 * EXISchema encapsulates compiled schema octets to serve
 * as accessors to otherwise obscure data.
 */
public final class EXISchema implements Externalizable {

  private static final long serialVersionUID = 8320171785143781579L;
  
  public static final int NIL_NODE = -1;
  public static final int CH_NODE = -2;
  public static final int NIL_VALUE = -1;
  public static final int EMPTY_STRING = 0;

  public static final int TRUE_VALUE = 1;
  public static final int FALSE_VALUE = 0;
  
  public static final int UNBOUNDED_OCCURS = -1;

  public static final int CONSTRAINT_NONE    = 0;
  public static final int CONSTRAINT_DEFAULT = 1;
  public static final int CONSTRAINT_FIXED   = 2;

  public static final int WHITESPACE_ABSENT   = 0;
  public static final int WHITESPACE_PRESERVE = 1;
  public static final int WHITESPACE_REPLACE  = 2;
  public static final int WHITESPACE_COLLAPSE = 3;

  public static final int VARIANT_NAME        = 0;
  public static final int VARIANT_STRING      = 1;
  public static final int VARIANT_FLOAT       = 2;
  public static final int VARIANT_DOUBLE      = 3;
  public static final int VARIANT_DECIMAL     = 4;
  public static final int VARIANT_INTEGER     = 5;
  public static final int VARIANT_INT         = 6;
  public static final int VARIANT_LONG        = 7;
  public static final int VARIANT_DATETIME    = 8;
  public static final int VARIANT_DURATION    = 9;
  public static final int VARIANT_BINARY      = 10;
  public static final int VARIANT_QNAME       = 11;
  public static final int VARIANT_BOOLEAN     = 12;
  public static final int VARIANT_LIST        = 13;

         static final int CONTENT_ABSENT       = -2;
  public static final int CONTENT_INVALID      = -1;
  // DO NOT CHANGE PUBLIC Constants!
  public static final int CONTENT_EMPTY        = 0;
  public static final int CONTENT_SIMPLE       = 1;
  public static final int CONTENT_MIXED        = 2;
  public static final int CONTENT_ELEMENT_ONLY = 3;

  public static final int TERM_TYPE_ABSENT   = -2;
  public static final int TERM_TYPE_INVALID  = -1;
  public static final int TERM_TYPE_GROUP    = 0;
  public static final int TERM_TYPE_WILDCARD = 1;
  public static final int TERM_TYPE_ELEMENT  = 2;

  public static final int GROUP_ALL      = 0;
  public static final int GROUP_CHOICE   = 1;
  public static final int GROUP_SEQUENCE = 2;

  public static final int WC_TYPE_INVALID    = -1;
  public static final int WC_TYPE_ANY        = 0;
  public static final int WC_TYPE_NOT        = 1;
  public static final int WC_TYPE_NAMESPACES = 2;

  public static final int WC_PROCESS_INVALID = -1;
  public static final int WC_PROCESS_SKIP    = 0;
  public static final int WC_PROCESS_LAX     = 1;
  public static final int WC_PROCESS_STRICT  = 2;

  public static final int INTEGER_CODEC_DEFAULT     = 0xFF;
  public static final int INTEGER_CODEC_NONNEGATIVE = 0xFE;
  
  public static final int TYPE_MASK   = 0x0100;
  static final int INODE_MASK  = 0x0200;
  public static final int INIT_MASK   = 0x0400;

  ///////////////////////////////////////////////////////////////////////////
  // Public node types
  ///////////////////////////////////////////////////////////////////////////

  // DO NOT USE 0x0000 as a node type identifier.
  public static final int SCHEMA_NODE            = 0x0001;
  public static final int NAMESPACE_NODE         = 0x0002;

  public static final int ELEMENT_NODE           = 0x0001 | INODE_MASK;
  public static final int ATTRIBUTE_NODE         = 0x0002 | INODE_MASK;

  public static final int SIMPLE_TYPE_NODE       = 0x01 | TYPE_MASK;
  public static final int COMPLEX_TYPE_NODE      = 0x02 | TYPE_MASK;

  public static final int PARTICLE_NODE          = 0x0003;
  public static final int GROUP_NODE             = 0x0004;
  public static final int WILDCARD_NODE          = 0x0005;
  public static final int ATTRIBUTE_USE_NODE     = 0x0006;

  public static final int OPAQUE_NODE            = 0x0000;

  ///////////////////////////////////////////////////////////////////////////
  // Public value enumerations
  ///////////////////////////////////////////////////////////////////////////

  public static final int UR_SIMPLE_TYPE     = 0;
  public static final int ATOMIC_SIMPLE_TYPE = 1;
  public static final int LIST_SIMPLE_TYPE   = 2;
  public static final int UNION_SIMPLE_TYPE  = 3;

  private static final String XMLSCHEMA_INSTANCE_URI = "http://www.w3.org/2001/XMLSchema-instance";

  int[]            m_nodes; // int array describing taxonomy structure
  private int[]    m_opaques; // int array describing opaque data
  private String[] m_uris; // array of interned strings representing uris
  private int[][]  m_localNames;
  String[]         m_names; // array of interned strings
  String[]         m_strings; // array of non-interned strings
  int[]            m_ints; // array of int values
  float[]          m_floats; // array of float values
  double[]         m_doubles; // array of double values
  BigDecimal[]     m_decimals; // array of decimal values
  BigInteger[]     m_integers; // array of integer values
  long[]           m_longs; // array of long values
  XSDateTime[]     m_datetimes; // array of datetime values
  Duration[]       m_durations; // array of duration values
  byte[][]         m_binaries; // array of binary values
  int[]            m_qnames; // array of qname values (i.e. uri, name)
  int[][]          m_lists; // array of lists
  int[]            m_variantTypes; // array of variant types
  int[]            m_variants; // array of variant values
  
  int  m_n_elems; // grand total number of element nodes (both global and local) 
  int  m_n_attrs; // grand total number of attribute nodes (both global and local)
  int  m_n_types; // grand total number of type nodes (both global and local)
  int  m_n_stypes; // grand total number of simple type nodes (both global and local)
  int  m_n_groups; // total number of group nodes
  
  private int[] m_fragmentINodes;
  private int m_n_fragmentElems;

  private HashMap<String,Integer[]> m_globalElementsDirectory;
  private HashMap<String,Integer[]> m_globalAttributesDirectory;
  private HashMap<String,Integer[]> m_globalTypesDirectory;

  public EXISchema() { // for deserialization
  }

  public EXISchema(int[] nodes, int n_nodes, int[] opaques, int n_opaques,
               String[] uris, int n_uris, 
               String[] names, int n_names, int[][] localNames, 
               String[] strings, int n_strings, int[] ints, int n_ints,
               float[] floats, int n_floats, double[] doubles, int n_doubles,
               BigDecimal[] decimals, int n_decimals, 
               BigInteger[] integers, int n_integers, 
               long[] longs, int n_longs,
               XSDateTime[] datetimes, int n_datetimes,
               Duration[] durations, int n_durations,
               byte[][] binaries, int n_binaries,
               int[] qnames, int n_qnames,
               int[][] lists, int n_lists,
               int[] variantTypes, int[] variants, int n_variants,
               int[] fragmentINodes, int n_fragmentElems,
               int n_elems, int n_attrs, int n_types, int n_stypes, int n_groups) {

    m_nodes = new int[n_nodes];
    System.arraycopy(nodes, 0, m_nodes, 0, n_nodes);
    
    m_opaques = new int[n_opaques];
    System.arraycopy(opaques, 0, m_opaques, 0, n_opaques);
    
    m_uris = new String[n_uris];
    System.arraycopy(uris, 0, m_uris, 0, n_uris);

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

    m_floats = new float[n_floats];
    System.arraycopy(floats, 0, m_floats, 0, n_floats);

    m_doubles = new double[n_doubles];
    System.arraycopy(doubles, 0, m_doubles, 0, n_doubles);

    m_decimals = new BigDecimal[n_decimals];
    System.arraycopy(decimals, 0, m_decimals, 0, n_decimals);

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

    m_qnames = new int[n_qnames];
    System.arraycopy(qnames, 0, m_qnames, 0, n_qnames);

    m_lists = new int[n_lists][];
    System.arraycopy(lists, 0, m_lists, 0, n_lists);

    m_variants = new int[n_variants];
    System.arraycopy(variants, 0, m_variants, 0, n_variants);

    m_variantTypes = new int[n_variants];
    System.arraycopy(variantTypes, 0, m_variantTypes, 0, n_variants);

    m_fragmentINodes = new int[fragmentINodes.length];
    System.arraycopy(fragmentINodes, 0, m_fragmentINodes, 0, fragmentINodes.length);
    m_n_fragmentElems = n_fragmentElems;

    m_n_elems = n_elems;
    m_n_attrs = n_attrs;
    m_n_types = n_types;
    m_n_stypes = n_stypes;
    m_n_groups = n_groups;

    computeGlobalDirectory();
  }

  private void computeGlobalDirectory() {
    m_globalElementsDirectory = new HashMap<String,Integer[]>();
    m_globalAttributesDirectory = new HashMap<String,Integer[]>();
    m_globalTypesDirectory = new HashMap<String,Integer[]>();

    int n_namespaces = getNamespaceCountOfSchema();
    for (int j = 0; j < n_namespaces; j++) {
      final int _namespace = getNamespaceOfSchema(j);
      final int n_elems = getElemCountOfNamespace(_namespace);
      for (int k = 0; k < n_elems; k++) {
        final int elem = getElemOfNamespace(_namespace, k);
        String ename = getNameOfElem(elem);
        Integer[] nodes;
        if ((nodes = m_globalElementsDirectory.get(ename)) != null) {
          Integer[] _nodes = new Integer[nodes.length + 1];
          System.arraycopy(nodes, 0, _nodes, 0, nodes.length);
          _nodes[nodes.length] = elem;
          nodes = _nodes;
        }
        else {
          nodes = new Integer[1];
          nodes[0] = elem;
        }
        m_globalElementsDirectory.put(ename, nodes);
      }
      final int n_attrs = getAttrCountOfNamespace(_namespace);
      for (int k = 0; k < n_attrs; k++) {
        final int attr = getAttrOfNamespace(_namespace, k);
        String aname = getNameOfAttr(attr);
        Integer[] nodes;
        if ((nodes = m_globalAttributesDirectory.get(aname)) != null) {
          Integer[] _nodes = new Integer[nodes.length + 1];
          System.arraycopy(nodes, 0, _nodes, 0, nodes.length);
          _nodes[nodes.length] = attr;
          nodes = _nodes;
        }
        else {
          nodes = new Integer[1];
          nodes[0] = attr;
        }
        m_globalAttributesDirectory.put(aname, nodes);
      }
      final int n_types = getTypeCountOfNamespace(_namespace);
      for (int k = 0; k < n_types; k++) {
        final int _type = getTypeOfNamespace(_namespace, k);
        String tname = getNameOfType(_type);
        Integer[] nodes;
        if ((nodes = m_globalTypesDirectory.get(tname)) != null) {
          Integer[] _nodes = new Integer[nodes.length + 1];
          System.arraycopy(nodes, 0, _nodes, 0, nodes.length);
          _nodes[nodes.length] = _type;
          nodes = _nodes;
        }
        else {
          nodes = new Integer[1];
          nodes[0] = _type;
        }
        m_globalTypesDirectory.put(tname, nodes);
      }
    }
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Corpus methods
  ///////////////////////////////////////////////////////////////////////////

  public boolean isUPAGotchaFree() {
    return m_nodes[0] == 1;
  }
  
  /**
   * Schema node in the corpus.
   */
  public static final int THE_SCHEMA = EXISchemaLayout.SZ_CORPUS;

  public int[] getNodes() {
    return m_nodes;
  }

  public int[] getOpaques() {
    return m_opaques;
  }

  public String[] getNames() {
    return m_names;
  }
  
  public String[] getUris() {
    return m_uris;
  }

  public int[][] getLocalNames() {
    return m_localNames;
  }
  
  public int[] getFragmentINodes() {
    return m_fragmentINodes;
  }
  
  public int getFragmentElemCount() {
    return this.m_n_fragmentElems;
  }
  
  /**
   * Returns the grand total number of elements contained in the corpus.
   * @return grand total number of elements
   */
  public int getTotalElemCount() {
    return m_n_elems;
  }

  /**
   * Returns the grand total number of attributes contained in the corpus.
   * @return grand total number of attributes
   */
  public int getTotalAttrCount() {
    return m_n_attrs;
  }

  /**
   * Returns the grand total number of types contained in the corpus.
   * @return grand total number of types
   */
  public int getTotalTypeCount() {
    return m_n_types;
  }

  /**
   * Returns the total number of simple types (both global and local) available in the schema.
   * @param schema schema node
   * @return total number of simple types available in the schema.
   */
  public int getTotalSimpleTypeCount() {
    return m_n_stypes;
  }

  /**
   * Returns the total number of groups contained in the corpus.
   * @return total number of groups
   */
  public int getTotalGroupCount() {
    return m_n_groups;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Generic methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Return the type of the node.
   * @param nd a node
   * @return the type of a node
   */
  public final int getNodeType(int nd) {
    return m_nodes[nd];
  }

  /**
   * Returns the size of a node.
   * @param nd a node
   * @return size of the node if it is a node, otherwise returns NIL_VALUE
   */
  public static int _getNodeSize(final int nd, final int[] nodes) {
    if (nd != NIL_NODE) {
      switch (nodes[nd]) {
        case SCHEMA_NODE:
          return _getSizeOfSchema(nodes);
        case ELEMENT_NODE:
          return _getSizeOfElem(nd, nodes);
        case ATTRIBUTE_NODE:
          return EXISchemaLayout.SZ_ATTR;
        case NAMESPACE_NODE:
          return _getSizeOfNamespace(nd, nodes);
        case SIMPLE_TYPE_NODE:
          return _getSizeOfSimpleType(nd, nodes);
        case COMPLEX_TYPE_NODE:
          return _getSizeOfComplexType(nd, nodes);
        case PARTICLE_NODE:
          return EXISchemaLayout.SZ_PARTICLE;
        case GROUP_NODE:
          return _getSizeOfGroup(nd, nodes);
        case WILDCARD_NODE:
          return _getSizeOfWildcard(nd, nodes);
        case ATTRIBUTE_USE_NODE:
          return EXISchemaLayout.SZ_ATTR_USE;
        default:
          assert false;
          break;
      }
    }
    return NIL_VALUE;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Schema methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the size of a schema.
   * @param schema schema node
   * @return size of the schema
   */
  static private int _getSizeOfSchema(int[] nodes) {
    return EXISchemaLayout.SZ_SCHEMA + _getElemCountOfSchema(nodes) +
        _getAttrCountOfSchema(nodes) + 
        EXISchemaConst.N_BUILTIN_TYPES +
        _getNamespaceCountOfSchema(nodes);
  }

  /**
   * Returns the total number of global elements available in the schema.
   * @param schema schema node
   * @return total number of global elements available in the schema.
   */
  public int getElemCountOfSchema() {
    return _getElemCountOfSchema(m_nodes);
  }

  static int _getElemCountOfSchema(int[] nodes) {
    return nodes[THE_SCHEMA + EXISchemaLayout.SCHEMA_N_ELEMS];
  }

  /**
   * Returns the i-th element of the schema.
   * @param schema schema node
   * @param i index of the element
   * @return i-th element
   * @throws EXISchemaRuntimeException If the index is out of array bounds
   */
  public int getElemOfSchema(int i)
      throws EXISchemaRuntimeException {
    int n = _getElemCountOfSchema(m_nodes);
    if (i < 0 || i >= n)
      throw new EXISchemaRuntimeException(
        EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
        new String[] { String.valueOf(i), String.valueOf(0), String.valueOf(n - 1) });

    return m_nodes[THE_SCHEMA + EXISchemaLayout.SZ_SCHEMA + i];
  }

  public int getElemOfSchema(String namespaceName, String name) {
    final Integer[] nodes;
    if ((nodes = m_globalElementsDirectory.get(name)) != null) {
      final int len = nodes.length;
      for (int i = 0; i < len; i++) {
        final int elem = nodes[i];
        if (namespaceName.equals(getTargetNamespaceNameOfElem(elem))) {
          return elem;
        }
      }
    }
    return NIL_NODE;
  }

  /**
   * Returns the total number of global attributes available in the schema.
   * @param schema schema node
   * @return total number of global attributes available in the schema.
   */
  public int getAttrCountOfSchema() {
    return _getAttrCountOfSchema(m_nodes);
  }

  static int _getAttrCountOfSchema(int[] nodes) {
    return nodes[THE_SCHEMA + EXISchemaLayout.SCHEMA_N_ATTRS];
  }

  int getAttrOfSchema(String namespaceName, String name) {
    int namespaceNode = _getNamespaceOfSchema(namespaceName, m_nodes, m_uris);
    if (namespaceNode != NIL_NODE) {
      return getAttrOfNamespace(namespaceNode, name);
    }
    return NIL_NODE;
  }
  
  /**
   * Returns the attribute of the specified name and namespace name. 
   * @param schema schema node
   * @param namespaceName namespace name
   * @param name name of the attribute
   * @return the attribute of the specified name and namespace name
   */
  static int _getAttrOfSchema(String namespaceName, String name, int[] nodes, String[] uris, String[] names) {
    int namespaceNode = _getNamespaceOfSchema(namespaceName, nodes, uris);
    if (namespaceNode != NIL_NODE) {
      int attr;
      if ((attr = _getAttrOfNamespace(namespaceNode, name, nodes, names)) != NIL_NODE)
        return attr;
    }
    return NIL_NODE;
  }

  /**
   * Returns the i-th type of the schema.
   * @param schema schema node
   * @param i index of the type
   * @return i-th type
   * @throws EXISchemaRuntimeException If the index is out of array bounds
   */
  public int getBuiltinTypeOfSchema(int i)
      throws EXISchemaRuntimeException {
    if (i < 0 || i > EXISchemaConst.N_BUILTIN_TYPES - 1)
      throw new EXISchemaRuntimeException(
        EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
        new String[] { String.valueOf(i), String.valueOf(0), String.valueOf(EXISchemaConst.N_BUILTIN_TYPES - 1) });
    return _getBuiltinTypeOfSchema(i, m_nodes);
  }

  public static int _getBuiltinTypeOfSchema(int i, int[] nodes)
      throws EXISchemaRuntimeException {
    assert i < EXISchemaConst.N_BUILTIN_TYPES;
    final int n_elems = _getElemCountOfSchema(nodes);
    final int n_attrs = _getAttrCountOfSchema(nodes);
    return nodes[THE_SCHEMA + EXISchemaLayout.SZ_SCHEMA + n_elems + n_attrs + i];
  }

  /**
   * Returns the number of namespaces incorporated in the schema.
   * @param schema schema node
   * @return number of namespaces incorporated in the schema.
   */
  public int getNamespaceCountOfSchema() {
    return _getNamespaceCountOfSchema(m_nodes);
  }

  static int _getNamespaceCountOfSchema(int[] nodes) {
    return nodes[THE_SCHEMA + EXISchemaLayout.SCHEMA_N_NAMESPACES];
  }

  /**
   * Returns the i-th namespace of the schema.
   * @param schema schema node
   * @param i index of the namespace
   * @return i-th namespace
   * @throws EXISchemaRuntimeException If the index is out of array bounds
   */
  public int getNamespaceOfSchema(int i) {
    return _getNamespaceOfSchema(i, m_nodes);
  }

  static private int _getNamespaceOfSchema(int i, int[] nodes)
      throws EXISchemaRuntimeException {
    final int n = nodes[THE_SCHEMA + EXISchemaLayout.SCHEMA_N_NAMESPACES]; 
    if (i < 0 || i >= n)
      throw new EXISchemaRuntimeException(
        EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
        new String[] { String.valueOf(i), String.valueOf(0), String.valueOf(n - 1) });

    return nodes[THE_SCHEMA + EXISchemaLayout.SZ_SCHEMA +
                 nodes[THE_SCHEMA + EXISchemaLayout.SCHEMA_N_ELEMS] +
                 nodes[THE_SCHEMA + EXISchemaLayout.SCHEMA_N_ATTRS] +
                 EXISchemaConst.N_BUILTIN_TYPES + i];
  }

  /**
   * Returns the namespace of the schema that has the specified name.
   * @param schema schema node
   * @param namespaceName name of the namespace. Use null for default namespace.
   * @return namespace
   */
  public int getNamespaceOfSchema(String namespaceName) {
    return _getNamespaceOfSchema(namespaceName, m_nodes, m_uris);
  }

  static int _getNamespaceOfSchema(final String namespaceName,
                                   final int[] nodes, final String[] uris) {
    final int count = nodes[THE_SCHEMA + EXISchemaLayout.SCHEMA_N_NAMESPACES];
    for (int i = 0; i < count; i++) {
      final int namespace = nodes[THE_SCHEMA + EXISchemaLayout.SZ_SCHEMA +
                                  nodes[THE_SCHEMA + EXISchemaLayout.SCHEMA_N_ELEMS] +
                                  nodes[THE_SCHEMA + EXISchemaLayout.SCHEMA_N_ATTRS] +
                                  EXISchemaConst.N_BUILTIN_TYPES + i];
      final String ith = uris[nodes[namespace + EXISchemaLayout.NAMESPACE_NAME]];
      if (ith.equals(namespaceName))
        return namespace;
    }
    return NIL_NODE;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Namespace methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the size of a namespace.
   * @param namespace namespace node
   * @return size of the namespace
   */
  static private int _getSizeOfNamespace(int namespace, int[] nodes) {
    return EXISchemaLayout.SZ_NAMESPACE + _getElemCountOfNamespace(namespace, nodes) +
        _getAttrCountOfNamespace(namespace, nodes) + _getTypeCountOfNamespace(namespace, nodes);
  }

  /**
   * Returns name of a namespace.
   * @param namespace namespace node
   * @return name of a namespace node if available, otherwise null.
   */
  public String getNameOfNamespace(int namespace)
    throws EXISchemaRuntimeException {
    if (namespace < 0 || m_nodes[namespace] != NAMESPACE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_NAMESPACE,
          new String[] { String.valueOf(namespace) });
    }
    return m_uris[m_nodes[namespace + EXISchemaLayout.NAMESPACE_NAME]];
  }

  /**
   * Returns the number of global elements available in the namespace.
   * @param namespace namespace node
   * @return number of global elements available in the namespace.
   */
  public int getElemCountOfNamespace(int namespace)
    throws EXISchemaRuntimeException {
    if (namespace < 0 || m_nodes[namespace] != NAMESPACE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_NAMESPACE,
          new String[] { String.valueOf(namespace) });
    }
    return _getElemCountOfNamespace(namespace, m_nodes);
  }

  private static int _getElemCountOfNamespace(int namespace, int[] nodes) {
    return nodes[namespace + EXISchemaLayout.NAMESPACE_N_ELEMS];
  }

  /**
   * Returns the i-th element of the namespace.
   * @param namespace namespace node
   * @param i index of the element
   * @return i-th element
   */
  public int getElemOfNamespace(int namespace, int i)
    throws EXISchemaRuntimeException {
    if (namespace < 0 || m_nodes[namespace] != NAMESPACE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_NAMESPACE,
          new String[] { String.valueOf(namespace) });
    }
    return _getElemOfNamespace(namespace, i, m_nodes);
  }

  static int _getElemOfNamespace(int namespace, int i, int[] nodes) {
    return nodes[namespace + EXISchemaLayout.SZ_NAMESPACE + i];
  }
  
  /**
   * Returns the element of the namespace that has the specified name.
   * @param namespace namespace node
   * @param name name of the element.
   * @return element
   */
  public final int getElemOfNamespace(int namespace, String name)
    throws EXISchemaRuntimeException {
    if (namespace < 0 || m_nodes[namespace] != NAMESPACE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_NAMESPACE,
          new String[] { String.valueOf(namespace) });
    }
    final String namespaceName = getNameOfNamespace(namespace);
    final Integer[] nodes;
    if ((nodes = m_globalElementsDirectory.get(name)) != null) {
      final int len = nodes.length;
      for (int i = 0; i < len; i++) {
        final int elem = nodes[i];
        if (namespaceName.equals(getTargetNamespaceNameOfElem(elem))) {
          return elem;
        }
      }
    }
    return NIL_NODE;
  }

  /**
   * Returns the number of global attributes available in the namespace.
   * @param namespace namespace node
   * @return number of global attributes available in the namespace.
   */
  public int getAttrCountOfNamespace(int namespace)
    throws EXISchemaRuntimeException {
    if (namespace < 0 || m_nodes[namespace] != NAMESPACE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_NAMESPACE,
          new String[] { String.valueOf(namespace) });
    }
    return _getAttrCountOfNamespace(namespace, m_nodes);
  }

  private static int _getAttrCountOfNamespace(int namespace, int[] nodes) {
    return nodes[namespace + EXISchemaLayout.NAMESPACE_N_ATTRS];
  }

  /**
   * Returns the i-th attribute of the namespace.
   * @param namespace namespace node
   * @param i index of the attribute
   * @return i-th attribute
   */
  public int getAttrOfNamespace(int namespace, int i)
    throws EXISchemaRuntimeException {
    if (namespace < 0 || m_nodes[namespace] != NAMESPACE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_NAMESPACE,
          new String[] { String.valueOf(namespace) });
    }
    return _getAttrOfNamespace(namespace, i, m_nodes);
  }

  static int _getAttrOfNamespace(int namespace, int i, int[] nodes) {
    int n_elems = _getElemCountOfNamespace(namespace, nodes);
    return nodes[namespace + EXISchemaLayout.SZ_NAMESPACE + n_elems + i];
  }
  
  /**
   * Returns the attribute of the namespace that has the specified name.
   * @param namespace namespace node
   * @param name name of the attribute.
   * @return attribute
   */
  public final int getAttrOfNamespace(final int namespace, final String name) {
    final String namespaceName = getNameOfNamespace(namespace);
    final Integer[] nodes;
    if ((nodes = m_globalAttributesDirectory.get(name)) != null) {
      final int len = nodes.length;
      for (int i = 0; i < len; i++) {
        final int attr = nodes[i];
        if (namespaceName.equals(getTargetNamespaceNameOfAttr(attr))) {
          return attr;
        }
      }
    }
    return NIL_NODE;
  }

  static final int _getAttrOfNamespace(int namespace, String name, int[] nodes, String[] names) {
    int i, len;
    for (i = 0, len = _getAttrCountOfNamespace(namespace, nodes); i < len; i++) {
      int attr = _getAttrOfNamespace(namespace, i, nodes);
      if (_getNameOfAttr(attr, nodes, names) == name)
        return attr;
    }
    return NIL_NODE;
  }
  
  /**
   * Returns the number of global types available in the namespace.
   * @param namespace namespace node
   * @return number of global types available in the namespace.
   */
  public int getTypeCountOfNamespace(int namespace)
    throws EXISchemaRuntimeException {
    if (namespace < 0 || m_nodes[namespace] != NAMESPACE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_NAMESPACE,
          new String[] { String.valueOf(namespace) });
    }
    return _getTypeCountOfNamespace(namespace, m_nodes);
  }

  private static int _getTypeCountOfNamespace(int namespace, int[] nodes) {
    return nodes[namespace + EXISchemaLayout.NAMESPACE_N_TYPES];
  }

  /**
   * Returns the i-th global (i.e. named) type of the namespace.
   * @param namespace namespace node
   * @param i index of the type
   * @return i-th type
   */
  public int getTypeOfNamespace(int namespace, int i)
    throws EXISchemaRuntimeException {
    if (namespace < 0 || m_nodes[namespace] != NAMESPACE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_NAMESPACE,
          new String[] { String.valueOf(namespace) });
    }
    return _getTypeOfNamespace(namespace, i, m_nodes);
  }

  static int _getTypeOfNamespace(int namespace, int i, int[] nodes) {
    int n_elems = _getElemCountOfNamespace(namespace, nodes);
    int n_attrs = _getAttrCountOfNamespace(namespace, nodes);
    return nodes[namespace + EXISchemaLayout.SZ_NAMESPACE + n_elems + n_attrs + i];
  }

  /**
   * Returns the type of the namespace that has the specified name.
   * @param namespace namespace node
   * @param name name of the type.
   * @return type
   */
  public final int getTypeOfNamespace(int namespace, String name) {
    final String namespaceName = getNameOfNamespace(namespace);
    final Integer[] nodes;
    if ((nodes = m_globalTypesDirectory.get(name)) != null) {
      final int len = nodes.length;
      for (int i = 0; i < len; i++) {
        final int tp = nodes[i];
        if (namespaceName.equals(getTargetNamespaceNameOfType(tp))) {
          return tp;
        }
      }
    }
    return NIL_NODE;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // INode methods (common to elements and attributes)
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the type of an inode.
   * @param nd element or attribute node
   * @return type of the inode
   */
  public int getTypeOfINode(int nd)
    throws EXISchemaRuntimeException {
    if (nd < 0 || (m_nodes[nd] & INODE_MASK) == 0) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_INODE,
          new String[] { String.valueOf(nd) });
    }
    return _getTypeOfINode(nd, m_nodes);
  }

  static int _getTypeOfINode(int nd, int[] nodes) {
    return nodes[nd + EXISchemaLayout.INODE_TYPE];
  }

  public final boolean isSpecificINodeInFragment(int nd) {
    return _isSpecificINodeInFragment(nd, m_nodes);
  }
  
  public static final boolean _isSpecificINodeInFragment(int nd, int[] nodes) {
    return (nodes[nd + EXISchemaLayout.INODE_BOOLEANS] & EXISchemaLayout.INODE_ISSPECIFIC_IN_FRAGMENT_MASK) != 0;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Element methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the size of an element.
   * @param elem element node
   * @return size of the element
   */
  static private int _getSizeOfElem(int elem, int[] nodes) {
    return EXISchemaLayout.SZ_ELEM;
  }

  /**
   * Returns name of an element.
   * @param elem element node
   * @return name of an element node.
   */
  public String getNameOfElem(int elem)
    throws EXISchemaRuntimeException {
    if (elem < 0 || m_nodes[elem] != ELEMENT_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_ELEMENT,
          new String[] { String.valueOf(elem) });
    }
    return m_names[m_nodes[elem + EXISchemaLayout.INODE_NAME]];
  }

  public static String _getNameOfElem(int elem, int[] nodes, String[] names) {
    return names[nodes[elem + EXISchemaLayout.INODE_NAME]];
  }

  /**
   * Returns target namespace name of an element.
   * @param elem element node
   * @return target namespace name
   */
  public String getTargetNamespaceNameOfElem(int elem)
    throws EXISchemaRuntimeException {
    if (elem < 0 || m_nodes[elem] != ELEMENT_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_ELEMENT,
          new String[] { String.valueOf(elem) });
    }
    return m_uris[m_nodes[elem + EXISchemaLayout.INODE_TARGET_NAMESPACE]];
  }

  public static String _getTargetNamespaceNameOfElem(int elem, int[] nodes, String[] uris) {
    return uris[nodes[elem + EXISchemaLayout.INODE_TARGET_NAMESPACE]];
  }

  /**
   * Returns serial number of an element.
   * @param elem element node
   * @return serial number
   * @throws EXISchemaRuntimeException if it is not an element node
   */
  public int getSerialOfElem(int elem)
      throws EXISchemaRuntimeException {
    if (elem < 0 || m_nodes[elem] != ELEMENT_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_ELEMENT,
          new String[] { String.valueOf(elem) });
    }
    return m_nodes[elem + EXISchemaLayout.ELEM_NUMBER];
  }

  /**
   * Returns constraint type of an element.
   * @param elem element node
   * @return constraint type (i.e. CONSTRAINT_NONE, CONSTRAINT_DEFAULT or CONSTRAINT_FIXED)
   */
  public int getConstraintOfElem(int elem) {
    return m_nodes[elem + EXISchemaLayout.ELEM_CONSTRAINT];
  }

  /**
   * Returns constraint value of an element if any.
   * @param elem element node
   * @return constraint value variant
   */
  public int getConstraintValueOfElem(int elem)
    throws EXISchemaRuntimeException {
    if (elem < 0 || ELEMENT_NODE != m_nodes[elem]) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_ELEMENT,
          new String[] { String.valueOf(elem) });
    }
    return _getConstraintValueOfElem(elem, m_nodes);
  }
  
  static int _getConstraintValueOfElem(int elem, int[] nodes) {
    return nodes[elem + EXISchemaLayout.ELEM_CONSTRAINT_VALUE];
  }

  /**
   * Determines if the element is nillable.
   * @param elem element node
   * @return true if nillable
   */
  public boolean isNillableElement(int elem) {
    return EXISchemaLayout.ELEMENT_ISNILLABLE_MASK ==
           (m_nodes[elem + EXISchemaLayout.INODE_BOOLEANS] & EXISchemaLayout.ELEMENT_ISNILLABLE_MASK);
  }

  public static boolean _isNillableElement(int elem, int[] nodes) {
    return EXISchemaLayout.ELEMENT_ISNILLABLE_MASK ==
           (nodes[elem + EXISchemaLayout.INODE_BOOLEANS] & EXISchemaLayout.ELEMENT_ISNILLABLE_MASK);
  }

  /**
   * Determines if the element is global.
   * @param elem element node
   * @return true if global
   */
  public static final boolean _isGlobalElement(int elem, int[] nodes) {
    return (nodes[elem + EXISchemaLayout.INODE_BOOLEANS] & EXISchemaLayout.INODE_ISGLOBAL_MASK) != 0;
  }

  /**
   * Determines if the element is typed as simple type or complex type.
   * @param elem element node
   * @return true if typed as simple
   */
  public boolean isSimpleTypedElement(int elem) {
    return EXISchemaLayout.ELEMENT_ISSIMPLETYPE_MASK ==
           (m_nodes[elem + EXISchemaLayout.INODE_BOOLEANS] & EXISchemaLayout.ELEMENT_ISSIMPLETYPE_MASK);
  }

  /**
   * Determines if the element is typable.
   * @param elem element node
   * @return true if the element is typable
   */
  public boolean isTypableElement(int elem) {
    return EXISchemaLayout.ELEMENT_ISTYPABLE_MASK ==
           (m_nodes[elem + EXISchemaLayout.INODE_BOOLEANS] & EXISchemaLayout.ELEMENT_ISTYPABLE_MASK);
  }

  /**
   * Returns the type of an element.
   * @param elem element node
   * @return type of the element
   */
  public int getTypeOfElem(int elem) {
    return m_nodes[elem + EXISchemaLayout.INODE_TYPE];
  }

  public static int _getTypeOfElem(int elem, int[] nodes) {
    return nodes[elem + EXISchemaLayout.INODE_TYPE];
  }

  /**
   * Returns substitution affiliation element of an element.
   * @param elem element node
   * @return substitution affiliation element
   * @throws EXISchemaRuntimeException if it is not an element node
   */
  public int getSubstOfElem(int elem)
    throws EXISchemaRuntimeException {
    if (elem < 0 || m_nodes[elem] != ELEMENT_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_ELEMENT,
          new String[] { String.valueOf(elem) });
    }
    return _getSubstOfElem(elem, m_nodes);
  }

  static int _getSubstOfElem(int elem, int[] nodes) {
    return nodes[elem + EXISchemaLayout.ELEM_SUBST];
  }

  /**
   * Returns the number of elements substitutable for the element.
   * @param elem element node
   * @return number of substitutable elements
   */
  public int getSubstitutableCountOfElem(int elem) {
    return _getSubstitutableCountOfElem(elem, m_nodes);
  }

  static int _getSubstitutableCountOfElem(int elem, int[] nodes) {
    return nodes[elem + EXISchemaLayout.ELEM_N_SUBSTITUTABLES];
  }

  /**
   * Returns the i-th element substitutable for the element.
   * @param elem element node
   * @param i index of the substitutable
   * @return i-th substitutable element
   * @throws EXISchemaRuntimeException thrown if the index is out of bounds
   */
  public int getSubstitutableOfElem(int elem, int i)
      throws EXISchemaRuntimeException {
    return _getSubstitutableOfElem(elem, i, m_nodes, m_opaques);
  }

  static int _getSubstitutableOfElem(int elem, int i, int[] nodes, int[] opaques) {
    int n = _getSubstitutableCountOfElem(elem, nodes);
    if (i < 0 || i >= n)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
          new String[] {String.valueOf(i), String.valueOf(0),
          String.valueOf(n - 1)});
    int substitutables;
    if ((substitutables = nodes[elem + EXISchemaLayout.ELEM_SUBSTITUTABLES]) != NIL_NODE)
      return opaques[substitutables + i];
    else
      return NIL_NODE;
  }


  /**
   * Determines if an element is substitutable by the specified element.
   * @param elem an element
   * @param byElem the one that attempts to substitute the element
   * @return true if it is substitutable
   */
  public final boolean isSubstitutableElemByAnother(int elem, int byElem)
    throws EXISchemaRuntimeException {
    if (elem < 0 || m_nodes[elem] != ELEMENT_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_ELEMENT,
          new String[] { String.valueOf(elem) });
    }
    return _isSubstitutableElemByAnother(elem, byElem, m_nodes);
  }

  /**
   * Determines if an element is substitutable by the specified element.
   * @param elem an element
   * @param byElem the one that attempts to substitute the element
   * @return true if it is substitutable
   */
  static final boolean _isSubstitutableElemByAnother(int elem, final int byElem, int[] nodes) {
    if (byElem != NIL_NODE) {
      int iterItem = byElem;
      do {
        if (elem == iterItem)
          return true;
        iterItem = _getSubstOfElem(iterItem, nodes);
      } while (iterItem != NIL_NODE && iterItem != byElem);
    }
    return false;
  }
  
  /**
   * Returns content class of an element.
   * Elements of simple type have CONTENT_SIMPLE content class. Otherwise the
   * value corresponds to that of its complex type.
   * @param elem element node
   * @return one of CONTENT_EMPTY, CONTENT_SIMPLE, CONTENT_MIXED or CONTENT_ELEMENT_ONLY
   */
  public int getContentClassOfElem(int elem) {
    return _getContentClassOfElem(elem, m_nodes);
  }

  static final int _getContentClassOfElem(int elem, int[] nodes) {
    return nodes[elem + EXISchemaLayout.INODE_BOOLEANS] >>> EXISchemaLayout.ELEMENT_CONTENT_CLASS_OFFSET;
  }

  /**
   * Returns simple type of an element if its content class is CONTENT_SIMPLE.
   * @param elem element node
   * @return simple type of the element if applicable, otherwise NIL_NODE
   */
  public int getSimpleTypeOfElem(int elem) {
    return m_nodes[elem + EXISchemaLayout.ELEM_SIMPLE_TYPE];
  }

  /**
   * Returns the number of attribute uses of an element.
   * @param elem element node
   * @return number of attribute uses
   */
  public int getAttrUseCountOfElem(int elem) {
    return m_nodes[elem + EXISchemaLayout.ELEM_N_ATTRIBUTE_USES];
  }

  static int _getAttrUseCountOfElem(int elem, int[] nodes) {
    return nodes[elem + EXISchemaLayout.ELEM_N_ATTRIBUTE_USES];
  }

  /**
   * Returns the i-th attribute use of an element.
   * @param elem element node
   * @param i index of the attribute use
   * @return ith attribute use
   * @throws EXISchemaRuntimeException If the index is out of array bounds
   */
  public int getAttrUseOfElem(int elem, int i)
      throws EXISchemaRuntimeException {
    return _getAttrUseOfElem(elem, i, m_nodes);
  }

  static final int _getAttrUseOfElem(int elem, int i, int[] nodes)
      throws EXISchemaRuntimeException {
    /**
     * final int n = _getAttrUseCountOfElem(elem, nodes);
     */
    final int n = nodes[elem + EXISchemaLayout.ELEM_N_ATTRIBUTE_USES]; 
    if (i < 0 || i >= n) {
      throw new EXISchemaRuntimeException(
        EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
        new String[] { String.valueOf(i), String.valueOf(0), String.valueOf(n - 1) });
    }
    return nodes[elem + EXISchemaLayout.ELEM_ATTRIBUTE_USES] + i * EXISchemaLayout.SZ_ATTR_USE;
  }

  /**
   * Returns an attribute use or an attribute that is relevant to the
   * specified element given namespace name and name of the attribute.
   * An attribute use is returned if the specified attribute name matches
   * one of the element's attribute uses. Otherwise, an attribute is returned
   * if the attribute name matches the attribute wildcard and its definition
   * is available in the schema corpus. If no match was found or the definition
   * of the attribute matched to the wildcard was not available in the schema
   * corpus, NIL_NODE is returned.
   * @param elem element node
   * @param namespaceName target namespace name of the attribute
   * @param name name of the attribute
   * @return an attribute use or an attribute if available, otherwise NIL_NODE
   */
  public int getAttrUseOrAttrOfElem(int elem,
                                    String namespaceName,
                                    String name) {
    int auora = getAttrUseOfElem(elem, namespaceName, name);
    if (auora == EXISchema.NIL_NODE) {
      if (XMLSCHEMA_INSTANCE_URI != namespaceName) {
        int attrwc = getAttrWildcardOfElem(elem);
        if (attrwc != EXISchema.NIL_NODE &&
            matchWildcard(attrwc, namespaceName)) {
          int processContents = getProcessContentsOfWildcard(attrwc);
          if (EXISchema.WC_PROCESS_SKIP != processContents) {
            // attempt to resolve namespace and attr
            int namespace = getNamespaceOfSchema(namespaceName);
            if (namespace != EXISchema.NIL_NODE)
              auora = getAttrOfNamespace(namespace, name);
          }
        }
      }
      else { // XMLSCHEMA_INSTANCE_URI attribute
        int xsins = getNamespaceOfSchema(XMLSCHEMA_INSTANCE_URI);
        if (xsins != EXISchema.NIL_NODE)
          auora = getAttrOfNamespace(xsins, name);
      }
    }
    return auora;
  }

  /**
   * Returns an attribute use that uses an attribute of the specified name.
   * @param elem element node
   * @param namespaceName target namespace name of the attribute
   * @param attrName name of the attribute
   * @return an attribute use if found otherwise NIL_NODE
   */
  public int getAttrUseOfElem(int elem, String namespaceName, String attrName) {
    final int n = _getAttrUseCountOfElem(elem, m_nodes);
    int i, atuse;
    for (i = 0, atuse = EXISchema.NIL_NODE; i < n; i++) {
      if ((atuse = getAttrUseOfElem(elem, i)) != EXISchema.NIL_NODE) {
        if (getNameOfAttrUse(atuse).equals(attrName) &&
            getTargetNamespaceNameOfAttrUse(atuse).equals(namespaceName))
          break;
      }
    }
    if (i < n)
      return atuse;
    else
      return EXISchema.NIL_NODE;
  }

  /**
   * Returns attribute wildcard of an element.
   * @param elem element node
   * @return wildcard node if available, otherwise NIL_NODE
   */
  public int getAttrWildcardOfElem(int elem) {
    return m_nodes[elem + EXISchemaLayout.ELEM_ATTRIBUTE_WC];
  }

  /**
   * Returns group of an element.
   * @param elem element node
   * @return group node if CONTENT_MIXED or CONTENT_ELEMENT_ONLY, otherwise NIL_NODE
   */
  public int getGroupOfElem(int elem) {
    return m_nodes[elem + EXISchemaLayout.ELEM_GROUP];
  }

  /**
   * Returns minOccurs of the group in an element.
   * @param elem element node
   * @return minOccurs
   */
  public int getGroupMinOccursOfElem(int elem) {
    return m_nodes[elem + EXISchemaLayout.ELEM_GROUP_MINOCCURS];
  }

  /**
   * Returns maxOccurs of the group in an element.
   * @param elem element node
   * @return maxOccurs
   */
  public int getGroupMaxOccursOfElem(int elem) {
    return m_nodes[elem + EXISchemaLayout.ELEM_GROUP_MAXOCCURS];
  }

  /**
   * Returns the number of head instances possible for the group in an element.
   * @param elem element node
   * @return number of head instances possible
   */
  public int getGroupHeadInstanceCountOfElem(int elem) {
    return _getGroupHeadInstanceCountOfElem(elem, m_nodes);
  }

  static int _getGroupHeadInstanceCountOfElem(int elem, int[] nodes) {
    return nodes[elem + EXISchemaLayout.ELEM_GROUP_N_INITIALS];
  }

  /**
   * Returns the list of head instances for the group in an element.
   * @param elem element node
   * @return the list of head instances if available, otherwise NIL_NODE
   */
  public int getGroupHeadInstanceListOfElem(int elem) {
    return m_nodes[elem + EXISchemaLayout.ELEM_GROUP_INITIALS];
  }

  ///////////////////////////////////////////////////////////////////////////
  // Type methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns name of a type.
   * @param type type node
   * @return name of a type node.
   */
  public String getNameOfType(int type)
    throws EXISchemaRuntimeException {
    if (type < 0 || (m_nodes[type] & TYPE_MASK) == 0) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_TYPE,
          new String[] { String.valueOf(type) });
    }
    return m_names[m_nodes[type + EXISchemaLayout.TYPE_NAME]];
  }

  public static String _getNameOfType(int type, int[] nodes, String[] names) {
    return names[nodes[type + EXISchemaLayout.TYPE_NAME]];
  }

  /**
   * Returns target namespace name of a type.
   * @param type type node
   * @return target namespace name
   */
  public String getTargetNamespaceNameOfType(int type)
    throws EXISchemaRuntimeException {
    if (type < 0 || (m_nodes[type] & TYPE_MASK) == 0) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_TYPE,
          new String[] { String.valueOf(type) });
    }
    return _getTargetNamespaceNameOfType(type, m_nodes, m_uris);
  }

  public static String _getTargetNamespaceNameOfType(int type, int[] nodes, String[] uris) {
    return uris[nodes[type + EXISchemaLayout.TYPE_TARGET_NAMESPACE]];
  }
  
  /**
   * Returns serial number of a type. Those serial numbers of built-in
   * primitive types are static and do not change.
   * @param type type node
   * @return serial number
   */
  public int getSerialOfType(int type)
    throws EXISchemaRuntimeException {
    if (type < 0 || (m_nodes[type] & TYPE_MASK) == 0) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_TYPE,
          new String[] { String.valueOf(type) });
    }
    return _getSerialOfType(type, m_nodes);
  }

  static int _getSerialOfType(int type, int[] nodes) {
    return nodes[type + EXISchemaLayout.TYPE_NUMBER];
  }

  ///////////////////////////////////////////////////////////////////////////
  // SimpleType methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the size of a simple type.
   * @param stype simple type node
   * @return size of the simple type
   */
  private static int _getSizeOfSimpleType(int stype, int[] nodes) {
    int size = EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SZ_SIMPLE_TYPE;
    size += _getRestrictedCharacterCountOfSimpleType(stype, nodes); 
    if (_isEnumeratedSimpleType(stype, nodes)) {
      size += 1 + _getEnumerationFacetCountOfSimpleType(stype, nodes); 
    }
    if (_getVarietyOfSimpleType(stype, nodes) == UNION_SIMPLE_TYPE) {
      size += _getMemberTypesCountOfUnionSimpleType(stype, nodes); 
    }
    return size; 
  }

  /**
   * Returns base type of an atomic simple type.
   * @param stype atomic type node
   * @return base type
   */
  public int getBaseTypeOfAtomicSimpleType(int stype)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    if (getVarietyOfSimpleType(stype) == ATOMIC_SIMPLE_TYPE) {
      return m_nodes[stype + (EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_FIELD_INT)];
    }
    return NIL_NODE;
  }

  /**
   * Returns variety property of a simple type.
   * @param stype simple type node
   * @return one of UR_SIMPLE_TYPE, ATOMIC_SIMPLE_TYPE, LIST_SIMPLE_TYPE or UNION_SIMPLE_TYPE
   */
  public int getVarietyOfSimpleType(int stype)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    return _getVarietyOfSimpleType(stype, m_nodes);
  }

  public static int _getVarietyOfSimpleType(int stype, int[] nodes) {
    return nodes[stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_VARIETY_MASK;
  }

  /**
   * Returns the ancestry type ID of a simple type. Ancestry ID is the type ID of
   * one of the primitive types, xsd:anySimpleType or xsd:integer.
   * @param stype simple type
   * @return ancestry type ID
   */
  public int getAncestryIdOfSimpleType(int stype)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    return _getAncestryIdOfSimpleType(stype, m_nodes);
  }
  
  private static final int _getAncestryIdOfSimpleType(int stype, int[] nodes) {
    if (_getVarietyOfSimpleType(stype, nodes) == ATOMIC_SIMPLE_TYPE) {
      return (nodes[stype + (EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_AUX)] & 
        EXISchemaLayout.SIMPLE_TYPE_ATOMIC_ANCESTRYID_MASK) >> EXISchemaLayout.SIMPLE_TYPE_ATOMIC_ANCESTRYID_OFFSET;
    }
    return EXISchemaConst.ANY_SIMPLE_TYPE;
  }

  /**
   * Returns item type of a list simple type.
   * @param stype list simple type
   * @return item type
   */
  public int getItemTypeOfListSimpleType(int stype)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    if (getVarietyOfSimpleType(stype) == LIST_SIMPLE_TYPE) {
      return m_nodes[stype + (EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_FIELD_INT)];
    }
    return NIL_NODE;

  }

  /**
   * Determines if a simple type is primitive.
   * @param stype simple type
   * @return true if the simple type is primitive
   */
  public boolean isPrimitiveSimpleType(int stype)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    final int serial = _getSerialOfType(stype, m_nodes);
    // primitive simple types' serial is [2...20]
    return EXISchemaConst.ANY_SIMPLE_TYPE < serial && 
      serial < EXISchemaConst.STRING_TYPE + EXISchemaConst.N_PRIMITIVE_TYPES;
  }

  /**
   * Determines if a simple type represents integral numbers.
   * @param stype simple type
   * @return true if the simple type represents integral numbers
   */
  public boolean isIntegralSimpleType(int stype)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    return _getAncestryIdOfSimpleType(stype, m_nodes) == EXISchemaConst.INTEGER_TYPE; 
  }

  /**
   * Returns the representation of an integral simple type.
   * @param stype integral simple type
   * @return INTEGER_CODEC_NONNEGATIVE for a non-negative 
   * integer representation, INTEGER_CODEC_DEFAULT for a default 
   * integer representation, otherwise width of n-bits integer 
   * representation.
   */
  public int getWidthOfIntegralSimpleType(int stype)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    return (m_nodes[stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_MASK) >> 
      EXISchemaLayout.SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_OFFSET;
  }

  /**
   * Returns whiteSpace facet value of a simple type.
   * @param stype simple type
   * @return one of WHITESPACE_* enumerated values
   */
  public int getWhitespaceFacetValueOfSimpleType(int stype)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    final int variety;
    if ((variety = getVarietyOfSimpleType(stype)) == ATOMIC_SIMPLE_TYPE) {
      if (getAncestryIdOfSimpleType(stype) == EXISchemaConst.STRING_TYPE)
        return (m_nodes[stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_MASK) >> 
          EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_OFFSET;   
      else
        return WHITESPACE_COLLAPSE;
    }
    else if (variety == LIST_SIMPLE_TYPE)
      return WHITESPACE_COLLAPSE;
    else
      return WHITESPACE_ABSENT;
  }
  
  public boolean isPatternedBooleanSimpleType(int stype) {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    assert getAncestryIdOfSimpleType(stype) == EXISchemaConst.BOOLEAN_TYPE;
    return (m_nodes[stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_ATOMIC_BOOLEAN_HAS_PATTERN_MASK) != 0; 
  }

  /**
   * Returns minInclusive facet variant of a simple type.
   * @param stype simple type
   * @return minInclusive facet variant if available, otherwise NIL_VALUE
   */
  public int getMinInclusiveFacetOfSimpleType(int stype)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    return m_nodes[stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_FACET_MININCLUSIVE];
  }

  /**
   * Returns the number of characters in restricted characters set pertinent to a simple type.
   * @param stype simple type
   * @return number of characters in restricted character set
   */
  public int getRestrictedCharacterCountOfSimpleType(int stype)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    return _getRestrictedCharacterCountOfSimpleType(stype, m_nodes);
  }

  private static int _getRestrictedCharacterCountOfSimpleType(int stype, int[] nodes) {
    final int ancestryId = _getAncestryIdOfSimpleType(stype, nodes);  
    if (ancestryId == EXISchemaConst.STRING_TYPE)
      return (nodes[stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_AUX] & EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_MASK) >> 
        EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_OFFSET;   
    return 0;
  }

  /**
   * Returns the start index of the restricted charset. 
   * @param stype simple type
   */
  public int getRestrictedCharacterOfSimpleType(int stype)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    return stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SZ_SIMPLE_TYPE;
  }

  private static boolean _isEnumeratedSimpleType(int stype, int[] nodes) {
    return (nodes[stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_AUX] & 
        EXISchemaLayout.SIMPLE_TYPE_HAS_ENUMERATED_VALUES_MASK) != 0;
  }
  
  /**
   * Returns the number of enumeration facets pertinent to a simple type.
   * @param stype simple type
   * @return number of enumeration facets
   */
  public int getEnumerationFacetCountOfSimpleType(int stype)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    return _getEnumerationFacetCountOfSimpleType(stype, m_nodes);
  }

  private static int _getEnumerationFacetCountOfSimpleType(int stype, int[] nodes)
    throws EXISchemaRuntimeException {
    if (_isEnumeratedSimpleType(stype, nodes)) {
      final int n_patterns = _getRestrictedCharacterCountOfSimpleType(stype, nodes);
      return nodes[stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SZ_SIMPLE_TYPE + n_patterns];
    }
    return 0;
  }
  
  /**
   * Returns the i-th enumeration facet variant of a simple type.
   * @param stype simple type
   * @param i index of the enumeration facet
   * @return ith enumeration facet variant
   */
  public int getEnumerationFacetOfSimpleType(int stype, int i)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    final int n = _getEnumerationFacetCountOfSimpleType(stype, m_nodes);
    if (i < 0 || i >= n) {
      throw new EXISchemaRuntimeException(
        EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
        new String[] { String.valueOf(i), String.valueOf(0), String.valueOf(n - 1) });
    }
    final int n_patterns = _getRestrictedCharacterCountOfSimpleType(stype, m_nodes);
    return m_nodes[stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SZ_SIMPLE_TYPE + n_patterns + 1 + i];
  }

  /**
   * Returns the number of member types of an union simple type.
   * @param stype simple type
   * @return number of member types
   */
  public int getMemberTypesCountOfUnionSimpleType(int stype)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    if (_getVarietyOfSimpleType(stype, m_nodes) != UNION_SIMPLE_TYPE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_UNION_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    return _getMemberTypesCountOfUnionSimpleType(stype, m_nodes);
  }

  private static int _getMemberTypesCountOfUnionSimpleType(int stype, int[] nodes) {
    return nodes[stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_FIELD_INT];
  }

  /**
   * Returns the i-th member type of an union simple type.
   * @param stype simple type
   * @param i index of the member type
   * @return ith member type
   */
  public int getMemberTypeOfUnionSimpleType(int stype, int i)
    throws EXISchemaRuntimeException {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    if (_getVarietyOfSimpleType(stype, m_nodes) != UNION_SIMPLE_TYPE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_UNION_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    int trailerPos = stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SZ_SIMPLE_TYPE;
    trailerPos += _getRestrictedCharacterCountOfSimpleType(stype, m_nodes);
    if (_isEnumeratedSimpleType(stype, m_nodes)) {
      trailerPos += 1 + _getEnumerationFacetCountOfSimpleType(stype, m_nodes); 
    }
    return m_nodes[trailerPos + i];
  }

  public int getNextSimpleType(int stype) {
    if (stype < 0 || m_nodes[stype] != SIMPLE_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_SIMPLE_TYPE,
          new String[] { String.valueOf(stype) });
    }
    return m_nodes[stype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_NEXT_SIMPLE_TYPE];
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // ComplexType methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the size of a complex type.
   * @param ctype complex type node
   * @return size of the complex type
   */
  static private int _getSizeOfComplexType(int ctype, int[] nodes) {
      return EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SZ_COMPLEX_TYPE +
          2 * nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_N_ATTRIBUTE_USES]; 
  }
  
  /**
   * Returns content type class of a complex type.
   * @param ctype complex type node
   * @return one of CONTENT_EMPTY, CONTENT_SIMPLE, CONTENT_MIXED or CONTENT_ELEMENT_ONLY
   */
  public int getContentClassOfComplexType(int ctype)
    throws EXISchemaRuntimeException {
    if (ctype < 0 || m_nodes[ctype] != COMPLEX_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_COMPLEX_TYPE,
          new String[] { String.valueOf(ctype) });
    }
    return _getContentClassOfComplexType(ctype, m_nodes);
  }

  public static int _getContentClassOfComplexType(int ctype, int[] nodes) {
    return nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_CONTENT_CLASS];
  }

  /**
   * Returns content type (i&#46;e&#46; particle) of a complex type.
   * The type of the node it returns depends on the content type class of
   * the specified complex type. It returns a content model (i&#46;e&#46;
   * particle) node if CONTENT_MIXED or CONTENT_ELEMENT_ONLY. It returns a
   * simple type node if CONTENT_SIMPLE. Otherwise, it returns NIL_NODE.
   * @param ctype complex type node
   * @return content type
   * @see #getContentClassOfComplexType
   */
  public int getContentTypeOfComplexType(int ctype)
    throws EXISchemaRuntimeException {
    if (ctype < 0 || m_nodes[ctype] != COMPLEX_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_COMPLEX_TYPE,
          new String[] { String.valueOf(ctype) });
    }
    return _getContentTypeOfComplexType(ctype, m_nodes);
  }

  public static int _getContentTypeOfComplexType(int ctype, int[] nodes) {
    return nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_CONTENT_TYPE];
  }

  /**
   * Returns minOccurs property value of complex type's particle.
   * @param ctype complex type node
   * @return minOccurs property value
   * @throws EXISchemaRuntimeException Thrown if the node is not a complex type.
   * @see #getContentClassOfComplexType
   */
  public int getParticleMinOccursOfComplexType(int ctype)
    throws EXISchemaRuntimeException {
    if (ctype < 0 || m_nodes[ctype] != COMPLEX_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_COMPLEX_TYPE,
          new String[] { String.valueOf(ctype) });
    }
    boolean hasParticle =
      EXISchemaLayout.COMPLEX_TYPE_HASPARTICLE_MASK ==
        (EXISchemaLayout.COMPLEX_TYPE_HASPARTICLE_MASK &
        m_nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_BOOLEANS]);

    if (hasParticle) {
        final int ctsz = _getSizeOfComplexType(ctype, m_nodes);
        return m_nodes[ctype + ctsz + EXISchemaLayout.PARTICLE_MINOCCURS];
    }
    return 0;
  }

  /**
   * Returns maxOccurs property value of complex type's particle.
   * @param ctype complex type node
   * @return maxOccurs property value
   * @throws EXISchemaRuntimeException Thrown if the node is not a complex type.
   * @see #getContentClassOfComplexType
   */
  public int getParticleMaxOccursOfComplexType(int ctype)
    throws EXISchemaRuntimeException {
    if (ctype < 0 || m_nodes[ctype] != COMPLEX_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_COMPLEX_TYPE,
          new String[] { String.valueOf(ctype) });
    }
    boolean hasParticle =
      EXISchemaLayout.COMPLEX_TYPE_HASPARTICLE_MASK ==
        (EXISchemaLayout.COMPLEX_TYPE_HASPARTICLE_MASK &
        m_nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_BOOLEANS]);

    if (hasParticle) {
      final int ctsz = _getSizeOfComplexType(ctype, m_nodes);
      return m_nodes[ctype + ctsz + EXISchemaLayout.PARTICLE_MAXOCCURS];
    }
    else
      return 0;
  }

  /**
   * Returns term node of complex type's particle.
   * @param ctype complex type node
   * @return term node
   * @throws EXISchemaRuntimeException Thrown if the node is not a complex type.
   * @see #getContentClassOfComplexType
   */
  public int getParticleTermOfComplexType(int ctype)
    throws EXISchemaRuntimeException {
    if (ctype < 0 || m_nodes[ctype] != COMPLEX_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_COMPLEX_TYPE,
          new String[] { String.valueOf(ctype) });
    }
    boolean hasParticle =
      EXISchemaLayout.COMPLEX_TYPE_HASPARTICLE_MASK ==
        (EXISchemaLayout.COMPLEX_TYPE_HASPARTICLE_MASK &
        m_nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_BOOLEANS]);

    if (hasParticle) {
      final int ctsz = _getSizeOfComplexType(ctype, m_nodes);
      return m_nodes[ctype + ctsz + EXISchemaLayout.PARTICLE_TERM];
    }
    else
      return EXISchema.NIL_NODE;
  }

  /**
   * Returns the number of attribute uses of a complex type.
   * @param ctype complex type
   * @return number of attribute uses
   */
  public int getAttrUseCountOfComplexType(int ctype)
    throws EXISchemaRuntimeException {
    if (ctype < 0 || m_nodes[ctype] != COMPLEX_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_COMPLEX_TYPE,
          new String[] { String.valueOf(ctype) });
    }
    return _getAttrUseCountOfComplexType(ctype, m_nodes);
  }

  public static int _getAttrUseCountOfComplexType(int ctype, int[] nodes) {
    return nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_N_ATTRIBUTE_USES];
  }

  /**
   * Returns the i-th attribute use of a complex type.
   * @param ctype complex type
   * @param i index of the attribute use
   * @return ith attribute use
   */
  public int getAttrUseOfComplexType(int ctype, int i)
    throws EXISchemaRuntimeException {
    if (ctype < 0 || m_nodes[ctype] != COMPLEX_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_COMPLEX_TYPE,
          new String[] { String.valueOf(ctype) });
    }
    final int n = _getAttrUseCountOfComplexType(ctype, m_nodes);
    if (i < 0 || i >= n)
      throw new EXISchemaRuntimeException(
        EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
        new String[] { String.valueOf(i), String.valueOf(0), String.valueOf(n - 1) });
  
    return _getAttrUseOfComplexType(ctype, i, m_nodes);
  }

  static int _getAttrUseOfComplexType(int ctype, int i, int[] nodes)
    throws EXISchemaRuntimeException {
    final int base = _getAttrUseBaseOfComplexType(ctype, nodes);
    if (base != EXISchema.NIL_NODE)
      return base + i * EXISchemaLayout.SZ_ATTR_USE;
    else
      return EXISchema.NIL_NODE;
  }
  
  public static int _getAttrUseBaseOfComplexType(int ctype, int[] nodes) {
    return nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_ATTRIBUTE_USES];
  }

  /**
   * Returns an attribute use that uses an attribute of the specified name.
   * @param ctype complex type node
   * @param namespaceName target namespace name of the attribute
   * @param attrName name of the attribute
   * @return an attribute use if found otherwise NIL_NODE
   */
  public int getAttrUseOfComplexType(int ctype, String namespaceName, String attrName) {
    final int n = _getAttrUseCountOfComplexType(ctype, m_nodes);
    int i, atuse;
    for (i = 0, atuse = EXISchema.NIL_NODE; i < n; i++) {
      if ((atuse = _getAttrUseOfComplexType(ctype, i, m_nodes)) != EXISchema.NIL_NODE) {
        if (getNameOfAttrUse(atuse).equals(attrName) &&
            getTargetNamespaceNameOfAttrUse(atuse).equals(namespaceName))
          break;
      }
    }
    if (i < n)
      return atuse;
    else
      return EXISchema.NIL_NODE;
  }

  public int getNextAttrUsesCountOfComplexType(int ctype, int i) {
    return m_nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SZ_COMPLEX_TYPE + i +
               m_nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_N_ATTRIBUTE_USES]];
  }

  /**
   * Returns an opaque node representing the attribute uses permitted at i-th position.
   */
  public int getNextAttrUsesOfComplexType(int ctype, int i) {
      return m_nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SZ_COMPLEX_TYPE + i];
  }
  
  /**
   * Returns attribute wildcard of a complex type.
   * @param ctype complex type node
   * @return wildcard node if available, otherwise NIL_NODE
   */
  public int getAttrWildcardOfComplexType(int ctype)
    throws EXISchemaRuntimeException {
    if (ctype < 0 || m_nodes[ctype] != COMPLEX_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_COMPLEX_TYPE,
          new String[] { String.valueOf(ctype) });
    }
    return _getAttrWildcardOfComplexType(ctype, m_nodes);
  }

  public static int _getAttrWildcardOfComplexType(int ctype, int[] nodes) {
    return nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_ATTRIBUTE_WC];
  }

  /**
   * Returns simple type of a complex type if its content class is CONTENT_SIMPLE.
   * @param ctype complex type node
   * @return simple type of the complex type if applicable, otherwise NIL_NODE
   * @throws EXISchemaRuntimeException Thrown if the node is not a complex type
   */
  public int getSimpleTypeOfComplexType(int ctype)
    throws EXISchemaRuntimeException {
    if (ctype < 0 || m_nodes[ctype] != COMPLEX_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_COMPLEX_TYPE,
          new String[] { String.valueOf(ctype) });
    }
    int simpleType = EXISchema.NIL_NODE;
    if (_getContentClassOfComplexType(ctype, m_nodes) == EXISchema.CONTENT_SIMPLE) {
      simpleType = EXISchema._getContentTypeOfComplexType(ctype, m_nodes);
    }
    return simpleType;
  }

  /**
   * Returns the number of substance particles of a complex type.
   * @param ctype complex type node
   * @return the number of substance particles
   * @throws EXISchemaRuntimeException Thrown if the node is not a complex type
   */
  public int getSubstanceCountOfComplexType(int ctype)
    throws EXISchemaRuntimeException {
    if (ctype < 0 || m_nodes[ctype] != COMPLEX_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_COMPLEX_TYPE,
          new String[] { String.valueOf(ctype) });
    }
    return m_nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_N_PARTICLES];
  }

  /**
   * Returns the number of head substance particles of a complex type.
   * Head substance particles are the only substance particles that
   * are initially relevant.  
   * @param ctype
   * @return the number of head substance particles
   * @throws EXISchemaRuntimeException Thrown if the node is not a complex type
   */
  public int getHeadSubstanceCountOfComplexType(int ctype)
    throws EXISchemaRuntimeException {
    if (ctype < 0 || m_nodes[ctype] != COMPLEX_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_COMPLEX_TYPE,
          new String[] { String.valueOf(ctype) });
    }
    return m_nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_N_INITIALS];
  }

  /**
   * Returns the list of substance particles of a complex type.
   * The last entry in the list is *always* NIL_NODE.
   * @param ctype complex type node
   * @return the list of substance particles if available, otherwise NIL_NODE
   * @throws EXISchemaRuntimeException Thrown if the node is not a complex type
   */
  public int getSubstanceListOfComplexType(int ctype)
    throws EXISchemaRuntimeException {
    if (ctype < 0 || m_nodes[ctype] != COMPLEX_TYPE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_COMPLEX_TYPE,
          new String[] { String.valueOf(ctype) });
    }
    return m_nodes[ctype + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.COMPLEX_TYPE_PARTICLES];
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Particle methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns minOccurs property value of a particle.
   * @param particle particle node
   * @return minOccurs property value
   */
  public int getMinOccursOfParticle(int particle) {
    return _getMinOccursOfParticle(particle, m_nodes);
  }

  public static int _getMinOccursOfParticle(int particle, int[] nodes) {
    return nodes[particle + EXISchemaLayout.PARTICLE_MINOCCURS];
  }

  /**
   * Returns maxOccurs property value of a particle.
   * @param particle particle node
   * @return maxOccurs property value
   */
  public int getMaxOccursOfParticle(int particle) {
    return _getMaxOccursOfParticle(particle, m_nodes);
  }

  public static int _getMaxOccursOfParticle(int particle, int[] nodes) {
    return nodes[particle + EXISchemaLayout.PARTICLE_MAXOCCURS];
  }

  /**
   * Returns term type of a particle.
   * @param particle particle node
   * @return one of TERM_TYPE_GROUP, TERM_TYPE_WILDCARD or TERM_TYPE_ELEMENT
   */
  public int getTermTypeOfParticle(int particle) {
    return _getTermTypeOfParticle(particle, m_nodes);
  }

  public static int _getTermTypeOfParticle(int particle, int[] nodes) {
    return nodes[particle + EXISchemaLayout.PARTICLE_TERM_TYPE];
  }
  
  /**
   * Returns term node of a particle.
   * @param particle particle node
   * @return term node
   */
  public int getTermOfParticle(int particle) {
    return _getTermOfParticle(particle, m_nodes);
  }

  public static final int _getTermOfParticle(int particle, int[] nodes) {
    return nodes[particle + EXISchemaLayout.PARTICLE_TERM];
  }

  /**
   * Determines if the particle is a fixture.
   * A fixture particle is known to always appear in the PSVI infoset.
   * @param particle particle node
   * @return true if it is a fixture.
   */
  boolean isFixtureParticle(int particle) {
    return _isFixtureParticle(particle, m_nodes);
  }

  public static final boolean _isFixtureParticle(int particle, int[] nodes) {
    return (nodes[particle + EXISchemaLayout.PARTICLE_BOOLEANS] & EXISchemaLayout.PARTICLE_ISFIXTURE_MASK) != 0;
  }

  /**
   * Returns the number of substance particles of a particle.
   * @param particle particle node
   * @return number of substance particles of a particle.
   */
  int getSubstanceCountOfParticle(int particle) {
    return _getSubstanceCountOfParticle(particle, m_nodes);
  }

  public static int _getSubstanceCountOfParticle(int particle, int[] nodes) {
    return nodes[particle + EXISchemaLayout.PARTICLE_N_SUBSTANCES];
  }

  /**
   * Returns the list of substance particles of a particle
   * @param particle particle node
   * @return the list of substance particles if available, otherwise NIL_NODE
   */
  public static int _getSubstanceListOfParticle(int particle, int[] nodes) {
    return nodes[particle + EXISchemaLayout.PARTICLE_SUBSTANCES];
  }

  /**
   * Returns the number of head substance particles of a particle.
   * @param particle particle node
   * @return number of head substance particles
   */
  public int getHeadSubstanceCountOfParticle(int particle) {
    return _getHeadSubstanceCountOfParticle(particle, m_nodes);
  }

  public static int _getHeadSubstanceCountOfParticle(int particle, int[] nodes) {
    return nodes[particle + EXISchemaLayout.PARTICLE_N_INITIALS];
  }

  /**
   * Returns the list of head substance particles of a particle
   * @param particle particle node
   * @return the list of head substance particles if available, otherwise NIL_NODE
   */
  public static int _getHeadSubstanceListOfParticle(int particle, int[] nodes) {
    return nodes[particle + EXISchemaLayout.PARTICLE_INITIALS];
  }

  /**
   * Returns the ith head substance particle of a particle
   * @param particle particle node
   * @return ith head substance particle
   */
  public int getHeadSubstanceOfParticle(int particle, int i) {
    int n = _getHeadSubstanceCountOfParticle(particle, m_nodes);
    if (i < 0 || i >= n) {
      throw new EXISchemaRuntimeException(
        EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
        new String[] { String.valueOf(i), String.valueOf(0), String.valueOf(n - 1) });
    }
    int first = _getHeadSubstanceListOfParticle(particle, m_nodes);
    return m_opaques[first + i];
  }
  
  /**
   * Return the serial number of a particle within the scope of the complex type that
   * it belongs to.
   * @param particle particle node
   * @return serial number of a particle
   */
  public int getSerialInTypeOfParticle(int particle) {
    if (particle < 0 || m_nodes[particle] != PARTICLE_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_PARTICLE,
          new String[] { String.valueOf(particle) });
    }
    return m_nodes[particle + EXISchemaLayout.PARTICLE_SERIAL_INTYPE];
  }

  ///////////////////////////////////////////////////////////////////////////
  // Group methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the size of a group.
   * @param group group node
   * @return size of the group
   */
  static private int _getSizeOfGroup(int group, int[] nodes) {
    return EXISchemaLayout.SZ_GROUP + 3 * (_getParticleCountOfGroup(group, nodes) + 1);
  }

  /**
   * Returns compositor of a group.
   * @param group model group
   * @return one of GROUP_ALL, GROUP_CHOICE or GROUP_SEQUENCE
   */
  public int getCompositorOfGroup(int group) {
    if (group < 0 || m_nodes[group] != GROUP_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_GROUP,
          new String[] { String.valueOf(group) });
    }
    return _getCompositorOfGroup(group, m_nodes);
  }

  public static int _getCompositorOfGroup(int group, int[] nodes) {
    return nodes[group + EXISchemaLayout.GROUP_COMPOSITOR];
  }

  /**
   * Returns the number of particles in the group.
   * @param group model group
   * @return number of particles in the group
   */
  public int getParticleCountOfGroup(int group) {
    if (group < 0 || m_nodes[group] != GROUP_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_GROUP,
          new String[] { String.valueOf(group) });
    }
    return _getParticleCountOfGroup(group, m_nodes);
  }

  public static int _getParticleCountOfGroup(int group, int[] nodes) {
    return nodes[group + EXISchemaLayout.GROUP_N_PARTICLES];
  }

  /**
   * Returns the i-th particle in the group.
   * @param group model group
   * @param i position in the list of particles
   * @return the i-th particle
   */
  public final int getParticleOfGroup(int group, int i) {
    if (group < 0 || m_nodes[group] != GROUP_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_GROUP,
          new String[] { String.valueOf(group) });
    }
    final int n_particles = m_nodes[group + EXISchemaLayout.GROUP_N_PARTICLES];
    return group + EXISchemaLayout.SZ_GROUP + 3 * (n_particles + 1) + i * EXISchemaLayout.SZ_PARTICLE;
  }

  public boolean isFixtureGroup(int group) {
    if (group < 0 || m_nodes[group] != GROUP_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_GROUP,
          new String[] { String.valueOf(group) });
    }
    return isFixtureGroup(group, m_nodes);
  }

  /**
   * For used by SchemaCompiler
   */
  public static boolean isFixtureGroup(int group, int[] nodes) {
    return EXISchemaLayout.GROUP_ISFIXTURE_MASK ==
           (nodes[group + EXISchemaLayout.GROUP_BOOLEANS] & EXISchemaLayout.GROUP_ISFIXTURE_MASK);
  }

  /**
   * Returns the number of member substance particles of a group.
   * @param group model group
   * @return number of member substance particles
   */
  public int getMemberSubstanceCountOfGroup(int group) {
    if (group < 0 || m_nodes[group] != GROUP_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_GROUP,
          new String[] { String.valueOf(group) });
    }
    return _getMemberSubstanceCountOfGroup(group, m_nodes);
  }

  public static int _getMemberSubstanceCountOfGroup(int group, int[] nodes) {
    return nodes[group + EXISchemaLayout.GROUP_N_MEMBER_SUBSTANCE_NODES];
  }

  /**
   * Returns the i-th member substance particle of a group.
   * @param group model group
   * @param i index of the substance particle
   * @return i-th member substance particle in the group
   * @throws EXISchemaRuntimeException If the index is out of bounds
   */
  public int getMemberSubstanceOfGroup(int group, int i) throws EXISchemaRuntimeException {
    if (group < 0 || m_nodes[group] != GROUP_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_GROUP,
          new String[] { String.valueOf(group) });
    }
    return _getMemberSubstanceOfGroup(group, i, m_nodes, m_opaques);
  }

  public static int _getMemberSubstanceOfGroup(int group, int i, int[] nodes, int[] opaques) throws EXISchemaRuntimeException {
    final int n = _getMemberSubstanceCountOfGroup(group, nodes);
    if (i < 0 || i >= n)
      throw new EXISchemaRuntimeException(
        EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
        new String[] { String.valueOf(i), String.valueOf(0), String.valueOf(n - 1) });
    return opaques[_getMemberSubstanceListOfGroup(group, nodes) + i];
  }
  
  /**
   * Returns the list of member substance particles of a group.
   * @param group model group
   * @return the list of member substance particles if available, otherwise NIL_NODE
   */
  public int getMemberSubstanceListOfGroup(int group) {
    if (group < 0 || m_nodes[group] != GROUP_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_GROUP,
          new String[] { String.valueOf(group) });
    }
    return _getMemberSubstanceListOfGroup(group, m_nodes);
  }

  public static int _getMemberSubstanceListOfGroup(int group, int[] nodes) {
    return nodes[group + EXISchemaLayout.GROUP_MEMBER_SUBSTANCE_NODES];
  }

  /**
   * Returns the number of uniform head substance particles of a group.
   * @param model group
   * @return number of uniform head substance particles
   */
  public int getHeadSubstanceCountOfGroup(int group) {
    if (group < 0 || m_nodes[group] != GROUP_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_GROUP,
          new String[] { String.valueOf(group) });
    }
    return _getHeadSubstanceCountOfGroup(group, m_nodes);
  }

  public static final int _getHeadSubstanceCountOfGroup(int group, int[] nodes) {
    return nodes[group + EXISchemaLayout.GROUP_N_HEAD_SUBSTANCE_NODES];
  }

  /**
   * Returns the list of uniform substance particles of a group.
   * @param model group
   * @return the list of uniform head substance particles if available, otherwise NIL_NODE
   */
  public int getHeadSubstanceListOfGroup(int group) {
    if (group < 0 || m_nodes[group] != GROUP_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_GROUP,
          new String[] { String.valueOf(group) });
    }
    return _getHeadSubstanceListOfGroup(group, m_nodes);
  }

  public static int _getHeadSubstanceListOfGroup(int group, int[] nodes) {
    return nodes[group + EXISchemaLayout.GROUP_HEAD_SUBSTANCE_NODES];
  }

  /**
   * Returns the number of head substance particles for the ith state.
   * @param model group
   * @param i state position (0 upto n_particles for sequence, otherwise 0)
   * @return number of head substance particles possible for the state
   */
  public int getHeadSubstanceCountOfGroup(int group, int i) {
    if (group < 0 || m_nodes[group] != GROUP_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_GROUP,
          new String[] { String.valueOf(group) });
    }
    return _getHeadSubstanceCountOfGroup(group, i, m_nodes);
  }

  public static final int _getHeadSubstanceCountOfGroup(int group, int i, int[] nodes) {
    final int n = nodes[group + EXISchemaLayout.GROUP_N_PARTICLES];
    if (i < 0 || i > n) {
      throw new EXISchemaRuntimeException(
        EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
        new String[] { String.valueOf(i), String.valueOf(0), String.valueOf(n - 1) });
    }
    return nodes[group + EXISchemaLayout.SZ_GROUP + 3 * i];
  }

  /**
   * Returns the number of backward head substance particles for the ith state.
   * @param group model group
   * @param i state position (0 upto n_particles for sequence, otherwise 0)
   * @return number of backward head substance particles possible for the state
   */
  public int getBackwardHeadSubstanceCountOfGroup(int group, int i) {
    if (group < 0 || m_nodes[group] != GROUP_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_GROUP,
          new String[] { String.valueOf(group) });
    }
    return _getBackwardHeadSubstanceCountOfGroup(group, i, m_nodes);
  }

  static final int _getBackwardHeadSubstanceCountOfGroup(int group, int i, int[] nodes) {
    final int n = nodes[group + EXISchemaLayout.GROUP_N_PARTICLES];
    if (i < 0 || i > n) {
      throw new EXISchemaRuntimeException(
        EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
        new String[] { String.valueOf(i), String.valueOf(0), String.valueOf(n - 1) });
    }
    return nodes[group + EXISchemaLayout.SZ_GROUP + 3 * i + 2];
  }
  
  /**
   * Returns the list of head substance particles for the ith state
   * @param group model group
   * @param i state position (0 upto n_particles for sequence, otherwise 0)
   * @return the list of head substance particles if available, otherwise NIL_NODE
   */
  public int getHeadSubstanceListOfGroup(int group, int i) {
    if (group < 0 || m_nodes[group] != GROUP_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_GROUP,
          new String[] { String.valueOf(group) });
    }
    return _getHeadSubstanceListOfGroup(group, i, m_nodes);
  }

  public static final int _getHeadSubstanceListOfGroup(int group, int i, int[] nodes) {
    final int n = nodes[group + EXISchemaLayout.GROUP_N_PARTICLES];
    if (i < 0 || i > n) {
      throw new EXISchemaRuntimeException(
        EXISchemaRuntimeException.INDEX_OUT_OF_BOUNDS,
        new String[] { String.valueOf(i), String.valueOf(0), String.valueOf(n - 1) });
    }
    return nodes[group + EXISchemaLayout.SZ_GROUP + 3 * i + 1];
  }

  ///////////////////////////////////////////////////////////////////////////
  // Wildcard methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the size of a wildcard.
   * @param wc wildcard node
   * @return size of the wildcard
   */
  static private int _getSizeOfWildcard(int wc, int[] nodes) {
    return EXISchemaLayout.SZ_WILDCARD + 2 * _getNamespaceCountOfWildcard(wc, nodes);
  }

  /**
   * Returns constraint type of a wildcard.
   * @param wc wildcard
   * @return one of WC_TYPE_ANY, WC_TYPE_NOT or WC_TYPE_NAMESPACES
   */
  public int getConstraintTypeOfWildcard(int wc)
    throws EXISchemaRuntimeException {
    if (wc < 0 || m_nodes[wc] != WILDCARD_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_WILDCARD,
          new String[] { String.valueOf(wc) });
    }
    return _getConstraintTypeOfWildcard(wc, m_nodes);
  }

  static int _getConstraintTypeOfWildcard(int wc, int[] nodes) {
    return nodes[wc + EXISchemaLayout.WILDCARD_CONSTRAINT_TYPE];
  }
  
  /**
   * Returns process contents property value of a wildcard.
   * @param wc wildcard
   * @return one of WC_PROCESS_SKIP, WC_PROCESS_LAX or WC_PROCESS_STRICT
   */
  public int getProcessContentsOfWildcard(int wc)
    throws EXISchemaRuntimeException {
    if (wc < 0 || m_nodes[wc] != WILDCARD_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_WILDCARD,
          new String[] { String.valueOf(wc) });
    }
    return m_nodes[wc + EXISchemaLayout.WILDCARD_PROCESS_CONTENTS];
  }

  /**
   * Returns the number of namespaces in the wildcard.
   * @param wc wildcard
   * @return number of namespaces in the wildcard
   */
  public int getNamespaceCountOfWildcard(int wc)
    throws EXISchemaRuntimeException {
    if (wc < 0 || m_nodes[wc] != WILDCARD_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_WILDCARD,
          new String[] { String.valueOf(wc) });
    }
    return _getNamespaceCountOfWildcard(wc, m_nodes);
  }

  static int _getNamespaceCountOfWildcard(int wc, int[] nodes) {
    return nodes[wc + EXISchemaLayout.WILDCARD_N_NAMESPACES];
  }

  /**
   * Returns the i-th namespace name in the wildcard.
   * @param wc wildcard
   * @param i position in the list of namespaces
   * @return the i-th namespace name
   */
  public final String getNamespaceNameOfWildcard(final int wc, final int i)
    throws EXISchemaRuntimeException {
    if (wc < 0 || m_nodes[wc] != WILDCARD_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_WILDCARD,
          new String[] { String.valueOf(wc) });
    }
    return _getNamespaceNameOfWildcard(wc, i, m_nodes, m_uris);
  }

  final static String _getNamespaceNameOfWildcard(final int wc, final int i, final int[] nodes, final String[] uris) {
    return uris[nodes[wc + EXISchemaLayout.SZ_WILDCARD + i]];
  }
  
  /**
   * Returns the i-th namespace in the wildcard.
   * @param wc wildcard
   * @param i position in the list of namespaces
   * @return the i-th namespace
   */
  public int getNamespaceOfWildcard(int wc, int i)
    throws EXISchemaRuntimeException {
    if (wc < 0 || m_nodes[wc] != WILDCARD_NODE) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_WILDCARD,
          new String[] { String.valueOf(wc) });
    }
    int n_namespaces = m_nodes[wc + EXISchemaLayout.WILDCARD_N_NAMESPACES];
    return m_nodes[wc + EXISchemaLayout.SZ_WILDCARD + n_namespaces + i];
  }

  /**
   * Returns true if an uri satisfies wildcard constraint.
   */
  final boolean matchWildcard(final int wc, final String uri) {
    return _matchWildcard(wc, uri, m_nodes, m_uris);
  }

  /**
   * Returns true if an uri satisfies wildcard constraint.
   */
  final static boolean _matchWildcard(final int wc, final String uri, final int[] nodes, final String[] uris) {
    switch (nodes[wc + EXISchemaLayout.WILDCARD_CONSTRAINT_TYPE]) {
      case EXISchema.WC_TYPE_ANY:
        return true;
      case EXISchema.WC_TYPE_NOT:
        int n_namespaces = nodes[wc + EXISchemaLayout.WILDCARD_N_NAMESPACES];
        for (int i = 0; i < n_namespaces; i++) {
          if (uri == uris[nodes[wc + EXISchemaLayout.SZ_WILDCARD + i]]) {
            return false;
          }
        }
        return true;
      case EXISchema.WC_TYPE_NAMESPACES:
        n_namespaces = nodes[wc + EXISchemaLayout.WILDCARD_N_NAMESPACES];
        for (int i = 0; i < n_namespaces; i++) {
          if (uri == uris[nodes[wc + EXISchemaLayout.SZ_WILDCARD + i]]) {
            return true;
          }
        }
        return false;
      default:
        assert false;
        return false;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // AttributeUse methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns name of the attribute used by an attribute use.
   * @param atuse attribute use node
   * @return attribute name
   */
  public String getNameOfAttrUse(int atuse) {
    // REVISIT: check node type
    return m_names[m_nodes[atuse + EXISchemaLayout.ATTR_USE_ATTR_NAME]];
  }

  static int _getNameOfAttrUse(int atuse, int[] nodes) {
    return nodes[atuse + EXISchemaLayout.ATTR_USE_ATTR_NAME];
  }

  /**
   * Returns target namespace name of the attribute used by an attribute use.
   * @param attr attribute use node
   * @return target namespace name of the attribute
   */
  public String getTargetNamespaceNameOfAttrUse(int atuse) {
    // REVISIT: check node type
    return m_uris[m_nodes[atuse + EXISchemaLayout.ATTR_USE_ATTR_TARGET_NAMESPACE]];
  }

  /**
   * Returns attribute declaration of an attribute use.
   * @param atuse attribute use node
   * @return attribute node
   */
  public int getAttrOfAttrUse(int atuse) {
    return _getAttrOfAttrUse(atuse, m_nodes);
  }

  static int _getAttrOfAttrUse(int atuse, int[] nodes) {
    return nodes[atuse + EXISchemaLayout.ATTR_USE_ATTRIBUTE];
  }

  /**
   * Returns constraint type of an attribute use.
   * @param atuse attribute use node
   * @return constraint type (i.e. CONSTRAINT_NONE, CONSTRAINT_DEFAULT or CONSTRAINT_FIXED)
   */
  public int getConstraintOfAttrUse(int atuse) {
    return m_nodes[atuse + EXISchemaLayout.ATTR_USE_CONSTRAINT];
  }

  /**
   * Returns constraint value of an attribute use if any.
   * @param atuse attribute use node
   * @return constraint value variant
   */
  public int getConstraintValueOfAttrUse(int atuse) {
    return _getConstraintValueOfAttrUse(atuse, m_nodes);
  }

  static int _getConstraintValueOfAttrUse(int atuse, int[] nodes) {
    return nodes[atuse + EXISchemaLayout.ATTR_USE_CONSTRAINT_VALUE];
  }

  /**
   * Determines if the attribute use requires the attribute to be present.
   * @param atuse attribute use node
   * @return true if required
   */
  public final boolean isRequiredAttrUse(int atuse) {
    return (m_nodes[atuse + EXISchemaLayout.ATTR_USE_BOOLEANS] & EXISchemaLayout.ATTR_USE_ISREQUIRED_MASK) != 0;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Attribute methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns name of an attribute.
   * @param attr attribute node
   * @return name of an attribute node.
   */
  public String getNameOfAttr(int attr)
    throws EXISchemaRuntimeException {
    if (attr < 0 || ATTRIBUTE_NODE != m_nodes[attr]) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_ATTRIBUTE,
          new String[] { String.valueOf(attr) });
    }
    return _getNameOfAttr(attr, m_nodes, m_names);
  }

  public static int _getNameOfAttr(int attr, int[] nodes) {
    return nodes[attr + EXISchemaLayout.INODE_NAME];
  }

  public static String _getNameOfAttr(int attr, int[] nodes, String[] names) {
    return names[nodes[attr + EXISchemaLayout.INODE_NAME]];
  }

  /**
   * Returns target namespace name of an attribute.
   * @param attr attribute node
   * @return target namespace name
   */
  public String getTargetNamespaceNameOfAttr(int attr)
    throws EXISchemaRuntimeException {
    if (attr < 0 || ATTRIBUTE_NODE != m_nodes[attr]) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_ATTRIBUTE,
          new String[] { String.valueOf(attr) });
    }
    return _getTargetNamespaceNameOfAttr(attr, m_nodes, m_uris);
  }

  public static String _getTargetNamespaceNameOfAttr(int attr, int[] nodes, String[] uris) {
    return uris[nodes[attr + EXISchemaLayout.INODE_TARGET_NAMESPACE]];
  }
  
  public static int _getTargetNamespaceNameOfAttr(int attr, int[] nodes) {
    return nodes[attr + EXISchemaLayout.INODE_TARGET_NAMESPACE];
  }

  /**
   * Returns the type of an attribute.
   * @param attr attribute node
   * @return type of the attribute
   */
  public int getTypeOfAttr(int attr)
    throws EXISchemaRuntimeException {
    if (attr < 0 || ATTRIBUTE_NODE != m_nodes[attr]) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_ATTRIBUTE,
          new String[] { String.valueOf(attr) });
    }
    return _getTypeOfAttr(attr, m_nodes);
  }

  public static int _getTypeOfAttr(int attr, int[] nodes) {
    return nodes[attr + EXISchemaLayout.INODE_TYPE];
  }

  /**
   * Returns constraint type of an attribute.
   * @param attr attribute node
   * @return constraint type (i.e. CONSTRAINT_NONE, CONSTRAINT_DEFAULT or CONSTRAINT_FIXED)
   * @throws EXISchemaRuntimeException If the node is not an attribute
   */
  public int getConstraintOfAttr(int attr)
    throws EXISchemaRuntimeException {
    if (attr < 0 || ATTRIBUTE_NODE != m_nodes[attr]) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_ATTRIBUTE,
          new String[] { String.valueOf(attr) });
    }
    return _getConstraintOfAttr(attr, m_nodes);
  }

  public static int _getConstraintOfAttr(int attr, int[] nodes) {
    return nodes[attr + EXISchemaLayout.ATTR_CONSTRAINT];
  }

  /**
   * Returns constraint value of an attribute if any.
   * @param attr attribute node
   * @return constraint value variant
   * @throws EXISchemaRuntimeException If the node is not an attribute
   */
  public int getConstraintValueOfAttr(int attr)
    throws EXISchemaRuntimeException {
    if (attr < 0 || ATTRIBUTE_NODE != m_nodes[attr]) {
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.NOT_ATTRIBUTE,
          new String[] { String.valueOf(attr) });
    }
    return _getConstraintValueOfAttr(attr, m_nodes);
  }

  public static int _getConstraintValueOfAttr(int attr, int[] nodes) {
    return nodes[attr + EXISchemaLayout.ATTR_CONSTRAINT_VALUE];
  }

  ///////////////////////////////////////////////////////////////////////////
  // Variant methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns the type of a variant.
   * @param variant a variant
   * @return one of VARIANT_* enumerated values
   */
  public int getTypeOfVariant(int variant) {
    return m_variantTypes[variant];
  }

  /**
   * Returns name value of a variant.
   * @param variant a variant of type VARIANT_NAME
   * @return Name value
   * @throws EXISchemaRuntimeException Thrown if variant type is not compatible
   */
  String getNameValueOfVariant(int variant)
    throws EXISchemaRuntimeException {
    if (variant < 0 || m_variantTypes[variant] != VARIANT_NAME)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.INCOMPATIBLE_VARIANT_TYPE,
          new String[] { "Name" } );
    return m_names[m_variants[variant]];
  }

  /**
   * Returns String value of a variant.
   * @param variant a variant of type VARIANT_STRING
   * @return String value
   * @throws EXISchemaRuntimeException Thrown if variant type is not compatible
   */
  public String getStringValueOfVariant(int variant)
    throws EXISchemaRuntimeException {
    if (variant < 0 || m_variantTypes[variant] != VARIANT_STRING)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.INCOMPATIBLE_VARIANT_TYPE,
          new String[] { "String" } );
    return m_strings[m_variants[variant]];
  }

  /**
   * Returns boolean value of a variant.
   * @param variant a variant of type VARIANT_BOOLEAN
   * @return boolean value
   * @throws EXISchemaRuntimeException Thrown if variant type is not compatible
   */
  public boolean getBooleanValueOfVariant(int variant)
    throws EXISchemaRuntimeException {
    if (variant < 0 || m_variantTypes[variant] != VARIANT_BOOLEAN)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.INCOMPATIBLE_VARIANT_TYPE,
          new String[] { "boolean" } );
    assert m_variants[variant] == 0 || m_variants[variant] ==1;
    return m_variants[variant] == 1;
  }

  /**
   * Returns float value of a variant.
   * @param variant a variant of type VARIANT_FLOAT
   * @return float value
   * @throws EXISchemaRuntimeException Thrown if variant type is not compatible
   */
  public float getFloatValueOfVariant(int variant)
      throws EXISchemaRuntimeException {
    if (variant < 0 || m_variantTypes[variant] != VARIANT_FLOAT)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.INCOMPATIBLE_VARIANT_TYPE,
          new String[] { "float" } );
    return m_floats[m_variants[variant]];
  }

  /**
   * Returns double value of a variant.
   * @param variant a variant of type VARIANT_DOUBLE
   * @return double value
   * @throws EXISchemaRuntimeException Thrown if variant type is not compatible
   */
  public double getDoubleValueOfVariant(int variant)
    throws EXISchemaRuntimeException {
    if (variant < 0 || m_variantTypes[variant] != VARIANT_DOUBLE)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.INCOMPATIBLE_VARIANT_TYPE,
          new String[] { "double" } );
    return m_doubles[m_variants[variant]];
  }

  /**
   * Returns decimal value of a variant.
   * @param variant a variant of type VARIANT_DECIMAL
   * @return decimal value as BigDecimal
   * @throws EXISchemaRuntimeException Thrown if variant type is not compatible
   */
  public BigDecimal getDecimalValueOfVariant(int variant)
    throws EXISchemaRuntimeException {
    BigDecimal ret = null;
    switch (getTypeOfVariant(variant)) {
      case VARIANT_DECIMAL:
        ret = m_decimals[m_variants[variant]];
        break;
      case VARIANT_INTEGER:
        ret = new BigDecimal(m_integers[m_variants[variant]]);
        break;
      case VARIANT_INT:
        ret = BigDecimal.valueOf(m_ints[m_variants[variant]]);
        break;
      case VARIANT_LONG:
        ret = BigDecimal.valueOf(m_longs[m_variants[variant]]);
        break;
      default:
        throw new EXISchemaRuntimeException(
            EXISchemaRuntimeException.INCOMPATIBLE_VARIANT_TYPE,
            new String[] { "BigDecimal" } );
    }
    return ret;
  }

  /**
   * Returns int value of a variant.
   * @param variant a variant of type VARIANT_INT
   * @return int value
   * @throws EXISchemaRuntimeException Thrown if variant type is not compatible
   */
  public int getIntValueOfVariant(int variant)
    throws EXISchemaRuntimeException {
    if (variant < 0 || m_variantTypes[variant] != VARIANT_INT)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.INCOMPATIBLE_VARIANT_TYPE,
          new String[] { "int" } );
    return m_ints[m_variants[variant]];
  }

  /**
   * Returns long value of a variant.
   * @param variant a variant of type VARIANT_LONG
   * @return long value
   * @throws EXISchemaRuntimeException Thrown if variant type is not compatible
   */
  public long getLongValueOfVariant(int variant)
    throws EXISchemaRuntimeException {
    if (variant < 0 || m_variantTypes[variant] != VARIANT_LONG)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.INCOMPATIBLE_VARIANT_TYPE,
          new String[] { "long" } );
    return m_longs[m_variants[variant]];
  }

  /**
   * Returns BigInteger value of a variant.
   * @param variant a variant of type VARIANT_INTEGER
   * @return BigInteger value
   * @throws EXISchemaRuntimeException Thrown if variant type is not compatible
   */
  public BigInteger getIntegerValueOfVariant(int variant)
    throws EXISchemaRuntimeException {
    if (variant < 0 || m_variantTypes[variant] != VARIANT_INTEGER)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.INCOMPATIBLE_VARIANT_TYPE,
          new String[] { "BigInteger" } );
    return m_integers[m_variants[variant]];
  }

  /**
   * Returns datetime value of a variant.
   * @param variant a variant of type VARIANT_DATETIME
   * @return datetime value as XSDateTime
   * @throws EXISchemaRuntimeException Thrown if variant type is not compatible
   */
  public XSDateTime getDateTimeValueOfVariant(int variant)
    throws EXISchemaRuntimeException {
    if (variant < 0 || m_variantTypes[variant] != VARIANT_DATETIME)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.INCOMPATIBLE_VARIANT_TYPE,
          new String[] { "XSDateTime" } );
    return m_datetimes[m_variants[variant]];
  }

  /**
   * Returns duration value of a variant.
   * @param variant a variant of type VARIANT_DURATION
   * @return duration value as XSDuration
   * @throws EXISchemaRuntimeException Thrown if variant type is not compatible
   */
  public Duration getDurationValueOfVariant(int variant)
    throws EXISchemaRuntimeException {
    if (variant < 0 || m_variantTypes[variant] != VARIANT_DURATION)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.INCOMPATIBLE_VARIANT_TYPE,
          new String[] { "Duration" } );
    return m_durations[m_variants[variant]];
  }

  /**
   * Returns binary value of a variant.
   * @param variant a variant of type VARIANT_BINARY
   * @return duration value as byte array
   * @throws EXISchemaRuntimeException Thrown if variant type is not compatible
   */
  public byte[] getBinaryValueOfVariant(int variant)
    throws EXISchemaRuntimeException {
    if (variant < 0 || m_variantTypes[variant] != VARIANT_BINARY)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.INCOMPATIBLE_VARIANT_TYPE,
          new String[] { "byte[]" } );
    return m_binaries[m_variants[variant]];
  }

  /**
   * Returns qname value of a variant.
   * @param variant a variant of type VARIANT_QNAME
   * @return qname value
   * @throws EXISchemaRuntimeException Thrown if variant type is not compatible
   */
  public int getQNameValueOfVariant(int variant) {
    if (variant < 0 || m_variantTypes[variant] != VARIANT_QNAME)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.INCOMPATIBLE_VARIANT_TYPE,
          new String[] { "QName" } );
    return m_variants[variant];
  }

  /**
   * Returns list value of a variant.
   * @param variant a variant of type VARIANT_LIST
   * @return list value as int array
   * @throws EXISchemaRuntimeException Thrown if variant type is not compatible
   */
  public int[] getListValueOfVariant(int variant)
    throws EXISchemaRuntimeException {
    if (variant < 0 || m_variantTypes[variant] != VARIANT_LIST)
      throw new EXISchemaRuntimeException(
          EXISchemaRuntimeException.INCOMPATIBLE_VARIANT_TYPE,
          new String[] { "list" } );
    return m_lists[m_variants[variant]];
  }

  /**
   * Returns the namespace name of a qname.
   * @param qname
   * @return 
   */
  public String getNamespaceNameOfQName(int qname) {
    return m_names[m_qnames[qname]];
  }
  
  public String getNameOfQName(int qname) {
    return m_names[m_qnames[qname + 1]];
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Serialization/Deserialization
  ///////////////////////////////////////////////////////////////////////////

  public void readExternal(ObjectInput in)
      throws IOException, ClassNotFoundException {
    int i, len;

    len = in.readInt();
    m_nodes = new int[len];
    for (i = 0; i < len; i++)
      m_nodes[i] = in.readInt();

    len = in.readInt();
    m_opaques = new int[len];
    for (i = 0; i < len; i++)
      m_opaques[i] = in.readInt();
    
    len = in.readInt();
    m_uris = new String[len + 4];
    m_uris[0] = "";
    m_uris[1] = "http://www.w3.org/XML/1998/namespace";
    m_uris[2] = "http://www.w3.org/2001/XMLSchema-instance";
    m_uris[3] = "http://www.w3.org/2001/XMLSchema";
    for (i = 4; i < len + 4; i++)
      m_uris[i] = in.readUTF().intern();

    len = in.readInt();
    m_names = new String[len + 1];
    m_names[0] = "";
    for (i = 1; i < len + 1; i++)
      m_names[i] = in.readUTF().intern();

    len = in.readInt();
    m_localNames = new int[len][];
    for (i = 0; i < len; i++) {
      m_localNames[i] = new int[in.readInt()];
      for (int j = 0; j < m_localNames[i].length; j++) {
        m_localNames[i][j] = in.readInt();
      }
    }
    
    len = in.readInt();
    m_strings = new String[len];
    for (i = 0; i < len; i++)
      m_strings[i] = in.readUTF();

    len = in.readInt();
    m_ints = new int[len];
    for (i = 0; i < len; i++)
      m_ints[i] = in.readInt();

    len = in.readInt();
    m_floats = new float[len];
    for (i = 0; i < len; i++)
      m_floats[i] = in.readFloat();

    len = in.readInt();
    m_doubles = new double[len];
    for (i = 0; i < len; i++)
      m_doubles[i] = in.readDouble();

    len = in.readInt();
    m_decimals = new BigDecimal[len];
    for (i = 0; i < len; i++)
      m_decimals[i] = (BigDecimal)in.readObject();

    len = in.readInt();
    m_integers = new BigInteger[len];
    for (i = 0; i < len; i++)
      m_integers[i] = (BigInteger)in.readObject();

    len = in.readInt();
    m_longs = new long[len];
    for (i = 0; i < len; i++)
      m_longs[i] = in.readLong();

    len = in.readInt();
    m_datetimes = new XSDateTime[len];
    for (i = 0; i < len; i++)
      m_datetimes[i] = (XSDateTime)in.readObject();

    len = in.readInt();
    m_durations = new Duration[len];
    for (i = 0; i < len; i++)
      m_durations[i] = (Duration)in.readObject();

    len = in.readInt();
    m_binaries = new byte[len][];
    for (i = 0; i < len; i++) {
      int n, n_bytes;
      m_binaries[i] = new byte[in.readInt()];
      for (n = 0; n < m_binaries[i].length; n += n_bytes) {
        if ((n_bytes = in.read(m_binaries[i], n, m_binaries[i].length - n)) < 0)
          break;
      }
      if (n < m_binaries[i].length)
        ;
    }

    len = in.readInt();
    m_qnames = new int[len];
    for (i = 0; i < len; i++)
      m_qnames[i] = in.readInt();

    m_lists = new int[in.readInt()][];
    for (i = 0; i < m_lists.length; i++) {
      m_lists[i] = new int[in.readInt()];
      for (int j = 0; j < m_lists[i].length; j++) {
        m_lists[i][j] = in.readInt();
      }
    }
    
    len = in.readInt();
    m_variantTypes = new int[len];
    for (i = 0; i < len; i++)
      m_variantTypes[i] = in.readInt();

    len = in.readInt();
    m_variants = new int[len];
    for (i = 0; i < len; i++)
      m_variants[i] = in.readInt();

    len = in.readInt();
    m_fragmentINodes = new int[len];
    for (i = 0; i < len; i++)
      m_fragmentINodes[i] = in.readInt();
    m_n_fragmentElems = in.readInt();
    
    m_n_elems = in.readInt();
    m_n_attrs = in.readInt();
    m_n_types = in.readInt();
    m_n_stypes = in.readInt();
    m_n_groups = in.readInt();
    
    computeGlobalDirectory();
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    int i, len;

    len = m_nodes.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeInt(m_nodes[i]);

    len = m_opaques.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeInt(m_opaques[i]);
    
    len = m_uris.length;
    assert len >= 4;
    out.writeInt(len - 4);
    for (i = 4; i < len; i++)
      out.writeUTF(m_uris[i]);
    
    len = m_names.length;
    assert len > 0;
    out.writeInt(len - 1);
    for (i = 1; i < len; i++)
      out.writeUTF(m_names[i]);

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
      out.writeUTF(m_strings[i]);

    len = m_ints.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeInt(m_ints[i]);

    len = m_floats.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeFloat(m_floats[i]);

    len = m_doubles.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeDouble(m_doubles[i]);

    len = m_decimals.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeObject(m_decimals[i]);

    len = m_integers.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeObject(m_integers[i]);

    len = m_longs.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeLong(m_longs[i]);

    len = m_datetimes.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeObject(m_datetimes[i]);

    len = m_durations.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeObject(m_durations[i]);

    len = m_binaries.length;
    out.writeInt(len);
    for (i = 0; i < len; i++) {
      out.writeInt(m_binaries[i].length);
      out.write(m_binaries[i]);
    }

    len = m_qnames.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeInt(m_qnames[i]);
    
    len = m_lists.length;
    out.writeInt(len);
    for (i = 0; i < len; i++) {
      final int[] list = m_lists[i];
      out.writeInt(list.length);
      for (int j = 0; j < list.length; j++)
        out.writeInt(list[j]);
    }

    len = m_variantTypes.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeInt(m_variantTypes[i]);

    len = m_variants.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeInt(m_variants[i]);

    len = m_fragmentINodes.length;
    out.writeInt(len);
    for (i = 0; i < len; i++)
      out.writeInt(m_fragmentINodes[i]);
    out.writeInt(m_n_fragmentElems);

    out.writeInt(m_n_elems);
    out.writeInt(m_n_attrs);
    out.writeInt(m_n_types);
    out.writeInt(m_n_stypes);
    out.writeInt(m_n_groups);
  }

}
