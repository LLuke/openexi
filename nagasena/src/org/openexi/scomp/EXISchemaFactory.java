package org.openexi.scomp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.LocatorImpl;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import org.apache.xerces.xs.*;
import org.apache.xerces.xs.datatypes.*;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.dv.DatatypeException;
import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.impl.xs.util.LSInputListImpl;
import org.apache.xerces.impl.xs.util.XSGrammarPool;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.grammars.XMLSchemaDescription;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLInputSource;

import org.openexi.proc.io.DateTimeValueScriber;
import org.openexi.proc.io.DateValueScriber;
import org.openexi.proc.io.DecimalValueScriber;
import org.openexi.proc.io.FloatValueScriber;
import org.openexi.proc.io.GDayValueScriber;
import org.openexi.proc.io.GMonthDayValueScriber;
import org.openexi.proc.io.GMonthValueScriber;
import org.openexi.proc.io.GYearMonthValueScriber;
import org.openexi.proc.io.GYearValueScriber;
import org.openexi.proc.io.Scribble;
import org.openexi.proc.io.Scriber;
import org.openexi.proc.io.TimeValueScriber;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.EXISchemaLayout;

import com.thaiopensource.exi.xsd.regex.RegexSyntaxException;
import com.thaiopensource.exi.xsd.regex.jdk1_4.Regexi;

/**
 * EXISchemaFactory compiles XML Schema into an EXISchema instance.
 */
public class EXISchemaFactory extends EXISchemaStruct {

  private final HashMap<XSObject,NodeUse> m_doneNodes; // schema node (Object) -> nodeID (Integer)
  private final Map<Integer,XSElementDeclaration> m_doneElementsReverse; // nodeID (Integer) -> schema element node 
  private final Map<Integer,XSTypeDefinition> m_doneTypeReverse; // nodeID (Integer) -> schema type node 
  // XSElementDeclaration -> Set (of its substitutables)
  private final Map<XSElementDeclaration, Set<XSElementDeclaration>> m_mapSubst;
  private final HashSet<XSTypeDefinition> m_subTypableTypes;
  
  private final EventTypeCache m_eventTypeCache;
  
  private final Scribble m_scribble;
  
  private static final String[] BUILTIN_SIMPLE_TYPE_NAMES;
  static {
    BUILTIN_SIMPLE_TYPE_NAMES = new String[] {
      /* NEVER CHANGE ORDER! all the order as listed in EXISchemaConst. */
      // primitive types
      "anySimpleType",
      "string", "boolean", "decimal", "float", "double", "duration", "dateTime",
      "time",  "date", "gYearMonth", "gYear", "gMonthDay", "gDay", "gMonth",
      "hexBinary", "base64Binary", "anyURI", "QName", "NOTATION",
      // non-primitive atomic types - decimal-derived
      "integer", "nonNegativeInteger", "unsignedLong", "positiveInteger",
      "nonPositiveInteger", "negativeInteger", "int", "short", "byte",
      "unsignedShort", "unsignedByte", "long", "unsignedInt",
      // non-primitive atomic types - string-derived
      "normalizedString", "token", "language", "Name", "NCName", "NMTOKEN",
      "ENTITY", "IDREF", "ID",
      // list types
      "ENTITIES", "IDREFS", "NMTOKENS"
    };
    assert EXISchemaConst.N_BUILTIN_TYPES == BUILTIN_SIMPLE_TYPE_NAMES.length + 1;
  }
  private final XSSimpleTypeDefinition[] m_xscBuiltinSTypes;
  private int m_anyType;
  private XSTypeDefinition m_xscAnySimpleType;
  
  private final DOMErrorAdapter m_domErrorAdapter;
  private final EntityResolverAdapter m_entityResolverAdapter;

  private EXISchemaFactoryErrorHandler m_compiler_errorHandler;
  private EXISchemaFactoryException m_schemaFactoryException;

  private XSNamespaceItem m_xsdSchema;

  // REVISIT: may need to reset?
  private DOMImplementationLS m_domImplementationLS;

  // 
  /**
   * Singleton grammar for simple types.
   * m_gramSimpleType is based on the four proto grammars.
   */
  private int m_gramSimpleType;
  private static ProtoGrammar m_simpleTypeGrammar, m_simpleTypeContentGrammar; 
  private static ProtoGrammar m_simpleTypeEmptyGrammar, m_emptyContentGrammar;
  
  private final ArrayList<ProtoGrammar> m_syntheticGrammarRegistry = new ArrayList<ProtoGrammar>();
  int protoGrammarSerial; // proto-grammar serial number (should start with m_startSerialNumber)
  private static final int m_startSerialNumber;
  static {
    // Generate proto grammars for simple type
    int serialNumber = 0;
    m_emptyContentGrammar = new ProtoGrammar(serialNumber++, (EXISchemaFactory)null);
    m_emptyContentGrammar.addSubstance(new Goal(m_emptyContentGrammar), null);
    
    m_simpleTypeContentGrammar = new ProtoGrammar(serialNumber++, (EXISchemaFactory)null);
    m_simpleTypeContentGrammar.addSubstance(new Production(
        EventTypeCache.eventCharactersTyped, m_emptyContentGrammar), null);
    m_simpleTypeContentGrammar.importGoals(m_emptyContentGrammar);

    m_simpleTypeGrammar = new ProtoGrammar(serialNumber++, (EXISchemaFactory)null);
    m_simpleTypeGrammar.setIndex(0);
    Substance[] contentSubstances = m_simpleTypeContentGrammar.getSubstances();
    final int n_contentSubstances = contentSubstances.length;
    for (int i = 0; i < n_contentSubstances; i++) {
      m_simpleTypeGrammar.addSubstance(contentSubstances[i], null);
    }
    m_simpleTypeGrammar.importGoals(m_simpleTypeContentGrammar);
    
    m_simpleTypeEmptyGrammar = new ProtoGrammar(serialNumber++, (EXISchemaFactory)null);
    m_simpleTypeEmptyGrammar.setIndex(0);
    Substance[] emptyContentSubstances = m_emptyContentGrammar.getSubstances();
    final int n_emptyContentSubstances = emptyContentSubstances.length;
    for (int i = 0; i < n_emptyContentSubstances; i++) {
      m_simpleTypeEmptyGrammar.addSubstance(emptyContentSubstances[i], null);
    }
    m_simpleTypeEmptyGrammar.importGoals(m_emptyContentGrammar);
    
    m_startSerialNumber = serialNumber;
  }

  private static final String W3C_2001_XMLSCHEMA_URI = "http://www.w3.org/2001/XMLSchema";
  private static final String W3C_2001_XMLSCHEMA_INSTANCE_URI = "http://www.w3.org/2001/XMLSchema-instance";
  private static final String W3C_XML_1998_URI = "http://www.w3.org/XML/1998/namespace";
  
  public EXISchemaFactory()  {
    m_doneNodes = new HashMap<XSObject,NodeUse>();
    m_doneElementsReverse = new HashMap<Integer,XSElementDeclaration>();
    m_doneTypeReverse = new HashMap<Integer,XSTypeDefinition>();
    m_mapSubst = new HashMap<XSElementDeclaration, Set<XSElementDeclaration>>();
    m_subTypableTypes  = new HashSet<XSTypeDefinition>();
    m_xscBuiltinSTypes = new XSSimpleTypeDefinition[BUILTIN_SIMPLE_TYPE_NAMES.length];
    m_eventTypeCache = new EventTypeCache();
    m_domErrorAdapter = new DOMErrorAdapter();
    m_entityResolverAdapter = new EntityResolverAdapter();
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
    m_scribble = new Scribble();
  }

  /**
   * Prepares the compiler for the next compilation run.
   * @y.exclude
   */
  @Override
  protected void reset() {
    // initialize the arrays
    super.reset();

    // other variables

    m_anyType = EXISchema.NIL_NODE;
    m_xscAnySimpleType = null;

    m_xsdSchema = null;
    m_gramSimpleType = EXISchema.NIL_GRAM;
  }

  /**
   * Releases resources that was allocated in the previous compilation.
   * @y.exclude
   */
  @Override
  protected void clear() {
    // delete the arrays
    super.clear();

    // clear maps, sets, etc.
    m_doneNodes.clear();
    m_doneElementsReverse.clear();
    m_doneTypeReverse.clear();
    m_mapSubst.clear();
    m_subTypableTypes.clear();
    m_eventTypeCache.clear();
    m_scribble.clear();
  }

  /**
   * Set an error handler to report any errors encountered during
   * schema compilation.
   * @param errorHandler Error handler
   */
  public void setCompilerErrorHandler(EXISchemaFactoryErrorHandler errorHandler) {
    m_compiler_errorHandler = errorHandler;
  }

  /**
   * Set an entity resolver for use to resolve entities and schema documents.
   * @param entityResolverEx extended SAX entity resolver
   */
  public void setEntityResolver(EntityResolverEx entityResolver) {
    m_entityResolverAdapter.setEntityResolver(entityResolver);
  }

  /**
   * Compile an XML Schema Document into an EXISchema.
   * @param is XML Schema Document stream from an InputSource
   * @return an EXISchema instance
   * @throws IOException
   * @throws EXISchemaFactoryException
   */
  public final EXISchema compile(InputSource inputSource) 
      throws IOException, EXISchemaFactoryException {
    return compile(inputSource != null ? new InputSource[] { inputSource } : new InputSource[0]);
  }

  final EXISchema compile(InputSource[] inputSources)
      throws IOException, EXISchemaFactoryException {
    protoGrammarSerial = m_startSerialNumber;
    final int n_inputSources;
    final LSInput[] lsInputArray;
    if (inputSources.length != 0) {
      lsInputArray = new LSInput[n_inputSources = inputSources.length];
      for (int i = 0; i < n_inputSources; i++) {
        final LSInput lsInput = m_domImplementationLS.createLSInput();
        InputSource is = inputSources[i];
        lsInput.setSystemId(is.getSystemId());
        lsInput.setPublicId(is.getPublicId());
        lsInput.setByteStream(is.getByteStream());
        lsInput.setCharacterStream(is.getCharacterStream());
        lsInput.setEncoding(is.getEncoding());
        lsInputArray[i] = lsInput;
      }
    }
    else {
      lsInputArray = new LSInput[n_inputSources = 1];
      final LSInput lsInput = m_domImplementationLS.createLSInput();
      URL url = getClass().getResource("XMLSchema-empty.xsd");
      lsInput.setByteStream(url.openStream());
      lsInputArray[0] = lsInput;
    }
    LSInputList lsInputList = new LSInputListImpl(lsInputArray, n_inputSources);

    m_schemaFactoryException = null;
    final XMLSchemaLoader schemaLoader = new XMLSchemaLoader();
    schemaLoader.setEntityResolver(m_entityResolverAdapter);
    schemaLoader.setProperty(XMLSchemaLoader.XMLGRAMMAR_POOL, new XSGrammarPool());
    schemaLoader.setParameter(Constants.DOM_ERROR_HANDLER, m_domErrorAdapter);
    schemaLoader.setParameter("http://apache.org/xml/features/honour-all-schemaLocations", Boolean.TRUE);
    schemaLoader.setParameter("http://apache.org/xml/features/validation/schema-full-checking", Boolean.TRUE);
    final XSModel model = schemaLoader.loadInputList(lsInputList);
    if (m_schemaFactoryException != null && m_compiler_errorHandler == null) {
      throw m_schemaFactoryException;
    }
    if (model != null) {
      reset();

      // preprocess top-level schemas
      final IntHolder n_globalElems = new IntHolder();
      final IntHolder n_globalAttrs = new IntHolder();
      Set<XSElementDeclaration> elemDecls = new HashSet<XSElementDeclaration>();
      Set<XSAttributeDeclaration> attrDecls = new HashSet<XSAttributeDeclaration>();
      Set<XSTypeDefinition> typeDecls = new HashSet<XSTypeDefinition>();
      preProcessSchema(model.getNamespaceItems(), n_globalElems, n_globalAttrs, elemDecls, attrDecls, typeDecls);

      // compile Xerces schema into EXISchema
      doSchema(n_globalElems.value, n_globalAttrs.value, elemDecls, attrDecls, typeDecls);
      postProcessSchema();
      return new EXISchema(m_elems, m_n_elems,
                            m_attrs, m_n_attrs, m_types, m_n_types,
                            m_uris, m_n_uris,
                            m_names, m_n_names, m_localNames,
                            m_strings, m_n_strings,
                            m_ints, m_n_ints,
                            m_mantissas, m_exponents, m_n_floats,
                            m_signs, m_integralDigits, m_reverseFractionalDigits, m_n_decimals,
                            m_integers, m_n_integers,
                            m_longs, m_n_longs,
                            m_datetimes, m_n_datetimes,
                            m_durations, m_n_durations,
                            m_binaries, m_n_binaries,
                            m_variantTypes, m_variants, m_n_variants,
                            m_grammars, m_n_grammars, m_grammarCount,
                            m_productions, m_n_productions,
                            m_eventTypes, m_eventData, m_n_events,
                            m_n_stypes);
    }
    return null;
  }

