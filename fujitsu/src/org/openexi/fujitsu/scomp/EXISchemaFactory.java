package org.openexi.fujitsu.scomp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;

import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.helpers.LocatorImpl;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import org.apache.xerces.xs.*;
import org.apache.xerces.xs.datatypes.*;
import org.apache.xerces.impl.xs.XSLoaderImpl;
import org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import org.apache.xerces.impl.xs.util.LSInputListImpl;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaConst;
import org.openexi.fujitsu.schema.EXISchemaLayout;
import org.openexi.fujitsu.schema.IntBuffer;

import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.proc.grammars.GrammarCache;
import org.openexi.fujitsu.proc.grammars.GrammarRuntimeException;

import com.thaiopensource.datatype.xsd.regex.RegexSyntaxException;
import com.thaiopensource.datatype.xsd.regex.jdk1_4.Regexi;

/**
 * EXISchemaFactory turns XML Schemas into their processed form.
 */
public class EXISchemaFactory extends EXISchemaStruct {

  private final HashMap<Object,Integer> m_doneNodes; // schema node (Object) -> nodeID (Integer)
  private final HashMap<Integer,Object> m_doneNodesReverse; // nodeID (Integer) -> schema node (Object) 
  // XSElementDeclaration -> Set (of its substitutables)
  private final HashMap<XSElementDeclaration, Set<XSElementDeclaration>> m_mapSubst;
  // XSTypeDefinition -> Set (of *named* subtypes)
  private final HashMap<XSTypeDefinition, Set<XSTypeDefinition>> m_mapSubtypes;
  private final ArrayList<XSNamespaceItem> m_xscSchemas; // list of XSNamespaceItem

  // XSComplexType -> XSAttributeUse[]
  private final HashMap<XSComplexTypeDefinition, XSAttributeUse[]> m_sortedAttrUses;

  private static final String XMLSCHEMA_URI = "http://www.w3.org/2001/XMLSchema";

  private static final String[] BUILTIN_SIMPLE_TYPE_NAMES = {
    /* NEVER CHANGE ORDER! all the order as listed in EXISchemaConst. */
    // primitive types
    "anySimpleType",
    "string", "boolean", "decimal", "float", "double", "duration", "dateTime",
    "time",  "date", "gYearMonth", "gYear", "gMonthDay", "gDay", "gMonth",
    "hexBinary", "base64Binary", "anyURI", "QName", "NOTATION",
    // non-primitive atomic types - string-derived
    "normalizedString", "token", "language", "Name", "NCName", "NMTOKEN",
    "ENTITY", "IDREF", "ID",
    // non-primitive atomic types - decimal-derived
    "integer", "nonNegativeInteger", "unsignedLong", "positiveInteger",
    "nonPositiveInteger", "negativeInteger", "int", "short", "byte",
    "unsignedShort", "unsignedByte", "long", "unsignedInt",
    // list types
    "ENTITIES", "IDREFS", "NMTOKENS"
  };
  private final XSSimpleTypeDefinition[] m_xscBuiltinSTypes;
  private int m_anyType;
  private XSTypeDefinition m_xscAnyType;
  private XSTypeDefinition m_xscAnySimpleType;
  
  private final DOMErrorAdapter m_domErrorAdapter;

  private EXISchemaFactoryErrorHandler m_compiler_errorHandler;
  private boolean m_foundFatalSchemaError;

  private int m_n_namespaces; // number of namespaces incorporated in the schema.
  private String[] m_namespaces; // default namespace (null) comes first if it is used.
  
  private XSNamespaceItem m_xsdSchema;

  // REVISIT: may need to reset?
  private DOMImplementationLS m_domImplementationLS;
  private XSImplementation m_xsImplementation;
  
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
  
  private static final BigInteger INT_MIN_VALUE, INT_MAX_VALUE, INT_MAX_VALUE_MINUS_4095;
  private static final BigInteger LONG_MIN_VALUE, LONG_MAX_VALUE, LONG_MAX_VALUE_MINUS_4095;
  static {
    INT_MIN_VALUE = BigInteger.valueOf((long)Integer.MIN_VALUE);
    INT_MAX_VALUE_MINUS_4095 = BigInteger.valueOf((long)Integer.MAX_VALUE - 4095);
    INT_MAX_VALUE = BigInteger.valueOf((long)Integer.MAX_VALUE);
    LONG_MIN_VALUE = BigInteger.valueOf((long)Long.MIN_VALUE);
    LONG_MAX_VALUE_MINUS_4095 = BigInteger.valueOf((long)Long.MAX_VALUE - 4095);
    LONG_MAX_VALUE = BigInteger.valueOf((long)Long.MAX_VALUE);
  }

  /**
   * Construct a SchemaCompiler. causeResourceLinkageError is only for
   * unit test. Never set it to true.
   */
  public EXISchemaFactory()  {
    m_doneNodes    = new HashMap<Object,Integer>();
    m_doneNodesReverse = new HashMap<Integer,Object>();
    m_mapSubst     = new HashMap<XSElementDeclaration, Set<XSElementDeclaration>>();
    m_mapSubtypes  = new HashMap<XSTypeDefinition, Set<XSTypeDefinition>>();
    m_xscSchemas   = new ArrayList<XSNamespaceItem>();
    m_sortedAttrUses = new HashMap<XSComplexTypeDefinition, XSAttributeUse[]>();
    m_xscBuiltinSTypes = new XSSimpleTypeDefinition[BUILTIN_SIMPLE_TYPE_NAMES.length];
    m_domErrorAdapter = new DOMErrorAdapter();
    System.setProperty(DOMImplementationRegistry.PROPERTY,
        "org.apache.xerces.dom.DOMXSImplementationSourceImpl");
    DOMImplementationRegistry domImplRegistry;
    try {
      domImplRegistry = DOMImplementationRegistry.newInstance();
    }
    catch (ClassNotFoundException ce) {
      throw new EXISchemaFactoryLinkageError(
          "Class \"org.apache.xerces.dom.DOMXSImplementationSourceImpl\" not found.", ce);      
    }
    catch (InstantiationException ce) {
      throw new EXISchemaFactoryLinkageError(
          "Could not instantiate a DOMImplementationRegistry.", ce);      
    }
    catch (IllegalAccessException ce) {
      throw new EXISchemaFactoryLinkageError(
          "Illegal access detcted in instantiating a DOMImplementationRegistry.", ce);      
    }
    m_domImplementationLS = (DOMImplementationLS)domImplRegistry.getDOMImplementation("LS");
    m_xsImplementation = (XSImplementation)domImplRegistry.getDOMImplementation("XS-Loader");    
  }

  /**
   * Prepares the compiler for the next compilation run.
   */
  protected void reset() {
    // initialize the arrays
    super.reset();

    // other variables
    m_n_namespaces = 0;

    m_anyType = EXISchema.NIL_NODE;
    m_xscAnyType = null;
    m_xscAnySimpleType = null;

    m_xsdSchema = null;
  }

  /**
   * Releases resources that was allocated in the previous compilation.
   */
  protected void clear() {
    // delete the arrays
    super.clear();

    // other variables
    m_namespaces = null;

    // clear maps, sets, etc.
    m_doneNodes.clear();
    m_doneNodesReverse.clear();
    m_mapSubst.clear();
    m_mapSubtypes.clear();
    m_xscSchemas.clear();
    m_sortedAttrUses.clear();
  }

  /**
   * Set an error handler for use to report any errors encountered during
   * schema compilation.
   * @param errorHandler Error handler
   */
  public void setCompilerErrorHandler(EXISchemaFactoryErrorHandler errorHandler) {
    m_compiler_errorHandler = errorHandler;
  }
  
  final EXISchema compile() throws IOException, EXISchemaFactoryException {
    return compile(null);
  }

  /**
   * Compile a schema into an EXISchema.
   */
  public final EXISchema compile(InputSource is) 
    throws IOException, EXISchemaFactoryException {
    return compile(is, true);
  }

  /**
   * Compile a schema into an EXISchema with UPA Gotchas to be optionally checked.
   * @see <a href="http://www.w3.org/wiki/UniqueParticleAttribution#Gotchas">UPA Gotchas</a>
   */
  public final EXISchema compile(InputSource is, boolean checkUPAGotchas)
      throws IOException, EXISchemaFactoryException {

    final LSInput[] lsInputArray = new LSInput[2];
    int n_lsInputs = 0;
    LSInput lsInput;
    URL url = getClass().getResource("XMLSchema-instance.xsd");
    lsInput = m_domImplementationLS.createLSInput();
    lsInput.setByteStream(url.openStream());
    lsInputArray[n_lsInputs++] = lsInput;
    lsInput = m_domImplementationLS.createLSInput();
    if (is != null) {
      lsInput.setSystemId(is.getSystemId());
      lsInput.setPublicId(is.getPublicId());
      lsInput.setByteStream(is.getByteStream());
      lsInput.setCharacterStream(is.getCharacterStream());
      lsInput.setEncoding(is.getEncoding());
      lsInputArray[n_lsInputs++] = lsInput;
    }
    LSInputList lsInputList = new LSInputListImpl(lsInputArray, n_lsInputs);

    m_foundFatalSchemaError = false;
    XSLoaderImpl schemaLoader = (XSLoaderImpl)m_xsImplementation.createXSLoader(null);
    DOMConfiguration domConfig = schemaLoader.getConfig();
    domConfig.setParameter("error-handler", m_domErrorAdapter);
    schemaLoader.setParameter("http://apache.org/xml/features/honour-all-schemaLocations", Boolean.TRUE);
    schemaLoader.setParameter("http://apache.org/xml/features/validation/schema-full-checking", Boolean.TRUE);
    XSModel model;
    if ((model = schemaLoader.loadInputList(lsInputList)) == null || m_foundFatalSchemaError) {
      return null;
    }
    XSNamespaceItemList namespaceItemList = model.getNamespaceItems();

    final int len = namespaceItemList.getLength(); 
    XSNamespaceItem[] topLevelSchemas = new XSNamespaceItem[len];
    for (int i = 0; i < len; i++)
      topLevelSchemas[i] = (XSNamespaceItem)namespaceItemList.item(i);

    if (len > 0) {
      reset();

      // preprocess top-level schemas
      TreeSet<ComparableElementDeclaration> elemDecls  = new TreeSet<ComparableElementDeclaration>();
      LinkedHashSet<XSObject> attrDecls  = new LinkedHashSet<XSObject>();
      LinkedHashSet<XSObject> typeDecls  = new LinkedHashSet<XSObject>();
      preProcessSchema(topLevelSchemas, elemDecls, attrDecls, typeDecls);

      // compile Xerces schema into EXISchema
      int schema = doSchema(topLevelSchemas, elemDecls, attrDecls, typeDecls);
      if (schema != EXISchema.NIL_NODE) {
        postProcessSchema();
        EXISchema schemaCorpus;
        schemaCorpus = new EXISchema(m_nodes, m_n_nodes,
                                     m_names, m_n_names,
                                     m_strings, m_n_strings,
                                     m_ints, m_n_ints,
                                     m_floats, m_n_floats,
                                     m_doubles, m_n_doubles,
                                     m_decimals, m_n_decimals,
                                     m_integers, m_n_integers,
                                     m_longs, m_n_longs,
                                     m_datetimes, m_n_datetimes,
                                     m_durations, m_n_durations,
                                     m_binaries, m_n_binaries,
                                     m_qnames, m_n_qnames,
                                     m_lists, m_n_lists,
                                     m_variantTypes, m_variants, m_n_variants,
                                     m_fragmentINodes, m_n_fragmentElems,
                                     m_n_elems, m_n_attrs, m_n_types, m_n_stypes, m_n_groups);
        return checkUPAGotchas ? checkGrammars(schemaCorpus) : schemaCorpus;
      }
    }
    return null;
  }

  /**
   * Check UPA gotchas.
   * http://www.w3.org/wiki/UniqueParticleAttribution#Gotchas
   */
  private EXISchema checkGrammars(EXISchema schemaCorpus) 
    throws EXISchemaFactoryException {
    final int[] nodes = schemaCorpus.getNodes();
    GrammarCache grammarCache = new GrammarCache(schemaCorpus, true, GrammarOptions.STRICT_OPTIONS); 
    try {
      int pos, nodesLen;
      for (pos = EXISchema.THE_SCHEMA, nodesLen = nodes.length; pos < nodesLen;) {
        final int node = pos;
        pos += EXISchema._getNodeSize(node, nodes);
        int nodeType = schemaCorpus.getNodeType(node);
        if (nodeType == EXISchema.SIMPLE_TYPE_NODE || nodeType == EXISchema.COMPLEX_TYPE_NODE) {
          //String typeName = schemaCorpus.getNameOfType(node);
          //System.out.println("typename=" + (typeName != null ? typeName : ""));
          grammarCache.retrieveElementTagGrammar(node);
        }
      }
    }
    catch (GrammarRuntimeException ae) {
       EXISchemaFactoryException sce = new EXISchemaFactoryException(
          EXISchemaFactoryException.XMLSCHEMA_ERROR,
          new String[] { ae.getMessage() }, null);
       switch (ae.getCode()) {
         case GrammarRuntimeException.AMBIGUOUS_CONTEXT_OF_ELEMENT_PARTICLE:
         case GrammarRuntimeException.AMBIGUOUS_CONTEXT_OF_WILDCARD_PARTICLE:
           int particle = ae.getNode();
           assert m_nodes[particle] == EXISchema.PARTICLE_NODE;
           XSParticle xsParticle = (XSParticle)getDoneNode(particle);
           // REVISIT: setObject should not be public 
           ae.setObject(xsParticle);
           break;
       }
       sce.setException(ae);
       reportSchemaCompilerException(sce, true);
       return null;
    }
    nodes[0] = 1; // certify the corpus
    return schemaCorpus;
  }

  /////////////////////////////////////////////////////////////////////////
  // Compiler implementation
  /////////////////////////////////////////////////////////////////////////