  /////////////////////////////////////////////////////////////////////////
  // Compiler implementation
  /////////////////////////////////////////////////////////////////////////

  /**
   * Pre-process Schemas
   * @param globalElemDecls initially empty, will be populated
   * @param globalAttrDecls initially empty, will be populated
   * @param globalTypeDecls initially empty, will be populated
   */
  private void preProcessSchema(XSNamespaceItemList namespaceItemList,
                                IntHolder n_globalElems, IntHolder n_globalAttrs,
                                Set<XSElementDeclaration> elemDecls,
                                Set<XSAttributeDeclaration> attrDecls,
                                Set<XSTypeDefinition> typeDecls) {

    final Set<XSNamespaceItem> namespaceItems = new HashSet<XSNamespaceItem>(); 
    for (int i = 0; i < namespaceItemList.getLength(); i++) {
      namespaceItems.add(namespaceItemList.item(i));
    }

    final SortedSet<String> uris = new TreeSet<String>();

    Iterator<XSNamespaceItem> namespaceItemIterator;
    
    namespaceItemIterator = namespaceItems.iterator();
    while (namespaceItemIterator.hasNext()) {
      final XSNamespaceItem xscSchema = namespaceItemIterator.next();
      final String uri = xscSchema.getSchemaNamespace();
      uris.add(roundify(uri));
      if (W3C_2001_XMLSCHEMA_URI.equals(uri)) {
        m_xsdSchema = xscSchema;
      }
    }
    
    namespaceItemIterator = namespaceItems.iterator();
    while (namespaceItemIterator.hasNext()) {
      final XSNamespaceItem xscSchema = namespaceItemIterator.next();

      XSNamedMap xscElems = xscSchema.getComponents(XSConstants.ELEMENT_DECLARATION);
      XSNamedMap xscAttrs = xscSchema.getComponents(XSConstants.ATTRIBUTE_DECLARATION);
      XSNamedMap xscTypes = xscSchema.getComponents(XSConstants.TYPE_DEFINITION);
      
      int n_elems  = xscElems.getLength();
      int n_attrs  = xscAttrs.getLength();
      int n_types  = xscTypes.getLength();
      
      n_globalElems.value += n_elems;
      n_globalAttrs.value += n_attrs;

      for (int j = 0; j < n_elems; j++) {
        final XSElementDeclaration elem = (XSElementDeclaration)xscElems.item(j);
        scanElement(elem, elemDecls, attrDecls, typeDecls, uris);
        // Add the element to the substitution group if it belongs to any.
        // Note that substitutability is transitional.
        XSElementDeclaration subst = elem.getSubstitutionGroupAffiliation();
        for (; subst != null && subst != elem; subst = subst.getSubstitutionGroupAffiliation()) {
          Set<XSElementDeclaration> elems;
          if ((elems = m_mapSubst.get(subst)) == null)
            m_mapSubst.put(subst, elems = new HashSet<XSElementDeclaration>());
          elems.add(elem);
        }
      }
      for (int j = 0; j < n_attrs; j++) {
        final XSAttributeDeclaration attr = (XSAttributeDeclaration)xscAttrs.item(j);
        scanAttribute(attr, elemDecls, attrDecls, typeDecls, uris);
      }
      for (int j = 0; j < n_types; j++) {
        XSTypeDefinition typeDefinition = (XSTypeDefinition)xscTypes.item(j);
        assert typeDefinition.getName().length() != 0; // i.e. is a global type
        scanType(typeDefinition, elemDecls, attrDecls, typeDecls, uris);
        final XSTypeDefinition base = getBaseType(typeDefinition);
        // Only global types can be considered typable.
        if (base != null && base != typeDefinition && base.getName() != null)
          m_subTypableTypes.add(base);
      }
    }

    final Iterator<String> uriIterator = uris.iterator();
    while (uriIterator.hasNext())
      internUri(uriIterator.next());
    
    final HashMap<String,TreeSet<String>> allLocalNames = new HashMap<String,TreeSet<String>>();
    final TreeSet<String> xmlLocalNames = new TreeSet<String>();
    final TreeSet<String> xsiLocalNames = new TreeSet<String>();
    final TreeSet<String> xsdLocalNames = new TreeSet<String>();
    for (int i = 0; i < EXISchemaConst.XML_LOCALNAMES.length; i++)
      xmlLocalNames.add(EXISchemaConst.XML_LOCALNAMES[i]);
    for (int i = 0; i < EXISchemaConst.XSI_LOCALNAMES.length; i++)
      xsiLocalNames.add(EXISchemaConst.XSI_LOCALNAMES[i]);
    for (int i = 0; i < EXISchemaConst.XSD_LOCALNAMES.length; i++)
      xsdLocalNames.add(EXISchemaConst.XSD_LOCALNAMES[i]);
    allLocalNames.put(W3C_XML_1998_URI, xmlLocalNames);
    allLocalNames.put(W3C_2001_XMLSCHEMA_INSTANCE_URI, xsiLocalNames);
    allLocalNames.put(W3C_2001_XMLSCHEMA_URI, xsdLocalNames);

    final Iterator<XSElementDeclaration> iterElements = elemDecls.iterator();
    while (iterElements.hasNext()) {
      final XSElementDeclaration elem = iterElements.next();
      final String uri = roundify(elem.getNamespace());
      final String name = elem.getName();
      assert name != null;
      // Add name to a localName partition
      TreeSet<String> localNames;
      if ((localNames = allLocalNames.get(uri)) == null) {
        localNames = new TreeSet<String>();
        allLocalNames.put(uri, localNames);
      }
      localNames.add(name);
      internName(name);
    }

    final Iterator<XSAttributeDeclaration> iterAttributes;
    iterAttributes = attrDecls.iterator();
    while (iterAttributes.hasNext()) {
      final XSAttributeDeclaration attr = iterAttributes.next();
      final String uri = roundify(attr.getNamespace());
      final String name = attr.getName();
      assert name != null;
      if (!W3C_2001_XMLSCHEMA_INSTANCE_URI.equals(uri) || 
          !"schemaLocation".equals(name) && !"noNamespaceSchemaLocation".equals(name)) {
        // Add name to a localName partition
        TreeSet<String> localNames;
        if ((localNames = allLocalNames.get(uri)) == null) {
          localNames = new TreeSet<String>();
          allLocalNames.put(uri, localNames);
        }
        localNames.add(name);
        internName(name);
      }
    }
    
    final Iterator<XSTypeDefinition> iterTypes;
    iterTypes = typeDecls.iterator();
    while (iterTypes.hasNext()) {
      final XSTypeDefinition typeDefinition = iterTypes.next();
      final String name = typeDefinition.getName();
      if (name != null) {
        final String uri = roundify(typeDefinition.getNamespace());
        // Add name to a localName partition
        TreeSet<String> localNames;
        if ((localNames = allLocalNames.get(uri)) == null) {
          localNames = new TreeSet<String>();
          allLocalNames.put(uri, localNames);
        }
        localNames.add(name);
        internName(name);
      }
    }

    m_localNames = new int[m_n_uris][];
    for (int i = 0; i < m_n_uris; i++) {
      final String uri = m_uris[i];
      TreeSet<String> localNames;
      int[] localNamesArray;
      if ((localNames = allLocalNames.get(uri)) != null) {
        int n_localNames = localNames.size();
        localNamesArray = new int[n_localNames];
        Iterator<String> iterlocalNames = localNames.iterator();
        int j;
        for (j = 0; iterlocalNames.hasNext(); j++) {
          final int nm = indexOfName(iterlocalNames.next());
          assert nm != -1;
          localNamesArray[j] = nm;
        }
      }
      else {
        localNamesArray = new int[0];
      }
      m_localNames[i] = localNamesArray;
    }
  }

  private void doSchema(int n_globalElems, int n_globalAttrs,
      Set<XSElementDeclaration> elemDecls,
      Set<XSAttributeDeclaration> attrDecls,
      Set<XSTypeDefinition> typeDecls) {

    final SortedSet<ComparableSubstance> sortedElemDecls = new TreeSet<ComparableSubstance>();
    final Iterator<XSElementDeclaration> iterElements = elemDecls.iterator();
    for (int i = 0; iterElements.hasNext(); i++) {
      sortedElemDecls.add(new ComparableSubstance(iterElements.next(), i));
    }

    final SortedSet<ComparableSubstance> sortedAttributes = new TreeSet<ComparableSubstance>();
    Iterator<XSAttributeDeclaration> iterAttributes = attrDecls.iterator();
    for (int i = 0; iterAttributes.hasNext(); i++) {
      sortedAttributes.add(new ComparableSubstance(iterAttributes.next(), i));
    }

    // bootstrap elements (both globals and locals)
    final Iterator<ComparableSubstance> iterSortedElements;
    for (iterSortedElements = sortedElemDecls.iterator(); iterSortedElements.hasNext();) {
      final XSElementDeclaration elementDeclaration = (XSElementDeclaration)(iterSortedElements.next().substance); 
      bootElement(elementDeclaration);
    }
    
    // bootstrap attributes (both globals and locals)
    final Iterator<ComparableSubstance> iterSortedAttributes;
    for (iterSortedAttributes = sortedAttributes.iterator(); iterSortedAttributes.hasNext();) {
      final XSAttributeDeclaration attributeDeclaration = (XSAttributeDeclaration)iterSortedAttributes.next().substance; 
      bootAttribute(attributeDeclaration);
    }

    XSTypeDefinition xscAnyType = m_xsdSchema.getTypeDefinition("anyType");
    m_anyType = bootType(xscAnyType);
    m_types[m_anyType + EXISchemaLayout.TYPE_NUMBER] = 0; // xsd:anyType has serial number "0" 
    assert typeDecls.contains(xscAnyType);
    typeDecls.remove(xscAnyType);
    
    // pre-allocate space for builtin simple types
    for (int i = 0; i < BUILTIN_SIMPLE_TYPE_NAMES.length; i++)  {
      final XSSimpleTypeDefinition builtinSimpleType;
      builtinSimpleType = (XSSimpleTypeDefinition)m_xsdSchema.getTypeDefinition(BUILTIN_SIMPLE_TYPE_NAMES[i]);
      m_xscBuiltinSTypes[i] = builtinSimpleType;
      bootType(builtinSimpleType);
      assert typeDecls.contains(builtinSimpleType);
      typeDecls.remove(builtinSimpleType);
    }
    m_xscAnySimpleType = m_xscBuiltinSTypes[0];
    assert "anySimpleType".equals(m_xscAnySimpleType.getName());
    
    Iterator<XSTypeDefinition> iterTypes;
    iterTypes = typeDecls.iterator();
    while (iterTypes.hasNext()) { // pre-allocate space for simple types
      final XSTypeDefinition typeDefinition = iterTypes.next();
      if (typeDefinition.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE)
        bootType(typeDefinition);
    }
    iterTypes = typeDecls.iterator();
    while (iterTypes.hasNext()) { // pre-allocate space for complex types
      final XSTypeDefinition typeDefinition = iterTypes.next();
      if (typeDefinition.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE)
        bootType(typeDefinition);
    }

    final Map<ProtoGrammar,GrammarLocation> globalGMap = new HashMap<ProtoGrammar,GrammarLocation>();
    
    doType(xscAnyType, globalGMap);
    m_gramSimpleType = doWholeGrammar(m_simpleTypeGrammar, m_simpleTypeContentGrammar, m_simpleTypeEmptyGrammar, globalGMap);

    for (int i = 0; i < m_xscBuiltinSTypes.length; i++) // do builtin simple types
      doType(m_xscBuiltinSTypes[i], globalGMap);
    for (iterTypes = typeDecls.iterator(); iterTypes.hasNext();)
      doType(iterTypes.next(), globalGMap);
    
    for (iterAttributes = attrDecls.iterator(); iterAttributes.hasNext();)
      doAttribute(iterAttributes.next());
    
    for (int elem = 0; elem < m_n_elems; elem += EXISchemaLayout.SZ_ELEM) {
      final XSElementDeclaration element = getDoneXSElementDeclaration(elem);
      assert element != null;
      int tp = getDoneNodeId(element.getTypeDefinition()).nd;
      assert tp != EXISchema.NIL_NODE;
      if (element.getScope() == XSConstants.SCOPE_GLOBAL) {
        tp = (0 - tp) - 1; // minus one in case tp was 0
      }
      m_elems[elem + EXISchemaLayout.INODE_TYPE] = tp;
    }
  }

  private void postProcessSchema() {
    int n_simpleTypes = 0;
    int n_complexTypes = 1; // account for "anyType"
    for (int pos = 0; pos < m_n_types; pos += computeTypeSize(getDoneXSTypeDefinition(pos))) { 
      if (EXISchema._isSimpleType(pos, m_types))
        m_types[pos + EXISchemaLayout.TYPE_NUMBER] = 1 + n_simpleTypes++;
      else {
        if (m_types[pos + EXISchemaLayout.TYPE_NUMBER] == EXISchema.NIL_VALUE) // i.e. except for "anyType"
          m_types[pos + EXISchemaLayout.TYPE_NUMBER] = m_n_stypes + n_complexTypes++;
      }
    }
    assert m_n_stypes == n_simpleTypes;

    for (int gram = 0; gram < m_n_grammars; gram += EXISchema.getSizeOfGrammar(gram, m_grammars)) {
      m_grammars[gram + EXISchemaLayout.GRAMMAR_NUMBER] = m_grammarCount++;
    }
  }

  /////////////////////////////////////////////////////////////////////////
  // Scan Schema Components
  /////////////////////////////////////////////////////////////////////////

  private void scanElement(XSElementDeclaration element,
      Set<XSElementDeclaration> elemDecls, Set<XSAttributeDeclaration> attrDecls, 
      Set<XSTypeDefinition> typeDecls, SortedSet<String> uris) {
    if (elemDecls.contains(element))
      return;
    elemDecls.add(element);

    uris.add(roundify(element.getNamespace()));
    scanType(element.getTypeDefinition(), elemDecls, attrDecls, typeDecls, uris);
  }
  
  private void scanAttribute(XSAttributeDeclaration attribute,
      Set<XSElementDeclaration> elemDecls, Set<XSAttributeDeclaration> attrDecls, 
      Set<XSTypeDefinition> typeDecls, SortedSet<String> uris) {
    if (attrDecls.contains(attribute))
      return;
    attrDecls.add(attribute);
    
    uris.add(roundify(attribute.getNamespace()));
    scanType(attribute.getTypeDefinition(), elemDecls, attrDecls, typeDecls, uris); 
  }

  private void scanType(XSTypeDefinition typeDefinition, 
    Set<XSElementDeclaration> elemDecls, Set<XSAttributeDeclaration> attrDecls, 
    Set<XSTypeDefinition> typeDecls, SortedSet<String> uris) {
    if (typeDecls.contains(typeDefinition))
      return;
    typeDecls.add(typeDefinition);

    uris.add(roundify(typeDefinition.getNamespace()));

    final XSTypeDefinition baseTypeDefinition = getBaseType(typeDefinition);
    if (baseTypeDefinition != null)
      scanType(baseTypeDefinition, elemDecls, attrDecls, typeDecls, uris);
    
    final short typeCategory = typeDefinition.getTypeCategory(); 
    if (typeCategory == XSTypeDefinition.SIMPLE_TYPE) {
      final XSSimpleTypeDecl simpleType = (XSSimpleTypeDecl)typeDefinition;
      switch (simpleType.getVariety()) {
        case XSSimpleTypeDefinition.VARIETY_LIST:
          scanType(simpleType.getItemType(), elemDecls, attrDecls, typeDecls, uris);
          break;
        case XSSimpleTypeDefinition.VARIETY_UNION:
          final XSObjectList memberTypes = simpleType.getMemberTypes();
          for (int i = 0; i < memberTypes.getLength(); i++)
            scanType((XSSimpleTypeDefinition)memberTypes.item(i), elemDecls, attrDecls, typeDecls, uris);
          break;
        case XSSimpleTypeDefinition.VARIETY_ATOMIC:
          break;
        case XSSimpleTypeDefinition.VARIETY_ABSENT:
          // i.e. anySimpleType
          assert "anySimpleType".equals(simpleType.getName());
          break;
        default:
          assert false;
          break;
      }
    }
    else {
      assert typeCategory == XSTypeDefinition.COMPLEX_TYPE;
      final XSComplexTypeDefinition complexType = (XSComplexTypeDefinition)typeDefinition;
      final XSObjectList attributeUses = complexType.getAttributeUses();
      for (int i = 0; i < attributeUses.getLength(); i++)
        scanAttribute(((XSAttributeUse)attributeUses.item(i)).getAttrDeclaration(), elemDecls, attrDecls, typeDecls, uris);
      switch (complexType.getContentType()) {
        case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
          scanType(complexType.getSimpleType(), elemDecls, attrDecls, typeDecls, uris);
          break;
        case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
        case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
          scanParticle(complexType.getParticle(), elemDecls, attrDecls, typeDecls, uris);
          break;
        case XSComplexTypeDefinition.CONTENTTYPE_EMPTY:
          break;
        default:
          assert false;
          break;
      }
      final XSWildcard wildcard;
      if ((wildcard = complexType.getAttributeWildcard()) != null) {
        scanWildcard(wildcard, uris);
      }
    }
  }

  private void scanWildcard(XSWildcard wildcard, SortedSet<String> uris) {
    if (wildcard.getConstraintType() == XSWildcard.NSCONSTRAINT_LIST) {
      StringList namespaceList = wildcard.getNsConstraintList();
      final int n_namespaces = namespaceList.getLength();
      for (int i = 0; i < n_namespaces; i++) {
        uris.add(roundify(namespaceList.item(i)));
      }
    }
  }

  private void scanParticle(XSParticle particle, 
      Set<XSElementDeclaration> elemDecls, Set<XSAttributeDeclaration> attrDecls, 
      Set<XSTypeDefinition> typeDecls, SortedSet<String> uris) {
    final XSTerm term = particle.getTerm();
    switch (term.getType()) {
      case XSConstants.MODEL_GROUP:
        final XSObjectList _particles = ((XSModelGroup)term).getParticles();
        int n_particles = _particles.getLength();
        for (int i = 0; i < n_particles; i++)
          scanParticle((XSParticle)_particles.item(i), elemDecls, attrDecls, typeDecls, uris);
        break;
      case XSConstants.WILDCARD:
        scanWildcard((XSWildcard)term, uris);
        break;
      case XSConstants.ELEMENT_DECLARATION:
        scanElement((XSElementDeclaration)term, elemDecls, attrDecls, typeDecls, uris);
        break;
      default:
        assert false;
        break;
    }
  }

  /////////////////////////////////////////////////////////////////////////
  // Process Schema Components
  /////////////////////////////////////////////////////////////////////////

  /**
   * Bootstrap an element.
   */
  private void bootElement(XSElementDeclaration element) {
    assert getDoneNodeId(element).nd == EXISchema.NIL_NODE;
    final NodeUse elemInfo = internElem(element); 
    final int elem = elemInfo.nd;

    final int elem_sz = EXISchemaLayout.SZ_ELEM;
    ensureElems(elem_sz);
    m_n_elems += elem_sz;
    
    final int uri  = indexOfUri(roundify(element.getNamespace()));
    final int name = indexOfLocalName(element.getName(), uri); 
    assert 0 <= uri && 0 <= name;
    
    m_elems[elem + EXISchemaLayout.INODE_NAME] = name;
    m_elems[elem + EXISchemaLayout.INODE_URI]  = uri;
    
    final boolean isNillable = element.getNillable();
    m_elems[elem + EXISchemaLayout.ELEM_NILLABLE] = isNillable ? 1 : 0;
  }
  
  /**
   * Bootstrap a type.
   */
  private int bootType(XSTypeDefinition typeDefinition) {
    assert getDoneNodeId(typeDefinition).nd == EXISchema.NIL_NODE;
    
    final int tp = internType(typeDefinition).nd;
    assert tp == m_n_types;

    final short typeCategory = typeDefinition.getTypeCategory(); 

    int type_sz = computeTypeSize(typeDefinition);
    ensureTypes(type_sz);
    m_n_types += type_sz;
    m_types[tp + EXISchemaLayout.TYPE_NUMBER] = EXISchema.NIL_VALUE;
  
    if (typeCategory == XSTypeDefinition.SIMPLE_TYPE)
      m_n_stypes++;

    final int uri, localName;
    final String name;
    if ((name = typeDefinition.getName()) == null) { // i.e. anonymous type
      uri = localName = -1;
    }
    else {
      uri  = indexOfUri(roundify(typeDefinition.getNamespace()));
      localName = indexOfLocalName(name, uri);
    }
    assert uri == -1 && localName == -1 || 0 <= uri && (name != null && 0 <= localName);
    
    m_types[tp + EXISchemaLayout.TYPE_NAME] = localName;
    m_types[tp + EXISchemaLayout.TYPE_URI]  = uri;

    return tp;
  }