  /**
   * Pre-process Schemas
   * @param elemDecls initially empty, will be populated
   * @param attrDecls initially empty, will be populated
   * @param typeDecls initially empty, will be populated
   */
  private void preProcessSchema(XSNamespaceItem[] xscSchemaList,
                                Set<ComparableElementDeclaration> elemDecls, Set<XSObject> attrDecls,
                                Set<XSObject> typeDecls)
      throws EXISchemaFactoryException {

    // intern built-in names
    internName(XMLSCHEMA_URI);
    
    boolean defaultNamespace = false; // becomes true if default namespace is used.
    Set<String> namespaces = new TreeSet<String>();   // set of namespace names

    int i, j, len;
    String namespaceName;

    for (i = 0; i < xscSchemaList.length; i++) {
      XSNamespaceItem xscSchema = xscSchemaList[i];
      if (!m_xscSchemas.contains(xscSchema))
        m_xscSchemas.add(xscSchema);
    }

    for (i = 0, len = m_xscSchemas.size(); i < len; i++) {
      XSNamespaceItem xscSchema = (XSNamespaceItem)m_xscSchemas.get(i);
      if (XMLSCHEMA_URI.equals(xscSchema.getSchemaNamespace())) {
        m_xsdSchema = xscSchema;
      }

      XSNamedMap xscElems = xscSchema.getComponents(XSConstants.ELEMENT_DECLARATION);
      XSNamedMap xscAttrs = xscSchema.getComponents(XSConstants.ATTRIBUTE_DECLARATION);
      XSNamedMap xscTypes = xscSchema.getComponents(XSConstants.TYPE_DEFINITION);

      int n_elems  = xscElems.getLength();
      int n_attrs  = xscAttrs.getLength();
      int n_types  = xscTypes.getLength();

      for (j = 0; j < n_elems; j++)
        elemDecls.add(new ComparableElementDeclaration((XSElementDeclaration)xscElems.item(j)));
      for (j = 0; j < n_attrs; j++)
        attrDecls.add(xscAttrs.item(j));
      for (j = 0; j < n_types; j++)
        typeDecls.add(xscTypes.item(j));
    }

    Iterator<ComparableElementDeclaration> iterElements;
    for (iterElements = elemDecls.iterator(); iterElements.hasNext();) {
      XSElementDeclaration elem = ((ComparableElementDeclaration)iterElements.next()).elementDeclaration;
      namespaceName = roundify(elem.getNamespace());
      if (namespaceName == "")
        defaultNamespace = true;
      else {
        namespaceName = namespaceName.intern();
        namespaces.add(namespaceName);
      }
      // Add the element to the substitution group if it belongs to any.
      // Note that substitutability is transitional.
      XSElementDeclaration subst;
      for (subst = elem.getSubstitutionGroupAffiliation();
           subst != null && subst != elem;
           subst = subst.getSubstitutionGroupAffiliation()) {
        Set<XSElementDeclaration> elems;
        if ((elems = m_mapSubst.get(subst)) == null)
          m_mapSubst.put(subst, elems = new HashSet<XSElementDeclaration>());
        elems.add(elem);
      }
    }

    Iterator<XSObject> iterObjects;

    for (iterObjects = attrDecls.iterator(); iterObjects.hasNext();) {
      XSAttributeDeclaration attr = (XSAttributeDeclaration)iterObjects.next();
      namespaceName = roundify(attr.getNamespace());
      if (namespaceName == "")
        defaultNamespace = true;
      else {
        namespaceName = namespaceName.intern();
        namespaces.add(namespaceName);
      }
    }

    for (iterObjects = typeDecls.iterator(); iterObjects.hasNext();) {
      XSTypeDefinition dt = (XSTypeDefinition)iterObjects.next();
      namespaceName = roundify(dt.getNamespace());
      if (namespaceName == "")
        defaultNamespace = true;
      else {
        namespaceName = namespaceName.intern();
        namespaces.add(namespaceName);
      }
      if (dt.getName() != null) {
        XSTypeDefinition base = getBaseType(dt);
        if (base != null && base != dt) {
          Set<XSTypeDefinition> subTypes;
          if ((subTypes = m_mapSubtypes.get(base)) == null) {
            m_mapSubtypes.put(base, subTypes = new HashSet<XSTypeDefinition>());
          }
          subTypes.add(dt);
        }
      }
    }

    for (i = 0, len = m_xscSchemas.size(); i < len; i++) {
      XSNamespaceItem ithSchema = (XSNamespaceItem)m_xscSchemas.get(i);
      namespaceName = roundify(ithSchema.getSchemaNamespace());
      if (namespaceName == "")
        defaultNamespace = true;
      else {
        namespaceName = namespaceName.intern();
        namespaces.add(namespaceName);
      }
    }

    m_n_namespaces = namespaces.size();
    if (defaultNamespace)
      ++m_n_namespaces;

    m_namespaces = new String[m_n_namespaces];
    int base = defaultNamespace ? 2 : 1; // zero is reserved for XMLSCHEMA_URI
    if (defaultNamespace)
      m_namespaces[1] = "";
    Iterator<String> iterStrings = namespaces.iterator();
    while (iterStrings.hasNext()) {
      String namespace = (String)iterStrings.next();
      if (!XMLSCHEMA_URI.equals(namespace))
        m_namespaces[base++] = namespace;
    }
    m_namespaces[0] = XMLSCHEMA_URI;
  }

  private int doSchema(XSNamespaceItem[] topLevelSchemas, 
      Set<ComparableElementDeclaration> elemDecls, 
      Set<XSObject> attrDecls, Set<XSObject> typeDecls)
      throws EXISchemaFactoryException {

    assert topLevelSchemas.length >= 1;
    final XSNamespaceItem xscSchema = topLevelSchemas[0];
    int i;
    
    final int nd;
    if ((nd = internNode(xscSchema)) != m_n_nodes)
      return nd;

    final int n_elems      = elemDecls.size();
    final int n_attrs      = attrDecls.size();

    int sz = EXISchemaLayout.SZ_SCHEMA + n_elems + n_attrs + EXISchemaConst.N_BUILTIN_TYPES + m_n_namespaces;
    ensureNodes(sz);
    m_n_nodes += sz;

    m_nodes[nd]                                       = EXISchema.SCHEMA_NODE;
    m_nodes[nd + EXISchemaLayout.SCHEMA_N_ELEMS]      = n_elems;
    m_nodes[nd + EXISchemaLayout.SCHEMA_N_ATTRS]      = n_attrs;
    m_nodes[nd + EXISchemaLayout.SCHEMA_N_NAMESPACES] = m_n_namespaces;

    Iterator<ComparableElementDeclaration> iterElements;

    int base;
    base = nd + EXISchemaLayout.SZ_SCHEMA;
    for (i = 0, iterElements = elemDecls.iterator();
         iterElements.hasNext(); i++) { // pre-allocate space for global elements
      int elem = allocElement(((ComparableElementDeclaration)iterElements.next()).elementDeclaration, true);
      m_nodes[base + i] = elem;
    }
    for (i = 0, iterElements = elemDecls.iterator();
         iterElements.hasNext(); i++) { // bootstrap global elements
      bootElement(((ComparableElementDeclaration)iterElements.next()).elementDeclaration, true);
    }

    Iterator<XSObject> iter;
    
    base += n_elems;
    for (i = 0, iter = attrDecls.iterator();
         iter.hasNext(); i++) { // pre-allocate space for attributes
      int attr = allocAttribute((XSAttributeDeclaration)iter.next(), true);
      m_nodes[base + i] = attr;
    }

    base += n_attrs;
    
    m_xscAnyType = m_xsdSchema.getTypeDefinition("anyType");
    sz = computeTypeSize(m_xscAnyType);
    m_anyType = internNode(m_xscAnyType);
    ensureNodes(sz);
    m_n_nodes += sz;
    m_nodes[m_anyType] = EXISchema.COMPLEX_TYPE_NODE | EXISchema.INIT_MASK;
    assert m_n_types == 0; // xs:anyType has serial number "0"
    m_n_types++;
    m_nodes[m_anyType + EXISchemaLayout.TYPE_NUMBER] = 0;
    m_nodes[m_anyType + EXISchemaLayout.TYPE_BOOLEANS] |= EXISchemaLayout.TYPE_ISFIXTURE_MASK;
    m_nodes[base] = m_anyType;

    base++;
    // pre-allocate space for builtin simple types
    for (i = 0; i < BUILTIN_SIMPLE_TYPE_NAMES.length; i++)  {
      m_xscBuiltinSTypes[i] = (XSSimpleTypeDefinition)m_xsdSchema.getTypeDefinition(BUILTIN_SIMPLE_TYPE_NAMES[i]);
      sz = computeTypeSize(m_xscBuiltinSTypes[i]);
      int type = internNode(m_xscBuiltinSTypes[i]);
      ensureNodes(sz);
      m_n_nodes += sz;
      m_nodes[type] = EXISchema.SIMPLE_TYPE_NODE  | EXISchema.INIT_MASK;
      m_n_types++;
      m_n_stypes++;
      m_nodes[type + EXISchemaLayout.TYPE_NUMBER] = EXISchema.NIL_VALUE;
      m_nodes[type + EXISchemaLayout.TYPE_BOOLEANS] |= EXISchemaLayout.TYPE_ISFIXTURE_MASK;
      m_nodes[base + i] = type;
    }
    m_xscAnySimpleType = m_xscBuiltinSTypes[0];
    assert "anySimpleType".equals(m_xscAnySimpleType.getName());

    base += BUILTIN_SIMPLE_TYPE_NAMES.length;
    
    for (i = 0, iter = typeDecls.iterator();
         iter.hasNext(); i++) { // pre-allocate space for types
      XSTypeDefinition xscType = (XSTypeDefinition)iter.next();
      sz = computeTypeSize(xscType);
      int type = internNode(xscType);
      if (m_n_nodes == type) {
        ensureNodes(sz);
        m_n_nodes += sz;
        if (xscType.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
          m_nodes[type] = EXISchema.COMPLEX_TYPE_NODE | EXISchema.INIT_MASK;
        }
        else {
          m_nodes[type] = EXISchema.SIMPLE_TYPE_NODE  | EXISchema.INIT_MASK;
          m_n_stypes++;
        }
        m_n_types++;
        m_nodes[type + EXISchemaLayout.TYPE_NUMBER] = EXISchema.NIL_VALUE;
      }
    }

    for (i = 0; i < m_n_namespaces; i++)  { // do namespaces
      int namespace = doNamespace(m_namespaces[i], n_elems, elemDecls,
                      n_attrs, attrDecls, typeDecls);
      m_nodes[base + i] = namespace;
    }

//    doType(m_xscAnyType, new Stack());
//
//    for (i = 0; i < m_xscBuiltinSTypes.length; i++) { // do builtin simple types
//      doType(m_xscBuiltinSTypes[i], new Stack());
//    }

    for (iter = typeDecls.iterator(); iter.hasNext();)
      doType((XSTypeDefinition)iter.next(), new Stack<XSObject>());
    
    for (iter = attrDecls.iterator(); iter.hasNext();)
      doAttribute((XSAttributeDeclaration)iter.next(), false);

    for (iterElements = elemDecls.iterator(); iterElements.hasNext();)
      doElement(((ComparableElementDeclaration)iterElements.next()).elementDeclaration, new Stack<XSObject>());

    return nd;
  }

  private void postProcessSchema() {
    final TreeSet<ComparableINode> elemDecls  = new TreeSet<ComparableINode>();
    final TreeSet<ComparableINode> attrDecls  = new TreeSet<ComparableINode>();
    int n_simpleTypes = 0;
    int n_complexTypes = 1; // account for "anyType"
    int prevSimpleType = EXISchema.NIL_NODE;
    for (int pos = EXISchema.THE_SCHEMA; pos < m_n_nodes; pos += EXISchema._getNodeSize(pos, m_nodes)) {
      switch (m_nodes[pos]) {
        case EXISchema.ELEMENT_NODE:
          final ComparableINode elemDecl2 = new ComparableINode(pos, m_nodes, m_names);
          if (elemDecls.contains(elemDecl2)) {
            ComparableINode elemDecl1 = elemDecls.tailSet(elemDecl2).first();
            assert elemDecl1.compareTo(elemDecl2) == 0 && elemDecl1 != elemDecl2;
            final int typeOfElem1 = EXISchema._getTypeOfElem(elemDecl1.nd, m_nodes); 
            final int typeOfElem2 = EXISchema._getTypeOfElem(elemDecl2.nd, m_nodes);
            if (typeOfElem1 != typeOfElem2) {
              elemDecl1.isSpecific = false;
            }
            else {
              final boolean isNillable1 = EXISchema._isNillableElement(elemDecl1.nd, m_nodes);
              final boolean isNillable2 = EXISchema._isNillableElement(elemDecl2.nd, m_nodes);
              if (isNillable1 != isNillable2)
                elemDecl1.isSpecific = false;
            }
            elemDecl1.addIsotope(elemDecl2);
          }
          else
            elemDecls.add(elemDecl2);
          break;
        case EXISchema.ATTRIBUTE_NODE:
          final ComparableINode attrDecl2 = new ComparableINode(pos, m_nodes, m_names);
            if (!"http://www.w3.org/2001/XMLSchema-instance".equals(attrDecl2.m_uri)) {
            if (attrDecls.contains(attrDecl2)) {
              ComparableINode attrDecl1 = attrDecls.tailSet(attrDecl2).first();
              assert attrDecl1.compareTo(attrDecl2) == 0 && attrDecl1 != attrDecl2;
              final int typeOfAttr1 = EXISchema._getTypeOfAttr(attrDecl1.nd, m_nodes); 
              final int typeOfAttr2 = EXISchema._getTypeOfAttr(attrDecl2.nd, m_nodes);
              if (typeOfAttr1 != typeOfAttr2) {
                attrDecl1.isSpecific = false;
              }
              attrDecl1.addIsotope(attrDecl2);
            }
            else
              attrDecls.add(attrDecl2);
          }
          break;
        case EXISchema.SIMPLE_TYPE_NODE:
          if (prevSimpleType != EXISchema.NIL_NODE) {
            m_nodes[prevSimpleType + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_NEXT_SIMPLE_TYPE] = pos; 
          }
          m_nodes[pos + EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_NEXT_SIMPLE_TYPE] = EXISchema.NIL_NODE;
          m_nodes[pos + EXISchemaLayout.TYPE_NUMBER] = 1 + n_simpleTypes++;
          prevSimpleType = pos;
          break;
        case EXISchema.COMPLEX_TYPE_NODE:
          if (m_nodes[pos + EXISchemaLayout.TYPE_NUMBER] == EXISchema.NIL_VALUE) // i.e. except for "anyType"
            m_nodes[pos + EXISchemaLayout.TYPE_NUMBER] = m_n_stypes + n_complexTypes++;
          break;
        default:
          break;
      }
    }
    assert m_n_stypes == n_simpleTypes && m_n_types == n_simpleTypes + n_complexTypes;
    
    int i;
    Iterator<ComparableINode> iter;
    m_n_fragmentElems = elemDecls.size();
    m_n_fragmentAttrs = attrDecls.size();
    m_fragmentINodes = new int[m_n_fragmentElems + m_n_fragmentAttrs];
    iter = elemDecls.iterator();
    for (i = 0; iter.hasNext(); i++) {
      final ComparableINode element = iter.next(); 
      m_fragmentINodes[i] = element.nd;
      final ArrayList<ComparableINode> isotopes = element.getIsotopes();
      final int len = isotopes.size();
      int j, jth;
      jth = element.nd;
      j = -1;
      do {
        assert m_nodes[jth] == EXISchema.ELEMENT_NODE;
        int flags = m_nodes[jth + EXISchemaLayout.INODE_BOOLEANS];
        flags = element.isSpecific ? flags | EXISchemaLayout.INODE_ISSPECIFIC_IN_FRAGMENT_MASK :
          flags & ~EXISchemaLayout.INODE_ISSPECIFIC_IN_FRAGMENT_MASK;
        m_nodes[jth + EXISchemaLayout.INODE_BOOLEANS] = flags;
        if (++j < len) {
          jth = isotopes.get(j).nd;
          continue;
        }
        break;
      } 
      while (true);
    }
    assert i == m_n_fragmentElems;
    iter = attrDecls.iterator();
    for (; iter.hasNext(); i++) {
      final ComparableINode attribute = iter.next(); 
      m_fragmentINodes[i] = attribute.nd;
      final ArrayList<ComparableINode> isotopes = attribute.getIsotopes();
      final int len = isotopes.size();
      int j, jth;
      jth = attribute.nd;
      j = -1;
      do {
        assert m_nodes[jth] == EXISchema.ATTRIBUTE_NODE;
        int flags = m_nodes[jth + EXISchemaLayout.INODE_BOOLEANS];
        flags = attribute.isSpecific ? flags | EXISchemaLayout.INODE_ISSPECIFIC_IN_FRAGMENT_MASK :
          flags & ~EXISchemaLayout.INODE_ISSPECIFIC_IN_FRAGMENT_MASK;
        m_nodes[jth + EXISchemaLayout.INODE_BOOLEANS] = flags;
        if (++j < len) {
          jth = isotopes.get(j).nd;
          continue;
        }
        break;
      } 
      while (true);
    }
  }

  /**
   * Determines if the node has been fully processed by checking init flag.
   */
  private boolean isProcessed(int nd) {
    int nodeType = m_nodes[nd];
    return (nodeType & EXISchema.INIT_MASK) != EXISchema.INIT_MASK;
  }