  /**
   * Process a type.
   * @param typeDefinition a type
   */
  private int doType(XSTypeDefinition typeDefinition, Map<ProtoGrammar,GrammarLocation> globalGMap) {
    final int tp = internType(typeDefinition).nd;
    assert tp != EXISchema.NIL_NODE && tp != m_n_types;
    
    final int contentDatatype;
    if (typeDefinition.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
      final XSComplexTypeDefinition complexTypeDefinition = (XSComplexTypeDefinition)typeDefinition;
      if (complexTypeDefinition.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_SIMPLE) {
        final NodeUse typeInfo = getDoneNodeId(complexTypeDefinition.getSimpleType());
        contentDatatype = typeInfo.nd;
        assert contentDatatype != EXISchema.NIL_NODE;
      }
      else {
        /**
         * NOTE:
         * Signaling an absence, and will be interpreted as NIL_NODE in EXISchema.
         * (See getContentDatatypeOfComplexType() method.)
         * This is because negative numbers are used by simple types in TYPE_AUX in EXISchema.
         */
        contentDatatype = 0; 
      }
    }
    else {
      doSimpleType((XSSimpleTypeDecl)typeDefinition, tp);
      contentDatatype = tp;
    }
    final boolean isTypable;
    isTypable = m_subTypableTypes.contains(typeDefinition) || EXISchema._isSimpleType(tp, m_types) &&
      EXISchema._getVarietyOfSimpleType(tp, m_types) == EXISchema.UNION_SIMPLE_TYPE;
    m_types[tp + EXISchemaLayout.TYPE_TYPABLE] = isTypable ? 1 : 0;
    
    processType(typeDefinition, tp, contentDatatype, globalGMap);

    return tp;
  }

  /**
   * Bootstrap an attribute.
   */
  private void bootAttribute(XSAttributeDeclaration attribute) {
    assert getDoneNodeId(attribute).nd == EXISchema.NIL_NODE;
    final NodeUse attrInfo = internAttr(attribute); 
    final int attr = attrInfo.nd;
    assert attr == m_n_attrs;

    ensureAttrs(EXISchemaLayout.SZ_ATTR);
    m_n_attrs += EXISchemaLayout.SZ_ATTR;

    final int uri  = indexOfUri(roundify(attribute.getNamespace()));
    final int name = indexOfLocalName(attribute.getName(), uri);
    assert 0 <= uri && 0 <= name;
    
    m_attrs[attr + EXISchemaLayout.INODE_NAME] = name;
    m_attrs[attr + EXISchemaLayout.INODE_URI]  = uri;
  }
  
  private int doAttribute(XSAttributeDeclaration attribute) {
    final int attr = internAttr(attribute).nd;
    assert attr != m_n_types;
    
    final NodeUse typeInfo = getDoneNodeId(attribute.getTypeDefinition()); 
    int stype = typeInfo.nd;
    assert stype != EXISchema.NIL_NODE;
    if (attribute.getScope() == XSConstants.SCOPE_GLOBAL) {
      stype = (0 - stype) - 1; // minus one in case stype was 0
    }
    m_attrs[attr + EXISchemaLayout.INODE_TYPE] = stype;
    return attr;
  }
  
  private void doSimpleType(XSSimpleTypeDecl xscSType, int tp) {
    // properties specific to simple types

    final int base = tp + EXISchemaLayout.SZ_TYPE;

    int auxValue = EXISchemaLayout.TYPE_TYPE_OFFSET_MASK;
    
    final int variety = getVariety(xscSType);
    auxValue |= variety; 

    int trailerPos = tp + EXISchemaLayout.SZ_SIMPLE_TYPE;

    XSTypeDefinition baseTypeDefinition = getBaseType(xscSType);
    final NodeUse baseTypeInfo = getDoneNodeId(baseTypeDefinition);
    final int baseType;
    baseType = baseTypeInfo.nd;
    assert baseType != EXISchema.NIL_NODE;
    m_types[base + EXISchemaLayout.SIMPLE_TYPE_BASE_TYPE] = baseType;
    
    if (variety == EXISchema.ATOMIC_SIMPLE_TYPE) {
      final int ancestryId = getAncestryId(xscSType);
      // pattern facet
      if (ancestryId == EXISchemaConst.BOOLEAN_TYPE || ancestryId == EXISchemaConst.STRING_TYPE) {
        final int[] restrictedCharset = getPattern(xscSType); 
        int n_restrictedCharacters = restrictedCharset != null ? restrictedCharset.length : 0;
        if (ancestryId == EXISchemaConst.BOOLEAN_TYPE) {
          if (n_restrictedCharacters != 0) {
            auxValue |= EXISchemaLayout.SIMPLE_TYPE_ATOMIC_BOOLEAN_HAS_PATTERN_MASK;
            n_restrictedCharacters = 0;
          }
        }
        else {
          assert ancestryId == EXISchemaConst.STRING_TYPE;
          // whitespace facet
          int whiteSpace = EXISchema.WHITESPACE_PRESERVE; 
          try {
            switch (xscSType.getWhitespace()) {
              case XSSimpleType.WS_PRESERVE:
                break;
              case XSSimpleType.WS_REPLACE:
                whiteSpace = EXISchema.WHITESPACE_REPLACE;
                break;
              case XSSimpleType.WS_COLLAPSE:
                whiteSpace = EXISchema.WHITESPACE_COLLAPSE;
                break;
              default:
                assert false;
                break;
            }
          }
          catch (DatatypeException de) {
            throw new RuntimeException(de.getMessage(), de);
          }
          auxValue |= whiteSpace << EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_OFFSET;
          assert n_restrictedCharacters < 256;
          auxValue |= n_restrictedCharacters << EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_OFFSET;
        }
        // characters in restricted charset
        for (int i = 0; i < n_restrictedCharacters; i++) {
          m_types[trailerPos++] = restrictedCharset[i];
        }
      }
      // maxInclusive and maxExclusive facet
      else if (ancestryId == EXISchemaConst.INTEGER_TYPE) {
        int minInclusiveFacet = EXISchema.NIL_VALUE;
        BigInteger maxInclusiveInteger = null;
        String facetStr;
        if ((facetStr = getLexicalFacetValue(xscSType, XSSimpleTypeDefinition.FACET_MAXINCLUSIVE)) != null)
          maxInclusiveInteger = new BigInteger(facetStr);
        else if ((facetStr = getLexicalFacetValue(xscSType, XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE)) != null)
          maxInclusiveInteger = new BigInteger(facetStr).subtract(BigInteger.ONE);

        BigInteger minInclusiveInteger = null;
        if ((facetStr = getLexicalFacetValue(xscSType, XSSimpleTypeDefinition.FACET_MININCLUSIVE)) != null) {
          minInclusiveInteger = new BigInteger(facetStr);
        }
        if ((facetStr = getLexicalFacetValue(xscSType, XSSimpleTypeDefinition.FACET_MINEXCLUSIVE)) != null) {
          BigInteger minInclusiveInteger2;
          minInclusiveInteger2 = new BigInteger(facetStr).add(BigInteger.ONE);
          if (minInclusiveInteger != null) {
            if (minInclusiveInteger.compareTo(minInclusiveInteger2) < 0) {
              minInclusiveInteger = minInclusiveInteger2;
            }
          }
          else
            minInclusiveInteger = minInclusiveInteger2;
        }
        final int integralWidth;
        if ((integralWidth = computeIntegralWidth(minInclusiveInteger, maxInclusiveInteger)) <= 12) {
          minInclusiveFacet = doIntegralVariantValue(minInclusiveInteger, integralWidth);
        }
        auxValue |= integralWidth << EXISchemaLayout.SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_OFFSET;
        m_types[base + EXISchemaLayout.SIMPLE_TYPE_FACET_MININCLUSIVE] = minInclusiveFacet;
      }
      // enumeration facet
      final int n_enums;
      if ((n_enums  = getEnumerationCount(xscSType)) != 0) {
        final ObjectList enumValues = xscSType.getActualEnumeration();
        final StringList enumStrings = xscSType.getLexicalEnumeration();
        auxValue |= EXISchemaLayout.SIMPLE_TYPE_HAS_ENUMERATED_VALUES_MASK;
        m_types[trailerPos++] = n_enums; 
        ShortList enumTypeList = xscSType.getEnumerationTypeList();
        short prevValueType = -1;
        for (int i = 0; i < n_enums; i++) {
          final short valueType = enumTypeList.item(i);
          assert prevValueType == -1 || prevValueType == valueType; // i.e. homogeneity
          final int en = doEnumeratedValue(enumValues.item(i), enumStrings.item(i), valueType);
          m_types[trailerPos++] = en;
          prevValueType = valueType;
        }
      }
    }
    else if (variety == EXISchema.LIST_SIMPLE_TYPE) {
      int itemType = EXISchema.NIL_NODE;
      XSSimpleTypeDefinition xscItemType = xscSType.getItemType();
      if (isBuiltIn(xscItemType)) {
        for (int i = 0; i < m_xscBuiltinSTypes.length; i++)
          if (m_xscBuiltinSTypes[i].getName().equals(xscItemType.getName())) {
            itemType = getDoneNodeId(m_xscBuiltinSTypes[i]).nd;
            break;
          }
      }
      if (itemType == EXISchema.NIL_NODE) {
        final NodeUse typeInfo = getDoneNodeId(xscItemType);
        itemType = typeInfo.nd;
        assert itemType != EXISchema.NIL_NODE;
      }
      m_types[base + EXISchemaLayout.SIMPLE_TYPE_ITEM_TYPE] = itemType;
    }

    m_types[tp + EXISchemaLayout.TYPE_AUX] = auxValue;
  }
  
  private ProtoGrammar createAttributeUseGrammar(XSAttributeUse attrbuteUse, int indexNumber, Event[] wildcardEvents, ProtoGrammar subsequentGrammar) {
    XSAttributeDeclaration attributeDecl = attrbuteUse.getAttrDeclaration();
    assert subsequentGrammar != null;
    ProtoGrammar protoGrammar0 = new ProtoGrammar(protoGrammarSerial++, this);
    protoGrammar0.setIndex(indexNumber);
    protoGrammar0.addSubstance(new Production(
        m_eventTypeCache.getEventAT(attributeDecl), subsequentGrammar), m_syntheticGrammarRegistry);
    if (!attrbuteUse.getRequired()) {
      final Substance[] substances = subsequentGrammar.getSubstances();
      final int n_contentProductions = substances.length;
      for (int i = 0; i < n_contentProductions; i++) {
        final Substance rhs = substances[i];
        RightHandSide.RHSType rhsType = rhs.getRHSType();
        if (rhsType == RightHandSide.RHSType.PROD) {
          Production production = (Production)rhs;
          short eventType = production.getEvent().getEventType();
          if (eventType == Event.ATTRIBUTE_WILDCARD || eventType == Event.ATTRIBUTE_WILDCARD_NS) {
            continue;
          }
        }
        protoGrammar0.addSubstance(rhs, m_syntheticGrammarRegistry);
      }
    }
    if (wildcardEvents != null) {
      for (int i = 0; i < wildcardEvents.length; i++) {
        protoGrammar0.addSubstance(new Production(wildcardEvents[i], protoGrammar0), m_syntheticGrammarRegistry);
      }
    }
    protoGrammar0.importGoals(subsequentGrammar);
    return protoGrammar0;
  }
  
  private ProtoGrammar createAllGroupGrammar(XSModelGroup group, boolean mixed, IntHolder particleNumber) {
    final int n_particles;
    XSObjectList particles;
    if ((particles = group.getParticles()) != null)
      n_particles = particles.getLength();
    else
      n_particles = 0;
    
    ProtoGrammar protoGrammar;
    protoGrammar = new ProtoGrammar(protoGrammarSerial++, this);
    if (mixed)
      protoGrammar.addSubstance(new Production(EventTypeCache.eventCharactersUntyped, protoGrammar), m_syntheticGrammarRegistry);

    for (int i = 0; i < n_particles; i++) {
      XSParticle particle = (XSParticle)particles.item(i);
      ProtoGrammar particleGrammar = createParticleGrammar(particle, mixed, particleNumber);
      protoGrammar.appendProtoGrammar(particleGrammar);
      protoGrammar.importGoals(particleGrammar);
    }
    protoGrammar.entail(protoGrammar);
    protoGrammar.addSubstance(new Goal(protoGrammar), m_syntheticGrammarRegistry);
    
    return protoGrammar;
  }

  private ProtoGrammar createSequenceGroupGrammar(XSModelGroup group, boolean mixed, IntHolder particleNumber) {
    int n_particles = 0;
    XSObjectList particles;
    if ((particles = group.getParticles()) != null) {
      n_particles = particles.getLength();
    }
    ProtoGrammar protoGrammar = null;
    if (n_particles == 0) {
      protoGrammar = new ProtoGrammar(protoGrammarSerial++, this);
      protoGrammar.addSubstance(new Goal(protoGrammar), m_syntheticGrammarRegistry);
      if (mixed)
        protoGrammar.addSubstance(new Production(EventTypeCache.eventCharactersUntyped, protoGrammar), m_syntheticGrammarRegistry);
    }
    else {
      for (int i = 0; i < n_particles; i++) {
        XSParticle particle = (XSParticle)particles.item(i);
        ProtoGrammar particleGrammar = createParticleGrammar(particle, mixed, particleNumber);
        if (protoGrammar == null)
          protoGrammar = particleGrammar;
        else
          protoGrammar.entail(particleGrammar);
      }
    }
    return protoGrammar;
  }

  private ProtoGrammar createChoiceGroupGrammar(XSModelGroup group, boolean mixed, IntHolder particleNumber) {
    final int n_particles;
    XSObjectList particles;
    if ((particles = group.getParticles()) != null)
      n_particles = particles.getLength();
    else
      n_particles = 0;
    
    ProtoGrammar protoGrammar;
    protoGrammar = new ProtoGrammar(protoGrammarSerial++, this);
    if (mixed)
      protoGrammar.addSubstance(new Production(EventTypeCache.eventCharactersUntyped, protoGrammar), m_syntheticGrammarRegistry);
    if (n_particles == 0) {
      protoGrammar.addSubstance(new Goal(protoGrammar), m_syntheticGrammarRegistry);
    }
    else {
      for (int i = 0; i < n_particles; i++) {
        XSParticle particle = (XSParticle)particles.item(i);
        ProtoGrammar particleGrammar = createParticleGrammar(particle, mixed, particleNumber);
        protoGrammar.appendProtoGrammar(particleGrammar);
        protoGrammar.importGoals(particleGrammar);
      }
    }
    return protoGrammar;
  }

  private ProtoGrammar createGroupGrammar(XSModelGroup group, boolean mixed, IntHolder particleNumber) {
    switch (group.getCompositor()) {
      case XSModelGroup.COMPOSITOR_ALL:
        return createAllGroupGrammar(group, mixed, particleNumber);
      case XSModelGroup.COMPOSITOR_CHOICE:
        return createChoiceGroupGrammar(group, mixed, particleNumber);
      case XSModelGroup.COMPOSITOR_SEQUENCE:
        return createSequenceGroupGrammar(group, mixed, particleNumber);
      default:
        assert false;
        return null;
    }
  }
  
  private ProtoGrammar createWildcardGrammar(XSWildcard wildcard, boolean mixed, IntHolder particleNumber) {
    ProtoGrammar protoGrammar0 = new ProtoGrammar(protoGrammarSerial++, this);
    ProtoGrammar protoGrammar1 = new ProtoGrammar(protoGrammarSerial++, this);
    protoGrammar1.addSubstance(new Goal(protoGrammar1), m_syntheticGrammarRegistry);
    if (mixed) {
      protoGrammar0.addSubstance(new Production(EventTypeCache.eventCharactersUntyped, protoGrammar0), m_syntheticGrammarRegistry);
      protoGrammar1.addSubstance(new Production(EventTypeCache.eventCharactersUntyped, protoGrammar1), m_syntheticGrammarRegistry);
    }
    if (addWildcardProductions(wildcard, protoGrammar0, protoGrammar1, particleNumber) == 0) {
      return protoGrammar1;
    }
    protoGrammar0.importGoals(protoGrammar1);
    return protoGrammar0;
  }

  private int addWildcardProductions(XSWildcard wildcard, ProtoGrammar protoGrammar0, ProtoGrammar protoGrammar1, IntHolder particleNumber) {
    int n_productions = 0;
    switch (wildcard.getConstraintType()) {
      case XSWildcard.NSCONSTRAINT_ANY:
      case XSWildcard.NSCONSTRAINT_NOT:
        protoGrammar0.addSubstance(new Production(
            EventTypeCache.eventSEWildcard, protoGrammar1, particleNumber.value), m_syntheticGrammarRegistry);
        ++n_productions;
        break;
      case XSWildcard.NSCONSTRAINT_LIST:
        StringList namespaceList = wildcard.getNsConstraintList();
        final int n_namespaces;
        if ((n_namespaces = namespaceList.getLength()) > 0) {
          int i = 0;
          boolean hasNext;
          do {
            protoGrammar0.addSubstance(new Production(
                m_eventTypeCache.getEventSEWildcardNS(roundify(namespaceList.item(i))),
                protoGrammar1, particleNumber.value), m_syntheticGrammarRegistry);
            ++n_productions;            
            if (hasNext = (++i < n_namespaces))
              ++particleNumber.value;
          } while (hasNext);
        }
        else
          return 0;
        break;
      default:
        assert false;
        break;
    }
    return n_productions;
  }
  
  private ProtoGrammar createElementTermGrammarLooping(XSElementDeclaration elem, boolean mixed, IntHolder particleNumber) {
    ProtoGrammar protoGrammar0 = new ProtoGrammar(protoGrammarSerial++, this);
    
    if (mixed)
      protoGrammar0.addSubstance(new Production(EventTypeCache.eventCharactersUntyped, protoGrammar0), m_syntheticGrammarRegistry);
    Set<XSElementDeclaration> setOfElems, substitutablesSorted;
    substitutablesSorted = new TreeSet<XSElementDeclaration>(new XSElementDeclarationComparator());
    if ((setOfElems = m_mapSubst.get(elem)) != null) {
      substitutablesSorted.addAll(setOfElems);
    }
    substitutablesSorted.add(elem);
    
    Iterator<XSElementDeclaration> iter = substitutablesSorted.iterator();
    boolean hasNext;
    do {
      XSElementDeclaration elem_i = iter.next();
      protoGrammar0.addSubstance(new Production(
          m_eventTypeCache.getEventSE(elem_i), protoGrammar0, particleNumber.value), m_syntheticGrammarRegistry);
      if (hasNext = iter.hasNext())
        ++particleNumber.value;
    } while (hasNext);
  
    return protoGrammar0;
  }

  private ProtoGrammar createElementTermGrammar(XSElementDeclaration elem, boolean mixed, ProtoGrammar trailingGrammar, IntHolder particleNumber) {
    if (trailingGrammar == null) {
      trailingGrammar = new ProtoGrammar(protoGrammarSerial++, this);
      trailingGrammar.addSubstance(new Goal(trailingGrammar), m_syntheticGrammarRegistry);
      if (mixed) {
        trailingGrammar.addSubstance(new Production(EventTypeCache.eventCharactersUntyped, trailingGrammar), m_syntheticGrammarRegistry);
      }
    }
    
    ProtoGrammar protoGrammar0 = new ProtoGrammar(protoGrammarSerial++, this);
    if (mixed) {
      protoGrammar0.addSubstance(new Production(EventTypeCache.eventCharactersUntyped, protoGrammar0), m_syntheticGrammarRegistry);
    }
    Set<XSElementDeclaration> setOfElems, substitutablesSorted;
    substitutablesSorted = new TreeSet<XSElementDeclaration>(new XSElementDeclarationComparator());
    if ((setOfElems = m_mapSubst.get(elem)) != null) {
      substitutablesSorted.addAll(setOfElems);
    }
    substitutablesSorted.add(elem);
    
    Iterator<XSElementDeclaration> iter = substitutablesSorted.iterator();
    boolean hasNext;
    do {
      XSElementDeclaration elem_i = iter.next();
      protoGrammar0.addSubstance(new Production(
          m_eventTypeCache.getEventSE(elem_i), trailingGrammar, particleNumber.value), m_syntheticGrammarRegistry);
      if (hasNext = iter.hasNext())
        ++particleNumber.value;
    } while (hasNext);
  
    protoGrammar0.importGoals(trailingGrammar);
    //return new ProtoGrammar[] { protoGrammar0, trailingGrammar };
    return protoGrammar0;
  }

  private ProtoGrammar createTermGrammar(XSTerm term, boolean mixed, IntHolder particleNumber) {
    if (term instanceof XSModelGroup)
      return createGroupGrammar((XSModelGroup)term, mixed, particleNumber);
    else if (term instanceof XSWildcard)
      return createWildcardGrammar((XSWildcard)term, mixed, particleNumber);
    else if (term instanceof XSElementDeclaration)
      return createElementTermGrammar((XSElementDeclaration)term, mixed, (ProtoGrammar)null, particleNumber);
    else {
      assert false;
      return null;
    }
  }
  
  private ProtoGrammar createParticleGrammar(XSParticle particle, boolean mixed, IntHolder particleNumber) {
    final int spn = ++particleNumber.value;
    int epn = -1;
    
    final XSTerm term = particle.getTerm();
    short termType = term.getType();

    int i;
    final boolean maxOccursUnbounded = particle.getMaxOccursUnbounded(); 
    final int minOccurs = particle.getMinOccurs();
    
    ProtoGrammar tailGrammar = null;
    if (maxOccursUnbounded) {
      particleNumber.value = spn;
      if (termType == XSConstants.WILDCARD) {
        tailGrammar = new ProtoGrammar(protoGrammarSerial++, this);
        if (mixed) {
          tailGrammar.addSubstance(new Production(EventTypeCache.eventCharactersUntyped, tailGrammar), m_syntheticGrammarRegistry);
        }
        addWildcardProductions((XSWildcard)term, tailGrammar, tailGrammar, particleNumber);
      }
      else if (termType == XSConstants.ELEMENT_DECLARATION) {
        tailGrammar = createElementTermGrammarLooping((XSElementDeclaration)term, mixed, particleNumber);
      }
      else {
        tailGrammar = createTermGrammar(term, mixed, particleNumber);
        tailGrammar.entail(tailGrammar);
      }
      epn = particleNumber.value;
      tailGrammar.addSubstance(new Goal(tailGrammar), m_syntheticGrammarRegistry);
    }
    else {
      final int maxOccurs = particle.getMaxOccurs();
      assert maxOccurs > 0; // no component if maxOccurs was 0
      final int n_tailGrammars = maxOccurs - minOccurs;
      if (n_tailGrammars > 0) {
        ProtoGrammar[] tailGrammars = new ProtoGrammar[n_tailGrammars];
        for (i = 0; i < n_tailGrammars; i++) {
          particleNumber.value = spn;
          final ProtoGrammar termGrammar = createTermGrammar(term, mixed, particleNumber);
          tailGrammars[n_tailGrammars - 1 - i] = termGrammar;
          epn = particleNumber.value;
          if (tailGrammar != null)
            termGrammar.entail(tailGrammar);
          tailGrammar = termGrammar;
        }
        for (i = 0; i < n_tailGrammars; i++) {
          final ProtoGrammar termGrammar = tailGrammars[i];
          if (!termGrammar.hasGoal()) {
            termGrammar.addSubstance(new Goal(termGrammar, tailGrammar.getGoalBag()), m_syntheticGrammarRegistry);
          }
        }        
      }
    }
    ProtoGrammar particleGrammar = tailGrammar;
    for (i = 0; i < minOccurs; i++) {
      particleNumber.value = spn;
      final ProtoGrammar termGrammar;
      if (maxOccursUnbounded && termType == XSConstants.ELEMENT_DECLARATION) {
        // avoid duplicate grammars by directly appending particleGrammar
        particleGrammar = createElementTermGrammar((XSElementDeclaration)term, mixed, particleGrammar, particleNumber);
      }
      else {
        termGrammar = createTermGrammar(term, mixed, particleNumber);
        epn = particleNumber.value;
        if (particleGrammar != null) {
          termGrammar.entail(particleGrammar);
        }
        particleGrammar = termGrammar;
      }
    }
    assert epn != -1;
    return particleGrammar;
  }
  