  /**
   * Allocate an element space.
   */
  private int allocElement(XSElementDeclaration xscElem, boolean doIntern) {

    int elem;
    if (doIntern && (elem = getDoneNodeId(xscElem)) != EXISchema.NIL_NODE) {
      return elem; // The element has already been allocated.
    }

    // No need to remember the element if doIntern is false.
    elem = doIntern ? internNode(xscElem) : m_n_nodes;
    
    final boolean isGlobal = isGlobalElement(xscElem);
    int elem_sz = EXISchemaLayout.SZ_ELEM;
    ensureNodes(elem_sz);
    m_n_nodes += elem_sz;
    m_nodes[elem] = EXISchema.ELEMENT_NODE | EXISchema.INIT_MASK;
    int val = m_nodes[elem + EXISchemaLayout.INODE_BOOLEANS]; // better than 'val = 0;'
    if (isGlobal)
      val |= EXISchemaLayout.INODE_ISGLOBAL_MASK;
    m_nodes[elem + EXISchemaLayout.INODE_BOOLEANS] = val;

    return elem;
  }

  /**
   * Bootstrap an element.
   */
  private int bootElement(final XSElementDeclaration xscElem, final boolean doIntern) {

    // allocate space if it has not been done so.
    final int elem = allocElement(xscElem, doIntern);
    
    m_nodes[elem + EXISchemaLayout.INODE_NAME] = internName(xscElem.getName());
    m_nodes[elem + EXISchemaLayout.INODE_TARGET_NAMESPACE] = internName(roundify(xscElem.getNamespace()));
    m_nodes[elem + EXISchemaLayout.ELEM_SUBST] = getDoneNodeId(xscElem.getSubstitutionGroupAffiliation());
    m_nodes[elem + EXISchemaLayout.ELEM_NUMBER] = m_n_elems++;

    // allocate space for substututables if any
    final int n_subst, substitutables;
    final Set<XSElementDeclaration> setOfSubstitutables = m_mapSubst.get(xscElem);
    if ((n_subst = setOfSubstitutables != null ? setOfSubstitutables.size() : 0) > 0) {
      substitutables = allocOpaque(n_subst);
      Iterator<XSElementDeclaration> iter = (m_mapSubst.get(xscElem)).iterator();
      for (int i = 0; iter.hasNext(); i++) {
        final int ith = getDoneNodeId(iter.next());
        assert ith != EXISchema.NIL_NODE;
        m_nodes[substitutables + i] = ith;
      }
    }
    else {
      substitutables = EXISchema.NIL_NODE;
    }
    m_nodes[elem + EXISchemaLayout.ELEM_N_SUBSTITUTABLES] = n_subst;
    m_nodes[elem + EXISchemaLayout.ELEM_SUBSTITUTABLES] = substitutables;

    return elem;
  }

  private int _doElement(int nd, XSElementDeclaration xscElem, XSTypeDefinition xscType, 
      Stack<XSObject> nodePath)
      throws EXISchemaFactoryException {
    
    final int type, typeClass;
    m_nodes[nd] = EXISchema.ELEMENT_NODE;
    type = doType(xscType, nodePath);
    typeClass = m_nodes[type];
    m_nodes[nd + EXISchemaLayout.INODE_TYPE] = type;
    m_nodes[nd + EXISchemaLayout.ELEM_CONSTRAINT] = getValueConstraintType(xscElem);
    final XSValue constraintValue = xscElem.getValueConstraintValue();
    m_nodes[nd + EXISchemaLayout.ELEM_CONSTRAINT_VALUE] = constraintValue == null ? 
        EXISchema.NIL_VALUE : doVariantValue(constraintValue.getActualValue(), 
            constraintValue.getActualValueType(), constraintValue.getListValueTypes());

    int val = m_nodes[nd + EXISchemaLayout.INODE_BOOLEANS]; // never do 'val = 0;'
    if (xscElem.getNillable())
      val |= EXISchemaLayout.ELEMENT_ISNILLABLE_MASK;
    if (xscElem.getAbstract())
      val |= EXISchemaLayout.ELEMENT_ISABSTRACT_MASK;
    if (xscType.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE)
      val |= EXISchemaLayout.ELEMENT_ISSIMPLETYPE_MASK;
    if (EXISchema._isUrType(type, m_nodes))
      val |= EXISchemaLayout.ELEMENT_ISURTYPE_MASK;

    m_nodes[nd + EXISchemaLayout.INODE_BOOLEANS] = val;

    final int contentClass = EXISchema.SIMPLE_TYPE_NODE == typeClass ?
                         EXISchema.CONTENT_SIMPLE :
                         EXISchema._getContentClassOfComplexType(type, m_nodes);
    /** Top 3 bits in EXISchemaLayout.INODE_BOOLEANS represents element content class. */
    val = m_nodes[nd + EXISchemaLayout.INODE_BOOLEANS] |
        contentClass << EXISchemaLayout.ELEMENT_CONTENT_CLASS_OFFSET;
    m_nodes[nd + EXISchemaLayout.INODE_BOOLEANS] = val;

    // default builtinId for complex-content element (used by mixed content)
    int simpleType = EXISchema.NIL_NODE;
    if (EXISchema.CONTENT_SIMPLE == contentClass) {
      simpleType = EXISchema.SIMPLE_TYPE_NODE == typeClass ? type :
          EXISchema._getContentTypeOfComplexType(type, m_nodes);
    }
    m_nodes[nd + EXISchemaLayout.ELEM_SIMPLE_TYPE] = simpleType;

    int n_attrs = 0;
    int attrs   = EXISchema.NIL_NODE;
    int attr_wc = EXISchema.NIL_NODE;
    if (EXISchema.COMPLEX_TYPE_NODE == typeClass) {
      n_attrs = EXISchema._getAttrUseCountOfComplexType(type, m_nodes);
      attrs   = EXISchema._getAttrUseBaseOfComplexType(type, m_nodes);
      attr_wc = EXISchema._getAttrWildcardOfComplexType(type, m_nodes);
    }
    m_nodes[nd + EXISchemaLayout.ELEM_N_ATTRIBUTE_USES] = n_attrs;
    m_nodes[nd + EXISchemaLayout.ELEM_ATTRIBUTE_USES]   = attrs;
    m_nodes[nd + EXISchemaLayout.ELEM_ATTRIBUTE_WC]     = attr_wc;

    int group, particle, minOccurs, maxOccurs;
    group = particle = EXISchema.NIL_NODE;
    minOccurs = maxOccurs = 0;
    if (contentClass == EXISchema.CONTENT_ELEMENT_ONLY ||
        contentClass == EXISchema.CONTENT_MIXED) {
      particle = EXISchema._getContentTypeOfComplexType(type, m_nodes);
      if (particle != EXISchema.NIL_NODE) {
        group = EXISchema._getTermOfParticle(particle, m_nodes);
        minOccurs = EXISchema._getMinOccursOfParticle(particle, m_nodes);
        maxOccurs = EXISchema._getMaxOccursOfParticle(particle, m_nodes);
      }
    }
    m_nodes[nd + EXISchemaLayout.ELEM_GROUP]           = group;
    m_nodes[nd + EXISchemaLayout.ELEM_GROUP_MINOCCURS] = minOccurs;
    m_nodes[nd + EXISchemaLayout.ELEM_GROUP_MAXOCCURS] = maxOccurs;

    int n_initials = 0;
    int initials = EXISchema.NIL_NODE;
    if (particle != EXISchema.NIL_NODE) {
      n_initials = EXISchema._getHeadSubstanceCountOfParticle(particle, m_nodes);
      if (n_initials > 0)
        initials = EXISchema._getHeadSubstanceListOfParticle(particle, m_nodes);
    }
    m_nodes[nd + EXISchemaLayout.ELEM_GROUP_N_INITIALS] = n_initials;
    m_nodes[nd + EXISchemaLayout.ELEM_GROUP_INITIALS]   = initials;

    return nd;
  }

  private int doElement(XSElementDeclaration xscElem, Stack<XSObject> nodePath)
    throws EXISchemaFactoryException {
  
    boolean allocated = false;
    int nd;
    if ((nd = internNode(xscElem)) != m_n_nodes) {
      if (isProcessed(nd))
        return nd;
      else
        allocated = true;
    }
  
    if (!allocated) {
      nd = bootElement(xscElem, false);
    }
  
    XSTypeDefinition xscType = xscElem.getTypeDefinition();
    if (xscType == null) {
      // xscElem.getTypeDefinition() sometimes return null. Work around for now.
      // See SchemaCompilerTest.testAnyTypeElemSubstUnresolvable
      xscType = m_xscAnyType;
    }
    if (nodePath.contains(xscType))
      return nd;
  
    nodePath.push(xscElem);
    try {
      return _doElement(nd, xscElem, xscType, nodePath);
    }
    finally {
      nodePath.pop();
    }
  }

  /**
   */
  private boolean isListableSimpleType(int stype, int[] nodes) { 
    assert m_nodes[stype] == EXISchema.SIMPLE_TYPE_NODE;
    int variety = EXISchema._getVarietyOfSimpleType(stype, nodes); 
    switch (variety) {
      case EXISchema.LIST_SIMPLE_TYPE:
        return true;
      case EXISchema.UNION_SIMPLE_TYPE:
        int n_memberTypes = EXISchema._getMemberTypesCountOfSimpleType(stype, nodes);
        for (int i = 0; i < n_memberTypes; i++) {
          if (isListableSimpleType(EXISchema._getMemberTypeOfSimpleType(stype, i, nodes),  nodes))
            return true;
        }
        return false;
      case EXISchema.UR_SIMPLE_TYPE:        
      case EXISchema.ATOMIC_SIMPLE_TYPE:
        return false;
    }
    assert false;
    return false;
  }
  
  /**
   * Process a type.
   */
  private int doType(XSTypeDefinition xscType, Stack<XSObject> nodePath)
    throws EXISchemaFactoryException {
    return doType(xscType, (String[])null, nodePath);
  }
  
  /**
   * Process a type.
   * @param xscType a type
   * @param name a pair of local name and namespace name
   */
  private int doType(XSTypeDefinition xscType, String[] name, Stack<XSObject> nodePath) 
    throws EXISchemaFactoryException {
    boolean allocated = false;
    final Object key = name != null ? (Object)name : xscType;
    int nd;
    if ((nd = internNode(key)) != m_n_nodes) {
      if (isProcessed(nd))
        return nd;
      else
        allocated = true;
    }
    
    final short typeCategory = xscType.getTypeCategory(); 
    
    if (!allocated) {
      int type_sz = computeTypeSize(xscType);
      ensureNodes(type_sz);
      m_n_nodes += type_sz;
      m_n_types++;
      if (typeCategory == XSTypeDefinition.SIMPLE_TYPE)
        m_n_stypes++;
      m_nodes[nd + EXISchemaLayout.TYPE_NUMBER] = EXISchema.NIL_VALUE;
    }

    int baseType = EXISchema.NIL_NODE;
    XSTypeDefinition xscBaseType = null;

    int nm, nsnm;
    nm = internName(name != null ? name[0] : roundify(xscType.getName()));
    m_nodes[nd + EXISchemaLayout.TYPE_NAME] = nm;
    nsnm = internName(name != null ? name[1] : roundify(xscType.getNamespace())); 
    m_nodes[nd + EXISchemaLayout.TYPE_TARGET_NAMESPACE] = nsnm;
    if ((xscBaseType = getBaseType(xscType)) != null) {
      if ((baseType = getDoneNodeId(xscBaseType)) == EXISchema.NIL_NODE ||
          baseType != nd && !isProcessed(baseType)) {
        baseType = doType(xscBaseType, nodePath);
      }
    }
    m_nodes[nd + EXISchemaLayout.TYPE_BASE_TYPE] = baseType;

    int bvals = m_nodes[nd + EXISchemaLayout.TYPE_BOOLEANS];
    if (typeCategory == XSTypeDefinition.COMPLEX_TYPE) {
      _doComplexType((XSComplexTypeDefinition)xscType, nd, nodePath);

      /**
       * It is a common trait shared by XSCUnknownComplexTypeDefinition and
       * XSCAnyType that it is its own base type. The assumption is that no
       * other complex types have this trait.
       */
      if (baseType == nd)
        bvals |= EXISchemaLayout.TYPE_ISURTYPE_MASK;
    }
    else {
      _doSimpleType((XSSimpleTypeDecl)xscType, nd, nodePath);

      if (EXISchema._getVarietyOfSimpleType(nd, m_nodes) ==
          EXISchema.UR_SIMPLE_TYPE) {
        bvals |= EXISchemaLayout.TYPE_ISURTYPE_MASK;
      }
    }
    Set<XSTypeDefinition> subTypes;
    if ((subTypes = m_mapSubtypes.get(xscType)) != null && subTypes.size() != 0)
      bvals |= EXISchemaLayout.TYPE_HASSUBTYPE_MASK;
    m_nodes[nd + EXISchemaLayout.TYPE_BOOLEANS] = bvals;

    return nd;
  }

  private int allocAttribute(XSAttributeDeclaration xscAttr, boolean doIntern) {
    
    // No need to remember the attribute if doIntern is false.
    final int attr = doIntern ? internNode(xscAttr) : m_n_nodes;

    ensureNodes(EXISchemaLayout.SZ_ATTR);
    m_n_nodes += EXISchemaLayout.SZ_ATTR;
    m_nodes[attr] = EXISchema.ATTRIBUTE_NODE | EXISchema.INIT_MASK;
    
    m_n_attrs++;
    
    return attr;
  }

  private int doAttribute(XSAttributeDeclaration xscAttr, boolean requiredByAttrUse)
      throws EXISchemaFactoryException {
    boolean allocated = false;
    int nd;
    if ((nd = internNode(xscAttr)) != m_n_nodes) {
      if (isProcessed(nd))
        return nd;
      else
        allocated = true;
    }

    if (!allocated) {
      nd = allocAttribute(xscAttr, false);
    }

    m_nodes[nd] = EXISchema.ATTRIBUTE_NODE;
    m_nodes[nd + EXISchemaLayout.INODE_NAME] = internName(xscAttr.getName());
    m_nodes[nd + EXISchemaLayout.INODE_TARGET_NAMESPACE] = internName(roundify(xscAttr.getNamespace()));
    int stype = doType(xscAttr.getTypeDefinition(), new Stack<XSObject>());
    m_nodes[nd + EXISchemaLayout.INODE_TYPE] = stype;
    m_nodes[nd + EXISchemaLayout.ATTR_CONSTRAINT] = getValueConstraintType(xscAttr);
    final XSValue constraintValue = xscAttr.getValueConstraintValue();
    m_nodes[nd + EXISchemaLayout.ATTR_CONSTRAINT_VALUE] = constraintValue == null ? 
        EXISchema.NIL_VALUE : doVariantValue(constraintValue.getActualValue(),
            constraintValue.getActualValueType(), constraintValue.getListValueTypes());
    int bvals = m_nodes[nd + EXISchemaLayout.INODE_BOOLEANS];
    if (isGlobalAttribute(xscAttr))
      bvals |= EXISchemaLayout.INODE_ISGLOBAL_MASK;
    m_nodes[nd + EXISchemaLayout.INODE_BOOLEANS] = bvals;

    return nd;
  }

  private static boolean isIntegral(short builtInKind) {
    switch (builtInKind) {
      case XSConstants.INTEGER_DT:
      case XSConstants.NONPOSITIVEINTEGER_DT:
      case XSConstants.LONG_DT:
      case XSConstants.NONNEGATIVEINTEGER_DT:
      case XSConstants.NEGATIVEINTEGER_DT:
      case XSConstants.INT_DT:
      case XSConstants.UNSIGNEDLONG_DT:
      case XSConstants.POSITIVEINTEGER_DT:
      case XSConstants.SHORT_DT:
      case XSConstants.UNSIGNEDINT_DT:
      case XSConstants.BYTE_DT:
      case XSConstants.UNSIGNEDSHORT_DT:
      case XSConstants.UNSIGNEDBYTE_DT:
        return true;
      default:
        return false;
    }      
  }
  