  private int doEvent(Event event) {
    switch (event.getEventType()) {
      case Event.ATTRIBUTE_WILDCARD:
        return EXISchema.EVENT_AT_WILDCARD;
      case Event.ELEMENT_WILDCARD:
        return EXISchema.EVENT_SE_WILDCARD;
      case Event.CHARACTERS_TYPED:
        return EXISchema.EVENT_CH_TYPED;
      case Event.CHARACTERS_MIXED:
        return EXISchema.EVENT_CH_UNTYPED; 
      case Event.ATTRIBUTE:
        return addEvent(EXISchema.EVENT_TYPE_AT, getDoneNodeId(event.getDeclaration()).nd);
      case Event.ELEMENT:
        return addEvent(EXISchema.EVENT_TYPE_SE, getDoneNodeId(event.getDeclaration()).nd);
      case Event.ATTRIBUTE_WILDCARD_NS:
        return addEvent(EXISchema.EVENT_TYPE_AT_WILDCARD_NS, ((EventWildcardNS)event).getUri());
      case Event.ELEMENT_WILDCARD_NS:
        return addEvent(EXISchema.EVENT_TYPE_SE_WILDCARD_NS, ((EventWildcardNS)event).getUri());
      case Event.END_ELEMENT:
      default:
        assert false;
        return Integer.MIN_VALUE; // i.e. bad value
    }
  }
  
  private int doGrammar(ProtoGrammar grammar, ProtoGrammar emptyTypeGrammar,  
      Map<Production,Integer> pMap, Map<ProtoGrammar,GrammarLocation> gMap) {
    final int gram = gMap.get(grammar).position;
    final Substance[] substances = grammar.getSubstances();
    final int n_substances = substances.length;
    assert n_substances != 0;
    int n_productions = n_substances;
    int val;
    if (grammar.hasGoal()) {
      val = --n_productions;
      val |= EXISchemaLayout.GRAMMAR_HAS_END_ELEMENT_MASK;
    }
    else
      val = n_productions;
    final int index = grammar.getIndex();
    final boolean hasContentIndex = index != ProtoGrammar.NO_INDEX;
    int legup = 0;
    if (hasContentIndex) {
      val |= EXISchemaLayout.GRAMMAR_HAS_CONTENT_GRAMMAR_MASK;
      m_grammars[gram + EXISchemaLayout.SZ_GRAMMAR + EXISchemaLayout.GRAMMAR_EXT_CONTENT_GRAMMAR] = EXISchema.NIL_GRAM;
      ++legup;
      if (index == 0) {
        val |= EXISchemaLayout.GRAMMAR_HAS_EMPTY_GRAMMAR_MASK;
        assert emptyTypeGrammar != null;
        m_grammars[gram + EXISchemaLayout.SZ_GRAMMAR + EXISchemaLayout.GRAMMAR_EXT_EMPTY_GRAMMAR] = gMap.get(emptyTypeGrammar).position;
        ++legup;
      }
    }
    m_grammars[gram + EXISchemaLayout.GRAMMAR_N_PRODUCTION] = val;
    int pos = gram + EXISchemaLayout.SZ_GRAMMAR + legup;
    for (int i = 0; i < n_substances; i++) {
      int prod;
      Substance substance = substances[i];
      if (substance.isProduction()) {
        final Production production = (Production)substance;
        final Integer integerValue;
        if ((integerValue = pMap.get(production)) != null) {
          prod = integerValue.intValue();
        }
        else {
          prod = m_n_productions;
          pMap.put(production, Integer.valueOf(prod));
          ensureProduction();
          m_n_productions += EXISchemaLayout.SZ_PRODUCTION;
          m_productions[prod + EXISchemaLayout.PRODUCTION_EVENT] = doEvent(production.getEvent());
          m_productions[prod + EXISchemaLayout.PRODUCTION_GRAMMAR] = gMap.get(production.getSubsequentGrammar()).position;
        }
        m_grammars[pos++] = prod;
      }
      else {
        assert i == n_substances - 1 || i == n_substances - 2 && ((Production)substances[n_substances - 1]).getEvent().getEventType() == Event.CHARACTERS_MIXED;
      }
    }
    assert pos == gram + gMap.get(grammar).length;
    return gram;
  }
  
  private void placeGrammars(ProtoGrammar grammar, Map<ProtoGrammar,GrammarLocation> gMap, 
    List<ProtoGrammar> placedGrammars) {
    if (grammar != m_emptyContentGrammar && grammar.isImmutableEnd()) {
      gMap.put(grammar, gMap.get(m_emptyContentGrammar));
    }
    else if (gMap.get(grammar) == null) {
      final Stack<ProtoGrammar> grammarStack = new Stack<ProtoGrammar>();
      final Stack<Integer> indices = new Stack<Integer>();
      
      int gram_sz = computeGrammarSize(grammar);
      ensureGrammar(gram_sz);
      int gram = m_n_grammars;
      m_n_grammars += gram_sz;
      gMap.put(grammar, new GrammarLocation(gram, gram_sz));
      placedGrammars.add(grammar);
      
      grammarStack.push(grammar);
      indices.push(0);
    
      while (grammarStack.size() != 0) {
        int index;
        final ProtoGrammar currentGrammar = grammarStack.peek();
        final Substance[] substances = currentGrammar.getSubstances();
        if ((index = indices.peek()) == substances.length) {
          grammarStack.pop();
          indices.pop();
          continue;
        }
        final Substance substance = substances[index];
        indices.pop();
        indices.push(++index);
        if (substance.isProduction()) {
          final ProtoGrammar subsequentGrammar = ((Production)substance).getSubsequentGrammar();
          if (subsequentGrammar.isImmutableEnd()) {
            gMap.put(subsequentGrammar, gMap.get(m_emptyContentGrammar));
          }
          else if (gMap.get(subsequentGrammar) == null) {
            gram_sz = computeGrammarSize(subsequentGrammar);
            ensureGrammar(gram_sz);
            gram = m_n_grammars;
            m_n_grammars += gram_sz;
            gMap.put(subsequentGrammar, new GrammarLocation(gram, gram_sz));
            placedGrammars.add(subsequentGrammar);

            grammarStack.push(subsequentGrammar);
            indices.push(0);
          }
        }
      }
    }
  }
  
  private void processType(XSTypeDefinition typeDefinition, int tp, int contentDatatype, Map<ProtoGrammar,GrammarLocation> globalGMap) {
    final int gram;
    if (typeDefinition.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
      gram = m_gramSimpleType;
    }
    else {
      final ProtoGrammar typeGrammar, contentGrammar;
      final ProtoGrammar emptyTypeGrammar;
      final int contentIndex;
      
      m_syntheticGrammarRegistry.clear();
      assert typeDefinition.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE;
      final XSComplexTypeDefinition complexType = (XSComplexTypeDefinition)typeDefinition;
      final XSAttributeUse[] attributeUses = sortAttributeUses(complexType);
      final XSWildcard attributeWildcard = complexType.getAttributeWildcard();
      contentIndex = attributeWildcard != null ? attributeUses.length + 2 : attributeUses.length + 1;

      final short contentType;
      switch (contentType = complexType.getContentType()) {
        case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
          contentGrammar = m_simpleTypeContentGrammar; 
          break;
        case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
        case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
          final XSParticle particle;
          if ((particle = (XSParticle)complexType.getParticle()) != null) {
            contentGrammar = createParticleGrammar(particle, contentType == XSComplexTypeDefinition.CONTENTTYPE_MIXED, new IntHolder());
            contentGrammar.normalize(new HashSet<ProtoGrammar>(), new ArrayList<ProtoGrammar>());
            break;
          }        
        default:
          assert contentType == XSComplexTypeDefinition.CONTENTTYPE_EMPTY;
          contentGrammar = m_emptyContentGrammar;
          break;
      }
    
      typeGrammar = processAttributeUses(contentGrammar, contentIndex, attributeUses, attributeWildcard);
      
      emptyTypeGrammar = processAttributeUses(m_emptyContentGrammar, contentIndex, attributeUses, attributeWildcard);

      gram = doWholeGrammar(typeGrammar, contentGrammar, emptyTypeGrammar, globalGMap);
      
      m_types[tp + EXISchemaLayout.TYPE_AUX] = contentDatatype;
    }
    
    m_types[tp + EXISchemaLayout.TYPE_GRAMMAR] = gram;
  }
  
  private ProtoGrammar processAttributeUses(ProtoGrammar contentGrammar, int contentIndex, 
      XSAttributeUse[] attributeUses, XSWildcard attributeWildcard) {
    int indexNumber = contentIndex - 1;

    ProtoGrammar attributeUseGrammar;
    attributeUseGrammar = new ProtoGrammar(protoGrammarSerial++, this);
    attributeUseGrammar.setIndex(indexNumber--);
    Substance[] contentSubstances = contentGrammar.getSubstances();
    final int n_contentSubstances = contentSubstances.length;
    for (int i = 0; i < n_contentSubstances; i++) {
      attributeUseGrammar.addSubstance(contentSubstances[i], m_syntheticGrammarRegistry);
    }
    attributeUseGrammar.importGoals(contentGrammar);

    ProtoGrammar subsequentGrammar = attributeUseGrammar;
    
    Event[] wildcardEvents = null;
    if (attributeWildcard != null) {
      switch (attributeWildcard.getConstraintType()) {
        case XSWildcard.NSCONSTRAINT_ANY:
        case XSWildcard.NSCONSTRAINT_NOT:
          wildcardEvents = new Event[1];
          wildcardEvents[0] = EventTypeCache.eventATWildcard;
          break;
        case XSWildcard.NSCONSTRAINT_LIST:
          StringList namespaceList = attributeWildcard.getNsConstraintList();
          int n_namespaces = namespaceList.getLength();
          wildcardEvents = new Event[n_namespaces];
          for (int i = 0; i < n_namespaces; i++)
            wildcardEvents[i] = m_eventTypeCache.getEventATWildcardNS(roundify(namespaceList.item(i))); 
          break;
        default:
          assert false;
          break;
      }
      attributeUseGrammar = new ProtoGrammar(protoGrammarSerial++, this);
      attributeUseGrammar.setIndex(indexNumber--);
      Substance[] subsequentGrammarSubstances = subsequentGrammar.getSubstances();
      final int n_subsequentGrammarSubstances = subsequentGrammarSubstances.length;
      for (int i = 0; i < n_subsequentGrammarSubstances; i++) {
        attributeUseGrammar.addSubstance(subsequentGrammarSubstances[i], m_syntheticGrammarRegistry);
      }
      attributeUseGrammar.importGoals(subsequentGrammar);
      for (int i = 0; i < wildcardEvents.length; i++) {
        attributeUseGrammar.addSubstance(new Production(wildcardEvents[i], attributeUseGrammar), m_syntheticGrammarRegistry);
      }
      subsequentGrammar = attributeUseGrammar;
    }
    
    for (int i = attributeUses.length - 1; i >= 0; i--) {
      attributeUseGrammar = createAttributeUseGrammar(attributeUses[i], indexNumber--, wildcardEvents, subsequentGrammar);
      subsequentGrammar = attributeUseGrammar;
    }
    assert indexNumber == -1;
    return subsequentGrammar;
  }
  
  private int doWholeGrammar(ProtoGrammar typeGrammar, ProtoGrammar contentGrammar, 
      ProtoGrammar emptyTypeGrammar, Map<ProtoGrammar,GrammarLocation> gMap) {
    final Map<Production,Integer> pMap = new HashMap<Production,Integer>();
    doGrammars(emptyTypeGrammar, m_emptyContentGrammar, emptyTypeGrammar, pMap, gMap);
    doGrammars(typeGrammar, contentGrammar, emptyTypeGrammar, pMap, gMap);
    return gMap.get(typeGrammar).position;
  }
  
  private void doGrammars(ProtoGrammar typeGrammar, ProtoGrammar contentGrammar, ProtoGrammar emptyTypeGrammar, 
    Map<Production,Integer> pMap, Map<ProtoGrammar,GrammarLocation> gMap) {
    assert typeGrammar.getIndex() != 0 || emptyTypeGrammar != null;
    List<ProtoGrammar> placedGrammars = new ArrayList<ProtoGrammar>();
    /**
     * It is important to call placeGrammars method in the following sequence 
     * for xsd:anyType. This is because xsd:anyType grammars are fixtures.
     */
    placeGrammars(typeGrammar, gMap, placedGrammars);
    placeGrammars(contentGrammar, gMap, placedGrammars);
    for (int i = 0; i < placedGrammars.size(); i++) {
      doGrammar(placedGrammars.get(i), emptyTypeGrammar, pMap, gMap);
    }
    
    final int contentGram = gMap.get(contentGrammar).position;
    for (int i = 0; i < placedGrammars.size(); i++) {
      final ProtoGrammar protoGrammar = placedGrammars.get(i);
      if (protoGrammar.getIndex() != ProtoGrammar.NO_INDEX) {
        final int gram = gMap.get(protoGrammar).position;
        final int gramContent = m_grammars[gram + (EXISchemaLayout.SZ_GRAMMAR + EXISchemaLayout.GRAMMAR_EXT_CONTENT_GRAMMAR)];
        assert gramContent == EXISchema.NIL_GRAM;
        m_grammars[gram + EXISchemaLayout.SZ_GRAMMAR] = contentGram;
      }
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

  private int doEnumeratedValue(Object obj, String stringValue, short valueType) {
    int variant = EXISchema.NIL_VALUE;
    if (obj != null) {
      int i;
      boolean isSuccess;
      switch (valueType) {
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
        case XSConstants.BOOLEAN_DT:
          variant = super.addVariantBooleanValue(((Boolean)obj).booleanValue());
          break;
        case XSConstants.DECIMAL_DT:
          isSuccess = DecimalValueScriber.instance.doProcess(stringValue, m_scribble, new StringBuilder(), new StringBuilder());
          assert isSuccess;
          DecimalValueScriber.canonicalizeValue(m_scribble);
          variant = addVariantDecimalValue(DecimalValueScriber.getSign(m_scribble), 
              DecimalValueScriber.getIntegralDigits(m_scribble), 
              DecimalValueScriber.getReverseFractionalDigits(m_scribble));
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
          variant = doIntegralVariantValue(((org.apache.xerces.xs.datatypes.XSDecimal)obj).getBigInteger(), 0);
          break;
        case XSConstants.FLOAT_DT:
        case XSConstants.DOUBLE_DT:
          isSuccess = FloatValueScriber.instance.doProcess(stringValue, m_scribble, new StringBuilder());
          assert isSuccess;
          FloatValueScriber.canonicalizeValue(m_scribble);
          variant = addVariantFloatValue(FloatValueScriber.getMantissa(m_scribble), FloatValueScriber.getExponent(m_scribble));
          break;
        case XSConstants.DATETIME_DT:
          isSuccess = DateTimeValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null);
          assert isSuccess;
          variant = addVariantDateTimeValue(m_scribble.dateTime);
          break;
        case XSConstants.TIME_DT:
          isSuccess = TimeValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null);
          assert isSuccess;
          variant = addVariantDateTimeValue(m_scribble.dateTime);
          break;
        case XSConstants.DATE_DT:
          isSuccess = DateValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null);
          assert isSuccess;
          variant = addVariantDateTimeValue(m_scribble.dateTime);
          break;
        case XSConstants.GYEARMONTH_DT:
          isSuccess = GYearMonthValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null);
          assert isSuccess;
          variant = addVariantDateTimeValue(m_scribble.dateTime);
          break;
        case XSConstants.GYEAR_DT:
          isSuccess = GYearValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null);
          assert isSuccess;
          variant = addVariantDateTimeValue(m_scribble.dateTime);
          break;
        case XSConstants.GMONTHDAY_DT:
          isSuccess = GMonthDayValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null);
          assert isSuccess;
          variant = addVariantDateTimeValue(m_scribble.dateTime);
          break;
        case XSConstants.GDAY_DT:
          isSuccess = GDayValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null);
          assert isSuccess;
          variant = addVariantDateTimeValue(m_scribble.dateTime);
          break;
        case XSConstants.GMONTH_DT:
          if (GMonthValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null))
            variant = addVariantDateTimeValue(m_scribble.dateTime);
          break;
        case XSConstants.DURATION_DT:
          org.apache.xerces.xs.datatypes.XSDateTime dateTime = (org.apache.xerces.xs.datatypes.XSDateTime)obj;
          try {
            variant = addVariantDurationValue(
                EXISchema.datatypeFactory.newDuration(dateTime.getLexicalValue()));
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
          variant = addVariantBinaryValue(bytes, valueType == XSConstants.BASE64BINARY_DT ? EXISchema.VARIANT_BASE64 : EXISchema.VARIANT_HEXBIN);
          break;
        default:
          assert false;
          break;
      }
    }
    return variant;
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
    final Exception innerException = exc.getException();
    if (innerException != null && innerException instanceof EXISchemaFactoryRuntimeException) {
      // That's what we threw just a second ago! Let's throw it back!
      throw ((EXISchemaFactoryRuntimeException)innerException).exception;
    }
    try {
      if (m_compiler_errorHandler != null) {
        if (isFatal)
          m_compiler_errorHandler.fatalError(exc);
        else
          m_compiler_errorHandler.error(exc);
      }
      else {
        // nobody was listening. Let's just have it known hard way.
        throw new EXISchemaFactoryRuntimeException(exc);
      }
    }
    catch (EXISchemaFactoryException userexc) {
      exc.setException(userexc);
      throw exc;
    }
    finally {
      if (isFatal || m_compiler_errorHandler == null)
        m_schemaFactoryException = exc;
    }
    if (isFatal)
      throw new EXISchemaFactoryRuntimeException(exc);
  }

  /////////////////////////////////////////////////////////////////////////
  // House-keeping procedures
  /////////////////////////////////////////////////////////////////////////

  /**
   * Intern an element.
   */
  private NodeUse internElem(XSElementDeclaration element) {
    NodeUse nodeUse = NodeUse.NIL_NODEUSE;
    if (element != null) {
      if ((nodeUse = getDoneNodeId(element)).nd == EXISchema.NIL_NODE) {
        final int nd = m_n_elems;
        nodeUse = new NodeUse(nd);
        // Register an element.
        m_doneNodes.put(element, nodeUse);
        m_doneElementsReverse.put(nd, element);
      }
    }
    return nodeUse;
  }

  /**
   * Intern an attribute.
   */
  private NodeUse internAttr(XSAttributeDeclaration attribute) {
    NodeUse nodeUse = NodeUse.NIL_NODEUSE;
    if (attribute != null) {
      if ((nodeUse = getDoneNodeId(attribute)).nd == EXISchema.NIL_NODE) {
        final int attr = m_n_attrs;
        nodeUse = new NodeUse(attr);
        // Register an attribute.
        m_doneNodes.put(attribute, nodeUse);
      }
    }
    return nodeUse;
  }

  /**
   * Intern a type.
   */
  private NodeUse internType(XSTypeDefinition type) {
    NodeUse nodeUse = NodeUse.NIL_NODEUSE;
    if (type != null) {
      if ((nodeUse = getDoneNodeId(type)).nd == EXISchema.NIL_NODE) {
        final int tp = m_n_types;
        nodeUse = new NodeUse(tp);
        // Register a type.
        m_doneNodes.put(type, nodeUse);
        m_doneTypeReverse.put(tp, type);
      }
    }
    return nodeUse;
  }

  /**
   * Returns the id of a node if it is recognized, otherwise returns NIL_NODE.
   */
  private NodeUse getDoneNodeId(XSObject node) {
    if (node != null) {
      final NodeUse nodeUse;
      if ((nodeUse = m_doneNodes.get(node)) != null)
        return nodeUse;
    }
    return NodeUse.NIL_NODEUSE;
  }

  /**
   * Returns the element corresponding to an element id.
   */
  private XSElementDeclaration getDoneXSElementDeclaration(int elem) {
    return elem != EXISchema.NIL_NODE ? m_doneElementsReverse.get(elem) : null;
  }
 
  /**
   * Returns the type corresponding to a type id.
   */
  private XSTypeDefinition getDoneXSTypeDefinition(int type) {
    return type != EXISchema.NIL_NODE ? m_doneTypeReverse.get(type) : null;
  }

  /////////////////////////////////////////////////////////////////////////
  // Conveniences
  /////////////////////////////////////////////////////////////////////////

  /**
   * Estimate the size of a type.
   */
  private int computeTypeSize(XSTypeDefinition typeDefinition) {
    int size;
    if (typeDefinition.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
      size = EXISchemaLayout.SZ_SIMPLE_TYPE; 
      XSSimpleTypeDecl simpleTypeDeclaration = (XSSimpleTypeDecl)typeDefinition;
      if (getVariety(simpleTypeDeclaration) == EXISchema.ATOMIC_SIMPLE_TYPE && 
          getAncestryId(simpleTypeDeclaration) == EXISchemaConst.STRING_TYPE) {
        final int[] restrictedCharset = getPattern(simpleTypeDeclaration);
        size += restrictedCharset != null ? restrictedCharset.length : 0;
      }
      final int n_enums = getEnumerationCount(simpleTypeDeclaration); 
      if (n_enums != 0)
        size += 1 + n_enums;
    }
    else {
      size = EXISchemaLayout.SZ_COMPLEX_TYPE;
    }
    return size;
  }
  
  private int getEnumerationCount(XSSimpleTypeDecl simpleType) {
    if (getVariety(simpleType) == EXISchema.ATOMIC_SIMPLE_TYPE) {
      switch (getAncestryId(simpleType)) {
        case EXISchemaConst.BOOLEAN_TYPE:
        case EXISchemaConst.NOTATION_TYPE:
        case EXISchemaConst.QNAME_TYPE:
          break;
        default:
          return simpleType.getActualEnumeration().getLength();  
      }
    }
    return 0;
  }
  
  /**
   * Determines whether a simple type is an atomic, a list, an union 
   * or an ur-simple datatype. 
   * @param simpleTypeDecl a simple type declaration.
   */
  private int getVariety(XSSimpleTypeDecl simpleTypeDecl) {
    switch (simpleTypeDecl.getVariety()) {
      case XSSimpleTypeDefinition.VARIETY_ATOMIC:
        return EXISchema.ATOMIC_SIMPLE_TYPE;
      case XSSimpleTypeDefinition.VARIETY_LIST:
        return EXISchema.LIST_SIMPLE_TYPE;
      case XSSimpleTypeDefinition.VARIETY_UNION:
        return EXISchema.UNION_SIMPLE_TYPE;
      default:
        return EXISchema.UR_SIMPLE_TYPE;
    }
  }

  private int getAncestryId(XSSimpleTypeDecl simpleTypeDecl) {
    assert getVariety(simpleTypeDecl) == EXISchema.ATOMIC_SIMPLE_TYPE;
    switch (simpleTypeDecl.getBuiltInKind()) {
      case XSConstants.STRING_DT:
      case XSConstants.NORMALIZEDSTRING_DT:
      case XSConstants.TOKEN_DT:
      case XSConstants.LANGUAGE_DT:
      case XSConstants.NAME_DT:
      case XSConstants.NMTOKEN_DT:
      case XSConstants.NCNAME_DT:
      case XSConstants.ID_DT:
      case XSConstants.IDREF_DT:
      case XSConstants.ENTITY_DT:
        return EXISchemaConst.STRING_TYPE;
      case XSConstants.BOOLEAN_DT:
        return EXISchemaConst.BOOLEAN_TYPE;
      case XSConstants.DECIMAL_DT:
        return EXISchemaConst.DECIMAL_TYPE;
      case XSConstants.FLOAT_DT:
        return EXISchemaConst.FLOAT_TYPE;
      case XSConstants.DOUBLE_DT:
        return EXISchemaConst.DOUBLE_TYPE;
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
        return EXISchemaConst.INTEGER_TYPE;
      default:
        assert false;
        return EXISchemaConst.ANY_SIMPLE_TYPE;        
    }
  }
  
  private int[] getPattern(XSSimpleTypeDecl simpleTypeDefn) {
    if (getVariety(simpleTypeDefn) == EXISchema.ATOMIC_SIMPLE_TYPE) {
      if (!W3C_2001_XMLSCHEMA_URI.equals(simpleTypeDefn.getNamespace())) {
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
    }
    return null;
  }

  private final XSTypeDefinition getBaseType(XSTypeDefinition typeDefinition) {
    XSTypeDefinition baseTypeDefinition = typeDefinition.getBaseType();
    if (typeDefinition.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
      final XSSimpleTypeDecl simpleType = (XSSimpleTypeDecl)typeDefinition;
      final String typeName = typeDefinition.getName();
      if (W3C_2001_XMLSCHEMA_URI.equals(typeDefinition.getNamespace()) && "anySimpleType".equals(typeName)) {
        assert baseTypeDefinition == null;
        return m_xsdSchema.getTypeDefinition("anyType");
      }
      if (W3C_2001_XMLSCHEMA_URI.equals(baseTypeDefinition.getNamespace())) {
        if (baseTypeDefinition.getName() == null) {
          assert simpleType.getVariety() == XSSimpleTypeDefinition.VARIETY_LIST && 
              "ENTITIES".equals(typeName) || "IDREFS".equals(typeName) || "NMTOKENS".equals(typeName);
          return m_xscAnySimpleType;
        }
      }
    }
    return baseTypeDefinition;
  }
  
  private static String getLexicalFacetValue(XSSimpleTypeDecl simpleTypeDecl, short facetName) {
    final Object facetValue;
    switch (facetName) {
      case XSSimpleTypeDefinition.FACET_MAXINCLUSIVE:
        facetValue = simpleTypeDecl.getMaxInclusiveValue();
        break;
      case XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE:
        facetValue = simpleTypeDecl.getMaxExclusiveValue();
        break;
      case XSSimpleTypeDefinition.FACET_MININCLUSIVE:
        facetValue = simpleTypeDecl.getMinInclusiveValue();
        break;
      case XSSimpleTypeDefinition.FACET_MINEXCLUSIVE:
        facetValue = simpleTypeDecl.getMinExclusiveValue();
        break;
      default:
        assert false;
        return "";
    }
    if (facetValue != null) {
      assert facetValue instanceof XSDecimal;
      return facetValue.toString();
    }
    return null;
  }
  
  private static short getScope(XSObject scopedObject) {
    if (scopedObject instanceof XSElementDeclaration)
      return ((XSElementDeclaration)scopedObject).getScope();
    else if (scopedObject instanceof XSAttributeDeclaration)
      return ((XSAttributeDeclaration)scopedObject).getScope();
    else
      return XSConstants.SCOPE_ABSENT;
  }
  
  private static int computeGrammarSize(ProtoGrammar grammar) {
    final Substance[] substances = grammar.getSubstances();
    final int n_substances = substances.length;
    assert n_substances != 0;
    final int n_productions = grammar.hasGoal() ? n_substances - 1 : n_substances;
    int gram_sz = EXISchemaLayout.SZ_GRAMMAR + n_productions;
    final int index = grammar.getIndex();
    if (index != ProtoGrammar.NO_INDEX) {
      ++gram_sz;
      if (index == 0) {
        ++gram_sz;
      }
    }
    return gram_sz;
  }
  
  /////////////////////////////////////////////////////////////////////////
  // Utilities
  /////////////////////////////////////////////////////////////////////////

  private static String roundify(String str) {
    return str != null ? str : "";
  }

  private static boolean isBuiltIn(XSSimpleTypeDefinition simpleType) {
    if (W3C_2001_XMLSCHEMA_URI.equals(simpleType.getNamespace())) {
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
   * Compute the width of an integer.
   */
  private static int computeIntegralWidth(BigInteger minInclusiveInteger, BigInteger maxInclusiveInteger) {
    final int integralWidth;
    if (minInclusiveInteger != null) {
      final boolean isNonNegative = minInclusiveInteger.signum() >= 0;
      if (maxInclusiveInteger == null) // only lower bound
        integralWidth = isNonNegative ? EXISchema.INTEGER_CODEC_NONNEGATIVE : EXISchema.INTEGER_CODEC_DEFAULT;
      else {
        final BigInteger bigIntegralRange = maxInclusiveInteger.subtract(minInclusiveInteger);
        if (bigIntegralRange.signum() >= 0) {
          final int n_bits;
          if ((n_bits = bigIntegralRange.bitLength()) > 12)
            integralWidth = isNonNegative ? EXISchema.INTEGER_CODEC_NONNEGATIVE : EXISchema.INTEGER_CODEC_DEFAULT;  
          else
            integralWidth = n_bits;
        }
        else {
          assert false;
          integralWidth = 0;
        }
      }
    }
    else { // no lower bound
      integralWidth = EXISchema.INTEGER_CODEC_DEFAULT;
    }
    assert 0 <= integralWidth && integralWidth <= 12 || integralWidth == EXISchema.INTEGER_CODEC_NONNEGATIVE || 
        integralWidth == EXISchema.INTEGER_CODEC_DEFAULT;
    return integralWidth;
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
          EXISchemaFactoryException.XMLSCHEMA_ERROR,
          new String[] { de.getMessage() },
          toLocator(de));
      Object object;
      if ((object = de.getRelatedException()) instanceof Exception) {
        sce.setException((Exception)object);
      }
      try {
        if (isError) {
          final String message = sce.getMessage();
          if (message.startsWith("cos-nonambig:") || message.startsWith("schema_reference.4")) {
            // treat UPA violation and schema reference errors as fatal
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
  
  private static class ComparableQName implements Comparable<ComparableQName> {
    public final String name;
    public final String uri;
    public ComparableQName(String name, String uri) {
      this.name = name;
      this.uri = uri;
    }
    public int compareTo(ComparableQName that) {
      int res;
      if ((res = name.compareTo(that.name)) > 0)
        return 1;
      else if (res < 0)
        return -1;
      else {
        if ((res = uri.compareTo(that.uri)) > 0)
          return 1;
        else if (res < 0)
          return -1;
        else {
          return 0;
        }
      }
    }
  }
  
  private static class ComparableSubstance extends ComparableQName {
    final XSObject substance;
    final int serialNumber;
    ComparableSubstance(XSObject substance, int serialNumber) {
      super(substance.getName(), roundify(substance.getNamespace()));
      assert substance instanceof XSElementDeclaration || substance instanceof XSAttributeDeclaration;
      this.substance = substance;
      this.serialNumber = serialNumber;
    }
    @Override
    public int compareTo(ComparableQName that) {
      int val = 0;
      if (substance != ((ComparableSubstance)that).substance) {
        if ((val = super.compareTo(that)) == 0) { // i.e. same QName
          assert serialNumber != -1 && ((ComparableSubstance)that).serialNumber != -1;
          if (getScope(substance) == XSConstants.SCOPE_GLOBAL) {
            assert getScope(((ComparableSubstance)that).substance) != XSConstants.SCOPE_GLOBAL; 
            return -1;
          }
          else {
            val = serialNumber - ((ComparableSubstance)that).serialNumber;
            assert val != 0;
          }
        }
      }
      return val;
    }
  }
  
  private static class IntHolder {
    public int value = 0;
  }

  private static class GrammarLocation {
    public final int position;
    public final int length;
    public GrammarLocation(int pos, int len) {
      position = pos;
      length = len;
    }
  }

  private static class NodeUse {
    static final NodeUse NIL_NODEUSE;
    static {
      NIL_NODEUSE = new NodeUse(EXISchema.NIL_NODE);
    }
    final int nd;
    NodeUse(int nd) {
      this.nd = nd;
    }
  }
  
  private static class XSElementDeclarationComparator implements Comparator<XSElementDeclaration> {
    public int compare(XSElementDeclaration elem1, XSElementDeclaration elem2) {
      int res = 0;
      if (elem1 != elem2) {
        if ((res = elem1.getName().compareTo(elem2.getName())) == 0) {
          final String uri1 = roundify(elem1.getNamespace());
          final String uri2 = roundify(elem2.getNamespace());
          assert !uri1.equals(uri2);
          res = uri1.compareTo(uri2);
        }
      }
      return res;
    }
  }

  private static class EXISchemaFactoryRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 5045578738466000399L;
    final EXISchemaFactoryException exception;
    public EXISchemaFactoryRuntimeException(EXISchemaFactoryException exc) {
      exception = exc;
    }
  }
  
  private static class EntityResolverAdapter implements XMLEntityResolver {
    private EntityResolverEx m_entityResolver;
    
    EntityResolverAdapter() {
      m_entityResolver = null;
    }
    
    public void setEntityResolver(EntityResolverEx entityResolver) {
      m_entityResolver = entityResolver;
    }
   
    public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier)
        throws XNIException, IOException {
      final String publicId = resourceIdentifier.getPublicId();
      final String systemId = resourceIdentifier.getExpandedSystemId();
      if (m_entityResolver != null) {
        final InputSource inputSource;
        final String uri;
        try {
          if ((uri = resourceIdentifier.getNamespace()) != null)
            inputSource = m_entityResolver.resolveEntity(publicId, systemId, uri);
          else {
            if (resourceIdentifier instanceof XMLSchemaDescription)
              inputSource = m_entityResolver.resolveEntity(publicId, systemId, "");
            else
              inputSource = m_entityResolver.resolveEntity(publicId, systemId);
          }
        }
        catch (SAXException se) {
          Exception e = se.getException();
          if (e == null)
            e = se;
          throw new XNIException(e);
        }
        if (inputSource != null) {
          return saxToXMLInputSource(inputSource);
        }
      }
      return null;
    }

    private XMLInputSource saxToXMLInputSource(InputSource sis) {
      String publicId = sis.getPublicId();
      String systemId = sis.getSystemId();
      Reader charStream = sis.getCharacterStream();
      if (charStream != null) {
          return new XMLInputSource(publicId, systemId, null, charStream, null);
      }
      InputStream byteStream = sis.getByteStream();
      if (byteStream != null) {
          return new XMLInputSource(publicId, systemId, null, byteStream, sis.getEncoding());
      }
      return new XMLInputSource(publicId, systemId, null);
    }
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */
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

    File file = new File(xscUri);
    System.out.println(file.getParentFile().toURI().relativize(xscUri).toString()); 
  }

}