  private void _doSimpleType(XSSimpleTypeDecl xscSType, int nd, Stack<XSObject> nodePath)
      throws EXISchemaFactoryException {
    int i;

    m_nodes[nd] = EXISchema.SIMPLE_TYPE_NODE;

    // properties specific to simple types

    int base = nd + EXISchemaLayout.SZ_TYPE;

    int variety = EXISchema.UR_SIMPLE_TYPE;
    switch (xscSType.getVariety()) {
      case XSSimpleTypeDefinition.VARIETY_ATOMIC:
        variety = EXISchema.ATOMIC_SIMPLE_TYPE;
        break;
      case XSSimpleTypeDefinition.VARIETY_LIST:
        variety = EXISchema.LIST_SIMPLE_TYPE;
        break;
      case XSSimpleTypeDefinition.VARIETY_UNION:
        variety = EXISchema.UNION_SIMPLE_TYPE;
        break;
    }
    m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_VARIETY] = variety;

    int auxType = EXISchema.NIL_NODE;
    if (variety == EXISchema.ATOMIC_SIMPLE_TYPE) {
      int serial = getBuiltinSerialOfAtomicSimpleTypeDefinition(
          ((XSSimpleTypeDefinition)xscSType));
      auxType = EXISchema._getBuiltinTypeOfSchema(serial, m_nodes);
    }
    else if (variety == EXISchema.LIST_SIMPLE_TYPE) {
      XSSimpleTypeDefinition xscItemType = xscSType.getItemType();
      if (isBuiltIn(xscItemType)) {
        for (i = 0; i < m_xscBuiltinSTypes.length; i++)
          if (m_xscBuiltinSTypes[i].getName().equals(xscItemType.getName())) {
            auxType = getDoneNodeId(m_xscBuiltinSTypes[i]);
            break;
          }
      }
      if (auxType == EXISchema.NIL_NODE)
        auxType = doType(xscItemType, nodePath);
    }
    m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_AUX_TYPE] = auxType;

    int booleansField = 0;
    if (isBuiltIn(xscSType)) {
      booleansField |= EXISchemaLayout.SIMPLE_TYPE_ISBUILTIN_MASK;
      if (variety == EXISchema.ATOMIC_SIMPLE_TYPE &&
          "anySimpleType".equals(getBaseType(xscSType).getName()))
        booleansField |= EXISchemaLayout.SIMPLE_TYPE_ISPRIMITIVE_MASK;
    }
    final short builtInKind = xscSType.getBuiltInKind();
    final boolean isIntegral = isIntegral(builtInKind);
    if (isIntegral)
      booleansField |= EXISchemaLayout.SIMPLE_TYPE_ISINTEGRAL_MASK;

    for (i = EXISchemaLayout.SIMPLE_TYPE_FACET_LENGTH;
         i <= EXISchemaLayout.SIMPLE_TYPE_FACET_FRACTIONDIGITS; i++) {
      m_nodes[base + i] = EXISchema.NIL_VALUE;
    }

    int facet;
    String facetStr;

    // length facet
    facet = EXISchema.NIL_VALUE;
    facetStr = xscSType.getLexicalFacetValue(XSSimpleTypeDefinition.FACET_LENGTH);
    if (facetStr != null)
      facet = addIntValue(Integer.parseInt(facetStr));
    m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_LENGTH] = facet;

    // minLength facet
    facet = EXISchema.NIL_VALUE;
    facetStr = xscSType.getLexicalFacetValue(XSSimpleTypeDefinition.FACET_MINLENGTH);
    if (facetStr != null)
      facet = addIntValue(Integer.parseInt(facetStr));
    m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_MINLENGTH] = facet;

    // maxLength facet
    facet = EXISchema.NIL_VALUE;
    facetStr = xscSType.getLexicalFacetValue(XSSimpleTypeDefinition.FACET_MAXLENGTH);
    if (facetStr != null)
      facet = addIntValue(Integer.parseInt(facetStr));
    m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_MAXLENGTH] = facet;

    // whitespace facet
    facet = EXISchema.WHITESPACE_ABSENT;
    if (variety != EXISchema.UR_SIMPLE_TYPE) {
      facetStr = xscSType.getLexicalFacetValue(XSSimpleTypeDefinition.FACET_WHITESPACE);
      if (facetStr != null) {
        if ("preserve".equals(facetStr))
          facet = EXISchema.WHITESPACE_PRESERVE;
        else if ("replace".equals(facetStr))
          facet = EXISchema.WHITESPACE_REPLACE;
        else if ("collapse".equals(facetStr)) {
          facet = EXISchema.WHITESPACE_COLLAPSE;
        }
      }
    }
    m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_WHITESPACE] = facet;

    BigInteger maxInclusiveInteger = null;
    // maxInclusive facet
    facet = EXISchema.NIL_VALUE;
    facetStr = xscSType.getLexicalFacetValue(XSSimpleTypeDefinition.FACET_MAXINCLUSIVE);
    if (facetStr != null) {
      facet = doVariantFacet(facetStr, xscSType.getMaxInclusiveValue(), builtInKind, 
          EXISchemaLayout.SIMPLE_TYPE_FACET_MAXINCLUSIVE);
      if (isIntegral)
        maxInclusiveInteger = new BigInteger(facetStr);
    }
    m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_MAXINCLUSIVE] = facet;

    // maxExclusive facet
    facet = EXISchema.NIL_VALUE;
    facetStr = xscSType.getLexicalFacetValue(XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE);
    if (facetStr != null)
      facet = doVariantFacet(facetStr, xscSType.getMaxExclusiveValue(), builtInKind, 
          EXISchemaLayout.SIMPLE_TYPE_FACET_MAXEXCLUSIVE);
    if (facet != EXISchema.NIL_VALUE) {
      if (isIntegral) {
        m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_MAXINCLUSIVE] = facet;
        m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_MAXEXCLUSIVE] = EXISchema.NIL_VALUE;
        maxInclusiveInteger = new BigInteger(facetStr).subtract(BigInteger.ONE);
      }
      else
        m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_MAXEXCLUSIVE] = facet;
    }
    else
      m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_MAXEXCLUSIVE] = EXISchema.NIL_VALUE;

    BigInteger minInclusiveInteger = null;
    // minInclusive facet
    facet = EXISchema.NIL_VALUE;
    facetStr = xscSType.getLexicalFacetValue(XSSimpleTypeDefinition.FACET_MININCLUSIVE);
    if (facetStr != null) {
      facet = doVariantFacet(facetStr, xscSType.getMinInclusiveValue(), builtInKind, 
          EXISchemaLayout.SIMPLE_TYPE_FACET_MININCLUSIVE);
      if (isIntegral)
        minInclusiveInteger = new BigInteger(facetStr);
    }
    m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_MININCLUSIVE] = facet;

    // minExclusive facet
    facet = EXISchema.NIL_VALUE;
    facetStr = xscSType.getLexicalFacetValue(XSSimpleTypeDefinition.FACET_MINEXCLUSIVE);
    if (facetStr != null)
      facet = doVariantFacet(facetStr, xscSType.getMinExclusiveValue(), builtInKind, 
          EXISchemaLayout.SIMPLE_TYPE_FACET_MINEXCLUSIVE);
    if (facet != EXISchema.NIL_VALUE) {
      if (isIntegral) {
        m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_MININCLUSIVE] = facet;
        m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_MINEXCLUSIVE] = EXISchema.NIL_VALUE;
        minInclusiveInteger = new BigInteger(facetStr).add(BigInteger.ONE);
      }
      else
        m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_MINEXCLUSIVE] = facet;
    }
    else
      m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_MINEXCLUSIVE] = EXISchema.NIL_VALUE;

    if (isIntegral) {
      int integralWidth = 0xFF; // 0xFF represents the default representation.
      if (minInclusiveInteger != null) {
        boolean isNonNegative = false;
        if (minInclusiveInteger.signum() >= 0) {  
          isNonNegative = true;
        }
        if (maxInclusiveInteger != null) {
          BigInteger bigIntegralRange = maxInclusiveInteger.subtract(minInclusiveInteger);
          if (bigIntegralRange.signum() >= 0) {
            if ((integralWidth = bigIntegralRange.bitLength()) > 12)
              integralWidth = 0xFF;
          }
        }
        if (integralWidth == 0xFF && isNonNegative) {
          integralWidth = 0xFE; // 0xFE represents unsigned representation
        }
      }
      integralWidth <<= EXISchemaLayout.INTEGRAL_SIMPLE_TYPE_WIDTH_OFFSET;
      booleansField |= integralWidth;
    }
    m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_BOOLEANS] = booleansField;
    
    // totalDigits facet
    facet = EXISchema.NIL_VALUE;
    facetStr = xscSType.getLexicalFacetValue(XSSimpleTypeDefinition.FACET_TOTALDIGITS);
    if (facetStr != null)
      facet = addIntValue(Integer.parseInt(facetStr));
    m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_TOTALDIGITS] = facet;

    // fractionDigits facet
    facet = EXISchema.NIL_VALUE;
    facetStr = xscSType.getLexicalFacetValue(XSSimpleTypeDefinition.FACET_FRACTIONDIGITS);
    if (facetStr != null)
      facet = addIntValue(Integer.parseInt(facetStr));
    m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_FACET_FRACTIONDIGITS] = facet;

    // pattern facet
    final int[] restrictedCharset = getPattern(xscSType); 
    final int n_restrictedCharacters = restrictedCharset != null ? restrictedCharset.length : 0;
    m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_N_RESTRICTED_CHARSET] = n_restrictedCharacters;
    for (i = 0; i < n_restrictedCharacters; i++) {
      m_nodes[base + EXISchemaLayout.SZ_SIMPLE_TYPE + i] = restrictedCharset[i];
    }

    // enumeration facet
    ObjectList enums = xscSType.getActualEnumeration();
    ShortList enumTypeList = xscSType.getEnumerationTypeList();
    ObjectList enumItemTypeList = xscSType.getEnumerationItemTypeList();
    m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_N_FACET_ENUMERATIONS] = enums.getLength();
    for (i = 0; i < enums.getLength(); i++) {
      int en = doVariantValue(enums.item(i), enumTypeList.item(i), (ShortList)enumItemTypeList.item(i));
      m_nodes[base + EXISchemaLayout.SZ_SIMPLE_TYPE + n_restrictedCharacters + i] = en;
    }

    XSSimpleTypeDefinition[] memberTypes = getMemberTypes(xscSType);
    m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_N_MEMBER_TYPES] = memberTypes.length;
    for (i = 0; i < memberTypes.length; i++) {
      int mt = doType(memberTypes[i], nodePath);
      m_nodes[base + EXISchemaLayout.SZ_SIMPLE_TYPE + n_restrictedCharacters + enums.getLength() + i] = mt;
    }

    if (isListableSimpleType(nd, m_nodes)) {
      m_nodes[base + EXISchemaLayout.SIMPLE_TYPE_BOOLEANS] |= EXISchemaLayout.SIMPLE_TYPE_ISLIST_CONTENT_MASK;
    }
  }

  private void _doComplexType(XSComplexTypeDefinition xscCType, int nd, Stack<XSObject> nodePath)
      throws EXISchemaFactoryException {
    m_nodes[nd] = EXISchema.COMPLEX_TYPE_NODE;
    nodePath.push(xscCType);
    try {
      int i;
      // properties specific to complex types
  
      final int base = nd + EXISchemaLayout.SZ_TYPE;
  
      final int contentClass = getContentClass(xscCType);
      m_nodes[base + EXISchemaLayout.COMPLEX_TYPE_CONTENT_CLASS] = contentClass;

      int bvals = m_nodes[base + EXISchemaLayout.COMPLEX_TYPE_BOOLEANS];
      
      final int n_attrUses;
      XSAttributeUse[] attributeUsesSorted = m_sortedAttrUses.get(xscCType);
      assert attributeUsesSorted != null;
      n_attrUses = attributeUsesSorted.length; 
      
      final int contentType;
      switch (contentClass) {
        case EXISchema.CONTENT_SIMPLE:
          contentType = doType(xscCType.getSimpleType(), nodePath);
          break;
        case EXISchema.CONTENT_ELEMENT_ONLY:
        case EXISchema.CONTENT_MIXED:
          final XSParticle xscParticle;
          if ((xscParticle = (XSParticle)xscCType.getParticle()) != null) {
            contentType = base + EXISchemaLayout.SZ_COMPLEX_TYPE + 2 * n_attrUses; // particle is inlined
            _doParticle(xscParticle, contentType, nodePath, 0);
            bvals |= EXISchemaLayout.COMPLEX_TYPE_HASPARTICLE_MASK;
          }
          else {
            contentType = EXISchema.NIL_NODE;
          }
          break;
        default:
          contentType  = EXISchema.NIL_NODE;
          break;
      }
      m_nodes[base + EXISchemaLayout.COMPLEX_TYPE_CONTENT_TYPE]  = contentType;
      m_nodes[base + EXISchemaLayout.COMPLEX_TYPE_BOOLEANS] = bvals;

      int sz = n_attrUses * EXISchemaLayout.SZ_ATTR_USE;
      ensureNodes(sz);
      int attrUses = m_n_nodes;
      m_n_nodes += sz;
      
      for (i = 0 ; i < n_attrUses; i++) {
          _doAttributeUse(attributeUsesSorted[i],
                  attrUses + i * EXISchemaLayout.SZ_ATTR_USE, contentClass);
      }
      m_nodes[base + EXISchemaLayout.COMPLEX_TYPE_N_ATTRIBUTE_USES] = n_attrUses;
      m_nodes[base + EXISchemaLayout.COMPLEX_TYPE_ATTRIBUTE_USES] = attrUses;

      int wc = EXISchema.NIL_NODE;
      XSWildcard xscWildcard;
      if ((xscWildcard = xscCType.getAttributeWildcard()) != null)
          wc = doWildCard(xscWildcard, true);
      m_nodes[base + EXISchemaLayout.COMPLEX_TYPE_ATTRIBUTE_WC] = wc;

      IntBuffer entriesBufs[] = new IntBuffer[n_attrUses];
      IntBuffer prevBuf = new IntBuffer();
      prevBuf.append(EXISchema.NIL_NODE).append(n_attrUses);
      for (i = n_attrUses - 1; i >= 0; i--) {
          final int atuse = attrUses + i * EXISchemaLayout.SZ_ATTR_USE;
          IntBuffer intBuf = new IntBuffer(prevBuf.length() + 1);
          intBuf.append(atuse).append(i + 1);
          if (!attributeUsesSorted[i].getRequired()) {
              intBuf.append(prevBuf);
          }
          entriesBufs[i] = intBuf;
          prevBuf = intBuf;
      }
      for (i = 0; i < n_attrUses; i++) {
          IntBuffer intBuf = entriesBufs[i];
          int n_atuses = intBuf.length() / 2;
          int atuses = allocOpaque(2 * n_atuses);
          m_nodes[base + EXISchemaLayout.SZ_COMPLEX_TYPE + i] = atuses;
          m_nodes[base + EXISchemaLayout.SZ_COMPLEX_TYPE + n_attrUses + i] = n_atuses;
          for (int j = 0; j < n_atuses; j++) {
              m_nodes[atuses + j]= intBuf.intAt(2 * j);
              m_nodes[atuses + n_atuses + j]= intBuf.intAt(2 * j + 1);
          }
      }
      
      int n_particles = 0;
      int n_initials = 0;
      int particles = EXISchema.NIL_NODE;
      if (contentType != EXISchema.NIL_NODE &&
          m_nodes[contentType] == EXISchema.PARTICLE_NODE) {
        int termType = EXISchema._getTermTypeOfParticle(contentType, m_nodes);
        if (termType != EXISchema.TERM_TYPE_ABSENT &&
            termType != EXISchema.TERM_TYPE_INVALID) {
          int group = EXISchema._getTermOfParticle(contentType, m_nodes);
          assert m_nodes[group] == EXISchema.GROUP_NODE;
          int n_substances = EXISchema._getMemberSubstanceCountOfGroup(group, m_nodes);
          for (i = 0; i < n_substances; i++) {
            int particle = EXISchema._getMemberSubstanceOfGroup(group, i, m_nodes);
            assert m_nodes[particle] == EXISchema.PARTICLE_NODE;
            termType = EXISchema._getTermTypeOfParticle(particle, m_nodes);
            if (termType == EXISchema.TERM_TYPE_ELEMENT) {
              int elem = EXISchema._getTermOfParticle(particle, m_nodes);
              if (!isProcessed(elem) && !EXISchema._isGlobalElement(elem, m_nodes)) {
                XSElementDeclaration xscElem = (XSElementDeclaration)getDoneNode(elem);
                doElement(xscElem, new Stack<XSObject>());
              }
            }
          }

          n_particles = EXISchema._getHeadSubstanceCountOfParticle(contentType, m_nodes);
          if (n_particles != 0) {
            final int n_particlesOfGroup = EXISchema._getParticleCountOfGroup(group, m_nodes); 
            particles = EXISchema._getHeadSubstanceListOfParticle(contentType, m_nodes);
            if (m_nodes[particles + n_particles - 1] != EXISchema.NIL_NODE) {
              int _particles = allocOpaque(2 * (n_particles + 1));
              int len;
              for (i = 0, len = n_particles; i < len; i++) {
                m_nodes[_particles + i] = m_nodes[particles + i];
                m_nodes[_particles + i + (n_particles + 1)] = m_nodes[particles + i + n_particles];
              }
              m_nodes[_particles + n_particles] = EXISchema.NIL_NODE;
              m_nodes[_particles + n_particles + (n_particles + 1)] = n_particlesOfGroup;
              ++n_particles;
              particles = _particles;
            }
            n_initials = EXISchema._isFixtureParticle(contentType, m_nodes) ? 
                n_particles - 1 : n_particles;
          }
        }
      }
      if (particles == EXISchema.NIL_NODE) {
        particles = allocOpaque(2);
        m_nodes[particles] = EXISchema.NIL_NODE;
        m_nodes[particles + 1] = 0;
        n_initials = n_particles = 1;
      }
      assert particles != EXISchema.NIL_NODE;
      assert n_particles > 0 && n_initials > 0;
      m_nodes[base + EXISchemaLayout.COMPLEX_TYPE_PARTICLES] = particles;
      m_nodes[base + EXISchemaLayout.COMPLEX_TYPE_N_PARTICLES] = n_particles;
      m_nodes[base + EXISchemaLayout.COMPLEX_TYPE_N_INITIALS] = n_initials;
    }
    finally {
      nodePath.pop();
    }
  }
  
  private static XSAttributeUse[] sortAttributeUses(XSComplexTypeDefinition complexType) {
    SortedSet<ComparableAttributeUse> attributeUses = new TreeSet<ComparableAttributeUse>();
    XSObjectList attrUseList;
    attrUseList = complexType.getAttributeUses();
    int i;
    for (i = 0; i < attrUseList.getLength(); i++) {
      XSAttributeUse ith = (XSAttributeUse)attrUseList.item(i);
      attributeUses.add(new ComparableAttributeUse(ith));
    }
    XSAttributeUse[] array = new XSAttributeUse[attributeUses.size()]; 
    Iterator<ComparableAttributeUse> iter = attributeUses.iterator();
    for (i = 0; iter.hasNext(); i++) {
        array[i] = ((ComparableAttributeUse)iter.next()).attruse;
    }
    return array;
  }

  private void _doAttributeUse(XSAttributeUse xscAttrUse, int nd, int contextContentClass)
      throws EXISchemaFactoryException {
    m_nodes[nd] = EXISchema.ATTRIBUTE_USE_NODE;

    int attr = doAttribute(xscAttrUse.getAttrDeclaration(), xscAttrUse.getRequired());
    m_nodes[nd + EXISchemaLayout.ATTR_USE_ATTR_NAME] =
        EXISchema._getNameOfAttr(attr, m_nodes);
    m_nodes[nd + EXISchemaLayout.ATTR_USE_ATTR_TARGET_NAMESPACE] =
        EXISchema._getTargetNamespaceNameOfAttr(attr, m_nodes);
    m_nodes[nd + EXISchemaLayout.ATTR_USE_ATTRIBUTE]  = attr;
    int ctype = getValueConstraintType(xscAttrUse);
    int cvariant = EXISchema.NIL_VALUE;
    if (ctype == EXISchema.CONSTRAINT_NONE) {
      int attrConstraint = EXISchema._getConstraintOfAttr(attr, m_nodes);
      if (attrConstraint != EXISchema.CONSTRAINT_NONE) {
        ctype = attrConstraint;
        cvariant = EXISchema._getConstraintValueOfAttr(attr, m_nodes);
      }
    }
    m_nodes[nd + EXISchemaLayout.ATTR_USE_CONSTRAINT]       = ctype;
    final XSValue constraintValue = xscAttrUse.getValueConstraintValue();
    m_nodes[nd + EXISchemaLayout.ATTR_USE_CONSTRAINT_VALUE] =
      cvariant != EXISchema.NIL_VALUE ? cvariant : constraintValue == null ? EXISchema.NIL_VALUE : 
        doVariantValue(constraintValue.getActualValue(), constraintValue.getActualValueType(), constraintValue.getListValueTypes());
    int bvals = m_nodes[nd + EXISchemaLayout.ATTR_USE_BOOLEANS];
    if (xscAttrUse.getRequired())
      bvals |= EXISchemaLayout.ATTR_USE_ISREQUIRED_MASK;
    m_nodes[nd + EXISchemaLayout.ATTR_USE_BOOLEANS] = bvals;
  }

  /**
   * Process a particle 
   * @return Returns the number of subordinate particles. 
   */
  private int _doParticle(XSParticle xscParticle, int nd, Stack<XSObject> nodePath, int particleSerialInType)
      throws EXISchemaFactoryException {
    m_doneNodesReverse.put(nd, xscParticle); // so that we can later lookup particle by node id
    m_nodes[nd] = EXISchema.PARTICLE_NODE;

    m_nodes[nd + EXISchemaLayout.PARTICLE_SERIAL_INTYPE] = particleSerialInType++;

    int minOccurs, maxOccurs;

    minOccurs = xscParticle.getMinOccurs();
    maxOccurs = xscParticle.getMaxOccursUnbounded() ? 
        EXISchema.UNBOUNDED_OCCURS : xscParticle.getMaxOccurs();

    m_nodes[nd + EXISchemaLayout.PARTICLE_MINOCCURS] = minOccurs;
    m_nodes[nd + EXISchemaLayout.PARTICLE_MAXOCCURS] = maxOccurs;

    int n_particles = 0;
    
    int term     = EXISchema.NIL_NODE;
    int termType = EXISchema.TERM_TYPE_ABSENT;

    XSTerm xscTerm;
    if ((xscTerm = xscParticle.getTerm()) != null) {
      switch (termType = probeTermType(xscTerm)) {
        case EXISchema.TERM_TYPE_GROUP:
          final long composite = doGroup((XSModelGroup)xscTerm, nodePath, particleSerialInType);
          term = (int)(composite >>> 32);
          n_particles += (int)(composite & 0xFFFFFFFFL);
          break;
        case EXISchema.TERM_TYPE_WILDCARD:
          term = doWildCard((XSWildcard)xscTerm, false);
          break;
        case EXISchema.TERM_TYPE_ELEMENT:
          XSElementDeclaration xscElem = (XSElementDeclaration)xscTerm;
          if (isGlobalElement(xscElem)) {
            /** global elements may nest. do not call doElements. */
            int contentClass = getContentClass(xscElem);
            if (contentClass == EXISchema.CONTENT_ELEMENT_ONLY ||
                contentClass == EXISchema.CONTENT_MIXED) {
              term = getDoneNodeId(xscElem);
              assert term != EXISchema.NIL_NODE;
            }
          }
          if (term == EXISchema.NIL_NODE) {
            // local element, or global element of simple or empty content
            term = doElement((XSElementDeclaration)xscTerm, nodePath);
          }
          break;
        default:
          break;
      }
    }
    m_nodes[nd + EXISchemaLayout.PARTICLE_TERM_TYPE] = termType;
    m_nodes[nd + EXISchemaLayout.PARTICLE_TERM]      = term;

    int bvals = m_nodes[nd + EXISchemaLayout.PARTICLE_BOOLEANS];
    boolean fixture = true;
    if (minOccurs == 0)
      fixture = false;
    else if (termType == EXISchema.TERM_TYPE_GROUP) {
      if (!EXISchema.isFixtureGroup(term, m_nodes))
        fixture = false;
    }
    if (fixture)
      bvals |= EXISchemaLayout.PARTICLE_ISFIXTURE_MASK;
    m_nodes[nd + EXISchemaLayout.PARTICLE_BOOLEANS] = bvals;

    int n_initials, n_substances;
    int initials, substances;
    n_initials = n_substances = 0;
    initials = substances = EXISchema.NIL_NODE;
    switch (termType) {
      case EXISchema.TERM_TYPE_WILDCARD:
      case EXISchema.TERM_TYPE_ELEMENT:
        n_initials = n_substances = 1;
        initials = allocOpaque(2); // pseudo entries
        m_nodes[initials] = nd;
        m_nodes[initials + 1] = 0;
        substances = nd;
        break;
      case EXISchema.TERM_TYPE_GROUP:
        if (EXISchema._getParticleCountOfGroup(term, m_nodes) > 0) {
          if (EXISchema._getCompositorOfGroup(term, m_nodes) == EXISchema.GROUP_CHOICE) {
            n_initials = EXISchema._getHeadSubstanceCountOfGroup(term, m_nodes);
            initials = EXISchema._getHeadSubstanceListOfGroup(term, m_nodes);
          }
          else {
            n_initials = EXISchema._getHeadSubstanceCountOfGroup(term, 0, m_nodes);
            initials = EXISchema._getHeadSubstanceListOfGroup(term, 0, m_nodes);
          }
          /** Make sure list == EXISchema.NIL_NODE only when n_initials is zero */
          assert initials != EXISchema.NIL_NODE || n_initials == 0;
          if (initials != EXISchema.NIL_NODE) {
            assert n_initials > 0;
            // Get rid of end-of-group indicator if there is one.
            if (m_nodes[initials + (n_initials - 1)] == EXISchema.NIL_NODE) {
              final int n_initials2;
              if ((n_initials2 = n_initials - 1) == 0) {
                initials = EXISchema.NIL_NODE;
              }
              else {
                int initials2 = allocOpaque(2 * n_initials2);
                for (int j = 0; j < n_initials2; j++) {
                  m_nodes[initials2 + j] = m_nodes[initials + j];
                  m_nodes[initials2 + n_initials2 + j] = m_nodes[initials + n_initials + j];
                }
                initials = initials2;
              }
              n_initials = n_initials2;
            }
          }
          n_substances = EXISchema._getMemberSubstanceCountOfGroup(term, m_nodes);
          final int list = EXISchema._getMemberSubstanceListOfGroup(term, m_nodes);
          if (list != EXISchema.NIL_NODE) {
            substances = n_substances > 1 ? list : m_nodes[list];
          }
          else {
            // list == EXISchema.NIL_NODE only when n_substances is zero
            assert n_substances == 0;
          }
        }
        break;
      default:
        break;
    }
    m_nodes[nd + EXISchemaLayout.PARTICLE_N_INITIALS]   = n_initials;
    m_nodes[nd + EXISchemaLayout.PARTICLE_INITIALS]     = initials;
    m_nodes[nd + EXISchemaLayout.PARTICLE_N_SUBSTANCES] = n_substances;
    m_nodes[nd + EXISchemaLayout.PARTICLE_SUBSTANCES]   = substances;
    
    return n_particles;
  }

  private static int probeTermType(XSTerm xscTerm) {
    int termType = EXISchema.TERM_TYPE_INVALID;

    if (xscTerm instanceof XSModelGroup)
      termType = EXISchema.TERM_TYPE_GROUP;
    else if (xscTerm instanceof XSWildcard)
      termType = EXISchema.TERM_TYPE_WILDCARD;
    else if (xscTerm instanceof XSElementDeclaration)
      termType = EXISchema.TERM_TYPE_ELEMENT;

    return termType;
  }

  private int doWildCard(XSWildcard xscWildCard, boolean sortLexically) {
    int nd;
    if ((nd = internNode(xscWildCard)) != m_n_nodes)
      return nd;

    int constraintType = EXISchema.WC_TYPE_INVALID;
    switch (xscWildCard.getConstraintType()) {
      case XSWildcard.NSCONSTRAINT_ANY:
        constraintType = EXISchema.WC_TYPE_ANY;
        break;
      case XSWildcard.NSCONSTRAINT_NOT:
        constraintType = EXISchema.WC_TYPE_NOT;
        break;
      case XSWildcard.NSCONSTRAINT_LIST:
        constraintType = EXISchema.WC_TYPE_NAMESPACES;
        break;
      default:
        break;
    }

    int i;
    final int n_namespaces;
    final String[] namespaceNames;
    if (constraintType == EXISchema.WC_TYPE_NOT || constraintType == EXISchema.WC_TYPE_NAMESPACES) {
      StringList namespaceList = xscWildCard.getNsConstraintList();
      n_namespaces = namespaceList.getLength();
      namespaceNames = new String[n_namespaces];
      for (i = 0; i < n_namespaces; i++) {
        namespaceNames[i] = roundify(namespaceList.item(i));
      }
      if (sortLexically) {
        final int len;
        for (i = 0, len = n_namespaces - 1; i < len; i++) {
          for (int j = i + 1; j < n_namespaces; j++) {
            final String jth = namespaceNames[j];
            if (namespaceNames[i].compareTo(jth) > 0) {
              final String _saved = namespaceNames[i];
              namespaceNames[i] = namespaceNames[j];
              namespaceNames[j] = _saved;
              if (namespaceNames[i] == "")
                break;
            }
          }
        }
      }
    }
    else {
      n_namespaces = 0;
      namespaceNames = null;
    }
    
    int sz = EXISchemaLayout.SZ_WILDCARD + 2 * n_namespaces;
    ensureNodes(sz);
    m_n_nodes += sz;

    m_nodes[nd] = EXISchema.WILDCARD_NODE;

    int processContents = EXISchema.WC_PROCESS_INVALID;
    switch (xscWildCard.getProcessContents()) {
      case XSWildcard.PC_LAX:
        processContents = EXISchema.WC_PROCESS_LAX;
        break;
      case XSWildcard.PC_SKIP:
        processContents = EXISchema.WC_PROCESS_SKIP;
        break;
      case XSWildcard.PC_STRICT:
        processContents = EXISchema.WC_PROCESS_STRICT;
        break;
      default:
        break;
    }

    m_nodes[nd + EXISchemaLayout.WILDCARD_CONSTRAINT_TYPE]  = constraintType;
    m_nodes[nd + EXISchemaLayout.WILDCARD_PROCESS_CONTENTS] = processContents;
    m_nodes[nd + EXISchemaLayout.WILDCARD_N_NAMESPACES]     = n_namespaces;

    int base = nd + EXISchemaLayout.SZ_WILDCARD;
    for (i = 0; i < n_namespaces; i++) {
      m_nodes[base + i] = internName(namespaceNames[i]);
      m_nodes[base + n_namespaces + i] =
          getDoneNodeId(getKeyForNamespaceName(namespaceNames[i]));
    }

    return nd;
  }

  private long doGroup(XSModelGroup xscGroup, Stack<XSObject> nodePath, int particleSerialInType)
      throws EXISchemaFactoryException {
    final int nd = m_n_nodes;

    XSObjectList xscParticles;
    int n_particles = 0;

    int sz = EXISchemaLayout.SZ_GROUP;
    if ((xscParticles = xscGroup.getParticles()) != null) {
      n_particles = xscParticles.getLength();
    }
    sz += 3 * (n_particles + 1) + n_particles * EXISchemaLayout.SZ_PARTICLE;
    ensureNodes(sz);
    m_n_nodes += sz;

    m_nodes[nd] = EXISchema.GROUP_NODE;
    m_nodes[nd + EXISchemaLayout.GROUP_NUMBER] = m_n_groups++;

    final int compositor;
    switch (xscGroup.getCompositor()) {
      case XSModelGroup.COMPOSITOR_ALL:
        compositor = EXISchema.GROUP_ALL;
        break;
      case XSModelGroup.COMPOSITOR_CHOICE:
        compositor = EXISchema.GROUP_CHOICE;
        break;
      case XSModelGroup.COMPOSITOR_SEQUENCE:
        compositor = EXISchema.GROUP_SEQUENCE;
        break;
      default:
        compositor = EXISchema.NIL_VALUE;
        break;
    }
    m_nodes[nd + EXISchemaLayout.GROUP_COMPOSITOR]  = compositor;
    m_nodes[nd + EXISchemaLayout.GROUP_N_PARTICLES] = n_particles;

    boolean fixture = false; // not a fixture group if there is no particle
    // initial fixture value depends on compositor
    if (n_particles > 0)
      fixture = compositor == EXISchema.GROUP_CHOICE;

    int i, j, k;
    final int base = nd + EXISchemaLayout.SZ_GROUP + 3 * (n_particles + 1);

    IntBuffer entriesBufs[] = new IntBuffer[n_particles + 1];
    final IntBuffer startBuffer = new IntBuffer();
    startBuffer.append(EXISchema.NIL_NODE).append(n_particles);
    entriesBufs[n_particles] = startBuffer; // no longer a sentinel
    IntBuffer entriesBuf2 = new IntBuffer(); // for uniform mapping table
    entriesBuf2.append(EXISchema.NIL_NODE).append(n_particles);
    int n_totalSubordinateParticles = n_particles;
    for (i = 0; i < n_particles; i++) {
      final int particle = base + i * EXISchemaLayout.SZ_PARTICLE;
      // particles are nodes per se, but they are always inlined.
      final int n_subordinateParticles = _doParticle((XSParticle)xscParticles.item(i), particle, nodePath, particleSerialInType++);
      particleSerialInType += n_subordinateParticles;
      n_totalSubordinateParticles += n_subordinateParticles;
    }
    for (i = n_particles - 1; i >= 0; i--) {
      final int particle = base + i * EXISchemaLayout.SZ_PARTICLE;
      entriesBufs[i] = new IntBuffer(entriesBufs[i + 1].length());
      entriesBufs[i].append(entriesBufs[i + 1]);
      boolean pfixed;
      pfixed = EXISchema._isFixtureParticle(particle, m_nodes);
      switch (compositor) {
        case EXISchema.GROUP_ALL:
        case EXISchema.GROUP_SEQUENCE:
          if (pfixed) {
            fixture = true;
            if (compositor == EXISchema.GROUP_SEQUENCE)
              entriesBufs[i].clear();
          }
          break;
        case EXISchema.GROUP_CHOICE:
          if (!pfixed)
            fixture = false;
          break;
        default:
          break;
      }
      
      int n_heads = EXISchema._getHeadSubstanceCountOfParticle(particle, m_nodes);
      int heads   = EXISchema._getHeadSubstanceListOfParticle(particle, m_nodes);
      for (int entry = n_heads - 1; entry >= 0; entry--) {
        final int hd; // hd is a substance particle
        if ((hd = m_nodes[heads + entry]) != EXISchema.NIL_NODE) {
          int substance = EXISchema._getTermOfParticle(hd, m_nodes);
          // make sure it is either an element or a wildcard
          assert m_nodes[substance] != EXISchema.GROUP_NODE;
          for (j = 0; j < entriesBufs[i].length();) {
            final int jth = EXISchema._getTermOfParticle(
                entriesBufs[i].intAt(j), m_nodes);
            if (jth == substance) {
              entriesBufs[i].delete(j, j + 2);
              continue;
            }
            j += 2;
          }
          entriesBufs[i].append(hd);
          entriesBufs[i].append(i);
          for (j = 0; j < entriesBuf2.length();) {
            final int jth = EXISchema._getTermOfParticle(
                entriesBuf2.intAt(j), m_nodes);
            if (jth == substance) {
              entriesBuf2.delete(j, j + 2);
              continue;
            }
            j += 2;
          }
          entriesBuf2.append(hd);
          entriesBuf2.append(i);
        }
      }
    }

    int n_entries = 0;
    int entries   = EXISchema.NIL_NODE;
    for (i = n_particles; i >= 0; i--) {
      int n_backward_entries = 0;
      n_entries = 0;
      entries = EXISchema.NIL_NODE;

      if (i > 0 && compositor == EXISchema.GROUP_SEQUENCE) {
        final int prev = base + (i - 1) * EXISchemaLayout.SZ_PARTICLE;
        final int prevMaxOccurs = EXISchema._getMaxOccursOfParticle(prev, m_nodes);
        if (prevMaxOccurs > 1 || prevMaxOccurs == EXISchema.UNBOUNDED_OCCURS) {
          // append entriesBufs[i - 1] into entriesBufs[i] avoiding duplication
          jloop:
          for (j = 0; j < entriesBufs[i - 1].length(); j += 2) {
            final int particle;
            if ((particle = entriesBufs[i - 1].intAt(j)) > 0) {
              final int len;
              for (k = 0, len = entriesBufs[i].length(); k < len; k += 2) {
                final int intk = entriesBufs[i].intAt(k);
                // NOTE: Compare by particle instead of substance for
                // SequenceStateTest#testSequenceSameElementOK02
                if (particle == intk)
                  continue jloop; // avoid duplication
              }
              entriesBufs[i].append(entriesBufs[i - 1].intAt(j));
              entriesBufs[i].append(entriesBufs[i - 1].intAt(j + 1));
              ++n_backward_entries;
            }
          }
        }
      }

      if (compositor == EXISchema.GROUP_CHOICE && i < n_particles) {
        final int particle = base + i * EXISchemaLayout.SZ_PARTICLE;
        final int n_heads = EXISchema._getHeadSubstanceCountOfParticle(particle, m_nodes);
        final int heads   = EXISchema._getHeadSubstanceListOfParticle(particle, m_nodes);
        entriesBufs[i] = new IntBuffer(n_heads + 1); 
        entriesBufs[i].append(EXISchema.NIL_NODE);
        entriesBufs[i].append(n_particles);
        for (j = n_heads - 1; j >= 0; j--) {
          final int hd; // hd is a substance particle
          if ((hd = m_nodes[heads + j]) != EXISchema.NIL_NODE) {
            entriesBufs[i].append(hd);
            entriesBufs[i].append(i);
            ++n_backward_entries;
          }
        }
      }
      
      if (compositor == EXISchema.GROUP_SEQUENCE || compositor == EXISchema.GROUP_CHOICE || i == 0) {
        if (compositor == EXISchema.GROUP_ALL) {
          if (i == 0 && fixture) {
            assert entriesBufs[i].intAt(0) == EXISchema.NIL_NODE;
            assert entriesBufs[i].intAt(1) == n_particles;
            entriesBufs[i].delete(0, 2);
          }
        }
        if ( (n_entries = entriesBufs[i].length() / 2) > 0) {
          entries = allocOpaque(2 * n_entries);
          for (j = n_entries - 1, k = 0; j >= 0; j--, k++) {
            m_nodes[entries + k] = entriesBufs[i].intAt(2 * j);
            m_nodes[entries + k +
                n_entries] = entriesBufs[i].intAt(2 * j + 1);
          }
        }
      }
      m_nodes[nd + EXISchemaLayout.SZ_GROUP + 3 * i]     = n_entries;
      m_nodes[nd + EXISchemaLayout.SZ_GROUP + 3 * i + 1] = entries;
      m_nodes[nd + EXISchemaLayout.SZ_GROUP + 3 * i + 2] = n_backward_entries;
    }
    n_entries = m_nodes[nd + EXISchemaLayout.SZ_GROUP];
    entries   = m_nodes[nd + EXISchemaLayout.SZ_GROUP + 1];

    // populate uniform mapping table
    assert entriesBuf2.length() / 2 > 0;
    assert entriesBuf2.intAt(0) == EXISchema.NIL_NODE;
    assert entriesBuf2.intAt(1) == n_particles;
    if (fixture)
      entriesBuf2.delete(0, 2);
    n_entries = entriesBuf2.length() / 2;
    entries = EXISchema.NIL_NODE;
    if (n_entries > 0) {
      entries = allocOpaque(2 * n_entries);
      for (j = n_entries - 1, k = 0; j >= 0; j--, k++) {
        m_nodes[entries + k] = entriesBuf2.intAt(2 * j);
        m_nodes[entries + k + n_entries] = entriesBuf2.intAt(2 * j + 1);
      }
    }
    m_nodes[nd + EXISchemaLayout.GROUP_N_HEAD_SUBSTANCE_NODES]  = n_entries;
    m_nodes[nd + EXISchemaLayout.GROUP_HEAD_SUBSTANCE_NODES]    = entries;

    IntBuffer entriesBuf = new IntBuffer();
    for (i = 0; i < n_particles; i++) {
      int particle = base + i * EXISchemaLayout.SZ_PARTICLE;
      int n_substances = EXISchema._getSubstanceCountOfParticle(particle,
          m_nodes);
      int list = EXISchema._getSubstanceListOfParticle(particle, m_nodes);
      if (list != EXISchema.NIL_NODE) {
        for (j = 0; j < n_substances; j++) {
          entriesBuf.append(m_nodes[list + j]);
        }
      }
    }
    n_entries = entriesBuf.length();
    entries = n_entries > 0 ? allocOpaque(n_entries) : EXISchema.NIL_NODE;
    for (i = 0; i < n_entries; i++) {
      m_nodes[entries + i] = entriesBuf.intAt(i);
    }
    m_nodes[nd + EXISchemaLayout.GROUP_N_MEMBER_SUBSTANCE_NODES] = n_entries;
    m_nodes[nd + EXISchemaLayout.GROUP_MEMBER_SUBSTANCE_NODES] = entries;

    int bvals = m_nodes[nd + EXISchemaLayout.GROUP_BOOLEANS];
    if (fixture)
      bvals |= EXISchemaLayout.GROUP_ISFIXTURE_MASK;
    m_nodes[nd + EXISchemaLayout.GROUP_BOOLEANS] = bvals;
    
    return (((long)nd) << 32) + n_totalSubordinateParticles;
  }

  private int doVariantValue(Object obj, short valueType, ShortList itemValueTypes) {
    int variant = EXISchema.NIL_VALUE;
    if (obj != null) {
      int i, len;
      switch (valueType) {
        case XSConstants.ANYSIMPLETYPE_DT:
        case XSConstants.STRING_DT:
        case XSConstants.NORMALIZEDSTRING_DT:
        case XSConstants.TOKEN_DT:
        case XSConstants.LANGUAGE_DT:
        case XSConstants.NMTOKEN_DT:
        case XSConstants.NAME_DT:
        case XSConstants.NCNAME_DT:
        case XSConstants.ID_DT:
        case XSConstants.IDREF_DT:
        case XSConstants.ENTITY_DT:
        case XSConstants.ANYURI_DT:
          variant = addVariantStringValue((String)obj);
          break;
        case XSConstants.DECIMAL_DT:
        case XSConstants.INTEGER_DT:
        case XSConstants.NONPOSITIVEINTEGER_DT:
        case XSConstants.NEGATIVEINTEGER_DT:
        case XSConstants.NONNEGATIVEINTEGER_DT:
        case XSConstants.UNSIGNEDLONG_DT:
        case XSConstants.UNSIGNEDINT_DT:
        case XSConstants.UNSIGNEDSHORT_DT:
        case XSConstants.UNSIGNEDBYTE_DT:
        case XSConstants.POSITIVEINTEGER_DT:
          variant = addVariantDecimalValue(
              ((org.apache.xerces.xs.datatypes.XSDecimal)obj).getBigDecimal());
          break;
        case XSConstants.BOOLEAN_DT:
          variant = super.addVariantBooleanValue(((Boolean)obj).booleanValue());
          break;
        case XSConstants.LONG_DT:
          variant = addVariantLongValue(
              ((org.apache.xerces.xs.datatypes.XSDecimal)obj).getLong());
          break;
        case XSConstants.INT_DT:
        case XSConstants.SHORT_DT:
        case XSConstants.BYTE_DT:
          variant = addVariantIntValue(
              ((org.apache.xerces.xs.datatypes.XSDecimal)obj).getInt());
          break;
    
        case XSConstants.FLOAT_DT:
          variant = addVariantFloatValue(((XSFloat)obj).getValue());
          break;
        case XSConstants.DOUBLE_DT:
          variant = addVariantDoubleValue(((XSDouble)obj).getValue());
          break;
    
        case XSConstants.QNAME_DT:
        case XSConstants.NOTATION_DT:
          org.apache.xerces.xni.QName qname = ((XSQName)obj).getXNIQName();
          variant = addVariantQNameValue(qname.uri, qname.localpart);
          break;
          
        case XSConstants.DATETIME_DT:
        case XSConstants.TIME_DT:
        case XSConstants.DATE_DT:
        case XSConstants.GYEARMONTH_DT:
        case XSConstants.GYEAR_DT:
        case XSConstants.GMONTHDAY_DT:
        case XSConstants.GDAY_DT:
        case XSConstants.GMONTH_DT:
          org.apache.xerces.xs.datatypes.XSDateTime dateTime = (org.apache.xerces.xs.datatypes.XSDateTime)obj;
          variant = addVariantDateTimeValue(
              m_datatypeFactory.newXMLGregorianCalendar(dateTime.getLexicalValue()));
          break;
    
        case XSConstants.DURATION_DT:
          dateTime = (org.apache.xerces.xs.datatypes.XSDateTime)obj;
          try {
            variant = addVariantDurationValue(
                m_datatypeFactory.newDuration(dateTime.getLexicalValue()));
          }
          catch (IllegalArgumentException iae) {
            assert false;
          }
          break;
          
        case XSConstants.BASE64BINARY_DT:
        case XSConstants.HEXBINARY_DT:
          ByteList byteList = (org.apache.xerces.xs.datatypes.ByteList)obj;
          byte[] bytes = new byte[byteList.getLength()];
          for (i = 0; i < bytes.length; i++) {
            bytes[i] = byteList.item(i);
          }
          variant = addVariantBinaryValue(bytes);
          break;
    
        case XSConstants.LIST_DT:
        case XSConstants.LISTOFUNION_DT:
          ObjectList objectList = (ObjectList)obj;
          len = objectList.getLength();
          int[] variantList = new int[len];
          for (i = 0; i < len; i++) {
            variantList[i] = doVariantValue(objectList.item(i),
                itemValueTypes.item(valueType == XSConstants.LIST_DT ? 0 : i), 
                (ShortList)null);
          }
          variant = addVariantListValue(variantList);
          break;
          
        case XSConstants.UNAVAILABLE_DT:
          break;
          
        default:
          break;
      }
    }
    return variant;
  }

  
  private int doVariantFacet(String facetStr, Object value, short builtinKind, int facetKind) {
    int variant = EXISchema.NIL_VALUE;

    switch (builtinKind) {
      case XSConstants.DECIMAL_DT:
        variant = addVariantDecimalValue(new BigDecimal(facetStr));
        break;
      case XSConstants.INTEGER_DT:
      case XSConstants.NONPOSITIVEINTEGER_DT:
      case XSConstants.NEGATIVEINTEGER_DT:
      case XSConstants.NONNEGATIVEINTEGER_DT:
      case XSConstants.UNSIGNEDLONG_DT:
      case XSConstants.UNSIGNEDINT_DT:
      case XSConstants.UNSIGNEDSHORT_DT:
      case XSConstants.UNSIGNEDBYTE_DT:
      case XSConstants.POSITIVEINTEGER_DT:
      case XSConstants.LONG_DT:
      case XSConstants.INT_DT:
      case XSConstants.SHORT_DT:
      case XSConstants.BYTE_DT:
        BigInteger integer = new BigInteger(facetStr);
        if (facetKind == EXISchemaLayout.SIMPLE_TYPE_FACET_MAXEXCLUSIVE)
          integer = integer.subtract(BigInteger.ONE);
        else if (facetKind == EXISchemaLayout.SIMPLE_TYPE_FACET_MINEXCLUSIVE)
          integer = integer.add(BigInteger.ONE);
        final BigInteger intMaxValue, longMaxValue;
        if (facetKind == EXISchemaLayout.SIMPLE_TYPE_FACET_MININCLUSIVE || facetKind == EXISchemaLayout.SIMPLE_TYPE_FACET_MINEXCLUSIVE) {
          intMaxValue = INT_MAX_VALUE_MINUS_4095;
          longMaxValue = LONG_MAX_VALUE_MINUS_4095;
        }
        else {
          intMaxValue = INT_MAX_VALUE;
          longMaxValue = LONG_MAX_VALUE;
        }
        if (INT_MIN_VALUE.compareTo(integer) <= 0 && integer.compareTo(intMaxValue) <= 0)
          variant = addVariantIntValue(integer.intValue());
        else if (LONG_MIN_VALUE.compareTo(integer) <= 0 && integer.compareTo(longMaxValue) <= 0)
          variant = addVariantLongValue(integer.longValue());
        else
          variant = addVariantIntegerValue(integer);
        break;
      case XSConstants.FLOAT_DT:
        final int len;
        if ((len = facetStr.length()) != 0) {
          switch (facetStr.charAt(len - 1)) {
            case 'F':
              if (len == 3 && "INF".equals(facetStr))
                variant = addVariantFloatValue(Float.POSITIVE_INFINITY);
              else if (len == 4 && "-INF".equals(facetStr))
                variant = addVariantFloatValue(Float.NEGATIVE_INFINITY);
              break;
            case 'N':
              if (len == 3 && "NaN".equals(facetStr))
                variant = addVariantFloatValue(Float.NaN);
              break;
            default:
              try {
                variant = addVariantFloatValue(Float.parseFloat(facetStr));
              }
              catch (NumberFormatException nfe) {
                break;
              }
          }
        }
        break;
      case XSConstants.DOUBLE_DT:
        variant = addVariantDoubleValue(Double.parseDouble(facetStr));
        break;
  
      case XSConstants.DATETIME_DT:
      case XSConstants.TIME_DT:
      case XSConstants.DATE_DT:
      case XSConstants.GYEARMONTH_DT:
      case XSConstants.GYEAR_DT:
      case XSConstants.GMONTHDAY_DT:
      case XSConstants.GDAY_DT:
      case XSConstants.GMONTH_DT:
        variant = addVariantDateTimeValue(
            m_datatypeFactory.newXMLGregorianCalendar(((org.apache.xerces.xs.datatypes.XSDateTime)value).getLexicalValue()));
        break;
        
      case XSConstants.DURATION_DT:
        try {
          variant = addVariantDurationValue(
              m_datatypeFactory.newDuration(facetStr));
        }
        catch (IllegalArgumentException iae) {
          assert false;
        }
        break;
        
      case XSConstants.UNAVAILABLE_DT:
        break;
        
      default:
        break;
    }
    return variant;
  }

  private int doNamespace(String namespaceName,
                          int n_allElems, Set<ComparableElementDeclaration> elemDecls,
                          int n_allAttrs, Set<XSObject> attrDecls,
                          Set<XSObject> typeDecls)
      throws EXISchemaFactoryException {
    int nd;
    if ((nd = internNode(getKeyForNamespaceName(namespaceName))) != m_n_nodes)
      return nd;

    int i, n, base;
    int n_elems = 0, n_attrs = 0, n_types = 0;
    String targetNamespace;

    Iterator<ComparableElementDeclaration> iterElements;
    for (iterElements = elemDecls.iterator(); iterElements.hasNext();) {
      XSElementDeclaration xscElem = ((ComparableElementDeclaration)iterElements.next()).elementDeclaration;
      targetNamespace = roundify(xscElem.getNamespace());
      if (targetNamespace.equals(namespaceName))
        ++n_elems;
    }

    Iterator<XSObject> iter;
    for (iter = attrDecls.iterator(); iter.hasNext();) {
      XSAttributeDeclaration xscAttr = (XSAttributeDeclaration)iter.next();
      targetNamespace = roundify(xscAttr.getNamespace());
      if (targetNamespace.equals(namespaceName))
        ++n_attrs;
    }

    if (namespaceName == XMLSCHEMA_URI) {
      // builtin simple datatypes plus anyType
      n_types = m_xscBuiltinSTypes.length + 1;
    }
    else {
      for (iter = typeDecls.iterator(); iter.hasNext(); ) {
        XSTypeDefinition xscType = (XSTypeDefinition)iter.next();
        targetNamespace = roundify(xscType.getNamespace());
        if (targetNamespace.equals(namespaceName))
          ++n_types;
      }
    }

    int sz = EXISchemaLayout.SZ_NAMESPACE + n_elems + n_attrs + n_types;
    ensureNodes(sz);
    m_n_nodes += sz;

    m_nodes[nd]                          = EXISchema.NAMESPACE_NODE;
    m_nodes[nd + EXISchemaLayout.NAMESPACE_NAME] = internName(namespaceName);
    int serial = -1;
    for (i = 0; i < m_n_namespaces; i++) {
      if (m_namespaces[i] == namespaceName) {
        serial = i;
        break;
      }
    }
    assert serial >= 0 && serial < m_n_namespaces;
    m_nodes[nd + EXISchemaLayout.NAMESPACE_NUMBER] = serial;
    m_nodes[nd + EXISchemaLayout.NAMESPACE_N_ELEMS]              = n_elems;
    m_nodes[nd + EXISchemaLayout.NAMESPACE_N_ATTRS]              = n_attrs;
    m_nodes[nd + EXISchemaLayout.NAMESPACE_N_TYPES]              = n_types;

    base = nd + EXISchemaLayout.SZ_NAMESPACE;
    for (n = 0, iterElements = elemDecls.iterator(); iterElements.hasNext();) {
      XSElementDeclaration xscElem = ((ComparableElementDeclaration)iterElements.next()).elementDeclaration;
      targetNamespace = roundify(xscElem.getNamespace());
      if (targetNamespace.equals(namespaceName))
        m_nodes[base + n++] = getDoneNodeId(xscElem);
    }

    base += n_elems;
    for (n = 0, iter = attrDecls.iterator(); iter.hasNext();) {
      XSAttributeDeclaration xscAttr = (XSAttributeDeclaration)iter.next();
      targetNamespace = roundify(xscAttr.getNamespace());
      if (targetNamespace.equals(namespaceName))
        m_nodes[base + n++] = getDoneNodeId(xscAttr);
    }

    base += n_attrs;
    if (namespaceName == XMLSCHEMA_URI) {
      n = base;
      // xs:anyType and builtin simple datatypes
      assert m_anyType != EXISchema.NIL_NODE;
      m_nodes[n++] = m_anyType;
      for (i = 0; i < m_xscBuiltinSTypes.length; i++) {
        m_nodes[n++] = getDoneNodeId(m_xscBuiltinSTypes[i]);
      }
      assert n == base + n_types;
    }
    else {
      for (n = 0, iter = typeDecls.iterator(); iter.hasNext();) {
        XSTypeDefinition xscType = (XSTypeDefinition)iter.next();
        targetNamespace = roundify(xscType.getNamespace());
        if (targetNamespace.equals(namespaceName))
          m_nodes[base + n++] = getDoneNodeId(xscType);
      }
    }

    return nd;
  }

  /**
   * Report a SchemaCompilerException (warning) to the application through a handler.
   */
  private void reportSchemaCompilerWarning(EXISchemaFactoryException warning)
      throws EXISchemaFactoryException {
    try {
      if (m_compiler_errorHandler != null) {
        m_compiler_errorHandler.warning(warning);
      }
    }
    catch (EXISchemaFactoryException userexc) {
      warning.setException(userexc);
      throw warning;
    }
  }
  
  /**
   * Report a SchemaCompilerException to the application through a handler.
   */
  private void reportSchemaCompilerException(EXISchemaFactoryException exc,
                                             boolean isFatal)
      throws EXISchemaFactoryException {
    try {
      if (m_compiler_errorHandler != null) {
        if (isFatal)
          m_compiler_errorHandler.fatalError(exc);
        else
          m_compiler_errorHandler.error(exc);
      }
    }
    catch (EXISchemaFactoryException userexc) {
      exc.setException(userexc);
      throw exc;
    }
    finally {
      if (isFatal)
        m_foundFatalSchemaError = true;
    }
    if (isFatal)
      throw exc;
  }

  /////////////////////////////////////////////////////////////////////////
  // House-keeping procedures
  /////////////////////////////////////////////////////////////////////////

  private String getKeyForNamespaceName(String namespace) {
    String key = "{http://www.w3.org/XML/1998/namespace}:";
    if (namespace != null)
      key += namespace;
    return key.intern();
  }

  /**
   * Intern a node.
   * @return id of the node
   */
  private int internNode(Object node) {
    int nd = EXISchema.NIL_NODE;
    if (node != null) {
      if ((nd = getDoneNodeId(node)) == EXISchema.NIL_NODE) {
          nd = m_n_nodes;
          putDoneNodeId(node, nd);
      }
    }
    return nd;
  }

  /**
   * Returns the id of a node if it is recognized, otherwise returns NIL_NODE.
   */
  private int getDoneNodeId(Object node) {
    int id = EXISchema.NIL_NODE;
    if (node != null) {
      Integer val = m_doneNodes.get(node);
      if (val != null)
        id = val.intValue();
    }
    return id;
  }

  /**
   * Register a node if it has not yet been done so.
   */
  private void putDoneNodeId(Object node, int id) {
    if (m_doneNodes.get(node) == null) {
      Integer nd = new Integer(id);
      m_doneNodes.put(node, nd);
      m_doneNodesReverse.put(nd, node);
    }
  }

  /**
   * Returns the node corresponding to a node id.
   */
  private Object getDoneNode(int nd) {
    Object node = null;
    if (nd != EXISchema.NIL_NODE) {
      node = m_doneNodesReverse.get(new Integer(nd));
    }
    return node;
  }

  /////////////////////////////////////////////////////////////////////////
  // Conveniences
  /////////////////////////////////////////////////////////////////////////

//  private int getSubstitutablesCount(XSElementDeclaration elem) {
//    Set<XSElementDeclaration> substs = m_mapSubst.get(elem);
//    return substs != null ? substs.size() : 0;
//  }

  /**
   * Returns value constraint type of an element.
   * (One of CONSTRAINT_DEFAULT, CONSTRAINT_FIXED or CONSTRAINT_NONE defined
   *  in EXISchema.)
   */
  private static int getValueConstraintType(XSElementDeclaration elem) {
    return getValueConstraintType(elem.getConstraintType());
  }

  /**
   * Returns value constraint type of an attribute.
   * (One of CONSTRAINT_DEFAULT, CONSTRAINT_FIXED or CONSTRAINT_NONE defined
   *  in EXISchema.)
   */
  private static int getValueConstraintType(XSAttributeDeclaration attr) {
    return getValueConstraintType(attr.getConstraintType());
  }

  /**
   * Returns value constraint type of an attribute use.
   * (One of CONSTRAINT_DEFAULT, CONSTRAINT_FIXED or CONSTRAINT_NONE defined
   *  in EXISchema.)
   */
  private static int getValueConstraintType(XSAttributeUse attr) {
    return getValueConstraintType(attr.getConstraintType());
  }

  private static int getValueConstraintType(int xscConstraintType) {
    int constraint = EXISchema.CONSTRAINT_NONE;
    switch (xscConstraintType) {
      case XSConstants.VC_DEFAULT:
        constraint = EXISchema.CONSTRAINT_DEFAULT;
        break;
      case XSConstants.VC_FIXED:
        constraint = EXISchema.CONSTRAINT_FIXED;
        break;
    }
    return constraint;
  }

  /**
   * Returns the built-in serial number for an atomic simple type definition.
   * @param asType atomic simple type definition
   * @return one of the numbers defined in SConst
   */
  private static int getBuiltinSerialOfAtomicSimpleTypeDefinition(
      XSSimpleTypeDefinition asType) {
    switch (asType.getBuiltInKind()) {
      case XSConstants.STRING_DT:
        return EXISchemaConst.STRING_TYPE;
      case XSConstants.BOOLEAN_DT:
        return EXISchemaConst.BOOLEAN_TYPE;
      case XSConstants.FLOAT_DT:
        return EXISchemaConst.FLOAT_TYPE;
      case XSConstants.DOUBLE_DT:
        return EXISchemaConst.DOUBLE_TYPE;
      case XSConstants.DECIMAL_DT:
        return EXISchemaConst.DECIMAL_TYPE;
      case XSConstants.DURATION_DT:
        return EXISchemaConst.DURATION_TYPE;
      case XSConstants.DATETIME_DT:
        return EXISchemaConst.DATETIME_TYPE;
      case XSConstants.TIME_DT:
        return EXISchemaConst.TIME_TYPE;
      case XSConstants.DATE_DT:
        return EXISchemaConst.DATE_TYPE;
      case XSConstants.GYEARMONTH_DT:
        return EXISchemaConst.G_YEARMONTH_TYPE;
      case XSConstants.GYEAR_DT:
        return EXISchemaConst.G_YEAR_TYPE;
      case XSConstants.GMONTHDAY_DT:
        return EXISchemaConst.G_MONTHDAY_TYPE;
      case XSConstants.GDAY_DT:
        return EXISchemaConst.G_DAY_TYPE;
      case XSConstants.GMONTH_DT:
        return EXISchemaConst.G_MONTH_TYPE;
      case XSConstants.HEXBINARY_DT:
        return EXISchemaConst.HEXBINARY_TYPE;
      case XSConstants.BASE64BINARY_DT:
        return EXISchemaConst.BASE64BINARY_TYPE;
      case XSConstants.ANYURI_DT:
        return EXISchemaConst.ANYURI_TYPE;
      case XSConstants.QNAME_DT:
        return EXISchemaConst.QNAME_TYPE;
      case XSConstants.NOTATION_DT:
        return EXISchemaConst.NOTATION_TYPE;
      case XSConstants.NORMALIZEDSTRING_DT:
        return EXISchemaConst.NORMALIZED_STRING_TYPE;
      case XSConstants.TOKEN_DT:
        return EXISchemaConst.TOKEN_TYPE;
      case XSConstants.LANGUAGE_DT:
        return EXISchemaConst.LANGUAGE_TYPE;
      case XSConstants.NMTOKEN_DT:
        return EXISchemaConst.NMTOKEN_TYPE;
      case XSConstants.NAME_DT:
        return EXISchemaConst.NAME_TYPE;
      case XSConstants.NCNAME_DT:
        return EXISchemaConst.NCNAME_TYPE;
      case XSConstants.ID_DT:
        return EXISchemaConst.ID_TYPE;
      case XSConstants.IDREF_DT:
        return EXISchemaConst.IDREF_TYPE;
      case XSConstants.ENTITY_DT:
        return EXISchemaConst.ENTITY_TYPE;
      case XSConstants.INTEGER_DT:
        return EXISchemaConst.INTEGER_TYPE;
      case XSConstants.NONPOSITIVEINTEGER_DT:
        return EXISchemaConst.NON_POSITIVE_INTEGER_TYPE;
      case XSConstants.NEGATIVEINTEGER_DT:
        return EXISchemaConst.NEGATIVE_INTEGER_TYPE;
      case XSConstants.LONG_DT:
        return EXISchemaConst.LONG_TYPE;
      case XSConstants.INT_DT:
        return EXISchemaConst.INT_TYPE;
      case XSConstants.SHORT_DT:
        return EXISchemaConst.SHORT_TYPE;
      case XSConstants.BYTE_DT:
        return EXISchemaConst.BYTE_TYPE;
      case XSConstants.NONNEGATIVEINTEGER_DT:
        return EXISchemaConst.NON_NEGATIVE_INTEGER_TYPE;
      case XSConstants.UNSIGNEDLONG_DT:
        return EXISchemaConst.UNSIGNED_LONG_TYPE;
      case XSConstants.UNSIGNEDINT_DT:
        return EXISchemaConst.UNSIGNED_INT_TYPE;
      case XSConstants.UNSIGNEDSHORT_DT:
        return EXISchemaConst.UNSIGNED_SHORT_TYPE;
      case XSConstants.UNSIGNEDBYTE_DT:
        return EXISchemaConst.UNSIGNED_BYTE_TYPE;
      case XSConstants.POSITIVEINTEGER_DT:
        return EXISchemaConst.POSITIVE_INTEGER_TYPE;
      case XSConstants.ANYSIMPLETYPE_DT:
        return EXISchemaConst.ANY_SIMPLE_TYPE;
      default:
        assert false;
        break;
    }
    return EXISchemaConst.ANY_SIMPLE_TYPE;
  }
  
  /**
   * Estimate the size of a type.
   */
  private int computeTypeSize(XSTypeDefinition typeDefn) {
    int size = EXISchemaLayout.SZ_TYPE;
    if (typeDefn.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
      XSComplexTypeDefinition ctypeDefn = (XSComplexTypeDefinition)typeDefn;
      size += EXISchemaLayout.SZ_COMPLEX_TYPE;
      XSAttributeUse[] attributeUsesSorted = sortAttributeUses(ctypeDefn);
      m_sortedAttrUses.put(ctypeDefn, attributeUsesSorted);
      size += 2 * attributeUsesSorted.length;
      int contentType = ctypeDefn.getContentType();
      if (ctypeDefn.getParticle() != null &&
          (contentType == XSComplexTypeDefinition.CONTENTTYPE_ELEMENT ||
          contentType == XSComplexTypeDefinition.CONTENTTYPE_MIXED)) {
        size += EXISchemaLayout.SZ_PARTICLE;
      }
    }
    else {
      XSSimpleTypeDecl stypeDefn = (XSSimpleTypeDecl)typeDefn;
      size += EXISchemaLayout.SZ_SIMPLE_TYPE;
      final int[] restrictedCharset = getPattern(stypeDefn); 
      size += restrictedCharset != null ? restrictedCharset.length : 0;
      size += stypeDefn.getActualEnumeration().getLength();
      size += getMemberTypes(stypeDefn).length;
    }
    return size;
  }

  private int[] getPattern(XSSimpleTypeDecl simpleTypeDefn) {
    if (!"http://www.w3.org/2001/XMLSchema".equals(simpleTypeDefn.getNamespace())) {
      final StringList patternList = simpleTypeDefn.getLexicalPattern();
      if (patternList.getLength() > 0) {
        final String pattern = patternList.item(0);
        try {
          return Regexi.compute(pattern);
        }
        catch (RegexSyntaxException rse) {
        }
      }
    }
    return null;
  }

  private XSSimpleTypeDefinition[] getMemberTypes(XSSimpleTypeDefinition simpleTypeDefn) {
    XSObjectList objectList  = simpleTypeDefn.getMemberTypes();
    final int len = objectList.getLength();
    XSSimpleTypeDefinition[] memberTypes = new XSSimpleTypeDefinition[len];
    for (int i = 0; i < len; i++) {
      memberTypes[i] = (XSSimpleTypeDefinition)objectList.item(i);
    }
    return memberTypes;
  }

  private static final boolean isGlobalElement(XSElementDeclaration xscElem) {
    return xscElem.getScope() == XSConstants.SCOPE_GLOBAL;
  }

  private static final boolean isGlobalAttribute(XSAttributeDeclaration xscAttr) {
    return xscAttr.getScope() == XSConstants.SCOPE_GLOBAL;
  }

  /**
   * Returns one of:
   * EXISchema.CONTENT_EMPTY, XSCComplexTypeDefinition.SIMPLETYPE,
   * EXISchema.CONTENT_ELEMENT_ONLY, EXISchema.CONTENT_MIXED
   */
  private static final int getContentClass(XSComplexTypeDefinition xscCType) {
    switch (xscCType.getContentType()) {
      case XSComplexTypeDefinition.CONTENTTYPE_EMPTY:
        return EXISchema.CONTENT_EMPTY;
      case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
        return EXISchema.CONTENT_SIMPLE;
      case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
        return EXISchema.CONTENT_ELEMENT_ONLY;
      case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
        return EXISchema.CONTENT_MIXED;
      default:
        return EXISchema.CONTENT_INVALID;
    }
  }

  /**
   * Returns one of:
   * EXISchema.CONTENT_EMPTY, XSCComplexTypeDefinition.SIMPLETYPE,
   * EXISchema.CONTENT_ELEMENT_ONLY, EXISchema.CONTENT_MIXED
   */
  private static final int getContentClass(XSElementDeclaration xscElem) {
    XSTypeDefinition xscType;
    if ((xscType = xscElem.getTypeDefinition()) != null) {
      if (xscType.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE)
        return getContentClass((XSComplexTypeDefinition)xscType);
      else
        return EXISchema.CONTENT_SIMPLE;
    }
    return EXISchema.CONTENT_MIXED;
  }

  private final XSTypeDefinition getBaseType(XSTypeDefinition xscType) {
    if (xscType.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
      switch (((XSSimpleTypeDecl)xscType).getVariety()) {
        case XSSimpleTypeDefinition.VARIETY_LIST:
        case XSSimpleTypeDefinition.VARIETY_UNION:
          return m_xscAnySimpleType;        
      }
    }
    if (XMLSCHEMA_URI.equals(xscType.getNamespace())) {
      // for xerces
      if ("negativeInteger".equals(xscType.getName())) {
        return m_xsdSchema.getTypeDefinition("nonPositiveInteger");
      }
      else if ("anySimpleType".equals(xscType.getName())) {
        return m_xsdSchema.getTypeDefinition("anyType");
      }
    }
    return xscType.getBaseType();
  }
  
  /////////////////////////////////////////////////////////////////////////
  // Utilities
  /////////////////////////////////////////////////////////////////////////

  private static String roundify(String str) {
    return str != null ? str : "";
  }

  private static boolean isBuiltIn(XSSimpleTypeDefinition simpleType) {
    if (XMLSCHEMA_URI.equals(simpleType.getNamespace())) {
      final String name = simpleType.getName();
      int i;
      final int len;
      for (i = 0, len = BUILTIN_SIMPLE_TYPE_NAMES.length; i < len; i++) {
        if (BUILTIN_SIMPLE_TYPE_NAMES[i].equals(name))
          return true;
      }
    }
    return false;
  }
  
  /**
   * Adapts DOM ErrorHandler for use with EXISchemaFactoryErrorHandler
   */
  private class DOMErrorAdapter implements DOMErrorHandler {

    public boolean handleError(DOMError de) {
      switch (de.getSeverity()) {
        case DOMError.SEVERITY_WARNING:
          return handleError(de, false, false);
        case DOMError.SEVERITY_FATAL_ERROR:
          return handleError(de, true, true);
        default:
          assert de.getSeverity() == DOMError.SEVERITY_ERROR;
          return handleError(de, true, false);
      }
    }
    
    private boolean handleError(DOMError de, boolean isError, boolean isFatal) {
      EXISchemaFactoryException sce = new EXISchemaFactoryException(
          EXISchemaFactoryException.SCHEMAPARSE_ERROR,
          new String[] { de.getMessage() },
          toLocator(de));
      Object object;
      if ((object = de.getRelatedException()) instanceof Exception) {
        sce.setException((Exception)object);
      }
      try {
        if (isError) {
          if (sce.getMessage().startsWith("cos-nonambig:")) {
            isFatal = true; // treat UPA violations as fatal
          }
          reportSchemaCompilerException(sce, isFatal);
        }
        else
          reportSchemaCompilerWarning(sce);
      }
      catch (EXISchemaFactoryException user) {
        return false;
      }
      return true;
    }
    
    private Locator toLocator(DOMError de) {
      LocatorImpl locator = new LocatorImpl();
      locator.setSystemId(de.getLocation().getUri());
      locator.setPublicId((String)null);
      locator.setLineNumber(de.getLocation().getLineNumber());
      locator.setColumnNumber(de.getLocation().getColumnNumber());
      return locator;
    }
  }

  private static class ComparableAttributeUse implements Comparable<ComparableAttributeUse> {
    final XSAttributeDeclaration attrdecl;
    final XSAttributeUse attruse;
    ComparableAttributeUse(XSAttributeUse attributeUse) {
        attruse = attributeUse;
        attrdecl = attruse.getAttrDeclaration();
    }
    public int compareTo(ComparableAttributeUse other) {
      int res;
      if ((res = attrdecl.getName().compareTo(other.attrdecl.getName())) != 0)
          return res;
      else {
        String thisNamespace;
        String thatNamespace;
        thisNamespace = roundify(attrdecl.getNamespace());
        thatNamespace = roundify(other.attrdecl.getNamespace());
        res = thisNamespace.compareTo(thatNamespace);
        return res;
      }
    }
  }
  
  private static class ComparableElement implements Comparable {
    protected String m_name;
    protected String m_uri;
    public int compareTo(Object obj) {
      ComparableElement ced = (ComparableElement)obj;
      int res;
      if ((res = m_name.compareTo(ced.m_name)) > 0)
        return 1;
      else if (res < 0)
        return -1;
      else {
        if ((res = m_uri.compareTo(ced.m_uri)) > 0)
          return 1;
        else if (res < 0)
          return -1;
        else {
          return 0;
        }
      }
    }
  }
  
  private static class ComparableElementDeclaration extends ComparableElement {
    final XSElementDeclaration elementDeclaration;
    ComparableElementDeclaration(XSElementDeclaration ed) {
      elementDeclaration = ed;
      m_name = ed.getName();
      m_uri = roundify(ed.getNamespace());
    }
    @Override
    public int compareTo(Object obj) {
      final int val = super.compareTo(obj);
      assert val != 0;
      return val;
    }
  }

  private static class ComparableINode extends ComparableElement {
    final int nd;
    boolean isSpecific;
    private ArrayList<ComparableINode> m_isotopes;
    private static final ArrayList<ComparableINode> noIsotopes = new ArrayList<ComparableINode>(); 
    ComparableINode(int inode, int[] nodes, String[] names) {
      nd = inode;
      String uri;
      if (nodes[inode] == EXISchema.ELEMENT_NODE) {
        m_name = EXISchema._getNameOfElem(nd, nodes, names);
        uri = EXISchema._getTargetNamespaceNameOfElem(nd, nodes, names);
      }
      else {
        assert nodes[inode] == EXISchema.ATTRIBUTE_NODE;
        m_name = EXISchema._getNameOfAttr(nd, nodes, names);
        uri = EXISchema._getTargetNamespaceNameOfAttr(nd, nodes, names);
      }
      m_uri = uri != null ? uri : "";
      isSpecific = true;
      m_isotopes = null;
    }
    void addIsotope(ComparableINode isotope) {
      if (m_isotopes == null) {
        m_isotopes = new ArrayList<ComparableINode>();
      }
      m_isotopes.add(isotope);
    }
    ArrayList<ComparableINode> getIsotopes() {
      return m_isotopes != null ? m_isotopes : noIsotopes;
    }
  }

  public static void main(String args[])
      throws IOException {

    if (args.length < 2) {
      System.err.println("USAGE: " + EXISchemaFactory.class.getName() +
                         " <XMLSchema File> [Output File]");
      System.exit(1);
      return;
    }

    URI baseURI = new File(System.getProperty("user.dir")).
        toURI().resolve("whatever");

    URI schemaUri;
    try {
      schemaUri = URIHelper.resolveURI(args[0], baseURI);
    }
    catch (URISyntaxException use) {
      System.err.println("'" + args[0] + "' is not a valid URI.");
      System.exit(1);
      return;
    }
    assert schemaUri != null;
    int lastPos = schemaUri.toString().lastIndexOf('/');
    String schemaName = schemaUri.toString().substring(lastPos + 1);
    String baseName;

    URI xscUri;
    if (args.length > 1) {
      try {
        xscUri = URIHelper.resolveURI(args[1], baseURI);
      }
      catch (URISyntaxException use) {
        System.err.println("'" + args[1] + "' is not a valid URI.");
        System.exit(1);
        return;
      }
    }
    else {
      baseName = schemaName;
      if ((lastPos = schemaName.lastIndexOf('.')) > 0) {
        baseName = schemaName.substring(0, lastPos);
      }
      xscUri = schemaUri.resolve(baseName + ".xsc");
    }
    assert xscUri != null;

    InputSource inputSource = new InputSource(schemaUri.toString());

    EXISchemaFactory schemaCompiler = new EXISchemaFactory();
    schemaCompiler.setCompilerErrorHandler(new EXISchemaFactoryErrorHandler() {
      public void warning(EXISchemaFactoryException exc)
        throws EXISchemaFactoryException {
        System.err.println("Warning: " + exc.getMessage());
      }
      public void error(EXISchemaFactoryException exc) throws
          EXISchemaFactoryException {
        System.err.println("Error: " + exc.getMessage());
      }
      public void fatalError(EXISchemaFactoryException exc) throws
          EXISchemaFactoryException {
        System.err.println("Fatal Error:" + exc.getMessage());
      }
    });
    inputSource.setByteStream(schemaUri.toURL().openStream());
    EXISchema corpus;
    try {
      corpus = schemaCompiler.compile(inputSource);
    }
    catch (EXISchemaFactoryException sce) {
      System.err.println(sce.getMessage());
      sce.printStackTrace();
      System.exit(1);
      return;
    }

    FileOutputStream fos;
    fos = new FileOutputStream(xscUri.toURL().getFile());
    ObjectOutputStream oos = new ObjectOutputStream(fos);
    oos.writeObject(corpus);
    oos.close();
    fos.close();

    URI disUri;
    lastPos = xscUri.toString().lastIndexOf('/');
    String xscName = xscUri.toString().substring(lastPos + 1);
    baseName = xscName;
    if ((lastPos = xscName.lastIndexOf('.')) > 0) {
      baseName = xscName.substring(0, lastPos);
    }
    disUri = xscUri.resolve(baseName + ".dis");
    assert disUri != null;

//    fos = new FileOutputStream(disUri.toURL().getFile());
//    corpus.writeXDISchema(fos);
//    fos.close();
    
    File file = new File(xscUri);
    System.out.println(file.getParentFile().toURI().relativize(xscUri).toString() + " : " +
        corpus.getTotalElemCount() + "(elements) " +
        corpus.getTotalAttrCount() + "(attributes) " +
        corpus.getTotalGroupCount() + "(groups)"); 
  }

}
