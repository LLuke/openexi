package org.openexi.fujitsu.scomp;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConstants;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.xml.sax.InputSource;
import org.xml.sax.Locator;

import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xni.parser.XMLParseException;

import org.openexi.fujitsu.proc.grammars.GrammarRuntimeException;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaConst;
import org.openexi.fujitsu.schema.EXISchemaRuntimeException;
import org.openexi.fujitsu.schema.EXISchemaUtil;
import org.openexi.fujitsu.schema.XSDateTime;

/**
 */
public class EXISchemaFactoryTest extends TestCase {

  public EXISchemaFactoryTest(String name) {
    super(name);
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

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  private static final String XMLSCHEMA_URI = "http://www.w3.org/2001/XMLSchema";
  private static final String XMLSCHEMA_INSTANCE_URI = "http://www.w3.org/2001/XMLSchema-instance";

  private EXISchemaFactoryErrorMonitor m_compilerErrorHandler;
  private final DatatypeFactory m_datatypeFactory;

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Compilation succeeds even for empty input source.
   */
  public void testSystemIDRequired() throws Exception {
    EXISchemaFactory schemaCompiler = new EXISchemaFactory();
    schemaCompiler.setCompilerErrorHandler(m_compilerErrorHandler);
    InputSource is = new InputSource();
    EXISchema corpus = schemaCompiler.compile(is);

    // xsd, XMLSchema-instance.xsd
    Assert.assertEquals(2, corpus.getNamespaceCountOfSchema());

    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
    Assert.assertTrue(xsdns != EXISchema.NIL_NODE);

    int xsins = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema-instance");
    Assert.assertTrue(xsins != EXISchema.NIL_NODE);
  }

  /**
   * Compilation succeeds even for empty list of schemas.
   */
  public void testCompileEmptyListOfSchemas() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    // xsd, XMLSchema-instance.xsd
    Assert.assertEquals(2, corpus.getNamespaceCountOfSchema());

    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
    Assert.assertTrue(xsdns != EXISchema.NIL_NODE);

    int xsins = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema-instance");
    Assert.assertTrue(xsins != EXISchema.NIL_NODE);
    
    int xdtns = corpus.getNamespaceOfSchema("http://www.w3.org/2005/xpath-datatypes");
    Assert.assertEquals(EXISchema.NIL_NODE, xdtns);
  }

  /**
   * Test corpus methods
   */
  public void testCorpus() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/verySimple.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    // Check grand total number of elements and attributes
    Assert.assertEquals(13, corpus.getTotalElemCount());
    // 4 in XMLSchema-instance.xsd, 3 in verySimple.xsd, 1 in verySimpleImported.xsd
    Assert.assertEquals(8, corpus.getTotalAttrCount());
  }

  /**
   * Test schema methods
   */
  public void testSchema() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/verySimple.xsd", getClass());
    
        Assert.assertEquals(13, corpus.getElemCountOfSchema());
    // 4 in XMLSchema-instance.xsd, 1 in verySimple.xsd, 1 in verySimpleImported.xsd
    Assert.assertEquals(6, corpus.getAttrCountOfSchema());
    // 1 in verySimple.xsd, 2 in verySimpleImported.xsd,
    Assert.assertEquals(3 + EXISchemaConst.N_BUILTIN_TYPES, EXISchemaUtil.countTypesOfSchema(corpus, true));
    Assert.assertEquals(3 + (EXISchemaConst.N_BUILTIN_TYPES - 1), EXISchemaUtil.countLinkedSimpleTypesOfSchema(corpus));
    Assert.assertEquals(3 + (EXISchemaConst.N_BUILTIN_TYPES - 1), corpus.getTotalSimpleTypeCount()); 
    Assert.assertEquals(4, corpus.getNamespaceCountOfSchema());
    int i;
    // excercise getNamespaceOfSchema(i)
    for (i = 0; i < corpus.getNamespaceCountOfSchema(); i++)
      Assert.assertEquals(EXISchema.NAMESPACE_NODE,
                        corpus.getNodeType(corpus.getNamespaceOfSchema(i)));
    // excercise getNamespaceOfSchema(namespaceName)
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    int goons = corpus.getNamespaceOfSchema("urn:goo");
    Assert.assertTrue(foons != EXISchema.NIL_NODE &&
                      goons != EXISchema.NIL_NODE &&
                      foons != goons);
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(goons));
    // The first namespace is reserved for "http://www.w3.org/2001/XMLSchema"
    int ns0 = corpus.getNamespaceOfSchema(0);
    Assert.assertEquals(ns0, corpus.getNamespaceOfSchema(XMLSCHEMA_URI));
  }

  /**
   * Test XMLSchema-instance.xsd
   */
  public void testXMLSchemaInstanceSchema() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/verySimple.xsd", getClass());
    
    int xsdns = corpus.getNamespaceOfSchema(XMLSCHEMA_URI);
    int booleanType = corpus.getTypeOfNamespace(xsdns, "boolean");
    Assert.assertTrue(EXISchema.NIL_NODE != booleanType);
    int qnameType = corpus.getTypeOfNamespace(xsdns, "QName");
    Assert.assertTrue(EXISchema.NIL_NODE != qnameType);
    int anyURIType = corpus.getTypeOfNamespace(xsdns, "anyURI");
    Assert.assertTrue(EXISchema.NIL_NODE != anyURIType);

    int xsins = corpus.getNamespaceOfSchema(XMLSCHEMA_INSTANCE_URI);
    Assert.assertTrue(EXISchema.NIL_NODE != xsins);

    Assert.assertEquals(XMLSCHEMA_INSTANCE_URI, corpus.getNameOfNamespace(xsins));
    Assert.assertEquals(0, corpus.getElemCountOfNamespace(xsins));
    Assert.assertEquals(4, corpus.getAttrCountOfNamespace(xsins));
    Assert.assertEquals(0, corpus.getTypeCountOfNamespace(xsins));

    int a_nil = corpus.getAttrOfNamespace(xsins, "nil");
    Assert.assertTrue(EXISchema.NIL_NODE != a_nil);
    Assert.assertEquals("nil", corpus.getNameOfAttr(a_nil));
    Assert.assertEquals(XMLSCHEMA_INSTANCE_URI, corpus.getTargetNamespaceNameOfAttr(a_nil));
    Assert.assertEquals(booleanType, corpus.getTypeOfAttr(a_nil));

    int a_type = corpus.getAttrOfNamespace(xsins, "type");
    Assert.assertTrue(EXISchema.NIL_NODE != a_type);
    Assert.assertEquals("type", corpus.getNameOfAttr(a_type));
    Assert.assertEquals(XMLSCHEMA_INSTANCE_URI, corpus.getTargetNamespaceNameOfAttr(a_type));
    Assert.assertEquals(qnameType, corpus.getTypeOfAttr(a_type));

    int a_schemaLocation = corpus.getAttrOfNamespace(xsins, "schemaLocation");
    Assert.assertTrue(EXISchema.NIL_NODE != a_schemaLocation);
    Assert.assertEquals("schemaLocation", corpus.getNameOfAttr(a_schemaLocation));
    Assert.assertEquals(XMLSCHEMA_INSTANCE_URI, corpus.getTargetNamespaceNameOfAttr(a_schemaLocation));
    Assert.assertEquals(anyURIType, corpus.getTypeOfAttr(a_schemaLocation));

    int a_noNamespaceSchemaLocation = corpus.getAttrOfNamespace(xsins, "noNamespaceSchemaLocation");
    Assert.assertTrue(EXISchema.NIL_NODE != a_noNamespaceSchemaLocation);
    Assert.assertEquals("noNamespaceSchemaLocation", corpus.getNameOfAttr(a_noNamespaceSchemaLocation));
    Assert.assertEquals(XMLSCHEMA_INSTANCE_URI, corpus.getTargetNamespaceNameOfAttr(a_noNamespaceSchemaLocation));
    Assert.assertEquals(anyURIType, corpus.getTypeOfAttr(a_noNamespaceSchemaLocation));
  }

  /**
   * Test schema methods against a schema for default namespace
   */
  public void testSchemaDefault() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/verySimpleDefault.xsd", getClass());
        Assert.assertEquals(4, corpus.getElemCountOfSchema());
    Assert.assertEquals(4, corpus.getTotalElemCount());
    // 4 in XMLSchema-instance.xsd
    Assert.assertEquals(4, corpus.getAttrCountOfSchema());
    Assert.assertEquals(4, corpus.getTotalAttrCount());
    Assert.assertEquals(EXISchemaConst.N_BUILTIN_TYPES, EXISchemaUtil.countTypesOfSchema(corpus, true)); 
    Assert.assertEquals(EXISchemaConst.N_BUILTIN_TYPES - 1, EXISchemaUtil.countLinkedSimpleTypesOfSchema(corpus)); 
    Assert.assertEquals(EXISchemaConst.N_BUILTIN_TYPES - 1, corpus.getTotalSimpleTypeCount()); 
    Assert.assertEquals(3, corpus.getNamespaceCountOfSchema());
  }

  /**
   * Test namespace methods
   */
  public void testNamespace() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/verySimple.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals("urn:foo", corpus.getNameOfNamespace(foons));
    Assert.assertEquals(9, corpus.getElemCountOfNamespace(foons));
    Assert.assertEquals(1, corpus.getAttrCountOfNamespace(foons));
    Assert.assertEquals(1, corpus.getTypeCountOfNamespace(foons));

    int elemc = corpus.getElemOfNamespace(foons, "C");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elemc));

    int str10 = corpus.getTypeOfNamespace(foons, "string10");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(str10));

    int id = corpus.getAttrOfNamespace(foons, "id");
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
  }

  /**
   * Test element methods
   */
  public void testElement() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/verySimple.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int[] nodes = corpus.getNodes();

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    int goons = corpus.getNamespaceOfSchema("urn:goo");
    int goo_b = corpus.getElemOfNamespace(goons, "b");
    int goo_A = corpus.getAttrOfNamespace(goons, "A");

    Assert.assertEquals("b", corpus.getNameOfElem(goo_b));
    Assert.assertEquals("urn:goo", corpus.getTargetNamespaceNameOfElem(goo_b));
    Assert.assertFalse(corpus.isNillableElement(goo_b));
    Assert.assertFalse(corpus.isAbstractElement(goo_b));
    Assert.assertTrue(corpus.isSimpleTypedElement(goo_b));
    Assert.assertFalse(corpus.isUrTypedElement(goo_b));
    Assert.assertEquals(EXISchema.CONTENT_SIMPLE,
                      corpus.getContentClassOfElem(goo_b));
    int goo_string10 = corpus.getSimpleTypeOfElem(goo_b);
    Assert.assertEquals("string10", corpus.getNameOfType(goo_string10));
    Assert.assertEquals(0, corpus.getAttrUseCountOfElem(goo_b));
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getAttrWildcardOfElem(goo_b));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getGroupOfElem(goo_b));
    Assert.assertEquals(0, corpus.getGroupMinOccursOfElem(goo_b));
    Assert.assertEquals(0, corpus.getGroupMaxOccursOfElem(goo_b));
    Assert.assertEquals(0, corpus.getGroupHeadInstanceCountOfElem(goo_b));
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getGroupHeadInstanceListOfElem(goo_b));

    int foo_a = corpus.getElemOfNamespace(foons, "A");
    int foo_a_type = corpus.getTypeOfElem(foo_a);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(foo_a_type));
    Assert.assertFalse(corpus.isSimpleTypedElement(foo_a));
    Assert.assertFalse(corpus.isUrTypedElement(foo_a));
    Assert.assertEquals(EXISchema.CONTENT_ELEMENT_ONLY,
                      corpus.getContentClassOfElem(foo_a));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getSimpleTypeOfElem(foo_a));
    Assert.assertEquals(0, corpus.getAttrUseCountOfElem(foo_a));
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getAttrWildcardOfElem(foo_a));
    int a_group = corpus.getGroupOfElem(foo_a);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(a_group));
    Assert.assertEquals(3,  corpus.getGroupMinOccursOfElem(foo_a));
    Assert.assertEquals(10, corpus.getGroupMaxOccursOfElem(foo_a));
    Assert.assertEquals(2, corpus.getGroupHeadInstanceCountOfElem(foo_a));
    int heads = corpus.getGroupHeadInstanceListOfElem(foo_a);
    Assert.assertEquals(EXISchema.PARTICLE_NODE,
                      corpus.getNodeType(nodes[heads]));
    int elem_b = corpus.getTermOfParticle(nodes[heads]);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elem_b));
    Assert.assertEquals("B", corpus.getNameOfElem(elem_b));
    Assert.assertEquals(EXISchema.PARTICLE_NODE,
                      corpus.getNodeType(nodes[heads + 1]));
    int elem_c = corpus.getTermOfParticle(nodes[heads + 1]);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elem_c));
    Assert.assertEquals("C", corpus.getNameOfElem(elem_c));

    int foo_b = corpus.getElemOfNamespace(foons, "B");
    Assert.assertEquals(EXISchema.CONSTRAINT_DEFAULT, corpus.getConstraintOfElem(foo_b));
    int default_b = corpus.getConstraintValueOfElem(foo_b);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(default_b));
    Assert.assertEquals("baa", corpus.getStringValueOfVariant(default_b));
    Assert.assertTrue(corpus.isNillableElement(foo_b));
    int foo_b_type = corpus.getTypeOfElem(foo_b);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(foo_b_type));
    Assert.assertTrue(corpus.isSimpleTypedElement(foo_b));
    Assert.assertFalse(corpus.isUrTypedElement(foo_b));

    int foo_c = corpus.getElemOfNamespace(foons, "C");
    Assert.assertEquals(EXISchema.CONSTRAINT_FIXED, corpus.getConstraintOfElem(foo_c));
    int fixed_c = corpus.getConstraintValueOfElem(foo_c);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(fixed_c));
    Assert.assertEquals("abcdefghij", corpus.getStringValueOfVariant(fixed_c));

    int foo_d = corpus.getElemOfNamespace(foons, "D");
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE, corpus.getConstraintOfElem(foo_d));
    Assert.assertEquals(EXISchema.NIL_VALUE, corpus.getConstraintValueOfElem(foo_d));

    int foo_e = corpus.getElemOfNamespace(foons, "E");
    Assert.assertTrue(corpus.isAbstractElement(foo_e));

    int foo_f = corpus.getElemOfNamespace(foons, "F");
    Assert.assertTrue(corpus.isUrTypedElement(foo_f));

    int foo_g = corpus.getElemOfNamespace(foons, "G");
    Assert.assertTrue(corpus.isUrTypedElement(foo_g));

    int foo_h = corpus.getElemOfNamespace(foons, "H");
    Assert.assertTrue(corpus.isUrTypedElement(foo_h));

    int foo_i = corpus.getElemOfNamespace(foons, "I");
    int foo_string10 = corpus.getSimpleTypeOfElem(foo_i);
    Assert.assertEquals("string10", corpus.getNameOfType(foo_string10));
    Assert.assertEquals(2, corpus.getAttrUseCountOfElem(foo_i));
    int i_use_date = corpus.getAttrUseOfElem(foo_i, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE,
                      corpus.getNodeType(i_use_date));
    Assert.assertEquals(i_use_date, corpus.getAttrUseOfElem(foo_i, "", "date"));
    Assert.assertEquals(i_use_date, corpus.getAttrUseOrAttrOfElem(foo_i, "", "date"));
    int i_date = corpus.getAttrOfAttrUse(i_use_date);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(i_date));
    Assert.assertEquals("date", corpus.getNameOfAttr(i_date));
    int i_use_time = corpus.getAttrUseOfElem(foo_i, 1);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE,
                      corpus.getNodeType(i_use_time));
    Assert.assertEquals(i_use_time, corpus.getAttrUseOfElem(foo_i, "", "time"));
    Assert.assertEquals(i_use_time, corpus.getAttrUseOrAttrOfElem(foo_i, "", "time"));
    int i_time = corpus.getAttrOfAttrUse(i_use_time);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(i_time));
    Assert.assertEquals("time", corpus.getNameOfAttr(i_time));
    int i_wc = corpus.getAttrWildcardOfElem(foo_i);
    Assert.assertEquals(EXISchema.WILDCARD_NODE, corpus.getNodeType(i_wc));
    Assert.assertEquals(goo_A, corpus.getAttrUseOrAttrOfElem(foo_i, "urn:goo", "A"));
    // namespace "urn:goo" is available, but "B" is not.
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getAttrUseOrAttrOfElem(foo_i, "urn:goo", "B"));
    // namespace "urn:foo" is not permitted by the wildcard.
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getAttrUseOrAttrOfElem(foo_i, "urn:foo", "id"));
    // namespace "urn:xoo" is not available.
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getAttrUseOrAttrOfElem(foo_i, "urn:xoo", "none"));
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getAttrUseOrAttrOfElem(foo_i, null, "none"));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getGroupOfElem(foo_i));
    Assert.assertEquals(0, corpus.getGroupMinOccursOfElem(foo_i));
    Assert.assertEquals(0, corpus.getGroupMaxOccursOfElem(foo_i));
    Assert.assertEquals(0, corpus.getGroupHeadInstanceCountOfElem(foo_i));
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getGroupHeadInstanceListOfElem(foo_i));

    Assert.assertEquals(2, corpus.getSubstitutableCountOfElem(goo_b));
    int subst1 = corpus.getSubstitutableOfElem(goo_b, 0);
    int subst2 = corpus.getSubstitutableOfElem(goo_b, 1);
    Assert.assertTrue(subst1 == foo_c && subst2 == foo_d ||
                    subst1 == foo_d && subst2 == foo_c);
    Assert.assertEquals(goo_b, corpus.getSubstOfElem(foo_c));
    Assert.assertEquals(1, corpus.getSubstitutableCountOfElem(foo_c));
    Assert.assertEquals(foo_d, corpus.getSubstitutableOfElem(foo_c, 0));
    Assert.assertEquals(foo_c, corpus.getSubstOfElem(foo_d));
  }

  /**
   */
  public void testAttributeFixedValue() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixed.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int foo_decimal = corpus.getTypeOfNamespace(foons, "decimal");
    int use_decimal = corpus.getAttrUseOfComplexType(foo_decimal, "urn:foo", "decimal");
    int fixed_decimal = corpus.getConstraintValueOfAttrUse(use_decimal);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(fixed_decimal));
    Assert.assertEquals("1.2345", corpus.getDecimalValueOfVariant(fixed_decimal).toString());

    int foo_integer = corpus.getTypeOfNamespace(foons, "integer");
    int use_integer = corpus.getAttrUseOfComplexType(foo_integer, "urn:foo", "integer");
    int fixed_integer = corpus.getConstraintValueOfAttrUse(use_integer);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(fixed_integer));
    Assert.assertEquals(12345, corpus.getDecimalValueOfVariant(fixed_integer).intValue());

    int foo_long = corpus.getTypeOfNamespace(foons, "long");
    int use_long = corpus.getAttrUseOfComplexType(foo_long, "urn:foo", "long");
    int fixed_long = corpus.getConstraintValueOfAttrUse(use_long);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(fixed_long));
    Assert.assertEquals(100, corpus.getLongValueOfVariant(fixed_long));
    
    int foo_int = corpus.getTypeOfNamespace(foons, "int");
    int use_int = corpus.getAttrUseOfComplexType(foo_int, "urn:foo", "int");
    int fixed_int = corpus.getConstraintValueOfAttrUse(use_int);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(fixed_int));
    Assert.assertEquals(101, corpus.getIntValueOfVariant(fixed_int));

    int foo_short = corpus.getTypeOfNamespace(foons, "short");
    int use_short = corpus.getAttrUseOfComplexType(foo_short, "urn:foo", "short");
    int fixed_short = corpus.getConstraintValueOfAttrUse(use_short);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(fixed_short));
    Assert.assertEquals(102, corpus.getIntValueOfVariant(fixed_short));

    int foo_byte = corpus.getTypeOfNamespace(foons, "byte");
    int use_byte = corpus.getAttrUseOfComplexType(foo_byte, "urn:foo", "byte");
    int fixed_byte = corpus.getConstraintValueOfAttrUse(use_byte);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(fixed_byte));
    Assert.assertEquals(103, corpus.getIntValueOfVariant(fixed_byte));

    int foo_string = corpus.getTypeOfNamespace(foons, "string");
    int use_string = corpus.getAttrUseOfComplexType(foo_string, "urn:foo", "string");
    int fixed_string = corpus.getConstraintValueOfAttrUse(use_string);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(fixed_string));
    Assert.assertEquals("abc", corpus.getStringValueOfVariant(fixed_string));
    
    int foo_anySimpleType = corpus.getTypeOfNamespace(foons, "anySimpleType");
    int use_anySimpleType = corpus.getAttrUseOfComplexType(foo_anySimpleType, "urn:foo", "anySimpleType");
    int fixed_anySimpleType = corpus.getConstraintValueOfAttrUse(use_anySimpleType);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(fixed_anySimpleType));
    Assert.assertEquals("xyz", corpus.getStringValueOfVariant(fixed_anySimpleType));
    
    int foo_NMTOKEN = corpus.getTypeOfNamespace(foons, "NMTOKEN");
    int use_NMTOKEN = corpus.getAttrUseOfComplexType(foo_NMTOKEN, "urn:foo", "NMTOKEN");
    int fixed_NMTOKEN = corpus.getConstraintValueOfAttrUse(use_NMTOKEN);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(fixed_NMTOKEN));
    Assert.assertEquals("tokyo", corpus.getStringValueOfVariant(fixed_NMTOKEN));

    int foo_anyURI = corpus.getTypeOfNamespace(foons, "anyURI");
    int use_anyURI = corpus.getAttrUseOfComplexType(foo_anyURI, "urn:foo", "anyURI");
    int fixed_anyURI = corpus.getConstraintValueOfAttrUse(use_anyURI);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(fixed_anyURI));
    Assert.assertEquals("urn:foo", corpus.getStringValueOfVariant(fixed_anyURI));

    int foo_boolean = corpus.getTypeOfNamespace(foons, "boolean");
    int use_boolean = corpus.getAttrUseOfComplexType(foo_boolean, "urn:foo", "boolean");
    int fixed_boolean = corpus.getConstraintValueOfAttrUse(use_boolean);
    Assert.assertEquals(EXISchema.VARIANT_BOOLEAN, corpus.getTypeOfVariant(fixed_boolean));
    Assert.assertTrue(corpus.getBooleanValueOfVariant(fixed_boolean));
    
    int foo_QName = corpus.getTypeOfNamespace(foons, "QName");
    int use_QName = corpus.getAttrUseOfComplexType(foo_QName, "urn:foo", "QName");
    int fixed_QName = corpus.getConstraintValueOfAttrUse(use_QName);
    Assert.assertEquals(EXISchema.VARIANT_QNAME, corpus.getTypeOfVariant(fixed_QName));
    int qname = corpus.getQNameValueOfVariant(fixed_QName);
    Assert.assertEquals("urn:goo", corpus.getNamespaceNameOfQName(qname));
    Assert.assertEquals("xyz", corpus.getNameOfQName(qname));

    int foo_float = corpus.getTypeOfNamespace(foons, "float");
    int use_float = corpus.getAttrUseOfComplexType(foo_float, "urn:foo", "float");
    int fixed_float = corpus.getConstraintValueOfAttrUse(use_float);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(fixed_float));
    Assert.assertEquals(1.23, corpus.getFloatValueOfVariant(fixed_float), 0.001);
    
    int foo_double = corpus.getTypeOfNamespace(foons, "double");
    int use_double = corpus.getAttrUseOfComplexType(foo_double, "urn:foo", "double");
    int fixed_double = corpus.getConstraintValueOfAttrUse(use_double);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE, corpus.getTypeOfVariant(fixed_double));
    Assert.assertEquals(1.34, corpus.getDoubleValueOfVariant(fixed_double), 0.001);

    XSDateTime dateTime;
    
    int foo_dateTime = corpus.getTypeOfNamespace(foons, "dateTime");
    int use_dateTime = corpus.getAttrUseOfComplexType(foo_dateTime, "urn:foo", "dateTime");
    int fixed_dateTime = corpus.getConstraintValueOfAttrUse(use_dateTime);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(fixed_dateTime));
    dateTime = corpus.getDateTimeValueOfVariant(fixed_dateTime);
    // 2007-07-11T21:51:43+09:00
    Assert.assertEquals(2007, dateTime.getXMLGregorianCalendar().getYear());
    Assert.assertEquals(7, dateTime.getXMLGregorianCalendar().getMonth());
    Assert.assertEquals(11, dateTime.getXMLGregorianCalendar().getDay());
    Assert.assertEquals(21, dateTime.getXMLGregorianCalendar().getHour());
    Assert.assertEquals(51, dateTime.getXMLGregorianCalendar().getMinute());
    Assert.assertEquals(43, dateTime.getXMLGregorianCalendar().getSecond());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getMillisecond());
    Assert.assertEquals(9 * 60, dateTime.getXMLGregorianCalendar().getTimezone());
    
    int foo_time = corpus.getTypeOfNamespace(foons, "time");
    int use_time = corpus.getAttrUseOfComplexType(foo_time, "urn:foo", "time");
    int fixed_time = corpus.getConstraintValueOfAttrUse(use_time);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(fixed_time));
    dateTime = corpus.getDateTimeValueOfVariant(fixed_time);
    // 13:20:01-05:00
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getYear());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getMonth());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getDay());
    Assert.assertEquals(13, dateTime.getXMLGregorianCalendar().getHour());
    Assert.assertEquals(20, dateTime.getXMLGregorianCalendar().getMinute());
    Assert.assertEquals(1, dateTime.getXMLGregorianCalendar().getSecond());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getMillisecond());
    Assert.assertEquals(-5 * 60, dateTime.getXMLGregorianCalendar().getTimezone());

    int foo_date = corpus.getTypeOfNamespace(foons, "date");
    int use_date = corpus.getAttrUseOfComplexType(foo_date, "urn:foo", "date");
    int fixed_date = corpus.getConstraintValueOfAttrUse(use_date);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(fixed_date));
    dateTime = corpus.getDateTimeValueOfVariant(fixed_date);
    // 2003-04-25+09:00
    Assert.assertEquals(2003, dateTime.getXMLGregorianCalendar().getYear());
    Assert.assertEquals(4, dateTime.getXMLGregorianCalendar().getMonth());
    Assert.assertEquals(25, dateTime.getXMLGregorianCalendar().getDay());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getHour());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getMinute());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getSecond());
    Assert.assertEquals(9 * 60, dateTime.getXMLGregorianCalendar().getTimezone());

    int foo_gYearMonth = corpus.getTypeOfNamespace(foons, "gYearMonth");
    int use_gYearMonth = corpus.getAttrUseOfComplexType(foo_gYearMonth, "urn:foo", "gYearMonth");
    int fixed_gYearMonth = corpus.getConstraintValueOfAttrUse(use_gYearMonth);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(fixed_gYearMonth));
    dateTime = corpus.getDateTimeValueOfVariant(fixed_gYearMonth);
    // 2003-05+09:00
    Assert.assertEquals(2003, dateTime.getXMLGregorianCalendar().getYear());
    Assert.assertEquals(5, dateTime.getXMLGregorianCalendar().getMonth());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getDay());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getHour());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getMinute());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getSecond());
    Assert.assertEquals(9 * 60, dateTime.getXMLGregorianCalendar().getTimezone());
    
    int foo_gYear = corpus.getTypeOfNamespace(foons, "gYear");
    int use_gYear = corpus.getAttrUseOfComplexType(foo_gYear, "urn:foo", "gYear");
    int fixed_gYear = corpus.getConstraintValueOfAttrUse(use_gYear);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(fixed_gYear));
    dateTime = corpus.getDateTimeValueOfVariant(fixed_gYear);
    // 1970+09:00
    Assert.assertEquals(1970, dateTime.getXMLGregorianCalendar().getYear());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getMonth());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getDay());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getHour());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getMinute());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getSecond());
    Assert.assertEquals(9 * 60, dateTime.getXMLGregorianCalendar().getTimezone());
    
    int foo_gMonthDay = corpus.getTypeOfNamespace(foons, "gMonthDay");
    int use_gMonthDay = corpus.getAttrUseOfComplexType(foo_gMonthDay, "urn:foo", "gMonthDay");
    int fixed_gMonthDay = corpus.getConstraintValueOfAttrUse(use_gMonthDay);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(fixed_gMonthDay));
    dateTime = corpus.getDateTimeValueOfVariant(fixed_gMonthDay);
    // --09-17+09:00
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getYear());
    Assert.assertEquals(9, dateTime.getXMLGregorianCalendar().getMonth());
    Assert.assertEquals(17, dateTime.getXMLGregorianCalendar().getDay());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getHour());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getMinute());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getSecond());
    Assert.assertEquals(9 * 60, dateTime.getXMLGregorianCalendar().getTimezone());
    
    int foo_gDay = corpus.getTypeOfNamespace(foons, "gDay");
    int use_gDay = corpus.getAttrUseOfComplexType(foo_gDay, "urn:foo", "gDay");
    int fixed_gDay = corpus.getConstraintValueOfAttrUse(use_gDay);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(fixed_gDay));
    dateTime = corpus.getDateTimeValueOfVariant(fixed_gDay);
    // ---17+09:00
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getYear());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getMonth());
    Assert.assertEquals(17, dateTime.getXMLGregorianCalendar().getDay());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getHour());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getMinute());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getSecond());
    Assert.assertEquals(9 * 60, dateTime.getXMLGregorianCalendar().getTimezone());
    
    int foo_gMonth = corpus.getTypeOfNamespace(foons, "gMonth");
    int use_gMonth = corpus.getAttrUseOfComplexType(foo_gMonth, "urn:foo", "gMonth");
    int fixed_gMonth = corpus.getConstraintValueOfAttrUse(use_gMonth);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(fixed_gMonth));
    dateTime = corpus.getDateTimeValueOfVariant(fixed_gMonth);
    // --10+09:00
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getYear());
    Assert.assertEquals(10, dateTime.getXMLGregorianCalendar().getMonth());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getDay());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getHour());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getMinute());
    Assert.assertEquals(DatatypeConstants.FIELD_UNDEFINED, dateTime.getXMLGregorianCalendar().getSecond());
    Assert.assertEquals(9 * 60, dateTime.getXMLGregorianCalendar().getTimezone());
    
    int foo_duration = corpus.getTypeOfNamespace(foons, "duration");
    int use_duration = corpus.getAttrUseOfComplexType(foo_duration, "urn:foo", "duration");
    int fixed_duration = corpus.getConstraintValueOfAttrUse(use_duration);
    Assert.assertEquals(EXISchema.VARIANT_DURATION, corpus.getTypeOfVariant(fixed_duration));
    Assert.assertEquals(m_datatypeFactory.newDuration("P3DT10H30M"), 
        corpus.getDurationValueOfVariant(fixed_duration));
    
    int foo_base64Binary = corpus.getTypeOfNamespace(foons, "base64Binary");
    int use_base64Binary = corpus.getAttrUseOfComplexType(foo_base64Binary, "urn:foo", "base64Binary");
    int fixed_base64Binary = corpus.getConstraintValueOfAttrUse(use_base64Binary);
    Assert.assertEquals(EXISchema.VARIANT_BINARY, corpus.getTypeOfVariant(fixed_base64Binary));
    byte[] bytes = corpus.getBinaryValueOfVariant(fixed_base64Binary);
    Assert.assertEquals(10, bytes.length);
    Assert.assertEquals(65, bytes[0]);
    Assert.assertEquals(66, bytes[1]);
    Assert.assertEquals(67, bytes[2]);
    Assert.assertEquals(68, bytes[3]);
    Assert.assertEquals(69, bytes[4]);
    Assert.assertEquals(70, bytes[5]);
    Assert.assertEquals(71, bytes[6]);
    Assert.assertEquals(72, bytes[7]);
    Assert.assertEquals(73, bytes[8]);
    Assert.assertEquals(74, bytes[9]);
    
    int foo_hexBinary = corpus.getTypeOfNamespace(foons, "hexBinary");
    int use_hexBinary = corpus.getAttrUseOfComplexType(foo_hexBinary, "urn:foo", "hexBinary");
    int fixed_hexBinary = corpus.getConstraintValueOfAttrUse(use_hexBinary);
    Assert.assertEquals(EXISchema.VARIANT_BINARY, corpus.getTypeOfVariant(fixed_hexBinary));
    bytes = corpus.getBinaryValueOfVariant(fixed_hexBinary);
    Assert.assertEquals(10, bytes.length);
    Assert.assertEquals(65, bytes[0]);
    Assert.assertEquals(66, bytes[1]);
    Assert.assertEquals(67, bytes[2]);
    Assert.assertEquals(68, bytes[3]);
    Assert.assertEquals(69, bytes[4]);
    Assert.assertEquals(70, bytes[5]);
    Assert.assertEquals(71, bytes[6]);
    Assert.assertEquals(72, bytes[7]);
    Assert.assertEquals(73, bytes[8]);
    Assert.assertEquals(74, bytes[9]);

    // union type
    int foo_decimalOrINF = corpus.getTypeOfNamespace(foons, "decimalOrINF");
    use_decimal = corpus.getAttrUseOfComplexType(foo_decimalOrINF, "", "decimal");
    fixed_decimal = corpus.getConstraintValueOfAttrUse(use_decimal);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(fixed_decimal));
    Assert.assertEquals("4.0", corpus.getDecimalValueOfVariant(fixed_decimal).toString());
    int use_inf = corpus.getAttrUseOfComplexType(foo_decimalOrINF, "", "inf");
    int fixed_inf = corpus.getConstraintValueOfAttrUse(use_inf);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(fixed_inf));
    Assert.assertEquals("INF", corpus.getStringValueOfVariant(fixed_inf).toString());

    // list type
    int foo_listOfDecimal = corpus.getTypeOfNamespace(foons, "listOfDecimal");
    int use_decimals = corpus.getAttrUseOfComplexType(foo_listOfDecimal, "", "decimals");
    int fixed_decimals = corpus.getConstraintValueOfAttrUse(use_decimals);
    Assert.assertEquals(EXISchema.VARIANT_LIST, corpus.getTypeOfVariant(fixed_decimals));
    int[] list = corpus.getListValueOfVariant(fixed_decimals);
    Assert.assertEquals(3, list.length);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(list[0]));
    Assert.assertEquals("1.12", corpus.getDecimalValueOfVariant(list[0]).toString());
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(list[1]));
    Assert.assertEquals("2.23", corpus.getDecimalValueOfVariant(list[1]).toString());
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(list[2]));
    Assert.assertEquals("3.34", corpus.getDecimalValueOfVariant(list[2]).toString());

    // list of union
    int foo_listOfUnion = corpus.getTypeOfNamespace(foons, "listOfUnion");
    int use_listOfDecimalOrINF = corpus.getAttrUseOfComplexType(foo_listOfUnion, "", "listOfDecimalOrINF");
    int fixed_use_listOfDecimalOrINF = corpus.getConstraintValueOfAttrUse(use_listOfDecimalOrINF);
    Assert.assertEquals(EXISchema.VARIANT_LIST, corpus.getTypeOfVariant(fixed_use_listOfDecimalOrINF));
    list = corpus.getListValueOfVariant(fixed_use_listOfDecimalOrINF);
    Assert.assertEquals(3, list.length);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(list[0]));
    Assert.assertEquals("4.0", corpus.getDecimalValueOfVariant(list[0]).toString());
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(list[1]));
    Assert.assertEquals("INF", corpus.getStringValueOfVariant(list[1]).toString());
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(list[2]));
    Assert.assertEquals("1.23", corpus.getDecimalValueOfVariant(list[2]).toString());
  }
  
  /**
   * Test type methods against a simple type.
   */
  public void testTypeSimpleType() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int xsdns = corpus.getNamespaceOfSchema(XMLSCHEMA_URI);
    int _string = corpus.getTypeOfNamespace(xsdns, "string");
    int _normalizedString = corpus.getTypeOfNamespace(xsdns, "normalizedString");
    int _anySimpleType = corpus.getTypeOfNamespace(xsdns, "anySimpleType");

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int str10type = corpus.getTypeOfNamespace(foons, "string10");
    Assert.assertEquals("string10", corpus.getNameOfType(str10type));
    Assert.assertEquals("urn:foo", corpus.getTargetNamespaceNameOfType(str10type));
    Assert.assertEquals(_string, corpus.getBaseTypeOfType(str10type));
    Assert.assertTrue(corpus.hasSubType(str10type));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(str10type));
    Assert.assertFalse(corpus.isBuiltinSimpleType(str10type));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(str10type));
    Assert.assertFalse(corpus.isFixtureType(str10type));
    Assert.assertEquals(-1, corpus.getLengthFacetValueOfSimpleType(str10type));
    Assert.assertEquals(EXISchema.WHITESPACE_PRESERVE,
                      corpus.getWhitespaceFacetValueOfSimpleType(str10type));
    Assert.assertEquals(_string, corpus.getBuiltinTypeOfAtomicSimpleType(str10type));

    int str16type = corpus.getTypeOfNamespace(foons, "string16");
    Assert.assertEquals(_string, corpus.getBaseTypeOfType(str16type));
    Assert.assertFalse(corpus.hasSubType(str16type));
    Assert.assertEquals(16, corpus.getMaxLengthFacetValueOfSimpleType(str16type));
    Assert.assertEquals(_string, corpus.getBuiltinTypeOfAtomicSimpleType(str16type));

    int str3_10type = corpus.getTypeOfNamespace(foons, "string3_10");
    int _str10type = corpus.getBaseTypeOfType(str3_10type);
    Assert.assertFalse(corpus.hasSubType(str3_10type));
    Assert.assertEquals(str10type, _str10type);
    Assert.assertEquals(_string, corpus.getBuiltinTypeOfAtomicSimpleType(str3_10type));

    int foo_a = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(_anySimpleType, corpus.getTypeOfElem(foo_a));

    int str16list = corpus.getTypeOfNamespace(foons, "strings16");
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(str16list));
    Assert.assertFalse(corpus.hasSubType(str16list));
    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(str16list));
    Assert.assertFalse(corpus.isBuiltinSimpleType(str16list));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(str16list));
    Assert.assertFalse(corpus.isFixtureType(str16list));
    Assert.assertEquals(EXISchema.WHITESPACE_COLLAPSE,
                      corpus.getWhitespaceFacetValueOfSimpleType(str16list));
    Assert.assertEquals(str16type, corpus.getItemTypeOfListSimpleType(str16list));

    int length10 = corpus.getTypeOfNamespace(foons, "length10");
    Assert.assertEquals(10, corpus.getLengthFacetValueOfSimpleType(length10));
    Assert.assertEquals(-1, corpus.getMinLengthFacetValueOfSimpleType(length10));
    Assert.assertEquals(-1, corpus.getMaxLengthFacetValueOfSimpleType(length10));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(length10));
    Assert.assertEquals(-1, corpus.getTotalDigitsFacetValueOfSimpleType(length10));
    Assert.assertEquals(-1, corpus.getFractionDigitsFacetValueOfSimpleType(length10));

    int minLength10 = corpus.getTypeOfNamespace(foons, "minLength10");
    Assert.assertEquals(10, corpus.getMinLengthFacetValueOfSimpleType(minLength10));

    int pos;
    int[] nodes = corpus.getNodes();

    // pattern "(\d|\s)*"
    int digits10 = corpus.getTypeOfNamespace(foons, "digits10");
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(digits10));

    // pattern "[0-5\\s]*"
    int strictDigits8 = corpus.getTypeOfNamespace(foons, "strictDigits8");
    Assert.assertEquals(10, corpus.getRestrictedCharacterCountOfSimpleType(strictDigits8));
    pos = corpus.getRestrictedCharacterOfSimpleType(strictDigits8);
    Assert.assertEquals('\t', nodes[pos + 0]);
    Assert.assertEquals('\n', nodes[pos + 1]);
    Assert.assertEquals('\r', nodes[pos + 2]);
    Assert.assertEquals(' ', nodes[pos + 3]);
    Assert.assertEquals('0', nodes[pos + 4]);
    Assert.assertEquals('1', nodes[pos + 5]);
    Assert.assertEquals('2', nodes[pos + 6]);
    Assert.assertEquals('3', nodes[pos + 7]);
    Assert.assertEquals('4', nodes[pos + 8]);
    Assert.assertEquals('5', nodes[pos + 9]);

    // pattern "[01]*|[34]*"
    int strictDigits6 = corpus.getTypeOfNamespace(foons, "strictDigits6");
    Assert.assertEquals(4, corpus.getRestrictedCharacterCountOfSimpleType(strictDigits6));
    pos = corpus.getRestrictedCharacterOfSimpleType(strictDigits6);
    Assert.assertEquals('0', nodes[pos + 0]);
    Assert.assertEquals('1', nodes[pos + 1]);
    Assert.assertEquals('3', nodes[pos + 2]);
    Assert.assertEquals('4', nodes[pos + 3]);

    // pattern "[03]*|[14]*"
    int strictDigits4 = corpus.getTypeOfNamespace(foons, "strictDigits4");
    Assert.assertEquals(4, corpus.getRestrictedCharacterCountOfSimpleType(strictDigits4));
    pos = corpus.getRestrictedCharacterOfSimpleType(strictDigits4);
    Assert.assertEquals('0', nodes[pos + 0]);
    Assert.assertEquals('1', nodes[pos + 1]);
    Assert.assertEquals('3', nodes[pos + 2]);
    Assert.assertEquals('4', nodes[pos + 3]);

    // pattern "[04]*|[13]*"
    int strictDigits2 = corpus.getTypeOfNamespace(foons, "strictDigits2");
    Assert.assertEquals(4, corpus.getRestrictedCharacterCountOfSimpleType(strictDigits2));
    pos = corpus.getRestrictedCharacterOfSimpleType(strictDigits2);
    Assert.assertEquals('0', nodes[pos + 0]);
    Assert.assertEquals('1', nodes[pos + 1]);
    Assert.assertEquals('3', nodes[pos + 2]);
    Assert.assertEquals('4', nodes[pos + 3]);

    // pattern "[03]*|[14]*"
    int strictDigits3 = corpus.getTypeOfNamespace(foons, "strictDigits3");
    Assert.assertEquals(4, corpus.getRestrictedCharacterCountOfSimpleType(strictDigits3));
    pos = corpus.getRestrictedCharacterOfSimpleType(strictDigits3);
    Assert.assertEquals('0', nodes[pos + 0]);
    Assert.assertEquals('1', nodes[pos + 1]);
    Assert.assertEquals('3', nodes[pos + 2]);
    Assert.assertEquals('4', nodes[pos + 3]);

    // pattern "[03]*|[14]*"
    int strictDigits1 = corpus.getTypeOfNamespace(foons, "strictDigits1");
    Assert.assertEquals(4, corpus.getRestrictedCharacterCountOfSimpleType(strictDigits1));
    pos = corpus.getRestrictedCharacterOfSimpleType(strictDigits1);
    Assert.assertEquals('0', nodes[pos + 0]);
    Assert.assertEquals('1', nodes[pos + 1]);
    Assert.assertEquals('3', nodes[pos + 2]);
    Assert.assertEquals('4', nodes[pos + 3]);

    int replaceString = corpus.getTypeOfNamespace(foons, "replaceString");
    Assert.assertEquals(EXISchema.WHITESPACE_REPLACE,
                      corpus.getWhitespaceFacetValueOfSimpleType(replaceString));

    int decimal8_2 = corpus.getTypeOfNamespace(foons, "decimal8_2");
    Assert.assertEquals(8, corpus.getTotalDigitsFacetValueOfSimpleType(decimal8_2));
    Assert.assertEquals(2, corpus.getFractionDigitsFacetValueOfSimpleType(decimal8_2));

    int collapsedString = corpus.getTypeOfNamespace(foons, "collapsedString");
    Assert.assertEquals(EXISchema.WHITESPACE_COLLAPSE,
                      corpus.getWhitespaceFacetValueOfSimpleType(collapsedString));
    Assert.assertEquals(_normalizedString,
                      corpus.getBuiltinTypeOfAtomicSimpleType(collapsedString));
    Assert.assertEquals(_string,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(collapsedString));
  }

  /**
   * Test type methods against a complex type.
   */
  public void testTypeComplexType() throws Exception {
    EXISchema corpus =  EXISchemaFactoryTestUtil.getEXISchema("/complexType.xsd", getClass());
    // 46 (built-in) + 20 (global types, and content of "datedStringLength10")
    Assert.assertEquals(67, corpus.getTotalTypeCount());
    // 45 (built-in) + 2 ("string10", and content of "datedStringLength10") 
    Assert.assertEquals(47, corpus.getTotalSimpleTypeCount());
    
    int xsdns = corpus.getNamespaceOfSchema(XMLSCHEMA_URI);
    int _string = corpus.getTypeOfNamespace(xsdns, "string");

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int useOfDate, useOfTime;

    int datedString = corpus.getTypeOfNamespace(foons, "datedString");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE,
                      corpus.getNodeType(datedString));
    Assert.assertFalse(corpus.isFixtureType(datedString));
    Assert.assertEquals("datedString", corpus.getNameOfType(datedString));
    Assert.assertEquals("urn:foo",
                      corpus.getTargetNamespaceNameOfType(datedString));
    Assert.assertEquals(_string, corpus.getBaseTypeOfType(datedString));
    Assert.assertTrue(corpus.hasSubType(datedString));

    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(datedString));
    useOfDate = corpus.getAttrUseOfComplexType(datedString, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfDate));
    Assert.assertEquals(useOfDate, corpus.getAttrUseOfComplexType(datedString, "", "date"));

    /*
     * <complexContent>/<extension> is permitted to derive from simple-content
     * complex-type if the content of the derived type is empty. 
     */
    int datedString3 = corpus.getTypeOfNamespace(foons, "datedString3");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(datedString3));
    Assert.assertEquals(EXISchema.CONTENT_SIMPLE, corpus.getContentClassOfComplexType(datedString3));
    Assert.assertEquals(corpus.getBuiltinTypeOfSchema(EXISchemaConst.STRING_TYPE),
        corpus.getContentTypeOfComplexType(datedString3));
    Assert.assertEquals(datedString, corpus.getBaseTypeOfType(datedString3));
    Assert.assertFalse(corpus.hasSubType(datedString3));
    
    int dateTimedString = corpus.getTypeOfNamespace(foons, "dateTimedString");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(dateTimedString));
    Assert.assertFalse(corpus.isFixtureType(dateTimedString));
    Assert.assertEquals(2, corpus.getAttrUseCountOfComplexType(dateTimedString));
    useOfDate = corpus.getAttrUseOfComplexType(dateTimedString, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfDate));
    Assert.assertEquals(useOfDate, corpus.getAttrUseOfComplexType(dateTimedString, "", "date"));
    useOfTime = corpus.getAttrUseOfComplexType(dateTimedString, 1);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfTime));
    Assert.assertEquals(useOfTime, corpus.getAttrUseOfComplexType(dateTimedString, "", "time"));

    int namedString = corpus.getTypeOfNamespace(foons, "namedString");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(namedString));
    Assert.assertFalse(corpus.isFixtureType(namedString));
    Assert.assertEquals(0, corpus.getAttrUseCountOfComplexType(namedString));

    int choiceOccursZero = corpus.getTypeOfNamespace(foons, "choiceOccursZero");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(choiceOccursZero));
    Assert.assertFalse(corpus.isFixtureType(choiceOccursZero));
    Assert.assertEquals(EXISchema.CONTENT_EMPTY,
                      corpus.getContentClassOfComplexType(choiceOccursZero));
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getContentTypeOfComplexType(choiceOccursZero));
  }

  /**
   * Test Attribute Use node
   */
  public void testAttributeUse() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/complexType.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int datedString2 = corpus.getTypeOfNamespace(foons, "datedString2");

    int useOfAuthor = corpus.getAttrUseOfComplexType(datedString2, 0);
    int author = corpus.getAttrOfAttrUse(useOfAuthor);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(author));
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE,
                      corpus.getConstraintOfAttrUse(useOfAuthor));
    Assert.assertEquals(EXISchema.NIL_VALUE, corpus.getConstraintValueOfAttrUse(useOfAuthor));
    Assert.assertTrue(corpus.isRequiredAttrUse(useOfAuthor));

    int useOfDate = corpus.getAttrUseOfComplexType(datedString2, 1);
    int date = corpus.getAttrOfAttrUse(useOfDate);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(date));
    int dateGlobal = corpus.getAttrOfNamespace(foons, "date");
    Assert.assertEquals(dateGlobal, date);
    Assert.assertEquals(EXISchema.CONSTRAINT_FIXED,
                      corpus.getConstraintOfAttrUse(useOfDate));
    int fixedDate = corpus.getConstraintValueOfAttrUse(useOfDate);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(fixedDate));
    Assert.assertEquals("1999-05-31", corpus.getDateTimeValueOfVariant(fixedDate).getXMLGregorianCalendar().toString());
    Assert.assertFalse(corpus.isRequiredAttrUse(useOfDate));

    int useOfExpired = corpus.getAttrUseOfComplexType(datedString2, 2);
    int expired = corpus.getAttrOfAttrUse(useOfExpired);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(expired));
    Assert.assertEquals(EXISchema.CONSTRAINT_DEFAULT, corpus.getConstraintOfAttrUse(useOfExpired));
    int defaultExpired = corpus.getConstraintValueOfAttrUse(useOfExpired);
    Assert.assertEquals(EXISchema.VARIANT_BOOLEAN, corpus.getTypeOfVariant(defaultExpired));
    Assert.assertEquals(true, corpus.getBooleanValueOfVariant(defaultExpired));
    
    int useOfObsolete = corpus.getAttrUseOfComplexType(datedString2, 3);
    int obsolete = corpus.getAttrOfAttrUse(useOfObsolete);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(obsolete));
    Assert.assertEquals(EXISchema.CONSTRAINT_FIXED,
                      corpus.getConstraintOfAttrUse(useOfObsolete));
    int fixedObsolete = corpus.getConstraintValueOfAttrUse(useOfObsolete);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(fixedObsolete));
    Assert.assertEquals("yes", corpus.getStringValueOfVariant(fixedObsolete));
    Assert.assertFalse(corpus.isRequiredAttrUse(useOfObsolete));

    int useOfPublished = corpus.getAttrUseOfComplexType(datedString2, 4);
    int published = corpus.getAttrOfAttrUse(useOfPublished);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(published));
    Assert.assertEquals(EXISchema.CONSTRAINT_DEFAULT, corpus.getConstraintOfAttrUse(useOfPublished));
    int defaultPublished = corpus.getConstraintValueOfAttrUse(useOfPublished);
    Assert.assertEquals(EXISchema.VARIANT_BOOLEAN, corpus.getTypeOfVariant(defaultPublished));
    Assert.assertEquals(false, corpus.getBooleanValueOfVariant(defaultPublished));
    
    int useOfQName = corpus.getAttrUseOfComplexType(datedString2, 5);
    int qnameAttr = corpus.getAttrOfAttrUse(useOfQName);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(qnameAttr));
    Assert.assertEquals(EXISchema.CONSTRAINT_DEFAULT, corpus.getConstraintOfAttrUse(useOfQName));
    int defaultQName = corpus.getConstraintValueOfAttrUse(useOfQName);
    Assert.assertEquals(EXISchema.VARIANT_QNAME, corpus.getTypeOfVariant(defaultQName));
    int qname = corpus.getQNameValueOfVariant(defaultQName);
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", corpus.getNamespaceNameOfQName(qname));
    Assert.assertEquals("xyz", corpus.getNameOfQName(qname));

    int useOfType = corpus.getAttrUseOfComplexType(datedString2, 6);
    int type = corpus.getAttrOfAttrUse(useOfType);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(type));
    Assert.assertEquals(EXISchema.CONSTRAINT_DEFAULT,
                      corpus.getConstraintOfAttrUse(useOfType));
    int defaultType = corpus.getConstraintValueOfAttrUse(useOfType);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(defaultType));
    Assert.assertEquals("one", corpus.getStringValueOfVariant(defaultType));
    Assert.assertFalse(corpus.isRequiredAttrUse(useOfType));

    int nextAttrUses1;
    Assert.assertEquals(1, corpus.getNextAttrUsesCountOfComplexType(datedString2, 0));
    nextAttrUses1 = corpus.getNextAttrUsesOfComplexType(datedString2, 0);
    Assert.assertEquals(useOfAuthor, corpus.getNodes()[nextAttrUses1]);
    Assert.assertEquals(1, corpus.getNodes()[nextAttrUses1 + 1]);
    
    Assert.assertEquals(7, corpus.getNextAttrUsesCountOfComplexType(datedString2, 1));
    nextAttrUses1 = corpus.getNextAttrUsesOfComplexType(datedString2, 1);
    Assert.assertEquals(useOfDate, corpus.getNodes()[nextAttrUses1]);
    Assert.assertEquals(useOfExpired, corpus.getNodes()[nextAttrUses1 + 1]);
    Assert.assertEquals(useOfObsolete, corpus.getNodes()[nextAttrUses1 + 2]);
    Assert.assertEquals(useOfPublished, corpus.getNodes()[nextAttrUses1 + 3]);
    Assert.assertEquals(useOfQName, corpus.getNodes()[nextAttrUses1 + 4]);
    Assert.assertEquals(useOfType, corpus.getNodes()[nextAttrUses1 + 5]);
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[nextAttrUses1 + 6]);
    Assert.assertEquals(2, corpus.getNodes()[nextAttrUses1 + 7]);
    Assert.assertEquals(3, corpus.getNodes()[nextAttrUses1 + 8]);
    Assert.assertEquals(4, corpus.getNodes()[nextAttrUses1 + 9]);
    Assert.assertEquals(5, corpus.getNodes()[nextAttrUses1 + 10]);
    Assert.assertEquals(6, corpus.getNodes()[nextAttrUses1 + 11]);
    Assert.assertEquals(7, corpus.getNodes()[nextAttrUses1 + 12]);
    Assert.assertEquals(7, corpus.getNodes()[nextAttrUses1 + 13]);

    Assert.assertEquals(6, corpus.getNextAttrUsesCountOfComplexType(datedString2, 2));
    nextAttrUses1 = corpus.getNextAttrUsesOfComplexType(datedString2, 2);
    Assert.assertEquals(useOfExpired, corpus.getNodes()[nextAttrUses1]);
    Assert.assertEquals(useOfObsolete, corpus.getNodes()[nextAttrUses1 + 1]);
    Assert.assertEquals(useOfPublished, corpus.getNodes()[nextAttrUses1 + 2]);
    Assert.assertEquals(useOfQName, corpus.getNodes()[nextAttrUses1 + 3]);
    Assert.assertEquals(useOfType, corpus.getNodes()[nextAttrUses1 + 4]);
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[nextAttrUses1 + 5]);
    Assert.assertEquals(3, corpus.getNodes()[nextAttrUses1 + 6]);
    Assert.assertEquals(4, corpus.getNodes()[nextAttrUses1 + 7]);
    Assert.assertEquals(5, corpus.getNodes()[nextAttrUses1 + 8]);
    Assert.assertEquals(6, corpus.getNodes()[nextAttrUses1 + 9]);
    Assert.assertEquals(7, corpus.getNodes()[nextAttrUses1 + 10]);
    Assert.assertEquals(7, corpus.getNodes()[nextAttrUses1 + 11]);

    Assert.assertEquals(5, corpus.getNextAttrUsesCountOfComplexType(datedString2, 3));
    nextAttrUses1 = corpus.getNextAttrUsesOfComplexType(datedString2, 3);
    Assert.assertEquals(useOfObsolete, corpus.getNodes()[nextAttrUses1]);
    Assert.assertEquals(useOfPublished, corpus.getNodes()[nextAttrUses1 + 1]);
    Assert.assertEquals(useOfQName, corpus.getNodes()[nextAttrUses1 + 2]);
    Assert.assertEquals(useOfType, corpus.getNodes()[nextAttrUses1 + 3]);
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[nextAttrUses1 + 4]);
    Assert.assertEquals(4, corpus.getNodes()[nextAttrUses1 + 5]);
    Assert.assertEquals(5, corpus.getNodes()[nextAttrUses1 + 6]);
    Assert.assertEquals(6, corpus.getNodes()[nextAttrUses1 + 7]);
    Assert.assertEquals(7, corpus.getNodes()[nextAttrUses1 + 8]);
    Assert.assertEquals(7, corpus.getNodes()[nextAttrUses1 + 9]);
    
    Assert.assertEquals(4, corpus.getNextAttrUsesCountOfComplexType(datedString2, 4));
    nextAttrUses1 = corpus.getNextAttrUsesOfComplexType(datedString2, 4);
    Assert.assertEquals(useOfPublished, corpus.getNodes()[nextAttrUses1]);
    Assert.assertEquals(useOfQName, corpus.getNodes()[nextAttrUses1 + 1]);
    Assert.assertEquals(useOfType, corpus.getNodes()[nextAttrUses1 + 2]);
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[nextAttrUses1 + 3]);
    Assert.assertEquals(5, corpus.getNodes()[nextAttrUses1 + 4]);
    Assert.assertEquals(6, corpus.getNodes()[nextAttrUses1 + 5]);
    Assert.assertEquals(7, corpus.getNodes()[nextAttrUses1 + 6]);
    Assert.assertEquals(7, corpus.getNodes()[nextAttrUses1 + 7]);

    Assert.assertEquals(3, corpus.getNextAttrUsesCountOfComplexType(datedString2, 5));
    nextAttrUses1 = corpus.getNextAttrUsesOfComplexType(datedString2, 5);
    Assert.assertEquals(useOfQName, corpus.getNodes()[nextAttrUses1]);
    Assert.assertEquals(useOfType, corpus.getNodes()[nextAttrUses1 + 1]);
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[nextAttrUses1 + 2]);
    Assert.assertEquals(6, corpus.getNodes()[nextAttrUses1 + 3]);
    Assert.assertEquals(7, corpus.getNodes()[nextAttrUses1 + 4]);
    Assert.assertEquals(7, corpus.getNodes()[nextAttrUses1 + 5]);
    
    Assert.assertEquals(2, corpus.getNextAttrUsesCountOfComplexType(datedString2, 6));
    nextAttrUses1 = corpus.getNextAttrUsesOfComplexType(datedString2, 6);
    Assert.assertEquals(useOfType, corpus.getNodes()[nextAttrUses1 ]);
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[nextAttrUses1 + 1]);
    Assert.assertEquals(7, corpus.getNodes()[nextAttrUses1 + 2]);
    Assert.assertEquals(7, corpus.getNodes()[nextAttrUses1 + 3]);
  }

  /**
   * Invalid attribute default value "x y z" per NCName constraint. 
   */
  public void testAttributeDefaultNCName_01() throws Exception {
    
    EXISchema corpus;
    int foons, attrA, elemA, variant, atuse;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeDefaultOK03.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));

    attrA = corpus.getAttrOfNamespace(foons, "a");
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(attrA));
    elemA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elemA));
    atuse = corpus.getAttrUseOfElem(elemA, "urn:foo", "a");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(atuse));
    Assert.assertEquals(attrA, corpus.getAttrOfAttrUse(atuse));
    
    Assert.assertEquals(EXISchema.CONSTRAINT_DEFAULT, corpus.getConstraintOfAttrUse(atuse));
    variant = corpus.getConstraintValueOfAttrUse(atuse);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(variant));
    Assert.assertEquals("xyz", corpus.getStringValueOfVariant(variant));
    
    m_compilerErrorHandler.clear();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeDefaultNG03.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce1, sce2;
    XMLParseException se;
    int index;
    String message;
    sce1 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    sce2 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce1.getCode());
    Assert.assertEquals(9, sce1.getLocator().getLineNumber());
    se = (XMLParseException)sce1.getException();
    Assert.assertEquals(9, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("cvc-datatype-valid.1.2.1:"));
    
    message = sce1.getMessage();
    Assert.assertTrue((index = message.indexOf("'x y z'")) > 0);
    Assert.assertTrue(message.indexOf("NCName") > index);

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce2.getCode());
    Assert.assertEquals(9, sce2.getLocator().getLineNumber());
    se = (XMLParseException)sce2.getException();
    Assert.assertEquals(9, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("a-props-correct.2:"));
    
    message = sce2.getMessage();
    Assert.assertTrue((index = message.indexOf("'x y z'")) > 0);
    Assert.assertTrue(message.indexOf("'a'") > index);

    foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));

    attrA = corpus.getAttrOfNamespace(foons, "a");
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(attrA));
    elemA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elemA));
    atuse = corpus.getAttrUseOfElem(elemA, "urn:foo", "a");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(atuse));
    Assert.assertEquals(attrA, corpus.getAttrOfAttrUse(atuse));
    
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE, corpus.getConstraintOfAttrUse(atuse));
    Assert.assertEquals(EXISchema.NIL_VALUE, corpus.getConstraintValueOfAttrUse(atuse));
  }

  /**
   * Invalid attribute default value per length facet constraint. 
   */
  public void testAttributeDefaultLength_01() throws Exception {
    EXISchema corpus;
    int foons, ct, ause;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeDefaultOK01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    foons = corpus.getNamespaceOfSchema("urn:foo");

    ct = corpus.getTypeOfNamespace(foons, "ct");
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(ct));
    ause = corpus.getAttrUseOfComplexType(ct, 0);
    Assert.assertEquals(EXISchema.CONSTRAINT_DEFAULT, corpus.getConstraintOfAttrUse(ause));
    int variant = corpus.getConstraintValueOfAttrUse(ause);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(variant));
    Assert.assertEquals("X", corpus.getStringValueOfVariant(variant));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeDefaultNG01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce1, sce2;
    XMLParseException se;
    
    sce1 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    sce2 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce1.getCode());
    Assert.assertEquals(8, sce1.getLocator().getLineNumber());
    se = (XMLParseException)sce1.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("cvc-length-valid:"));

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce2.getCode());
    Assert.assertEquals(8, sce2.getLocator().getLineNumber());
    se = (XMLParseException)sce2.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("a-props-correct.2:"));
    
    foons = corpus.getNamespaceOfSchema("urn:foo");

    ct = corpus.getTypeOfNamespace(foons, "ct");
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(ct));
    ause = corpus.getAttrUseOfComplexType(ct, 0);
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE, corpus.getConstraintOfAttrUse(ause));
    Assert.assertEquals(EXISchema.NIL_VALUE, corpus.getConstraintValueOfAttrUse(ause));
  }

  /**
   * Invalid attribute default value per length facet constraint 
   * with the use of characters in SIP (Supplementary Ideographic Plane).
   */
  public void testAttributeDefaultLength_CharacterInSIP_01() throws Exception {
    EXISchema corpus;
    int foons, ct, ause;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeDefaultOK02.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    foons = corpus.getNamespaceOfSchema("urn:foo");

    ct = corpus.getTypeOfNamespace(foons, "ct");
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(ct));
    ause = corpus.getAttrUseOfComplexType(ct, 0);
    Assert.assertEquals(EXISchema.CONSTRAINT_DEFAULT, corpus.getConstraintOfAttrUse(ause));
    int variant = corpus.getConstraintValueOfAttrUse(ause);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(variant));
    // single character in SIP (U+2000B)
    Assert.assertEquals("\uD840\uDC0B", corpus.getStringValueOfVariant(variant));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeDefaultNG02.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce.getCode());
    Assert.assertEquals(8, sce.getLocator().getLineNumber());
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("cvc-length-valid:"));

    foons = corpus.getNamespaceOfSchema("urn:foo");

    ct = corpus.getTypeOfNamespace(foons, "ct");
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(ct));
    ause = corpus.getAttrUseOfComplexType(ct, 0);
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE, corpus.getConstraintOfAttrUse(ause));
    Assert.assertEquals(EXISchema.NIL_VALUE, corpus.getConstraintValueOfAttrUse(ause));
  }

  /**
   * Invalid attribute fixed value per length facet constraint. 
   */
  public void testAttributeFixedLength_01() throws Exception {
    EXISchema corpus;
    int foons, ct, ause;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedOK01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    foons = corpus.getNamespaceOfSchema("urn:foo");

    ct = corpus.getTypeOfNamespace(foons, "ct");
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(ct));
    ause = corpus.getAttrUseOfComplexType(ct, 0);
    Assert.assertEquals(EXISchema.CONSTRAINT_FIXED, corpus.getConstraintOfAttrUse(ause));
    int variant = corpus.getConstraintValueOfAttrUse(ause);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(variant));
    Assert.assertEquals("X", corpus.getStringValueOfVariant(variant));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedNG01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce1, sce2;
    XMLParseException se;
    sce1 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    sce2 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce1.getCode());
    Assert.assertEquals(8, sce1.getLocator().getLineNumber());
    se = (XMLParseException)sce1.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("cvc-length-valid:"));

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce2.getCode());
    Assert.assertEquals(8, sce2.getLocator().getLineNumber());
    se = (XMLParseException)sce2.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("a-props-correct.2:"));

    foons = corpus.getNamespaceOfSchema("urn:foo");

    ct = corpus.getTypeOfNamespace(foons, "ct");
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(ct));
    ause = corpus.getAttrUseOfComplexType(ct, 0);
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE, corpus.getConstraintOfAttrUse(ause));
    Assert.assertEquals(EXISchema.NIL_VALUE, corpus.getConstraintValueOfAttrUse(ause));
  }

  /**
   * Invalid attribute fixed value per length facet constraint 
   * with the use of characters in SIP (Supplementary Ideographic Plane).
   */
  public void testAttributeFixedLength_CharacterInSIP_01() throws Exception {
    EXISchema corpus;
    int foons, ct, ause;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedOK02.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    foons = corpus.getNamespaceOfSchema("urn:foo");

    ct = corpus.getTypeOfNamespace(foons, "ct");
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(ct));
    ause = corpus.getAttrUseOfComplexType(ct, 0);
    Assert.assertEquals(EXISchema.CONSTRAINT_FIXED, corpus.getConstraintOfAttrUse(ause));
    int variant = corpus.getConstraintValueOfAttrUse(ause);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(variant));
    // single character in SIP (U+2000B)
    Assert.assertEquals("\uD840\uDC0B", corpus.getStringValueOfVariant(variant));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedNG02.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce1, sce2;
    XMLParseException se;
    sce1 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    sce2 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce1.getCode());
    Assert.assertEquals(8, sce1.getLocator().getLineNumber());
    se = (XMLParseException)sce1.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("cvc-length-valid:"));

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce2.getCode());
    Assert.assertEquals(8, sce2.getLocator().getLineNumber());
    se = (XMLParseException)sce2.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("a-props-correct.2:"));

    foons = corpus.getNamespaceOfSchema("urn:foo");

    ct = corpus.getTypeOfNamespace(foons, "ct");
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(ct));
    ause = corpus.getAttrUseOfComplexType(ct, 0);
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE, corpus.getConstraintOfAttrUse(ause));
    Assert.assertEquals(EXISchema.NIL_VALUE, corpus.getConstraintValueOfAttrUse(ause));
  }

  /**
   * Test Attribute node
   */
  public void testAttribute() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexType.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int xsdns = corpus.getNamespaceOfSchema(XMLSCHEMA_URI);
    int _anySimpleType = corpus.getTypeOfNamespace(xsdns, "anySimpleType");

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int name10 = corpus.getAttrOfNamespace(foons, "name10");
    Assert.assertEquals("name10", corpus.getNameOfAttr(name10));
    Assert.assertEquals("urn:foo", corpus.getTargetNamespaceNameOfAttr(name10));
    int string10 = corpus.getTypeOfAttr(name10);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE,
                      corpus.getNodeType(string10));
    int string10Global = corpus.getTypeOfNamespace(foons, "string10");
    Assert.assertEquals(string10Global, string10);
    Assert.assertEquals(EXISchema.CONSTRAINT_DEFAULT,
                      corpus.getConstraintOfAttr(name10));
    int variant = corpus.getConstraintValueOfAttr(name10);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(variant));
    Assert.assertEquals("pikachu", corpus.getStringValueOfVariant(variant));
    Assert.assertTrue(corpus.isGlobalAttribute(name10));

    int datedString2 = corpus.getTypeOfNamespace(foons, "datedString2");
    int useOfAuthor = corpus.getAttrUseOfComplexType(datedString2, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfAuthor));
    Assert.assertEquals(useOfAuthor, corpus.getAttrUseOfComplexType(datedString2, "", "author"));
    int author = corpus.getAttrOfAttrUse(useOfAuthor);
    Assert.assertEquals("author", corpus.getNameOfAttr(author));
    Assert.assertEquals("", corpus.getTargetNamespaceNameOfAttr(author));
    Assert.assertEquals(_anySimpleType, corpus.getTypeOfAttr(author));
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE,
                      corpus.getConstraintOfAttr(author));
    Assert.assertEquals(EXISchema.NIL_VALUE, corpus.getConstraintValueOfAttr(author));
    Assert.assertFalse(corpus.isGlobalAttribute(author));
    
    int useOfObsolete = corpus.getAttrUseOfComplexType(datedString2, 3);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfObsolete));
    Assert.assertEquals(useOfObsolete, corpus.getAttrUseOfComplexType(datedString2, "", "obsolete"));
  }
  
  /**
   * Test maxInclusive variant values
   */
  public void testMaxInclusive() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/maxInclusive.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    int maxInclusiveFacet;

    int decimalDerived = corpus.getTypeOfNamespace(foons, "decimalDerived");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(decimalDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL,
                      corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(0, new BigDecimal(100).compareTo(corpus.getDecimalValueOfVariant(maxInclusiveFacet)));

    int integerDerived = corpus.getTypeOfNamespace(foons, "integerDerived");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(integerDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(1000, corpus.getIntValueOfVariant(maxInclusiveFacet));

    int integerDerived2 = corpus.getTypeOfNamespace(foons, "integerDerived2");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(integerDerived2);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(2147483647, corpus.getIntValueOfVariant(maxInclusiveFacet));

    int integerDerived3 = corpus.getTypeOfNamespace(foons, "integerDerived3");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(integerDerived3);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(2147483648L, corpus.getLongValueOfVariant(maxInclusiveFacet));

    int integerDerived4 = corpus.getTypeOfNamespace(foons, "integerDerived4");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(integerDerived4);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(12678967543233L, corpus.getLongValueOfVariant(maxInclusiveFacet));

    int integerDerived5 = corpus.getTypeOfNamespace(foons, "integerDerived5");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(integerDerived5);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(9223372036854775807L, corpus.getLongValueOfVariant(maxInclusiveFacet));

    int integerDerived6 = corpus.getTypeOfNamespace(foons, "integerDerived6");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(integerDerived6);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INTEGER, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals("9223372036854775808", corpus.getIntegerValueOfVariant(maxInclusiveFacet).toString()); 

    int floatDerived = corpus.getTypeOfNamespace(foons, "floatDerived");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(floatDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT,
                      corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals((float)101, corpus.getFloatValueOfVariant(maxInclusiveFacet), 0);

    int doubleDerived = corpus.getTypeOfNamespace(foons, "doubleDerived");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(doubleDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE,
                      corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals((double)102, corpus.getDoubleValueOfVariant(maxInclusiveFacet), 0);

    int intDerived = corpus.getTypeOfNamespace(foons, "intDerived");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(intDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT,
                      corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals((int)103, corpus.getIntValueOfVariant(maxInclusiveFacet));

    int longDerived = corpus.getTypeOfNamespace(foons, "longDerived");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(longDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(104, corpus.getIntValueOfVariant(maxInclusiveFacet));

    int longDerived2 = corpus.getTypeOfNamespace(foons, "longDerived2");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(longDerived2);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_LONG,
                      corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(12678967543233L, corpus.getLongValueOfVariant(maxInclusiveFacet));

    int longDerived3 = corpus.getTypeOfNamespace(foons, "longDerived3");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(longDerived3);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(9223372036854775807L, corpus.getLongValueOfVariant(maxInclusiveFacet));

    int dateTimeDerived = corpus.getTypeOfNamespace(foons, "dateTimeDerived");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(dateTimeDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME,
                      corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals("2003-03-19T13:20:00-05:00",
                      corpus.getDateTimeValueOfVariant(maxInclusiveFacet).getXMLGregorianCalendar().toString());

    int durationDerived = corpus.getTypeOfNamespace(foons, "durationDerived");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(durationDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DURATION,
                      corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertTrue(m_datatypeFactory.newDuration("P1Y2M3DT10H30M").equals(
                      corpus.getDurationValueOfVariant(maxInclusiveFacet)));
  }

  /**
   * Test maxExclusive variant values
   */
  public void testMaxExclusive() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/maxExclusive.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    int maxExclusiveFacet;
    int maxInclusiveFacet;

    int decimalDerived = corpus.getTypeOfNamespace(foons, "decimalDerived");
    maxExclusiveFacet = corpus.getMaxExclusiveFacetOfSimpleType(decimalDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxExclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL,
                      corpus.getTypeOfVariant(maxExclusiveFacet));
    Assert.assertEquals(0, new BigDecimal(100).compareTo(
        corpus.getDecimalValueOfVariant(maxExclusiveFacet)));

    int integerDerived = corpus.getTypeOfNamespace(foons, "integerDerived");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(integerDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(999, corpus.getIntValueOfVariant(maxInclusiveFacet));

    int integerDerived2 = corpus.getTypeOfNamespace(foons, "integerDerived2");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(integerDerived2);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(2147483647, corpus.getIntValueOfVariant(maxInclusiveFacet));

    int integerDerived3 = corpus.getTypeOfNamespace(foons, "integerDerived3");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(integerDerived3);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(2147483648L, corpus.getLongValueOfVariant(maxInclusiveFacet));

    int integerDerived4 = corpus.getTypeOfNamespace(foons, "integerDerived4");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(integerDerived4);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(12678967543232L, corpus.getLongValueOfVariant(maxInclusiveFacet));

    int integerDerived5 = corpus.getTypeOfNamespace(foons, "integerDerived5");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(integerDerived5);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(9223372036854775807L, corpus.getLongValueOfVariant(maxInclusiveFacet));

    int integerDerived6 = corpus.getTypeOfNamespace(foons, "integerDerived6");
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(integerDerived6);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INTEGER, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals("9223372036854775808", corpus.getIntegerValueOfVariant(maxInclusiveFacet).toString()); 
    
    int floatDerived = corpus.getTypeOfNamespace(foons, "floatDerived");
    maxExclusiveFacet = corpus.getMaxExclusiveFacetOfSimpleType(floatDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxExclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT,
                      corpus.getTypeOfVariant(maxExclusiveFacet));
    Assert.assertEquals((float)101, corpus.getFloatValueOfVariant(maxExclusiveFacet), 0);

    int doubleDerived = corpus.getTypeOfNamespace(foons, "doubleDerived");
    maxExclusiveFacet = corpus.getMaxExclusiveFacetOfSimpleType(doubleDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxExclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE,
                      corpus.getTypeOfVariant(maxExclusiveFacet));
    Assert.assertEquals((double)102, corpus.getDoubleValueOfVariant(maxExclusiveFacet), 0);

    int intDerived = corpus.getTypeOfNamespace(foons, "intDerived");
    maxExclusiveFacet = corpus.getMaxExclusiveFacetOfSimpleType(intDerived);
    Assert.assertEquals(EXISchema.NIL_VALUE, maxExclusiveFacet);
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(intDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT,
                      corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals((int)102, corpus.getIntValueOfVariant(maxInclusiveFacet));

    int longDerived = corpus.getTypeOfNamespace(foons, "longDerived");
    maxExclusiveFacet = corpus.getMaxExclusiveFacetOfSimpleType(longDerived);
    Assert.assertEquals(EXISchema.NIL_VALUE, maxExclusiveFacet);
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(longDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(103, corpus.getIntValueOfVariant(maxInclusiveFacet));

    int longDerived2 = corpus.getTypeOfNamespace(foons, "longDerived2");
    maxExclusiveFacet = corpus.getMaxExclusiveFacetOfSimpleType(longDerived2);
    Assert.assertEquals(EXISchema.NIL_VALUE, maxExclusiveFacet);
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(longDerived2);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(12678967543232L, corpus.getLongValueOfVariant(maxInclusiveFacet));

    int longDerived3 = corpus.getTypeOfNamespace(foons, "longDerived3");
    maxExclusiveFacet = corpus.getMaxExclusiveFacetOfSimpleType(longDerived3);
    Assert.assertEquals(EXISchema.NIL_VALUE, maxExclusiveFacet);
    maxInclusiveFacet = corpus.getMaxInclusiveFacetOfSimpleType(longDerived3);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(maxInclusiveFacet));
    Assert.assertEquals(9223372036854775806L, corpus.getLongValueOfVariant(maxInclusiveFacet));

    int dateTimeDerived = corpus.getTypeOfNamespace(foons, "dateTimeDerived");
    maxExclusiveFacet = corpus.getMaxExclusiveFacetOfSimpleType(dateTimeDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxExclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME,
                      corpus.getTypeOfVariant(maxExclusiveFacet));
    Assert.assertEquals("2003-03-19T13:20:00-05:00",
                      corpus.getDateTimeValueOfVariant(maxExclusiveFacet).getXMLGregorianCalendar().toString());

    int durationDerived = corpus.getTypeOfNamespace(foons, "durationDerived");
    maxExclusiveFacet = corpus.getMaxExclusiveFacetOfSimpleType(durationDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxExclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DURATION,
                      corpus.getTypeOfVariant(maxExclusiveFacet));
    Assert.assertTrue(m_datatypeFactory.newDuration("P1Y2M3DT10H30M").equals(
                      corpus.getDurationValueOfVariant(maxExclusiveFacet)));
  }

  /**
   * The maxExclusive facet of a derived type can take the same value
   * as the maxExclusive facet of the base type.
   */
  public void testMaxExclusiveDerivationDecimal_01() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/maxExclusiveDerivationDecimal_OK01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons;
    int decimalDerived, decimalDerived2;
    int maxExclusiveFacet, maxExclusiveFacet2;
    
    foons = corpus.getNamespaceOfSchema("urn:foo");
    
    decimalDerived = corpus.getTypeOfNamespace(foons, "decimalDerived");
    maxExclusiveFacet = corpus.getMaxExclusiveFacetOfSimpleType(decimalDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxExclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(maxExclusiveFacet));
    Assert.assertEquals(0, BigDecimal.valueOf(100).compareTo(
        corpus.getDecimalValueOfVariant(maxExclusiveFacet)));

    decimalDerived2 = corpus.getTypeOfNamespace(foons, "decimalDerived2");
    maxExclusiveFacet2 = corpus.getMaxExclusiveFacetOfSimpleType(decimalDerived2);
    Assert.assertTrue(EXISchema.NIL_VALUE != maxExclusiveFacet2);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(maxExclusiveFacet2));
    Assert.assertEquals(0, BigDecimal.valueOf(100).compareTo(
        corpus.getDecimalValueOfVariant(maxExclusiveFacet2)));

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/maxExclusiveDerivationDecimal_NG01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    Assert.assertEquals(14, sce.getLocator().getLineNumber());
  }
  
  /**
   * Test minExclusive variant values
   */
  public void testMinExclusive() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/minExclusive.xsd", getClass());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    int minExclusiveFacet;
    int minInclusiveFacet;

    int decimalDerived = corpus.getTypeOfNamespace(foons, "decimalDerived");
    minExclusiveFacet = corpus.getMinExclusiveFacetOfSimpleType(decimalDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != minExclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL,
                      corpus.getTypeOfVariant(minExclusiveFacet));
    Assert.assertEquals(0, new BigDecimal(100).compareTo(
                      corpus.getDecimalValueOfVariant(minExclusiveFacet)));

    int floatDerived = corpus.getTypeOfNamespace(foons, "floatDerived");
    minExclusiveFacet = corpus.getMinExclusiveFacetOfSimpleType(floatDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != minExclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT,
                      corpus.getTypeOfVariant(minExclusiveFacet));
    Assert.assertEquals((float)101, corpus.getFloatValueOfVariant(minExclusiveFacet), 0);

    int doubleDerived = corpus.getTypeOfNamespace(foons, "doubleDerived");
    minExclusiveFacet = corpus.getMinExclusiveFacetOfSimpleType(doubleDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != minExclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE,
                      corpus.getTypeOfVariant(minExclusiveFacet));
    Assert.assertEquals((double)102, corpus.getDoubleValueOfVariant(minExclusiveFacet), 0);

    int intDerived = corpus.getTypeOfNamespace(foons, "intDerived");
    minExclusiveFacet = corpus.getMinExclusiveFacetOfSimpleType(intDerived);
    Assert.assertEquals(EXISchema.NIL_VALUE, minExclusiveFacet);
    minInclusiveFacet = corpus.getMinInclusiveFacetOfSimpleType(intDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT,
                      corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertEquals((int)104, corpus.getIntValueOfVariant(minInclusiveFacet));

    int longDerived = corpus.getTypeOfNamespace(foons, "longDerived");
    minExclusiveFacet = corpus.getMinExclusiveFacetOfSimpleType(longDerived);
    Assert.assertEquals(EXISchema.NIL_VALUE, minExclusiveFacet);
    minInclusiveFacet = corpus.getMinInclusiveFacetOfSimpleType(longDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertEquals(105, corpus.getIntValueOfVariant(minInclusiveFacet));

    int longDerived2 = corpus.getTypeOfNamespace(foons, "longDerived2");
    minExclusiveFacet = corpus.getMinExclusiveFacetOfSimpleType(longDerived2);
    Assert.assertEquals(EXISchema.NIL_VALUE, minExclusiveFacet);
    minInclusiveFacet = corpus.getMinInclusiveFacetOfSimpleType(longDerived2);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertEquals(-12678967543232L, corpus.getLongValueOfVariant(minInclusiveFacet));

    int dateTimeDerived = corpus.getTypeOfNamespace(foons, "dateTimeDerived");
    minExclusiveFacet = corpus.getMinExclusiveFacetOfSimpleType(dateTimeDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != minExclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME,
                      corpus.getTypeOfVariant(minExclusiveFacet));
    Assert.assertEquals("2003-03-19T13:20:00-05:00",
                      corpus.getDateTimeValueOfVariant(minExclusiveFacet).getXMLGregorianCalendar().toString());

    int durationDerived = corpus.getTypeOfNamespace(foons, "durationDerived");
    minExclusiveFacet = corpus.getMinExclusiveFacetOfSimpleType(durationDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != minExclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DURATION,
                      corpus.getTypeOfVariant(minExclusiveFacet));
    Assert.assertTrue(m_datatypeFactory.newDuration("P1Y2M3DT10H30M").equals(
                      corpus.getDurationValueOfVariant(minExclusiveFacet)));
  }

  /**
   * Test minInclusive variant values
   */
  public void testMinInclusive() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd", getClass());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    int minInclusiveFacet;

    int decimalDerived = corpus.getTypeOfNamespace(foons, "decimalDerived");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfSimpleType(decimalDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL,
                      corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertEquals(0, new BigDecimal(100).compareTo(
                      corpus.getDecimalValueOfVariant(minInclusiveFacet)));

    int floatDerived = corpus.getTypeOfNamespace(foons, "floatDerived");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfSimpleType(floatDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT,
                      corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertEquals((float)101, corpus.getFloatValueOfVariant(minInclusiveFacet), 0);

    int doubleDerived = corpus.getTypeOfNamespace(foons, "doubleDerived");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfSimpleType(doubleDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE,
                      corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertEquals((double)102, corpus.getDoubleValueOfVariant(minInclusiveFacet), 0);

    int intDerived = corpus.getTypeOfNamespace(foons, "intDerived");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfSimpleType(intDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT,
                      corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertEquals((int)103, corpus.getIntValueOfVariant(minInclusiveFacet));

    int longDerived = corpus.getTypeOfNamespace(foons, "longDerived");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfSimpleType(longDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT,
                      corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertEquals(104, corpus.getIntValueOfVariant(minInclusiveFacet));

    int longDerived2 = corpus.getTypeOfNamespace(foons, "longDerived2");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfSimpleType(longDerived2);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_LONG,
                      corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertEquals(-12678967543233L, corpus.getLongValueOfVariant(minInclusiveFacet));

    int dateTimeDerived = corpus.getTypeOfNamespace(foons, "dateTimeDerived");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfSimpleType(dateTimeDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME,
                      corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertEquals("2003-03-19T13:20:00-05:00",
                      corpus.getDateTimeValueOfVariant(minInclusiveFacet).getXMLGregorianCalendar().toString());

    int durationDerived = corpus.getTypeOfNamespace(foons, "durationDerived");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfSimpleType(durationDerived);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_DURATION,
                      corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertTrue(m_datatypeFactory.newDuration("P1Y2M3DT10H30M").equals(
                      corpus.getDurationValueOfVariant(minInclusiveFacet)));
  }

  /**
   * The fractionDigits facet of a derived type is not permitted to
   * take a value greater than that of the base type.
   */
  public void testFractionDigitsDerivationDecimal_01() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fractionDigitsDerivationDecimal_OK01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons;
    int decimalDerived, decimalDerived2;
    int fractionDigits, fractionDigits2;
    
    foons = corpus.getNamespaceOfSchema("urn:foo");
    
    decimalDerived = corpus.getTypeOfNamespace(foons, "decimalDerived");
    fractionDigits = corpus.getFractionDigitsFacetValueOfSimpleType(decimalDerived);
    Assert.assertEquals(2, fractionDigits);

    decimalDerived2 = corpus.getTypeOfNamespace(foons, "decimalDerived2");
    fractionDigits2 = corpus.getFractionDigitsFacetValueOfSimpleType(decimalDerived2);
    Assert.assertEquals(1, fractionDigits2);

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/fractionDigitsDerivationDecimal_NG01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    Assert.assertEquals(14, sce.getLocator().getLineNumber());
  }
  
  /**
   * Test enumeration variant values
   */
  public void testEnumeration() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int e1, e2, e3, e4, e5, e6, e7, e8, e9;

    int stringDerived = corpus.getTypeOfNamespace(foons, "stringDerived");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfSimpleType(stringDerived));
    e1 = corpus.getEnumerationFacetOfSimpleType(stringDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e1));
    Assert.assertEquals("Tokyo", corpus.getStringValueOfVariant(e1));
    e2 = corpus.getEnumerationFacetOfSimpleType(stringDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e2));
    Assert.assertEquals("Osaka", corpus.getStringValueOfVariant(e2));
    e3 = corpus.getEnumerationFacetOfSimpleType(stringDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e3));
    Assert.assertEquals("Nagoya", corpus.getStringValueOfVariant(e3));

    int qname;

    int qNameDerived = corpus.getTypeOfNamespace(foons, "qNameDerived");
    Assert.assertEquals(2, corpus.getEnumerationFacetCountOfSimpleType(qNameDerived));
    e1 = corpus.getEnumerationFacetOfSimpleType(qNameDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_QNAME, corpus.getTypeOfVariant(e1));
    qname = corpus.getQNameValueOfVariant(e1);
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfQName(qname));
    Assert.assertEquals("A", corpus.getNameOfQName(qname));
    e2 = corpus.getEnumerationFacetOfSimpleType(qNameDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_QNAME, corpus.getTypeOfVariant(e2));
    qname = corpus.getQNameValueOfVariant(e2);
    Assert.assertEquals("urn:goo", corpus.getNamespaceNameOfQName(qname));
    Assert.assertEquals("A", corpus.getNameOfQName(qname));

    int notationDerived = corpus.getTypeOfNamespace(foons, "notationDerived");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfSimpleType(notationDerived));
    e1 = corpus.getEnumerationFacetOfSimpleType(notationDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_QNAME, corpus.getTypeOfVariant(e1));
    qname = corpus.getQNameValueOfVariant(e1);
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfQName(qname));
    Assert.assertEquals("cat", corpus.getNameOfQName(qname));
    e2 = corpus.getEnumerationFacetOfSimpleType(notationDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_QNAME, corpus.getTypeOfVariant(e2));
    qname = corpus.getQNameValueOfVariant(e2);
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfQName(qname));
    Assert.assertEquals("dog", corpus.getNameOfQName(qname));
    e3 = corpus.getEnumerationFacetOfSimpleType(notationDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_QNAME, corpus.getTypeOfVariant(e3));
    qname = corpus.getQNameValueOfVariant(e3);
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfQName(qname));
    Assert.assertEquals("pig", corpus.getNameOfQName(qname));
    
    int decimalDerived = corpus.getTypeOfNamespace(foons, "decimalDerived");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfSimpleType(decimalDerived));
    e1 = corpus.getEnumerationFacetOfSimpleType(decimalDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(e1));
    Assert.assertEquals("100.1234567", corpus.getDecimalValueOfVariant(e1).toPlainString());
    e2 = corpus.getEnumerationFacetOfSimpleType(decimalDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(e2));
    Assert.assertEquals("101.2345678", corpus.getDecimalValueOfVariant(e2).toPlainString());
    e3 = corpus.getEnumerationFacetOfSimpleType(decimalDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(e3));
    Assert.assertEquals("102.3456789", corpus.getDecimalValueOfVariant(e3).toPlainString());

    int floatDerived = corpus.getTypeOfNamespace(foons, "floatDerived");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfSimpleType(floatDerived));
    e1 = corpus.getEnumerationFacetOfSimpleType(floatDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e1));
    Assert.assertEquals((float)103.01, corpus.getFloatValueOfVariant(e1), 0);
    e2 = corpus.getEnumerationFacetOfSimpleType(floatDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e2));
    Assert.assertEquals((float)105.01, corpus.getFloatValueOfVariant(e2), 0);
    e3 = corpus.getEnumerationFacetOfSimpleType(floatDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e3));
    Assert.assertEquals((float)107.01, corpus.getFloatValueOfVariant(e3), 0);

    int doubleDerived = corpus.getTypeOfNamespace(foons, "doubleDerived");
    Assert.assertEquals(9, corpus.getEnumerationFacetCountOfSimpleType(doubleDerived));
    e1 = corpus.getEnumerationFacetOfSimpleType(doubleDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE, corpus.getTypeOfVariant(e1));
    Assert.assertEquals(Double.parseDouble("-1E4"), corpus.getDoubleValueOfVariant(e1), 0);
    e2 = corpus.getEnumerationFacetOfSimpleType(doubleDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE, corpus.getTypeOfVariant(e2));
    Assert.assertEquals(Double.parseDouble("1267.43233E12"), corpus.getDoubleValueOfVariant(e2), 0);
    e3 = corpus.getEnumerationFacetOfSimpleType(doubleDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE, corpus.getTypeOfVariant(e3));
    Assert.assertEquals(Double.parseDouble("12.78e-2"), corpus.getDoubleValueOfVariant(e3), 0);
    e4 = corpus.getEnumerationFacetOfSimpleType(doubleDerived, 3);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE, corpus.getTypeOfVariant(e4));
    Assert.assertEquals(Double.parseDouble("12"), corpus.getDoubleValueOfVariant(e4), 0);
    e5 = corpus.getEnumerationFacetOfSimpleType(doubleDerived, 4);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE, corpus.getTypeOfVariant(e5));
    Assert.assertEquals(Double.parseDouble("0"), corpus.getDoubleValueOfVariant(e5), 0);
    e6 = corpus.getEnumerationFacetOfSimpleType(doubleDerived, 5);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE, corpus.getTypeOfVariant(e6));
    Assert.assertEquals(Double.parseDouble("-0"), corpus.getDoubleValueOfVariant(e6), 0);
    e7 = corpus.getEnumerationFacetOfSimpleType(doubleDerived, 6);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE, corpus.getTypeOfVariant(e7));
    Assert.assertEquals(Double.POSITIVE_INFINITY, corpus.getDoubleValueOfVariant(e7), 0);
    e8 = corpus.getEnumerationFacetOfSimpleType(doubleDerived, 7);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE, corpus.getTypeOfVariant(e8));
    Assert.assertEquals(Double.NEGATIVE_INFINITY, corpus.getDoubleValueOfVariant(e8), 0);
    e9 = corpus.getEnumerationFacetOfSimpleType(doubleDerived, 8);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE, corpus.getTypeOfVariant(e9));
    Assert.assertTrue(Double.isNaN(corpus.getDoubleValueOfVariant(e9)));

    int intDerived = corpus.getTypeOfNamespace(foons, "intDerived");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfSimpleType(intDerived));
    e1 = corpus.getEnumerationFacetOfSimpleType(intDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(e1));
    Assert.assertEquals((int)109, corpus.getIntValueOfVariant(e1));
    e2 = corpus.getEnumerationFacetOfSimpleType(intDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(e2));
    Assert.assertEquals((int)110, corpus.getIntValueOfVariant(e2));
    e3 = corpus.getEnumerationFacetOfSimpleType(intDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(e3));
    Assert.assertEquals((int)111, corpus.getIntValueOfVariant(e3));

    int longDerived = corpus.getTypeOfNamespace(foons, "longDerived");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfSimpleType(longDerived));
    e1 = corpus.getEnumerationFacetOfSimpleType(longDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(e1));
    Assert.assertEquals((long)112, corpus.getLongValueOfVariant(e1));
    e2 = corpus.getEnumerationFacetOfSimpleType(longDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(e2));
    Assert.assertEquals((long)113, corpus.getLongValueOfVariant(e2));
    e3 = corpus.getEnumerationFacetOfSimpleType(longDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(e3));
    Assert.assertEquals((long)114, corpus.getLongValueOfVariant(e3));

    int dateTimeDerived = corpus.getTypeOfNamespace(foons, "dateTimeDerived");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfSimpleType(dateTimeDerived));
    e1 = corpus.getEnumerationFacetOfSimpleType(dateTimeDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(e1));
    Assert.assertEquals("2003-03-19T13:20:00-05:00",
                      corpus.getDateTimeValueOfVariant(e1).getXMLGregorianCalendar().toString());
    e2 = corpus.getEnumerationFacetOfSimpleType(dateTimeDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(e2));
    Assert.assertEquals("2003-03-20T13:20:00-05:00",
                      corpus.getDateTimeValueOfVariant(e2).getXMLGregorianCalendar().toString());
    e3 = corpus.getEnumerationFacetOfSimpleType(dateTimeDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(e3));
    Assert.assertEquals("2003-03-21T13:20:00-05:00",
                      corpus.getDateTimeValueOfVariant(e3).getXMLGregorianCalendar().toString());

    int durationDerived = corpus.getTypeOfNamespace(foons, "durationDerived");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfSimpleType(durationDerived));
    e1 = corpus.getEnumerationFacetOfSimpleType(durationDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_DURATION, corpus.getTypeOfVariant(e1));
    Assert.assertTrue(m_datatypeFactory.newDuration("P1Y2M3DT10H30M").equals(
                      corpus.getDurationValueOfVariant(e1)));
    e2 = corpus.getEnumerationFacetOfSimpleType(durationDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_DURATION, corpus.getTypeOfVariant(e2));
    Assert.assertTrue(m_datatypeFactory.newDuration("P1Y2M4DT10H30M").equals(
                      corpus.getDurationValueOfVariant(e2)));
    e3 = corpus.getEnumerationFacetOfSimpleType(durationDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_DURATION, corpus.getTypeOfVariant(e3));
    Assert.assertTrue(m_datatypeFactory.newDuration("P1Y2M5DT10H30M").equals(
                      corpus.getDurationValueOfVariant(e3)));

    int unionedEnum = corpus.getTypeOfNamespace(foons, "unionedEnum");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfSimpleType(unionedEnum));
    e1 = corpus.getEnumerationFacetOfSimpleType(unionedEnum, 0);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(e1));
    Assert.assertEquals((int)100, corpus.getIntValueOfVariant(e1));
    e2 = corpus.getEnumerationFacetOfSimpleType(unionedEnum, 1);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e2));
    Assert.assertEquals("Tokyo", corpus.getStringValueOfVariant(e2));
    e3 = corpus.getEnumerationFacetOfSimpleType(unionedEnum, 2);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(e3));
    Assert.assertEquals((int)101, corpus.getIntValueOfVariant(e3));
  }
  
  /**
   * Test the use of itemType in list simple types.
   */
  public void testListItemType() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/list.xsd", getClass());
    
    int xsdns = corpus.getNamespaceOfSchema(XMLSCHEMA_URI);
    int _anySimpleType = corpus.getTypeOfNamespace(xsdns, "anySimpleType");
    int _decimal = corpus.getTypeOfNamespace(xsdns, "decimal");
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    int goons = corpus.getNamespaceOfSchema("urn:goo");

    int decimal8 = corpus.getTypeOfNamespace(goons, "decimal8");
    Assert.assertEquals(_decimal, corpus.getBaseTypeOfType(decimal8));
    Assert.assertEquals(8, corpus.getTotalDigitsFacetValueOfSimpleType(decimal8));

    int decimal4 = corpus.getTypeOfNamespace(foons, "decimal4");
    Assert.assertEquals(decimal8, corpus.getBaseTypeOfType(decimal4));
    Assert.assertEquals(4, corpus.getTotalDigitsFacetValueOfSimpleType(decimal4));

    int listOfDecimal8 = corpus.getTypeOfNamespace(foons, "listOfDecimal8");
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(listOfDecimal8));
    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(listOfDecimal8));
    Assert.assertEquals(decimal8, corpus.getItemTypeOfListSimpleType(listOfDecimal8));

    int listOfDecimal4 = corpus.getTypeOfNamespace(goons, "listOfDecimal4");
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(listOfDecimal4));
    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(listOfDecimal4));
    Assert.assertEquals(decimal4, corpus.getItemTypeOfListSimpleType(listOfDecimal4));
  }
  
  /**
   * Test union simple type
   */
  public void testUnion() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/union.xsd", getClass());
    
    int xsdns = corpus.getNamespaceOfSchema(XMLSCHEMA_URI);
    int _int     = corpus.getTypeOfNamespace(xsdns, "int");
    int _NMTOKEN = corpus.getTypeOfNamespace(xsdns, "NMTOKEN");

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int unionedEnum = corpus.getTypeOfNamespace(foons, "unionedEnum");
    Assert.assertEquals(2, corpus.getMemberTypesCountOfSimpleType(unionedEnum));
    Assert.assertEquals(_int, corpus.getMemberTypeOfSimpleType(unionedEnum, 0));
    Assert.assertEquals(_NMTOKEN, corpus.getMemberTypeOfSimpleType(unionedEnum, 1));
  }

  /**
   * Test union of union simple type
   */
  public void testUnionOfUnion() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/union.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int unionOfUnion = corpus.getTypeOfNamespace(foons, "unionOfUnion");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(unionOfUnion));
    Assert.assertEquals(3, corpus.getMemberTypesCountOfSimpleType(unionOfUnion));
    int m1 = corpus.getMemberTypeOfSimpleType(unionOfUnion, 0);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(m1));
    Assert.assertEquals(corpus.getBuiltinTypeOfSchema(EXISchemaConst.NMTOKEN_TYPE), corpus.getBaseTypeOfType(m1));
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfSimpleType(m1));
    int e1 = corpus.getEnumerationFacetOfSimpleType(m1, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e1));
    Assert.assertEquals("main", corpus.getStringValueOfVariant(e1));
    int e2 = corpus.getEnumerationFacetOfSimpleType(m1, 1);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e2));
    Assert.assertEquals("secondary", corpus.getStringValueOfVariant(e2));
    int e3 = corpus.getEnumerationFacetOfSimpleType(m1, 2);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e3));
    Assert.assertEquals("alternative", corpus.getStringValueOfVariant(e3));
    int m2 = corpus.getMemberTypeOfSimpleType(unionOfUnion, 1);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(m2));
    Assert.assertEquals(corpus.getBuiltinTypeOfSchema(EXISchemaConst.NMTOKEN_TYPE), m2);
    int m3 = corpus.getMemberTypeOfSimpleType(unionOfUnion, 2);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(m3));
    Assert.assertEquals(corpus.getBuiltinTypeOfSchema(EXISchemaConst.ANYURI_TYPE), m3);
  }

  /**
   * Test list of union simple type
   */
  public void testListOfUnion() throws Exception {

    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/list.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int listOfUnion = corpus.getTypeOfNamespace(foons, "listOfUnion");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(listOfUnion));

    Assert.assertEquals(-1, corpus.getLengthFacetValueOfSimpleType(listOfUnion));

    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(listOfUnion));
    Assert.assertFalse(corpus.isBuiltinSimpleType(listOfUnion));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(listOfUnion));
    Assert.assertFalse(corpus.isFixtureType(listOfUnion));
    Assert.assertEquals(EXISchema.WHITESPACE_COLLAPSE,
          corpus.getWhitespaceFacetValueOfSimpleType(listOfUnion));
    
    int union = corpus.getItemTypeOfListSimpleType(listOfUnion);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(union));
    Assert.assertEquals(2, corpus.getMemberTypesCountOfSimpleType(union));
    int m1 = corpus.getMemberTypeOfSimpleType(union, 0);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(m1));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, corpus.getSerialOfType(m1));
    int m2 = corpus.getMemberTypeOfSimpleType(union, 1);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(m2));
    Assert.assertEquals(EXISchemaConst.NAME_TYPE, corpus.getSerialOfType(m2));
  }

  /**
   * Test list of union simple type with length facet
   */
  public void testList4OfUnion() throws Exception {

    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/list.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int list4OfUnion = corpus.getTypeOfNamespace(foons, "list4OfUnion");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(list4OfUnion));
    
    Assert.assertEquals(4, corpus.getLengthFacetValueOfSimpleType(list4OfUnion));

    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(list4OfUnion));
    Assert.assertFalse(corpus.isBuiltinSimpleType(list4OfUnion));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(list4OfUnion));
    Assert.assertFalse(corpus.isFixtureType(list4OfUnion));
    Assert.assertEquals(EXISchema.WHITESPACE_COLLAPSE,
          corpus.getWhitespaceFacetValueOfSimpleType(list4OfUnion));
    
    int union = corpus.getItemTypeOfListSimpleType(list4OfUnion);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(union));
    Assert.assertEquals(2, corpus.getMemberTypesCountOfSimpleType(union));
    int m1 = corpus.getMemberTypeOfSimpleType(union, 0);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(m1));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, corpus.getSerialOfType(m1));
    int m2 = corpus.getMemberTypeOfSimpleType(union, 1);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(m2));
    Assert.assertEquals(EXISchemaConst.NAME_TYPE, corpus.getSerialOfType(m2));
  }
  
  /**
   * Test IsGlobalElement method.
   */
  public void testIsGlobalElement() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/localGlobalElem.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int global_a = corpus.getElemOfNamespace(foons, "A");
    Assert.assertTrue(corpus.isGlobalElement(global_a));

    int global_b = corpus.getElemOfNamespace(foons, "B");
    Assert.assertTrue(corpus.isGlobalElement(global_b));

    int global_c = corpus.getElemOfNamespace(foons, "C");
    Assert.assertTrue(corpus.isGlobalElement(global_c));

    int global_d = corpus.getElemOfNamespace(foons, "D");
    Assert.assertTrue(corpus.isGlobalElement(global_d));

    int groupOfA = corpus.getGroupOfElem(global_a); 
    Assert.assertEquals(3, corpus.getParticleCountOfGroup(groupOfA));
    
    int particle0 = corpus.getMemberSubstanceOfGroup(groupOfA, 0);
    Assert.assertEquals(global_b, corpus.getTermOfParticle(particle0));

    int particle1 = corpus.getMemberSubstanceOfGroup(groupOfA, 1);
    int local_c = corpus.getTermOfParticle(particle1);
    Assert.assertFalse(corpus.isGlobalElement(local_c));

    int particle2 = corpus.getMemberSubstanceOfGroup(groupOfA, 2);
    Assert.assertEquals(global_d, corpus.getTermOfParticle(particle2));
  }

  /**
   * Test IsGlobalAttribute method.
   */
  public void testIsGlobalAttribute() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/localGlobalAttr.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int eA = corpus.getElemOfNamespace(foons, "A");
    
    int paB = corpus.getAttrUseOfElem(eA, "", "b");
    int aB = corpus.getAttrOfAttrUse(paB); 
    Assert.assertFalse(corpus.isGlobalAttribute(aB));

    int aA = corpus.getAttrOfNamespace(foons, "a");
    Assert.assertTrue(corpus.isGlobalAttribute(aA));
  }
  
  public void testContentClassOfComplexType() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexType.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int datedEmpty = corpus.getTypeOfNamespace(foons, "datedEmpty");
    Assert.assertEquals(EXISchema.CONTENT_EMPTY,
                      corpus.getContentClassOfComplexType(datedEmpty));

    int datedString = corpus.getTypeOfNamespace(foons, "datedString");
    Assert.assertEquals(EXISchema.CONTENT_SIMPLE,
                      corpus.getContentClassOfComplexType(datedString));

    int datedAddress = corpus.getTypeOfNamespace(foons, "datedAddress");
    Assert.assertEquals(EXISchema.CONTENT_ELEMENT_ONLY,
                      corpus.getContentClassOfComplexType(datedAddress));

    int datedAddressMixed = corpus.getTypeOfNamespace(foons, "datedAddressMixed");
    Assert.assertEquals(EXISchema.CONTENT_MIXED,
                      corpus.getContentClassOfComplexType(datedAddressMixed));
    
    int _string = corpus.getBuiltinTypeOfSchema(EXISchemaConst.STRING_TYPE);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_string));
    try {
      corpus.getContentClassOfComplexType(_string);
    }
    catch (EXISchemaRuntimeException scre) {
      Assert.assertEquals(EXISchemaRuntimeException.NOT_COMPLEX_TYPE, scre.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  public void testContentTypeOfComplexType() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/complexType.xsd",
                                             getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int xsdns = corpus.getNamespaceOfSchema(XMLSCHEMA_URI);
    int _string = corpus.getTypeOfNamespace(xsdns, "string");

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int datedEmpty = corpus.getTypeOfNamespace(foons, "datedEmpty");
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getContentTypeOfComplexType(datedEmpty));

    int datedString = corpus.getTypeOfNamespace(foons, "datedString");
    Assert.assertEquals(_string, corpus.getContentTypeOfComplexType(datedString));

    int contentType;

    int datedStringLength10 = corpus.getTypeOfNamespace(foons, "datedStringLength10");
    contentType = corpus.getContentTypeOfComplexType(datedStringLength10);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(contentType));

    int datedAddress = corpus.getTypeOfNamespace(foons, "datedAddress");
    contentType = corpus.getContentTypeOfComplexType(datedAddress);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(contentType));

    int datedAddressMixed = corpus.getTypeOfNamespace(foons, "datedAddressMixed");
    contentType = corpus.getContentTypeOfComplexType(datedAddressMixed);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(contentType));
  }

  public void testMinMaxOccursOfParticle() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/complexType.xsd", getClass());
    int[] nodes = corpus.getNodes();
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int particle, group;

    int datedNames_1_1 = corpus.getTypeOfNamespace(foons, "datedNames_1_1");
    particle = corpus.getContentTypeOfComplexType(datedNames_1_1);
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particle));
    group = corpus.getTermOfParticle(particle);
    Assert.assertEquals(1, corpus.getMemberSubstanceCountOfGroup(group));
    particle = corpus.getParticleOfGroup(group, 0); // substance particle
    Assert.assertEquals(particle, nodes[corpus.getMemberSubstanceListOfGroup(group)]);

    int datedNames_0_1 = corpus.getTypeOfNamespace(foons, "datedNames_0_1");
    particle = corpus.getContentTypeOfComplexType(datedNames_0_1);
    Assert.assertEquals(0, corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particle));
    group = corpus.getTermOfParticle(particle);
    Assert.assertEquals(1, corpus.getMemberSubstanceCountOfGroup(group));
    particle = corpus.getParticleOfGroup(group, 0);
    Assert.assertEquals(particle, nodes[corpus.getMemberSubstanceListOfGroup(group)]);

    int datedNames_1_2 = corpus.getTypeOfNamespace(foons, "datedNames_1_2");
    particle = corpus.getContentTypeOfComplexType(datedNames_1_2);
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(2, corpus.getMaxOccursOfParticle(particle));
    group = corpus.getTermOfParticle(particle);
    Assert.assertEquals(1, corpus.getMemberSubstanceCountOfGroup(group));
    particle = corpus.getParticleOfGroup(group, 0);
    Assert.assertEquals(particle, nodes[corpus.getMemberSubstanceListOfGroup(group)]);

    int datedNames_1_unbounded = corpus.getTypeOfNamespace(
                                          foons, "datedNames_1_unbounded");
    particle = corpus.getContentTypeOfComplexType(datedNames_1_unbounded);
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(EXISchema.UNBOUNDED_OCCURS,
                      corpus.getMaxOccursOfParticle(particle));
    group = corpus.getTermOfParticle(particle);
    Assert.assertEquals(1, corpus.getMemberSubstanceCountOfGroup(group));
    particle = corpus.getParticleOfGroup(group, 0);
    Assert.assertEquals(particle, nodes[corpus.getMemberSubstanceListOfGroup(group)]);
  }

  public void testSubstantialMaxOccursOfParticle()
      throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/complexType.xsd", getClass());

    int[] nodes = corpus.getNodes();
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int particle, group;

    int datedAddress4 = corpus.getTypeOfNamespace(foons, "datedAddress4");
    particle = corpus.getContentTypeOfComplexType(datedAddress4);
    group = corpus.getTermOfParticle(particle);
    Assert.assertEquals(5, corpus.getMemberSubstanceCountOfGroup(group));
    int list = corpus.getMemberSubstanceListOfGroup(group);

    int substanceParticle, substance;

    substanceParticle = nodes[list + 0];
    Assert.assertEquals(substanceParticle, corpus.getMemberSubstanceOfGroup(group, 0));
    // Assert.assertEquals(210, corpus.getSubstantialMaxOccursOfParticle(substanceParticle));
    Assert.assertEquals(EXISchema.TERM_TYPE_ELEMENT,
                      corpus.getTermTypeOfParticle(substanceParticle));
    substance = corpus.getTermOfParticle(substanceParticle);
    Assert.assertEquals("Street4a", corpus.getNameOfElem(substance));

    substanceParticle = nodes[list + 1];
    Assert.assertEquals(substanceParticle, corpus.getMemberSubstanceOfGroup(group, 1));
    // Assert.assertEquals(105, corpus.getSubstantialMaxOccursOfParticle(substanceParticle));
    Assert.assertEquals(EXISchema.TERM_TYPE_ELEMENT,
                      corpus.getTermTypeOfParticle(substanceParticle));
    substance = corpus.getTermOfParticle(substanceParticle);
    Assert.assertEquals("City4a", corpus.getNameOfElem(substance));

    substanceParticle = nodes[list + 2];
    Assert.assertEquals(substanceParticle, corpus.getMemberSubstanceOfGroup(group, 2));
    // Assert.assertEquals(EXISchema.UNBOUNDED_OCCURS,
    //                     corpus.getSubstantialMaxOccursOfParticle(substanceParticle));
    Assert.assertEquals(EXISchema.TERM_TYPE_ELEMENT,
                      corpus.getTermTypeOfParticle(substanceParticle));
    substance = corpus.getTermOfParticle(substanceParticle);
    Assert.assertEquals("Street4b", corpus.getNameOfElem(substance));

    substanceParticle = nodes[list + 3];
    Assert.assertEquals(substanceParticle, corpus.getMemberSubstanceOfGroup(group, 3));
    // Assert.assertEquals(70, corpus.getSubstantialMaxOccursOfParticle(substanceParticle));
    Assert.assertEquals(EXISchema.TERM_TYPE_ELEMENT,
                      corpus.getTermTypeOfParticle(substanceParticle));
    substance = corpus.getTermOfParticle(substanceParticle);
    Assert.assertEquals("City4b", corpus.getNameOfElem(substance));

    substanceParticle = nodes[list + 4];
    Assert.assertEquals(substanceParticle, corpus.getMemberSubstanceOfGroup(group, 4));
    // Assert.assertEquals(EXISchema.UNBOUNDED_OCCURS,
    //                     corpus.getSubstantialMaxOccursOfParticle(substanceParticle));
    Assert.assertEquals(EXISchema.TERM_TYPE_ELEMENT,
                      corpus.getTermTypeOfParticle(substanceParticle));
    substance = corpus.getTermOfParticle(substanceParticle);
    Assert.assertEquals("Zip3", corpus.getNameOfElem(substance));
  }

  public void testSubstantialMaxOccursOfParticleWithSubstitutables()
    throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/complexType.xsd", getClass());
    
    int[] nodes = corpus.getNodes();
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int particle, group;
    
    int datedAddress4 = corpus.getTypeOfNamespace(foons, "datedAddress5");
    particle = corpus.getContentTypeOfComplexType(datedAddress4);
    group = corpus.getTermOfParticle(particle);
    Assert.assertEquals(3, corpus.getMemberSubstanceCountOfGroup(group));
    int list = corpus.getMemberSubstanceListOfGroup(group);
    
    int substanceParticle, substance;
    
    substanceParticle = nodes[list + 0];
    Assert.assertEquals(substanceParticle, corpus.getMemberSubstanceOfGroup(group, 0));
    // Assert.assertEquals(70, corpus.getSubstantialMaxOccursOfParticle(substanceParticle));
    Assert.assertEquals(EXISchema.TERM_TYPE_ELEMENT,
                      corpus.getTermTypeOfParticle(substanceParticle));
    substance = corpus.getTermOfParticle(substanceParticle);
    Assert.assertEquals("Street5", corpus.getNameOfElem(substance));
    
    substanceParticle = nodes[list + 1];
    Assert.assertEquals(substanceParticle, corpus.getMemberSubstanceOfGroup(group, 1));
    // Assert.assertEquals(35, corpus.getSubstantialMaxOccursOfParticle(substanceParticle));
    Assert.assertEquals(EXISchema.TERM_TYPE_ELEMENT,
                      corpus.getTermTypeOfParticle(substanceParticle));
    substance = corpus.getTermOfParticle(substanceParticle);
    Assert.assertEquals("City5a", corpus.getNameOfElem(substance));
    
    substanceParticle = nodes[list + 2];
    Assert.assertEquals(substanceParticle, corpus.getMemberSubstanceOfGroup(group, 2));
    // Assert.assertEquals(EXISchema.UNBOUNDED_OCCURS,
    //                   corpus.getSubstantialMaxOccursOfParticle(substanceParticle));
    Assert.assertEquals(EXISchema.TERM_TYPE_ELEMENT,
                      corpus.getTermTypeOfParticle(substanceParticle));
    substance = corpus.getTermOfParticle(substanceParticle);
    Assert.assertEquals("Zip5", corpus.getNameOfElem(substance));
  }
  
  /**
   * Named model groups are assumed to be expanded when referenced.
   */
  public void testSubstantialMaxOccursOfParticleWithConflict()
      throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/complexType.xsd", getClass());

    int[] nodes = corpus.getNodes();
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int particle, group;
    int list;
    int substanceParticle1, substance1;
    int substanceParticle2, substance2;

    int namedString = corpus.getTypeOfNamespace(foons, "namedString");
    particle = corpus.getContentTypeOfComplexType(namedString);
    group = corpus.getTermOfParticle(particle);
    Assert.assertEquals(1, corpus.getMemberSubstanceCountOfGroup(group));
    list = corpus.getMemberSubstanceListOfGroup(group);

    substanceParticle1 = nodes[list];
    // Assert.assertEquals(1, corpus.getSubstantialMaxOccursOfParticle(substanceParticle1));
    Assert.assertEquals(EXISchema.TERM_TYPE_ELEMENT,
                      corpus.getTermTypeOfParticle(substanceParticle1));
    substance1 = corpus.getTermOfParticle(substanceParticle1);
    Assert.assertEquals("String", corpus.getNameOfElem(substance1));

    int namedString2 = corpus.getTypeOfNamespace(foons, "namedString2");
    particle = corpus.getContentTypeOfComplexType(namedString2);
    group = corpus.getTermOfParticle(particle);
    Assert.assertEquals(1, corpus.getMemberSubstanceCountOfGroup(group));
    list = corpus.getMemberSubstanceListOfGroup(group);

    substanceParticle2 = nodes[list];
    // Assert.assertEquals(2, corpus.getSubstantialMaxOccursOfParticle(substanceParticle2));
    Assert.assertEquals(EXISchema.TERM_TYPE_ELEMENT,
                      corpus.getTermTypeOfParticle(substanceParticle2));
    substance2 = corpus.getTermOfParticle(substanceParticle2);
    Assert.assertEquals("String", corpus.getNameOfElem(substance2));

    Assert.assertEquals(substance1, substance2);
  }

  public void testNestedGroup01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/nestedGroup01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce;
    sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce.getCode());
    Assert.assertEquals(12, sce.getLocator().getLineNumber());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int particle;
    
    int datedAddress = corpus.getTypeOfNamespace(foons, "datedAddress");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(datedAddress));
    
    int sequence = corpus.getParticleTermOfComplexType(datedAddress);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(sequence));

    particle = corpus.getParticleOfGroup(sequence, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));

    int zipElem = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(zipElem));
    Assert.assertEquals("Zip", corpus.getNameOfElem(zipElem));
    
    particle = corpus.getParticleOfGroup(sequence, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));
    
    int choice = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(choice));
    // 2nd particle has been disposed.
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(choice));

    particle = corpus.getParticleOfGroup(choice, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));

    int valueElem = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(valueElem));
    Assert.assertEquals("value", corpus.getNameOfElem(valueElem));
  }

  public void testNestedGroup02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/nestedGroup02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce;
    sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce.getCode());
    Assert.assertEquals(23, sce.getLocator().getLineNumber());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int particle, sequence, choice, zipElem, valueElem;
    
    int datedAddress = corpus.getTypeOfNamespace(foons, "datedAddress");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(datedAddress));
    
    sequence = corpus.getParticleTermOfComplexType(datedAddress);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(sequence));

    particle = corpus.getParticleOfGroup(sequence, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));

    zipElem = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(zipElem));
    Assert.assertEquals("Zip", corpus.getNameOfElem(zipElem));

    particle = corpus.getParticleOfGroup(sequence, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));
    
    choice = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(choice));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(choice));

    particle = corpus.getParticleOfGroup(choice, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));

    valueElem = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(valueElem));
    Assert.assertEquals("value", corpus.getNameOfElem(valueElem));
    
    particle = corpus.getParticleOfGroup(choice, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));
    
    sequence = corpus.getTermOfParticle(particle);
    
    particle = corpus.getParticleOfGroup(sequence, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));

    zipElem = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(zipElem));
    Assert.assertEquals("Zip", corpus.getNameOfElem(zipElem));

    particle = corpus.getParticleOfGroup(sequence, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));
    
    choice = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(choice));
    // 2nd particle has been disposed.
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(choice));

    particle = corpus.getParticleOfGroup(choice, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));

    valueElem = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(valueElem));
    Assert.assertEquals("value", corpus.getNameOfElem(valueElem));
  }

  /**
   * A model group definition ("group") is used by a complex type
   * definition ("tuple"). The model group definition ("group") in turn
   * references another model group definition ("hoge").
   */
  public void testNestedGroupReference_01() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/nestedGroupReference.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int item = corpus.getElemOfNamespace(foons, "item");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(item));

    int tuple = corpus.getElemOfNamespace(foons, "tuple");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(tuple));
    
    int g_group = corpus.getGroupOfElem(tuple);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(g_group));
    Assert.assertEquals(EXISchema.GROUP_CHOICE, corpus.getCompositorOfGroup(g_group));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(g_group));
    int p_hoge = corpus.getParticleOfGroup(g_group, 0);
    int g_hoge = corpus.getTermOfParticle(p_hoge);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(g_hoge));
    Assert.assertEquals(EXISchema.GROUP_CHOICE, corpus.getCompositorOfGroup(g_hoge));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(g_hoge));
    int p_item = corpus.getParticleOfGroup(g_hoge, 0);
    int e_item = corpus.getTermOfParticle(p_item);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(e_item));
    Assert.assertEquals(item, e_item);
    
    int list;

    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(g_group));
    Assert.assertEquals(p_item, corpus.getNodes()[corpus.getHeadSubstanceListOfGroup(g_group)]);

    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(g_group, 0));
    list = corpus.getHeadSubstanceListOfGroup(g_group, 0);
    Assert.assertEquals(p_item, corpus.getNodes()[list]);
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[list + 1]);
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(g_group, 1));
    list = corpus.getHeadSubstanceListOfGroup(g_group, 1);
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[list]);
    
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(g_hoge));
    Assert.assertEquals(p_item, corpus.getNodes()[corpus.getHeadSubstanceListOfGroup(g_hoge)]);
        
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(g_hoge, 0));
    list = corpus.getHeadSubstanceListOfGroup(g_hoge, 0);
    Assert.assertEquals(p_item, corpus.getNodes()[list]);
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[list + 1]);
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(g_hoge, 1));
    list = corpus.getHeadSubstanceListOfGroup(g_hoge, 1);
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[list]);
  }
  
  /**
   */
  public void testParticleSerial() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/modelGroupMultiUse02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int particleA, particleB, particleC, particleD, particleE;
    int sequenceParticle1, sequenceParticle2, choiceParticle;
    int sequenceParticle1_1;
    int sequence1, sequence1_1, sequence2, choice;
    
    int typeA = corpus.getTypeOfNamespace(foons, "typeA");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeA));
    int contentA = corpus.getContentTypeOfComplexType(typeA);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(contentA));
    Assert.assertEquals(0, corpus.getSerialInTypeOfParticle(contentA));
    int termOfContentA = corpus.getTermOfParticle(contentA) ;
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(termOfContentA));
    
    sequenceParticle1 = corpus.getParticleOfGroup(termOfContentA, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(sequenceParticle1));
    Assert.assertEquals(1, corpus.getSerialInTypeOfParticle(sequenceParticle1));
    sequence1 = corpus.getTermOfParticle(sequenceParticle1);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(sequence1));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(sequence1));
    Assert.assertEquals(3, corpus.getParticleCountOfGroup(sequence1));
    particleA = corpus.getParticleOfGroup(sequence1, 0);
    Assert.assertEquals(2, corpus.getSerialInTypeOfParticle(particleA));
    sequenceParticle1_1 = corpus.getParticleOfGroup(sequence1, 1);
    Assert.assertEquals(3, corpus.getSerialInTypeOfParticle(sequenceParticle1_1));
    sequence1_1 = corpus.getTermOfParticle(sequenceParticle1_1);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(sequence1_1));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(sequence1_1));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(sequence1_1));
    choiceParticle = corpus.getParticleOfGroup(sequence1_1, 0);
    Assert.assertEquals(4, corpus.getSerialInTypeOfParticle(choiceParticle));
    choice = corpus.getTermOfParticle(choiceParticle);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(choice));
    Assert.assertEquals(EXISchema.GROUP_CHOICE, corpus.getCompositorOfGroup(choice));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(choice));
    particleB = corpus.getParticleOfGroup(choice, 0);
    Assert.assertEquals(5, corpus.getSerialInTypeOfParticle(particleB));
    particleC = corpus.getParticleOfGroup(choice, 1);
    Assert.assertEquals(6, corpus.getSerialInTypeOfParticle(particleC));
    particleD = corpus.getParticleOfGroup(sequence1_1, 1);
    Assert.assertEquals(7, corpus.getSerialInTypeOfParticle(particleD));
    particleE = corpus.getParticleOfGroup(sequence1, 2);
    Assert.assertEquals(8, corpus.getSerialInTypeOfParticle(particleE));
    
    sequenceParticle2 = corpus.getParticleOfGroup(termOfContentA, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(sequenceParticle2));
    Assert.assertEquals(9, corpus.getSerialInTypeOfParticle(sequenceParticle2));
    sequence2 = corpus.getTermOfParticle(sequenceParticle2);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(sequence2));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(sequence2));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(sequence2));
    choiceParticle = corpus.getParticleOfGroup(sequence2, 0);
    Assert.assertEquals(10, corpus.getSerialInTypeOfParticle(choiceParticle));
    choice = corpus.getTermOfParticle(choiceParticle);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(choice));
    Assert.assertEquals(EXISchema.GROUP_CHOICE, corpus.getCompositorOfGroup(choice));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(choice));
    particleB = corpus.getParticleOfGroup(choice, 0);
    Assert.assertEquals(11, corpus.getSerialInTypeOfParticle(particleB));
    particleC = corpus.getParticleOfGroup(choice, 1);
    Assert.assertEquals(12, corpus.getSerialInTypeOfParticle(particleC));
    particleD = corpus.getParticleOfGroup(sequence2, 1);
    Assert.assertEquals(13, corpus.getSerialInTypeOfParticle(particleD));

    int typeB = corpus.getTypeOfNamespace(foons, "typeB");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeB));
    int contentB = corpus.getContentTypeOfComplexType(typeB);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(contentB));
    Assert.assertEquals(0, corpus.getSerialInTypeOfParticle(contentB));
    int termOfContentB = corpus.getTermOfParticle(contentB) ;
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(termOfContentB));

    sequenceParticle1 = corpus.getParticleOfGroup(termOfContentB, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(sequenceParticle1));
    Assert.assertEquals(1, corpus.getSerialInTypeOfParticle(sequenceParticle1));
    sequence2 = corpus.getTermOfParticle(sequenceParticle1);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(sequence2));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(sequence2));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(sequence2));
    choiceParticle = corpus.getParticleOfGroup(sequence2, 0);
    Assert.assertEquals(2, corpus.getSerialInTypeOfParticle(choiceParticle));
    choice = corpus.getTermOfParticle(choiceParticle);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(choice));
    Assert.assertEquals(EXISchema.GROUP_CHOICE, corpus.getCompositorOfGroup(choice));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(choice));
    particleB = corpus.getParticleOfGroup(choice, 0);
    Assert.assertEquals(3, corpus.getSerialInTypeOfParticle(particleB));
    particleC = corpus.getParticleOfGroup(choice, 1);
    Assert.assertEquals(4, corpus.getSerialInTypeOfParticle(particleC));
    particleD = corpus.getParticleOfGroup(sequence2, 1);
    Assert.assertEquals(5, corpus.getSerialInTypeOfParticle(particleD));

    sequenceParticle2 = corpus.getParticleOfGroup(termOfContentB, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(sequenceParticle2));
    Assert.assertEquals(6, corpus.getSerialInTypeOfParticle(sequenceParticle2));
    sequence1 = corpus.getTermOfParticle(sequenceParticle2);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(sequence1));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(sequence1));
    Assert.assertEquals(3, corpus.getParticleCountOfGroup(sequence1));
    particleA = corpus.getParticleOfGroup(sequence1, 0);
    Assert.assertEquals(7, corpus.getSerialInTypeOfParticle(particleA));
    sequenceParticle1_1 = corpus.getParticleOfGroup(sequence1, 1);
    Assert.assertEquals(8, corpus.getSerialInTypeOfParticle(sequenceParticle1_1));
    sequence1_1 = corpus.getTermOfParticle(sequenceParticle1_1);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(sequence1_1));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(sequence1_1));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(sequence1_1));
    choiceParticle = corpus.getParticleOfGroup(sequence1_1, 0);
    Assert.assertEquals(9, corpus.getSerialInTypeOfParticle(choiceParticle));
    choice = corpus.getTermOfParticle(choiceParticle);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(choice));
    Assert.assertEquals(EXISchema.GROUP_CHOICE, corpus.getCompositorOfGroup(choice));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(choice));
    particleB = corpus.getParticleOfGroup(choice, 0);
    Assert.assertEquals(10, corpus.getSerialInTypeOfParticle(particleB));
    particleC = corpus.getParticleOfGroup(choice, 1);
    Assert.assertEquals(11, corpus.getSerialInTypeOfParticle(particleC));
    particleD = corpus.getParticleOfGroup(sequence1_1, 1);
    Assert.assertEquals(12, corpus.getSerialInTypeOfParticle(particleD));
    particleE = corpus.getParticleOfGroup(sequence1, 2);
    Assert.assertEquals(13, corpus.getSerialInTypeOfParticle(particleE));
  }
  
  public void testParticleMisc() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/complexType.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int particle, group;

    int datedNames_1_1 = corpus.getTypeOfNamespace(foons, "datedNames_1_1");
    particle = corpus.getContentTypeOfComplexType(datedNames_1_1);

    Assert.assertEquals(EXISchema.TERM_TYPE_GROUP,
                      corpus.getTermTypeOfParticle(particle));
    group = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(group));
    
    Assert.assertEquals(0, corpus.getSerialInTypeOfParticle(particle));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(group));
    particle = corpus.getParticleOfGroup(group, 0);
    Assert.assertEquals(1, corpus.getSerialInTypeOfParticle(particle));
  }

  public void testComplexTypeInlinedParticle() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexType.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    //int particle;

    int datedNames_1_1 = corpus.getTypeOfNamespace(foons, "datedNames_1_1");
    Assert.assertEquals(1, corpus.getParticleMinOccursOfComplexType(datedNames_1_1));
    Assert.assertEquals(1, corpus.getParticleMaxOccursOfComplexType(datedNames_1_1));
    int term = corpus.getParticleTermOfComplexType(datedNames_1_1);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(term));

    int datedNames_0_1 = corpus.getTypeOfNamespace(foons, "datedNames_0_1");
    Assert.assertEquals(0, corpus.getParticleMinOccursOfComplexType(datedNames_0_1));
    Assert.assertEquals(1, corpus.getParticleMaxOccursOfComplexType(datedNames_0_1));

    int datedNames_1_2 = corpus.getTypeOfNamespace(foons, "datedNames_1_2");
    Assert.assertEquals(1, corpus.getParticleMinOccursOfComplexType(datedNames_1_2));
    Assert.assertEquals(2, corpus.getParticleMaxOccursOfComplexType(datedNames_1_2));

    int datedNames_1_unbounded = corpus.getTypeOfNamespace(
                                          foons, "datedNames_1_unbounded");
    Assert.assertEquals(1, corpus.getParticleMinOccursOfComplexType(datedNames_1_unbounded));
    Assert.assertEquals(EXISchema.UNBOUNDED_OCCURS,
                      corpus.getParticleMaxOccursOfComplexType(datedNames_1_unbounded));

    int datedEmpty = corpus.getTypeOfNamespace(foons, "datedEmpty");
    Assert.assertEquals(0, corpus.getParticleMinOccursOfComplexType(datedEmpty));

    int datedString = corpus.getTypeOfNamespace(foons, "datedString");
    Assert.assertEquals(0, corpus.getParticleMaxOccursOfComplexType(datedEmpty));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getParticleTermOfComplexType(datedString));
  }

  /**
   * Particle is optional (minOccurs="0") and the group is a fixture.
   * <xsd:complexType name="s0">
   *   <!-- optinal particle, fixture group -->
   *   <xsd:sequence minOccurs="0">
   *     <xsd:element name="B"/>
   *     <xsd:element name="C"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_Sequence_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "s0");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(2, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 2]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 1]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 3]); // particle index within the group
  }

  /**
   * Particle is optional (minOccurs="0") and the group is a fixture.
   * <xsd:complexType name="s1">
   *   <!-- optinal particle, fixture group -->
   *   <xsd:sequence minOccurs="0">
   *     <xsd:element name="B" minOccurs="0"/>
   *     <xsd:element name="C"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_Sequence_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "s1");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group

    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }
  
  /**
   * Particle is optional (minOccurs="0") and the group is not a fixture.
   * <xsd:complexType name="s2">
   *   <!-- optinal particle, non-fixture group -->
   *   <xsd:sequence minOccurs="0">
   *     <xsd:element name="B" minOccurs="0"/>
   *     <xsd:element name="C" minOccurs="0"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_Sequence_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "s2");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group

    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }
  
  /**
   * Particle is not optional (minOccurs="1") and the group is a fixture.
   * <xsd:complexType name="s3">
   *   <!-- non-optinal particle, fixture group -->
   *   <xsd:sequence>
   *     <xsd:element name="B"/>
   *     <xsd:element name="C"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_Sequence_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "s3");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(2, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 2]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 1]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 3]); // particle index within the group
  }

  /**
   * Particle is not optional (minOccurs="1") and the group is a fixture.
   * <xsd:complexType name="s4">
   *   <!-- non-optinal particle, fixture group -->
   *   <xsd:sequence>
   *     <xsd:element name="B" minOccurs="0"/>
   *     <xsd:element name="C"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_Sequence_05() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "s4");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group
    
    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }
  
  /**
   * Particle is not optional (minOccurs="1") and the group is not a fixture.
   * <xsd:complexType name="s5">
   *   <!-- non-optinal particle, non-fixture group -->
   *   <xsd:sequence>
   *     <xsd:element name="B" minOccurs="0"/>
   *     <xsd:element name="C" minOccurs="0"/>
   *   </xsd:sequence>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_Sequence_06() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "s5");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group
    
    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }
  
  /**
   * Particle is optional (minOccurs="0") and the group is a fixture.
   * <xsd:complexType name="c0">
   *   <!-- optinal particle, fixture group -->
   *   <xsd:choice minOccurs="0">
   *     <xsd:element name="B"/>
   *     <xsd:element name="C"/>
   *   </xsd:choice>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_Choice_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "c0");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group
    
    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }
  
  /**
   * Particle is optional (minOccurs="0") and the group is not a fixture.
   * <xsd:complexType name="c1">
   *   <!-- optinal particle, non-fixture group -->
   *   <xsd:choice minOccurs="0">
   *     <xsd:element name="B" minOccurs="0"/>
   *     <xsd:element name="C"/>
   *   </xsd:choice>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_Choice_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "c1");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group

    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }
  
  /**
   * Particle is optional (minOccurs="0") and the group is not a fixture.
   * <xsd:complexType name="c2">
   *   <!-- optinal particle, non-fixture group -->
   *   <xsd:choice minOccurs="0">
   *     <xsd:element name="B" minOccurs="0"/>
   *     <xsd:element name="C" minOccurs="0"/>
   *   </xsd:choice>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_Choice_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "c2");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group

    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }
  
  /**
   * Particle is not optional (minOccurs="1") and the group is a fixture.
   * <xsd:complexType name="c3">
   *   <!-- non-optinal particle, fixture group -->
   *   <xsd:choice>
   *     <xsd:element name="B"/>
   *     <xsd:element name="C"/>
   *   </xsd:choice>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_Choice_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "c3");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group
    
    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }

  /**
   * Particle is not optional (minOccurs="1") and the group is a fixture.
   * <xsd:complexType name="c4">
   *   <!-- non-optinal particle, non-fixture group -->
   *   <xsd:choice>
   *     <xsd:element name="B" minOccurs="0"/>
   *     <xsd:element name="C"/>
   *   </xsd:choice>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_Choice_05() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "c4");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group
    
    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }
  
  /**
   * Particle is not optional (minOccurs="1") and the group is not a fixture.
   * <xsd:complexType name="c5">
   *   <!-- non-optinal particle, non-fixture group -->
   *   <xsd:choice>
   *     <xsd:element name="B" minOccurs="0"/>
   *     <xsd:element name="C" minOccurs="0"/>
   *   </xsd:choice>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_Choice_06() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "c5");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group
    
    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }
  
  /**
   * Particle is not optional (minOccurs="1") and the group is not a fixture.
   * <xsd:complexType name="c6">
   *   <!-- non-optinal particle, non-fixture group -->
   *   <xsd:choice>
   *     <xsd:element name="B" minOccurs="0" maxOccurs="0"/>
   *     <xsd:element name="C" minOccurs="0" maxOccurs="0"/>
   *   </xsd:choice>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_Choice_07() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "c6");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));
    
    Assert.assertEquals(EXISchema.CONTENT_ELEMENT_ONLY, corpus.getContentClassOfComplexType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(1, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances]);
    Assert.assertEquals(0, corpus.getNodes()[substances + 1]); // particle index within the group
  }
  
  /**
   * Particle is optional (minOccurs="0") and the group is a fixture.
   * <xsd:complexType name="a0">
   *   <!-- optinal particle, fixture group -->
   *   <xsd:all minOccurs="0">
   *     <xsd:element name="B"/>
   *     <xsd:element name="C"/>
   *   </xsd:all>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_All_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "a0");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group
    
    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }

  /**
   * Particle is optional (minOccurs="0") and the group is a fixture.
   * <xsd:complexType name="a1">
   *   <!-- optinal particle, fixture group -->
   *   <xsd:all minOccurs="0">
   *     <xsd:element name="B" minOccurs="0"/>
   *     <xsd:element name="C"/>
   *   </xsd:all>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_All_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "a1");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group

    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }
  
  /**
   * Particle is optional (minOccurs="0") and the group is not a fixture.
   * <xsd:complexType name="a2">
   *   <!-- optinal particle, non-fixture group -->
   *   <xsd:all minOccurs="0">
   *     <xsd:element name="B" minOccurs="0"/>
   *     <xsd:element name="C" minOccurs="0"/>
   *   </xsd:all>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_All_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "a2");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group

    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }
  
  /**
   * Particle is not optional (minOccurs="1") and the group is a fixture.
   * <xsd:complexType name="a3">
   *   <!-- non-optinal particle, fixture group -->
   *   <xsd:all>
   *     <xsd:element name="B"/>
   *     <xsd:element name="C"/>
   *   </xsd:all>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_All_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "a3");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group
    
    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }

  /**
   * Particle is not optional (minOccurs="1") and the group is a fixture.
   * <xsd:complexType name="a4">
   *   <!-- non-optinal particle, fixture group -->
   *   <xsd:all>
   *     <xsd:element name="B" minOccurs="0"/>
   *     <xsd:element name="C"/>
   *   </xsd:all>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_All_05() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "a4");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group
    
    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }
  
  /**
   * Particle is not optional (minOccurs="1") and the group is not a fixture.
   * <xsd:complexType name="a5">
   *   <!-- non-optinal particle, non-fixture group -->
   *   <xsd:all>
   *     <xsd:element name="B" minOccurs="0"/>
   *     <xsd:element name="C" minOccurs="0"/>
   *   </xsd:all>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_All_06() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "a5");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    
    Assert.assertEquals(3, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfComplexType(ctype));
    
    int pB = corpus.getNodes()[substances + 0];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(pB)));
    Assert.assertEquals(0, corpus.getNodes()[substances + 3]); // particle index within the group
    
    int pC = corpus.getNodes()[substances + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(pC)));
    Assert.assertEquals(1, corpus.getNodes()[substances + 4]); // particle index within the group
    
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 2]);
    Assert.assertEquals(2, corpus.getNodes()[substances + 5]); // particle index within the group
  }
  
  /**
   * No particle in a complex type. (empty content)
   * <xsd:complexType name="e1" />
   */
  public void testComplexTypeParticles_Empty_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "e1");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 0]);
    Assert.assertEquals(0, corpus.getNodes()[substances + 1]); // particle index within the group
    
    Assert.assertEquals(1, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfComplexType(ctype));
  }
  
  /**
   * No particle in a complex type. (simple content)
   * <xsd:complexType name="st1">
   *   <xsd:simpleContent>
   *     <xsd:extension base="xsd:string"/>
   *   </xsd:simpleContent>
   * </xsd:complexType>
   */
  public void testComplexTypeParticles_Simple_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int ctype = corpus.getTypeOfNamespace(foons, "st1");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(ctype));

    int substances = corpus.getSubstanceListOfComplexType(ctype);
    Assert.assertTrue(substances != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNodes()[substances + 0]);
    Assert.assertEquals(0, corpus.getNodes()[substances + 1]); // particle index within the group
    
    Assert.assertEquals(1, corpus.getSubstanceCountOfComplexType(ctype));
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfComplexType(ctype));
  }
  
  /**
   * Extend an empty content model that was defined so implicitly
   * <xsd:complexType name="empty"/>
   */
  public void testExtendEmptyContent_01() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/extendEmptyContent01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int tA = corpus.getTypeOfElem(eA);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(tA));

    int particleOfTypeA = corpus.getContentTypeOfComplexType(tA);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particleOfTypeA));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particleOfTypeA));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particleOfTypeA));
    int groupOfTypeA = corpus.getTermOfParticle(particleOfTypeA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(groupOfTypeA));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(groupOfTypeA));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(groupOfTypeA));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(groupOfTypeA));
    int pWildcard = corpus.getParticleOfGroup(groupOfTypeA, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pWildcard));
    int wildcard = corpus.getTermOfParticle(pWildcard);
    Assert.assertEquals(EXISchema.WILDCARD_NODE, corpus.getNodeType(wildcard));
  }
  
  /**
   * Extend an empty content model that was explicitly derived from xsd:anyType
   * by <xsd:restriction base="xsd:anyType"/>
   */
  public void testExtendEmptyContent_02() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/extendEmptyContent02.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int empty = corpus.getTypeOfNamespace(foons, "empty");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(empty));

    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int tA = corpus.getTypeOfElem(eA);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(tA));

    int particleOfTypeA = corpus.getContentTypeOfComplexType(tA);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particleOfTypeA));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particleOfTypeA));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particleOfTypeA));
    int groupOfTypeA = corpus.getTermOfParticle(particleOfTypeA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(groupOfTypeA));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(groupOfTypeA));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(groupOfTypeA));
    int pWildcard = corpus.getParticleOfGroup(groupOfTypeA, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pWildcard));
    int wildcard = corpus.getTermOfParticle(pWildcard);
    Assert.assertEquals(EXISchema.WILDCARD_NODE, corpus.getNodeType(wildcard));
  }
  
  /**
   * A seemingly empty content model actually end up having a particle
   * if it was defined to be of mixed content.
   */
  public void testExtendEmptyContent_03() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/extendEmptyContent03.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int empty = corpus.getTypeOfNamespace(foons, "empty");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(empty));

    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int tA = corpus.getTypeOfElem(eA);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(tA));

    int particleOfTypeA = corpus.getContentTypeOfComplexType(tA);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particleOfTypeA));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particleOfTypeA));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particleOfTypeA));
    int groupOfTypeA = corpus.getTermOfParticle(particleOfTypeA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(groupOfTypeA));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(groupOfTypeA));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(groupOfTypeA));
    int particle1 = corpus.getParticleOfGroup(groupOfTypeA, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle1));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particle1));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particle1));
    int emptyGroup = corpus.getTermOfParticle(particle1);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(emptyGroup));
    Assert.assertEquals(0, corpus.getParticleCountOfGroup(emptyGroup));
    int particle2 = corpus.getParticleOfGroup(groupOfTypeA, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle2));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particle2));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particle2));
    int appendedGroup = corpus.getTermOfParticle(particle2);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(appendedGroup));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(appendedGroup));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(appendedGroup));
    int pWildcard = corpus.getParticleOfGroup(appendedGroup, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pWildcard));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(pWildcard));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(pWildcard));
    int wildcard = corpus.getTermOfParticle(pWildcard);
    Assert.assertEquals(EXISchema.WILDCARD_NODE, corpus.getNodeType(wildcard));
  }
  
  public void testGroup() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/complexType.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int group;
    int particle1, particle2, particle3;
    int e1, e2, e3;

    int datedAddress = corpus.getTypeOfNamespace(foons, "datedAddress");
    group = corpus.getParticleTermOfComplexType(datedAddress);
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE,
                      corpus.getCompositorOfGroup(group));
    Assert.assertEquals(3, corpus.getParticleCountOfGroup(group));
    particle1 = corpus.getParticleOfGroup(group, 0);
    Assert.assertEquals(EXISchema.TERM_TYPE_ELEMENT,
                      corpus.getTermTypeOfParticle(particle1));
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle1));
    e1 = corpus.getTermOfParticle(particle1);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(e1));
    Assert.assertEquals("Street", corpus.getNameOfElem(e1));
    particle2 = corpus.getParticleOfGroup(group, 1);
    Assert.assertEquals(EXISchema.TERM_TYPE_ELEMENT,
                      corpus.getTermTypeOfParticle(particle2));
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle2));
    e2 = corpus.getTermOfParticle(particle2);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(e2));
    Assert.assertEquals("City", corpus.getNameOfElem(e2));
    particle3 = corpus.getParticleOfGroup(group, 2);
    Assert.assertEquals(EXISchema.TERM_TYPE_ELEMENT,
                      corpus.getTermTypeOfParticle(particle3));
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle3));
    e3 = corpus.getTermOfParticle(particle3);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(e3));
    Assert.assertEquals("Zip", corpus.getNameOfElem(e3));

    int datedAddress2 = corpus.getTypeOfNamespace(foons, "datedAddress2");
    group = corpus.getParticleTermOfComplexType(datedAddress2);
    Assert.assertEquals(EXISchema.GROUP_ALL,
                      corpus.getCompositorOfGroup(group));
    Assert.assertEquals(3, corpus.getParticleCountOfGroup(group));
    particle1 = corpus.getParticleOfGroup(group, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle1));
    e1 = corpus.getTermOfParticle(particle1);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(e1));
    Assert.assertEquals("Street2", corpus.getNameOfElem(e1));
    particle2 = corpus.getParticleOfGroup(group, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle2));
    e2 = corpus.getTermOfParticle(particle2);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(e2));
    Assert.assertEquals("City2", corpus.getNameOfElem(e2));
    particle3 = corpus.getParticleOfGroup(group, 2);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle3));
    e3 = corpus.getTermOfParticle(particle3);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(e3));
    Assert.assertEquals("Zip2", corpus.getNameOfElem(e3));

    int datedAddress3 = corpus.getTypeOfNamespace(foons, "datedAddress3");
    group = corpus.getParticleTermOfComplexType(datedAddress3);
    Assert.assertEquals(EXISchema.GROUP_CHOICE,
                      corpus.getCompositorOfGroup(group));
    Assert.assertEquals(3, corpus.getParticleCountOfGroup(group));
    particle1 = corpus.getParticleOfGroup(group, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle1));
    e1 = corpus.getTermOfParticle(particle1);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(e1));
    Assert.assertEquals("Street3", corpus.getNameOfElem(e1));
    particle2 = corpus.getParticleOfGroup(group, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle2));
    e2 = corpus.getTermOfParticle(particle2);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(e2));
    Assert.assertEquals("City3", corpus.getNameOfElem(e2));
    particle3 = corpus.getParticleOfGroup(group, 2);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle3));
    e3 = corpus.getTermOfParticle(particle3);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(e3));
    Assert.assertEquals("Zip3", corpus.getNameOfElem(e3));
  }

  public void testWildcard() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/wildcard.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int datedABCWildcard = corpus.getTypeOfNamespace(foons, "datedABCWildcard");
    int group = corpus.getParticleTermOfComplexType(datedABCWildcard);

    int particle2, particle4, particle6;
    int wc1, wc2, wc3;
    int namespace;

    particle2 = corpus.getParticleOfGroup(group, 1); // 1st wildcard
    Assert.assertEquals(EXISchema.TERM_TYPE_WILDCARD,
                      corpus.getTermTypeOfParticle(particle2));
    wc1 = corpus.getTermOfParticle(particle2);
    Assert.assertEquals(EXISchema.WILDCARD_NODE,
                      corpus.getNodeType(wc1));
    Assert.assertEquals(EXISchema.WC_TYPE_ANY,
                      corpus.getConstraintTypeOfWildcard(wc1));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX,
                      corpus.getProcessContentsOfWildcard(wc1));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(wc1));

    particle4 = corpus.getParticleOfGroup(group, 3); // 2nd wildcard
    Assert.assertEquals(EXISchema.TERM_TYPE_WILDCARD,
                      corpus.getTermTypeOfParticle(particle4));
    wc2 = corpus.getTermOfParticle(particle4);
    Assert.assertEquals(EXISchema.WILDCARD_NODE,
                      corpus.getNodeType(wc2));
    Assert.assertEquals(EXISchema.WC_TYPE_NOT,
                      corpus.getConstraintTypeOfWildcard(wc2));
    Assert.assertEquals(EXISchema.WC_PROCESS_SKIP,
                      corpus.getProcessContentsOfWildcard(wc2));
    Assert.assertEquals(2, corpus.getNamespaceCountOfWildcard(wc2));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfWildcard(wc2, 0));
    Assert.assertEquals("", corpus.getNamespaceNameOfWildcard(wc2, 1));
    namespace = corpus.getNamespaceOfWildcard(wc2, 0);
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(namespace));
    Assert.assertEquals("urn:foo", corpus.getNameOfNamespace(namespace));

    particle6 = corpus.getParticleOfGroup(group, 5); // 3rd wildcard
    Assert.assertEquals(EXISchema.TERM_TYPE_WILDCARD,
                      corpus.getTermTypeOfParticle(particle6));
    wc3 = corpus.getTermOfParticle(particle6);
    Assert.assertEquals(EXISchema.WILDCARD_NODE,
                      corpus.getNodeType(wc3));
    Assert.assertEquals(EXISchema.WC_TYPE_NAMESPACES,
                      corpus.getConstraintTypeOfWildcard(wc3));
    Assert.assertEquals(EXISchema.WC_PROCESS_STRICT,
                      corpus.getProcessContentsOfWildcard(wc3));
    Assert.assertEquals(4, corpus.getNamespaceCountOfWildcard(wc3));
    Assert.assertEquals("urn:goo", corpus.getNamespaceNameOfWildcard(wc3, 0));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNamespaceOfWildcard(wc3, 0));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfWildcard(wc3, 1));
    namespace = corpus.getNamespaceOfWildcard(wc3, 1);
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(namespace));
    Assert.assertEquals("urn:foo", corpus.getNameOfNamespace(namespace));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNamespaceOfWildcard(wc3, 0));
    Assert.assertEquals("urn:hoo", corpus.getNamespaceNameOfWildcard(wc3, 2));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNamespaceOfWildcard(wc3, 2));
    Assert.assertEquals("", corpus.getNamespaceNameOfWildcard(wc3, 3));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getNamespaceOfWildcard(wc3, 3));
  }

  public void testIsFixtureGroupSequence() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/fixtureGroup.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    //int group;
    //int particle1, particle2, particle3;
    //int e1, e2, e3;

    int foo_type_s0 = corpus.getTypeOfNamespace(foons, "s0");
    Assert.assertEquals(EXISchema.CONTENT_EMPTY,
                      corpus.getContentClassOfComplexType(foo_type_s0));

    int foo_type_s1 = corpus.getTypeOfNamespace(foons, "s1");
    int group_s1 = corpus.getParticleTermOfComplexType(foo_type_s1);
    Assert.assertTrue(corpus.isFixtureGroup(group_s1));

    int foo_type_s2 = corpus.getTypeOfNamespace(foons, "s2");
    int group_s2 = corpus.getParticleTermOfComplexType(foo_type_s2);
    Assert.assertFalse(corpus.isFixtureGroup(group_s2));

    int foo_type_s3 = corpus.getTypeOfNamespace(foons, "s3");
    int group_s3 = corpus.getParticleTermOfComplexType(foo_type_s3);
    Assert.assertTrue(corpus.isFixtureGroup(group_s3));

    int foo_type_s4 = corpus.getTypeOfNamespace(foons, "s4");
    int group_s4 = corpus.getParticleTermOfComplexType(foo_type_s4);
    Assert.assertTrue(corpus.isFixtureGroup(group_s4));

    int foo_type_s5 = corpus.getTypeOfNamespace(foons, "s5");
    int group_s5 = corpus.getParticleTermOfComplexType(foo_type_s5);
    Assert.assertTrue(corpus.isFixtureGroup(group_s5));

    int foo_type_s6 = corpus.getTypeOfNamespace(foons, "s6");
    int group_s6 = corpus.getParticleTermOfComplexType(foo_type_s6);
    Assert.assertFalse(corpus.isFixtureGroup(group_s6));

    int foo_type_s7 = corpus.getTypeOfNamespace(foons, "s7");
    int group_s7 = corpus.getParticleTermOfComplexType(foo_type_s7);
    Assert.assertTrue(corpus.isFixtureGroup(group_s7));

    int foo_type_s8 = corpus.getTypeOfNamespace(foons, "s8");
    int group_s8 = corpus.getParticleTermOfComplexType(foo_type_s8);
    Assert.assertTrue(corpus.isFixtureGroup(group_s8));

    int foo_type_s9 = corpus.getTypeOfNamespace(foons, "s9");
    int group_s9 = corpus.getParticleTermOfComplexType(foo_type_s9);
    Assert.assertFalse(corpus.isFixtureGroup(group_s9));

    int foo_type_s10 = corpus.getTypeOfNamespace(foons, "s10");
    int group_s10 = corpus.getParticleTermOfComplexType(foo_type_s10);
    Assert.assertTrue(corpus.isFixtureGroup(group_s10));

    int foo_type_s11 = corpus.getTypeOfNamespace(foons, "s11");
    int group_s11 = corpus.getParticleTermOfComplexType(foo_type_s11);
    Assert.assertFalse(corpus.isFixtureGroup(group_s11));
  }

  public void testIsFixtureGroupChoice() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/fixtureGroup.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    //int group;
    //int particle1, particle2, particle3;
    //int e1, e2, e3;

    int foo_type_c0 = corpus.getTypeOfNamespace(foons, "c0");
    /**
     * REVISIT: It really should be
       Assert.assertEquals(EXISchema.CONTENT_EMPTY,
                         corpus.getContentClassOfComplexType(foo_type_c0));
     */
    int group_c0 = corpus.getParticleTermOfComplexType(foo_type_c0);
    Assert.assertFalse(corpus.isFixtureGroup(group_c0));

    int foo_type_c1 = corpus.getTypeOfNamespace(foons, "c1");
    int group_c1 = corpus.getParticleTermOfComplexType(foo_type_c1);
    Assert.assertTrue(corpus.isFixtureGroup(group_c1));

    int foo_type_c2 = corpus.getTypeOfNamespace(foons, "c2");
    int group_c2 = corpus.getParticleTermOfComplexType(foo_type_c2);
    Assert.assertFalse(corpus.isFixtureGroup(group_c2));

    int foo_type_c3 = corpus.getTypeOfNamespace(foons, "c3");
    int group_c3 = corpus.getParticleTermOfComplexType(foo_type_c3);
    Assert.assertFalse(corpus.isFixtureGroup(group_c3));

    int foo_type_c4 = corpus.getTypeOfNamespace(foons, "c4");
    int group_c4 = corpus.getParticleTermOfComplexType(foo_type_c4);
    Assert.assertFalse(corpus.isFixtureGroup(group_c4));

    int foo_type_c5 = corpus.getTypeOfNamespace(foons, "c5");
    int group_c5 = corpus.getParticleTermOfComplexType(foo_type_c5);
    Assert.assertTrue(corpus.isFixtureGroup(group_c5));

    int foo_type_c6 = corpus.getTypeOfNamespace(foons, "c6");
    int group_c6 = corpus.getParticleTermOfComplexType(foo_type_c6);
    Assert.assertFalse(corpus.isFixtureGroup(group_c6));

    int foo_type_c7 = corpus.getTypeOfNamespace(foons, "c7");
    int group_c7 = corpus.getParticleTermOfComplexType(foo_type_c7);
    Assert.assertTrue(corpus.isFixtureGroup(group_c7));

    int foo_type_c8 = corpus.getTypeOfNamespace(foons, "c8");
    int group_c8 = corpus.getParticleTermOfComplexType(foo_type_c8);
    Assert.assertTrue(corpus.isFixtureGroup(group_c8));

    int foo_type_c9 = corpus.getTypeOfNamespace(foons, "c9");
    int group_c9 = corpus.getParticleTermOfComplexType(foo_type_c9);
    Assert.assertFalse(corpus.isFixtureGroup(group_c9));

    int foo_type_c10 = corpus.getTypeOfNamespace(foons, "c10");
    int group_c10 = corpus.getParticleTermOfComplexType(foo_type_c10);
    Assert.assertFalse(corpus.isFixtureGroup(group_c10));

    int foo_type_c11 = corpus.getTypeOfNamespace(foons, "c11");
    int group_c11 = corpus.getParticleTermOfComplexType(foo_type_c11);
    Assert.assertFalse(corpus.isFixtureGroup(group_c11));
  }

  public void testIsFixtureGroupAll() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/fixtureGroup.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    //int group;
    //int particle1, particle2, particle3;
    //int e1, e2, e3;

    int foo_type_a0 = corpus.getTypeOfNamespace(foons, "a0");
    Assert.assertEquals(EXISchema.CONTENT_EMPTY,
                      corpus.getContentClassOfComplexType(foo_type_a0));

    int foo_type_a1 = corpus.getTypeOfNamespace(foons, "a1");
    int group_a1 = corpus.getParticleTermOfComplexType(foo_type_a1);
    Assert.assertTrue(corpus.isFixtureGroup(group_a1));

    int foo_type_a2 = corpus.getTypeOfNamespace(foons, "a2");
    int group_a2 = corpus.getParticleTermOfComplexType(foo_type_a2);
    Assert.assertFalse(corpus.isFixtureGroup(group_a2));

    int foo_type_a3 = corpus.getTypeOfNamespace(foons, "a3");
    int group_a3 = corpus.getParticleTermOfComplexType(foo_type_a3);
    Assert.assertTrue(corpus.isFixtureGroup(group_a3));

    int foo_type_a4 = corpus.getTypeOfNamespace(foons, "a4");
    int group_a4 = corpus.getParticleTermOfComplexType(foo_type_a4);
    Assert.assertTrue(corpus.isFixtureGroup(group_a4));
  }

  public void testStateTableSequence() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/stateTable.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int[] nodes = corpus.getNodes();

    int list, list2;

    int foo_type_s1 = corpus.getTypeOfNamespace(foons, "s1");
    int p1 = corpus.getContentTypeOfComplexType(foo_type_s1);
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfParticle(p1));
    int s1_pb = corpus.getHeadSubstanceOfParticle(p1, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(s1_pb));
    int s1_b = corpus.getTermOfParticle(s1_pb);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(s1_b));
    Assert.assertEquals("B", corpus.getNameOfElem(s1_b));
    int group_s1 = corpus.getParticleTermOfComplexType(foo_type_s1);
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_s1, 0));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s1, 0));
    list = corpus.getHeadSubstanceListOfGroup(group_s1, 0);
    Assert.assertEquals(s1_pb, nodes[list]);
    Assert.assertEquals(0, nodes[list + 1]);
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_s1, 1));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s1, 1));
    list = corpus.getHeadSubstanceListOfGroup(group_s1, 1);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list]);
    Assert.assertEquals(1, nodes[list + 1]);

    int foo_type_s2 = corpus.getTypeOfNamespace(foons, "s2");
    int p2 = corpus.getContentTypeOfComplexType(foo_type_s2);
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfParticle(p2));
    int s2_pb = corpus.getHeadSubstanceOfParticle(p2, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(s2_pb));
    int s2_b = corpus.getTermOfParticle(s2_pb);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(s2_b));
    Assert.assertEquals("B", corpus.getNameOfElem(s2_b));
    int s2_pc = corpus.getHeadSubstanceOfParticle(p2, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(s2_pc));
    int s2_c = corpus.getTermOfParticle(s2_pc);
    Assert.assertEquals("C", corpus.getNameOfElem(s2_c));
    int group_s2 = corpus.getParticleTermOfComplexType(foo_type_s2);

    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(group_s2, 0));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s2, 0));
    list = corpus.getHeadSubstanceListOfGroup(group_s2, 0);
    Assert.assertEquals(s2_pb, nodes[list]);
    Assert.assertEquals(0, nodes[list + 2]);
    Assert.assertEquals(s2_pc, nodes[list + 1]);
    Assert.assertEquals(1, nodes[list + 3]);

    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(group_s2));
    list = corpus.getHeadSubstanceListOfGroup(group_s2);
    Assert.assertEquals(s2_pb, nodes[list]);
    Assert.assertEquals(0, nodes[list + 2]);
    Assert.assertEquals(s2_pc, nodes[list + 1]);
    Assert.assertEquals(1, nodes[list + 3]);

    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_s2, 1));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s2, 1));
    list = corpus.getHeadSubstanceListOfGroup(group_s2, 1);
    Assert.assertEquals(s2_pc, nodes[list]);
    Assert.assertEquals(1, nodes[list + 1]);

    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_s2, 2));
    list = corpus.getHeadSubstanceListOfGroup(group_s2, 2);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list]);
    Assert.assertEquals(2, nodes[list + 1]);
    
    int foo_type_s3 = corpus.getTypeOfNamespace(foons, "s3");
    int p3 = corpus.getContentTypeOfComplexType(foo_type_s3);
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfParticle(p3));
    int s3_pb = corpus.getHeadSubstanceOfParticle(p3, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(s3_pb));
    int s3_b = corpus.getTermOfParticle(s3_pb);
    Assert.assertEquals("B", corpus.getNameOfElem(s3_b));
    int group_s3 = corpus.getParticleTermOfComplexType(foo_type_s3);

    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_s3, 0));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s3, 0));
    list = corpus.getHeadSubstanceListOfGroup(group_s3, 0);
    Assert.assertEquals(s3_pb, nodes[list]);
    Assert.assertEquals(0, nodes[list + 1]);

    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(group_s3));
    list2 = corpus.getHeadSubstanceListOfGroup(group_s3);
    Assert.assertEquals(s3_pb, nodes[list2]);
    Assert.assertEquals(0, nodes[list2 + 2]);
    int s3_pc = nodes[list2 + 1];
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(s3_pc));
    int s3_c = corpus.getTermOfParticle(s3_pc);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(s3_c));
    Assert.assertEquals("C", corpus.getNameOfElem(s3_c));
    Assert.assertEquals(1, nodes[list2 + 3]);

    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_s3, 1));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s3, 1));
    list = corpus.getHeadSubstanceListOfGroup(group_s3, 1);
    Assert.assertEquals(s3_pc, nodes[list]);
    Assert.assertEquals(1, nodes[list + 1]);
    
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_s3, 2));
    list = corpus.getHeadSubstanceListOfGroup(group_s3, 2);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list]);
    Assert.assertEquals(2, nodes[list + 1]);
  }

  /**
   * Successive use of minOccurs="0" and maxOccurs="unbounded".
   */
  public void testStateTableSequence02() throws Exception {
    
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/stateTable.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int[] nodes = corpus.getNodes();

    int list;

    int foo_type_s6 = corpus.getTypeOfNamespace(foons, "s6");
    int p1 = corpus.getContentTypeOfComplexType(foo_type_s6);
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfParticle(p1));
    int s6_pa = corpus.getHeadSubstanceOfParticle(p1, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(s6_pa));
    int s6_a = corpus.getTermOfParticle(s6_pa);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(s6_a));
    Assert.assertEquals("A", corpus.getNameOfElem(s6_a));
    int s6_pb = corpus.getHeadSubstanceOfParticle(p1, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(s6_pb));
    int s6_b = corpus.getTermOfParticle(s6_pb);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(s6_b));
    Assert.assertEquals("B", corpus.getNameOfElem(s6_b));
    int s6_pc = corpus.getHeadSubstanceOfParticle(p1, 2);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(s6_pc));
    int s6_c = corpus.getTermOfParticle(s6_pc);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(s6_c));
    Assert.assertEquals("C", corpus.getNameOfElem(s6_c));
    
    int group_s6 = corpus.getParticleTermOfComplexType(foo_type_s6);
    Assert.assertEquals(4, corpus.getHeadSubstanceCountOfGroup(group_s6, 0));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s6, 0));

    list = corpus.getHeadSubstanceListOfGroup(group_s6, 0);
    Assert.assertEquals(s6_pa, nodes[list]);
    Assert.assertEquals(0, nodes[list + 4]);
    Assert.assertEquals(s6_pb, nodes[list + 1]);
    Assert.assertEquals(1, nodes[list + 5]);
    Assert.assertEquals(s6_pc, nodes[list + 2]);
    Assert.assertEquals(2, nodes[list + 6]);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 3]);
    Assert.assertEquals(3, nodes[list + 7]);
    
    Assert.assertEquals(4, corpus.getHeadSubstanceCountOfGroup(group_s6, 1));
    Assert.assertEquals(1, corpus.getBackwardHeadSubstanceCountOfGroup(group_s6, 1));
    list = corpus.getHeadSubstanceListOfGroup(group_s6, 1);
    Assert.assertEquals(s6_pa, nodes[list]);
    Assert.assertEquals(0, nodes[list + 4]);
    Assert.assertEquals(s6_pb, nodes[list + 1]);
    Assert.assertEquals(1, nodes[list + 5]);
    Assert.assertEquals(s6_pc, nodes[list + 2]);
    Assert.assertEquals(2, nodes[list + 6]);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 3]);
    Assert.assertEquals(3, nodes[list + 7]);
    
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfGroup(group_s6, 2));
    Assert.assertEquals(1, corpus.getBackwardHeadSubstanceCountOfGroup(group_s6, 2));
    list = corpus.getHeadSubstanceListOfGroup(group_s6, 2);
    Assert.assertEquals(s6_pb, nodes[list]);
    Assert.assertEquals(1, nodes[list + 3]);
    Assert.assertEquals(s6_pc, nodes[list + 1]);
    Assert.assertEquals(2, nodes[list + 4]);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 2]);
    Assert.assertEquals(3, nodes[list + 5]);
    
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(group_s6, 3));
    Assert.assertEquals(1, corpus.getBackwardHeadSubstanceCountOfGroup(group_s6, 3));
    list = corpus.getHeadSubstanceListOfGroup(group_s6, 3);
    Assert.assertEquals(s6_pc, nodes[list]);
    Assert.assertEquals(2, nodes[list + 2]);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 1]);
    Assert.assertEquals(3, nodes[list + 3]);
  }
  
  /**
   * Successive use of minOccurs="0" and maxOccurs="unbounded"
   * with the last particle being a choice.
   */
  public void testStateTableSequence03() throws Exception {
    
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/stateTable.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int[] nodes = corpus.getNodes();

    int list;

    int foo_type_s7 = corpus.getTypeOfNamespace(foons, "s7");
    int p1 = corpus.getContentTypeOfComplexType(foo_type_s7);
    Assert.assertEquals(4, corpus.getHeadSubstanceCountOfParticle(p1));
    int s7_pa = corpus.getHeadSubstanceOfParticle(p1, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(s7_pa));
    int s7_a = corpus.getTermOfParticle(s7_pa);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(s7_a));
    Assert.assertEquals("A", corpus.getNameOfElem(s7_a));
    int s7_pb = corpus.getHeadSubstanceOfParticle(p1, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(s7_pb));
    int s7_b = corpus.getTermOfParticle(s7_pb);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(s7_b));
    Assert.assertEquals("B", corpus.getNameOfElem(s7_b));
    int s7_pc = corpus.getHeadSubstanceOfParticle(p1, 2);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(s7_pc));
    int s7_c = corpus.getTermOfParticle(s7_pc);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(s7_c));
    Assert.assertEquals("C", corpus.getNameOfElem(s7_c));
    int s7_pd = corpus.getHeadSubstanceOfParticle(p1, 3);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(s7_pc));
    int s7_d = corpus.getTermOfParticle(s7_pd);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(s7_d));
    Assert.assertEquals("D", corpus.getNameOfElem(s7_d));
    
    int group_s7 = corpus.getParticleTermOfComplexType(foo_type_s7);
    Assert.assertEquals(5, corpus.getHeadSubstanceCountOfGroup(group_s7, 0));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s7, 0));

    list = corpus.getHeadSubstanceListOfGroup(group_s7, 0);
    Assert.assertEquals(s7_pa, nodes[list]);
    Assert.assertEquals(0, nodes[list + 5]);
    Assert.assertEquals(s7_pb, nodes[list + 1]);
    Assert.assertEquals(1, nodes[list + 6]);
    Assert.assertEquals(s7_pc, nodes[list + 2]);
    Assert.assertEquals(2, nodes[list + 7]);
    Assert.assertEquals(s7_pd, nodes[list + 3]);
    Assert.assertEquals(2, nodes[list + 8]);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 4]);
    Assert.assertEquals(3, nodes[list + 9]);
    
    Assert.assertEquals(5, corpus.getHeadSubstanceCountOfGroup(group_s7, 1));
    Assert.assertEquals(1, corpus.getBackwardHeadSubstanceCountOfGroup(group_s7, 1));
    list = corpus.getHeadSubstanceListOfGroup(group_s7, 1);
    Assert.assertEquals(s7_pa, nodes[list]);
    Assert.assertEquals(0, nodes[list + 5]);
    Assert.assertEquals(s7_pb, nodes[list + 1]);
    Assert.assertEquals(1, nodes[list + 6]);
    Assert.assertEquals(s7_pc, nodes[list + 2]);
    Assert.assertEquals(2, nodes[list + 7]);
    Assert.assertEquals(s7_pd, nodes[list + 3]);
    Assert.assertEquals(2, nodes[list + 8]);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 4]);
    Assert.assertEquals(3, nodes[list + 9]);
    
    Assert.assertEquals(4, corpus.getHeadSubstanceCountOfGroup(group_s7, 2));
    Assert.assertEquals(1, corpus.getBackwardHeadSubstanceCountOfGroup(group_s7, 2));
    list = corpus.getHeadSubstanceListOfGroup(group_s7, 2);
    Assert.assertEquals(s7_pb, nodes[list]);
    Assert.assertEquals(1, nodes[list + 4]);
    Assert.assertEquals(s7_pc, nodes[list + 1]);
    Assert.assertEquals(2, nodes[list + 5]);
    Assert.assertEquals(s7_pd, nodes[list + 2]);
    Assert.assertEquals(2, nodes[list + 6]);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 3]);
    Assert.assertEquals(3, nodes[list + 7]);
    
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfGroup(group_s7, 3));
    Assert.assertEquals(2, corpus.getBackwardHeadSubstanceCountOfGroup(group_s7, 3));
    list = corpus.getHeadSubstanceListOfGroup(group_s7, 3);
    Assert.assertEquals(s7_pc, nodes[list]);
    Assert.assertEquals(2, nodes[list + 3]);
    Assert.assertEquals(s7_pd, nodes[list + 1]);
    Assert.assertEquals(2, nodes[list + 4]);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 2]);
    Assert.assertEquals(3, nodes[list + 5]);
  }
  
  public void testStateTableSequenceNested() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/stateTable.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int[] nodes = corpus.getNodes();

    int list, list2;

    int foo_type_s4 = corpus.getTypeOfNamespace(foons, "s4");
    int p4 = corpus.getContentTypeOfComplexType(foo_type_s4);
    Assert.assertEquals(5, corpus.getHeadSubstanceCountOfParticle(p4));
    int s4_b = corpus.getHeadSubstanceOfParticle(p4, 0);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(s4_b)));
    int s4_c = corpus.getHeadSubstanceOfParticle(p4, 1);
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(s4_c)));
    int s4_d = corpus.getHeadSubstanceOfParticle(p4, 2);
    Assert.assertEquals("D", corpus.getNameOfElem(corpus.getTermOfParticle(s4_d)));
    int s4_e = corpus.getHeadSubstanceOfParticle(p4, 3);
    Assert.assertEquals("E", corpus.getNameOfElem(corpus.getTermOfParticle(s4_e)));
    int s4_f = corpus.getHeadSubstanceOfParticle(p4, 4);
    Assert.assertEquals("F", corpus.getNameOfElem(corpus.getTermOfParticle(s4_f)));

    int group_s4 = corpus.getParticleTermOfComplexType(foo_type_s4);

    Assert.assertEquals(5, corpus.getHeadSubstanceCountOfGroup(group_s4, 0));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s4, 0));
    list = corpus.getHeadSubstanceListOfGroup(group_s4, 0);
    Assert.assertEquals("B", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals("C", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list + 1])));
    Assert.assertEquals("D", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list + 2])));
    Assert.assertEquals("E", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list + 3])));
    Assert.assertEquals("F", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list + 4])));
    Assert.assertEquals(0, nodes[list + 5]);
    Assert.assertEquals(1, nodes[list + 6]);
    Assert.assertEquals(1, nodes[list + 7]);
    Assert.assertEquals(2, nodes[list + 8]);
    Assert.assertEquals(2, nodes[list + 9]);

    Assert.assertEquals(6, corpus.getHeadSubstanceCountOfGroup(group_s4));
    list2 = corpus.getHeadSubstanceListOfGroup(group_s4);
    Assert.assertEquals("B", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list2])));
    Assert.assertEquals("C", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list2 + 1])));
    Assert.assertEquals("D", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list2 + 2])));
    Assert.assertEquals("E", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list2 + 3])));
    Assert.assertEquals("F", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list2 + 4])));
    Assert.assertEquals("G", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list2 + 5])));
    Assert.assertEquals(0, nodes[list2 + 6]);
    Assert.assertEquals(1, nodes[list2 + 7]);
    Assert.assertEquals(1, nodes[list2 + 8]);
    Assert.assertEquals(2, nodes[list2 + 9]);
    Assert.assertEquals(2, nodes[list2 + 10]);
    Assert.assertEquals(3, nodes[list2 + 11]);

    Assert.assertEquals(4, corpus.getHeadSubstanceCountOfGroup(group_s4, 1));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s4, 1));
    list = corpus.getHeadSubstanceListOfGroup(group_s4, 1);
    Assert.assertEquals("C", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals("D", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list + 1])));
    Assert.assertEquals("E", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list + 2])));
    Assert.assertEquals("F", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list + 3])));
    Assert.assertEquals(1, nodes[list + 4]);
    Assert.assertEquals(1, nodes[list + 5]);
    Assert.assertEquals(2, nodes[list + 6]);
    Assert.assertEquals(2, nodes[list + 7]);
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(group_s4, 2));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s4, 2));
    list = corpus.getHeadSubstanceListOfGroup(group_s4, 2);
    Assert.assertEquals("E", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals("F", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list + 1])));
    Assert.assertEquals(2, nodes[list + 2]);
    Assert.assertEquals(2, nodes[list + 3]);
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_s4, 3));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s4, 3));
    list = corpus.getHeadSubstanceListOfGroup(group_s4, 3);
    Assert.assertEquals("G", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals(3, nodes[list + 1]);
    
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_s4, 4));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s4, 4));
    list = corpus.getHeadSubstanceListOfGroup(group_s4, 4);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list]);
    Assert.assertEquals(4, nodes[list + 1]);
  }

  public void testStateTableSequenceWithWildcard() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/stateTable.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int[] nodes = corpus.getNodes();

    int list, list2;

    int foo_type_s5 = corpus.getTypeOfNamespace(foons, "s5");
    int p5 = corpus.getContentTypeOfComplexType(foo_type_s5);
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfParticle(p5));
    int s5_anyParticle = corpus.getHeadSubstanceOfParticle(p5, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(s5_anyParticle));
    int s5_any = corpus.getTermOfParticle(s5_anyParticle);
    Assert.assertEquals(EXISchema.WILDCARD_NODE, corpus.getNodeType(s5_any));
    int group_s5 = corpus.getParticleTermOfComplexType(foo_type_s5);

    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_s5, 0));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s5, 0));
    list = corpus.getHeadSubstanceListOfGroup(group_s5, 0);
    Assert.assertEquals(EXISchema.WILDCARD_NODE,
                      corpus.getNodeType(corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals(0, nodes[list + 1]);

    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(group_s5));
    list2 = corpus.getHeadSubstanceListOfGroup(group_s5);
    Assert.assertEquals(EXISchema.WILDCARD_NODE,
                      corpus.getNodeType(corpus.getTermOfParticle(nodes[list2])));
    Assert.assertEquals("C", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list2 + 1])));
    Assert.assertEquals(0, nodes[list2 + 2]);
    Assert.assertEquals(1, nodes[list2 + 3]);

    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_s5, 1));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s5, 1));
    list = corpus.getHeadSubstanceListOfGroup(group_s5, 1);
    Assert.assertEquals("C", corpus.getNameOfElem(
        corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals(1, nodes[list + 1]);
    
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_s5, 2));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_s5, 2));
    list = corpus.getHeadSubstanceListOfGroup(group_s5, 2);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list]);
    Assert.assertEquals(2, nodes[list + 1]);
  }

  public void testStateTableChoice() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/stateTable.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int[] nodes = corpus.getNodes();

    int list;

    int foo_type_c1 = corpus.getTypeOfNamespace(foons, "c1");
    int p1 = corpus.getContentTypeOfComplexType(foo_type_c1);
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfParticle(p1));
    int c1_b = corpus.getHeadSubstanceOfParticle(p1, 0);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(c1_b)));
    int group_c1 = corpus.getParticleTermOfComplexType(foo_type_c1);
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_c1));
    list = corpus.getHeadSubstanceListOfGroup(group_c1);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(group_c1, 0));
    Assert.assertEquals(1, corpus.getBackwardHeadSubstanceCountOfGroup(group_c1, 0));
    list = corpus.getHeadSubstanceListOfGroup(group_c1, 0);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 1]);
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_c1, 1));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_c1, 1));
    list = corpus.getHeadSubstanceListOfGroup(group_c1, 1);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list]);

    int foo_type_c2 = corpus.getTypeOfNamespace(foons, "c2");
    int p2 = corpus.getContentTypeOfComplexType(foo_type_c2);
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfParticle(p2));
    int c2_b = corpus.getHeadSubstanceOfParticle(p2, 0);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(c2_b)));
    int c2_c = corpus.getHeadSubstanceOfParticle(p2, 1);
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(c2_c)));
    int group_c2 = corpus.getParticleTermOfComplexType(foo_type_c2);

    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(group_c2));
    list = corpus.getHeadSubstanceListOfGroup(group_c2);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals(0, nodes[list + 2]);
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list + 1])));
    Assert.assertEquals(1, nodes[list + 3]);
  
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(group_c2, 0));
    Assert.assertEquals(1, corpus.getBackwardHeadSubstanceCountOfGroup(group_c2, 0));
    list = corpus.getHeadSubstanceListOfGroup(group_c2, 0);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals(0, nodes[list + 2]);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 1]);
    Assert.assertEquals(2, nodes[list + 3]);

    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(group_c2, 1));
    Assert.assertEquals(1, corpus.getBackwardHeadSubstanceCountOfGroup(group_c2, 1));
    list = corpus.getHeadSubstanceListOfGroup(group_c2, 1);
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals(1, nodes[list + 2]);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 1]);
    Assert.assertEquals(2, nodes[list + 3]);
    
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_c2, 2));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_c2, 2));
    list = corpus.getHeadSubstanceListOfGroup(group_c2, 2);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list]);
    Assert.assertEquals(2, nodes[list + 1]);
  }

  public void testStateTableChoiceNested() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/stateTable.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int[] nodes = corpus.getNodes();

    int list;

    int foo_type_c3 = corpus.getTypeOfNamespace(foons, "c3");
    int p4 = corpus.getContentTypeOfComplexType(foo_type_c3);
    Assert.assertEquals(6, corpus.getHeadSubstanceCountOfParticle(p4));
    int c3_b = corpus.getHeadSubstanceOfParticle(p4, 0);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(c3_b)));
    int c3_c = corpus.getHeadSubstanceOfParticle(p4, 1);
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(c3_c)));
    int c3_d = corpus.getHeadSubstanceOfParticle(p4, 2);
    Assert.assertEquals("D", corpus.getNameOfElem(corpus.getTermOfParticle(c3_d)));
    int c3_e = corpus.getHeadSubstanceOfParticle(p4, 3);
    Assert.assertEquals("E", corpus.getNameOfElem(corpus.getTermOfParticle(c3_e)));
    int c3_f = corpus.getHeadSubstanceOfParticle(p4, 4);
    Assert.assertEquals("F", corpus.getNameOfElem(corpus.getTermOfParticle(c3_f)));
    int c3_g = corpus.getHeadSubstanceOfParticle(p4, 5);
    Assert.assertEquals("G", corpus.getNameOfElem(corpus.getTermOfParticle(c3_g)));

    int group_c3 = corpus.getParticleTermOfComplexType(foo_type_c3);

    Assert.assertEquals(7, corpus.getHeadSubstanceCountOfGroup(group_c3));
    list = corpus.getHeadSubstanceListOfGroup(group_c3);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list + 1])));
    Assert.assertEquals("D", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list + 2])));
    Assert.assertEquals("E", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list + 3])));
    Assert.assertEquals("F", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list + 4])));
    Assert.assertEquals("G", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list + 5])));
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 6]);
    Assert.assertEquals(0, nodes[list + 7]);
    Assert.assertEquals(1, nodes[list + 8]);
    Assert.assertEquals(1, nodes[list + 9]);
    Assert.assertEquals(2, nodes[list + 10]);
    Assert.assertEquals(2, nodes[list + 11]);
    Assert.assertEquals(3, nodes[list + 12]);
    Assert.assertEquals(4, nodes[list + 13]);
    
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(group_c3, 0));
    Assert.assertEquals(1, corpus.getBackwardHeadSubstanceCountOfGroup(group_c3, 0));
    list = corpus.getHeadSubstanceListOfGroup(group_c3, 0);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals(0, nodes[list + 2]);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 1]);
    Assert.assertEquals(4, nodes[list + 3]);
    
    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfGroup(group_c3, 1));
    Assert.assertEquals(2, corpus.getBackwardHeadSubstanceCountOfGroup(group_c3, 1));
    list = corpus.getHeadSubstanceListOfGroup(group_c3, 1);
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list + 0])));
    Assert.assertEquals(1, nodes[list + 3]);
    Assert.assertEquals("D", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list + 1])));
    Assert.assertEquals(1, nodes[list + 4]);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 2]);
    Assert.assertEquals(4, nodes[list + 5]);

    Assert.assertEquals(3, corpus.getHeadSubstanceCountOfGroup(group_c3, 2));
    Assert.assertEquals(2, corpus.getBackwardHeadSubstanceCountOfGroup(group_c3, 2));
    list = corpus.getHeadSubstanceListOfGroup(group_c3, 2);
    Assert.assertEquals("E", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list + 0])));
    Assert.assertEquals(2, nodes[list + 3]);
    Assert.assertEquals("F", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list + 1])));
    Assert.assertEquals(2, nodes[list + 4]);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 2]);
    Assert.assertEquals(4, nodes[list + 5]);
    
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(group_c3, 3));
    Assert.assertEquals(1, corpus.getBackwardHeadSubstanceCountOfGroup(group_c3, 3));
    list = corpus.getHeadSubstanceListOfGroup(group_c3, 3);
    Assert.assertEquals("G", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list + 0])));
    Assert.assertEquals(3, nodes[list + 2]);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list + 1]);
    Assert.assertEquals(4, nodes[list + 3]);
    
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_c3, 4));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_c3, 4));
    list = corpus.getHeadSubstanceListOfGroup(group_c3, 4);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list]);
    Assert.assertEquals(4, nodes[list + 1]);
  }

  public void testStateTableAll() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/stateTable.xsd", getClass());
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int[] nodes = corpus.getNodes();

    int list;

    int foo_type_a1 = corpus.getTypeOfNamespace(foons, "a1");
    int p1 = corpus.getContentTypeOfComplexType(foo_type_a1);
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfParticle(p1));
    int a1_b = corpus.getHeadSubstanceOfParticle(p1, 0);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(a1_b)));
    int group_a1 = corpus.getParticleTermOfComplexType(foo_type_a1);
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(group_a1, 0));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_a1, 0));
    list = corpus.getHeadSubstanceListOfGroup(group_a1, 0);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals(0, nodes[list + 1]);

    int foo_type_a2 = corpus.getTypeOfNamespace(foons, "a2");
    int p2 = corpus.getContentTypeOfComplexType(foo_type_a2);
    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfParticle(p2));
    int a2_b = corpus.getHeadSubstanceOfParticle(p2, 0);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(a2_b)));
    int a2_c = corpus.getHeadSubstanceOfParticle(p2, 1);
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(a2_c)));
    int group_a2 = corpus.getParticleTermOfComplexType(foo_type_a2);

    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(group_a2, 0));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_a2, 0));
    list = corpus.getHeadSubstanceListOfGroup(group_a2, 0);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals(0, nodes[list + 2]);
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list + 1])));
    Assert.assertEquals(1, nodes[list + 3]);

    Assert.assertEquals(2, corpus.getHeadSubstanceCountOfGroup(group_a2));
    list = corpus.getHeadSubstanceListOfGroup(group_a2);
    Assert.assertEquals("B", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list])));
    Assert.assertEquals(0, nodes[list + 2]);
    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list + 1])));
    Assert.assertEquals(1, nodes[list + 3]);

    Assert.assertEquals(0, corpus.getHeadSubstanceCountOfGroup(group_a2, 1));
    Assert.assertEquals(0, corpus.getBackwardHeadSubstanceCountOfGroup(group_a2, 1));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getHeadSubstanceListOfGroup(group_a2, 1));
//    list = corpus.getHeadSubstanceListOfGroup(group_a2, 1);
//    Assert.assertEquals("C", corpus.getNameOfElem(corpus.getTermOfParticle(nodes[list])));
//    Assert.assertEquals(1, nodes[list + 1]);
  }

  /**
   * The schema document is not well-formed.
   */
  public void testSAXParseException01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/saxParseException01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertNull(corpus);
    
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce1, sce2;
    EXISchemaFactoryException[] sceList = m_compilerErrorHandler.getFatalErrors();
    sce1 = sceList[0];
    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce1.getCode());
    Assert.assertEquals(9, sce1.getLocator().getLineNumber());
    sce2 = sceList[1];
    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce2.getCode());
    // sce2 is a duplicate of sce1, except for missing line number.
    Assert.assertEquals(-1, sce2.getLocator().getLineNumber());
  }

  /**
   * nillable="OK", where "true" or "false" is expected.
   */
  public void testSAXParseException02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/saxParseException02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    Assert.assertNotNull(sce);
    Locator locator = sce.getLocator();
    Assert.assertNotNull(locator);
    Assert.assertTrue(locator.getSystemId().endsWith("saxParseException02.xsd"));
    Assert.assertEquals(7, locator.getLineNumber());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int elemA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elemA));
    Assert.assertFalse(corpus.isNillableElement(elemA));
  }

  /**
   * Importing the same schema multiple times.
   * Two schemas extending the same one.
   */
  public void testMultiSchemaParallel() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/xbrlBareBoneIoo.xsd",
        getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    /**
     * http://www.w3.org/2001/XMLSchema
     * http://www.xbrl.org/2003/instance
     * http://www.w3.org/2001/XMLSchema-instance
     * urn:foo
     * urn:goo
     * urn:hoo
     * urn:ioo
     */
    Assert.assertEquals(7, corpus.getNamespaceCountOfSchema());

    int xsns = corpus.getNamespaceOfSchema(XMLSCHEMA_URI);
    Assert.assertTrue(xsns != EXISchema.NIL_NODE);

    int xbrlins = corpus.getNamespaceOfSchema("http://www.xbrl.org/2003/instance");
    Assert.assertTrue(xbrlins != EXISchema.NIL_NODE);

    int xsins = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema-instance");
    Assert.assertTrue(xsins != EXISchema.NIL_NODE);

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertTrue(foons != EXISchema.NIL_NODE);
    int fooIA = corpus.getElemOfNamespace(foons, "IA");
    Assert.assertTrue(fooIA != EXISchema.NIL_NODE);

    int goons = corpus.getNamespaceOfSchema("urn:goo");
    Assert.assertTrue(goons != EXISchema.NIL_NODE);
    int gooIA = corpus.getElemOfNamespace(goons, "IA");
    Assert.assertEquals(fooIA, corpus.getSubstOfElem(gooIA));

    int hoons = corpus.getNamespaceOfSchema("urn:hoo");
    Assert.assertTrue(hoons != EXISchema.NIL_NODE);
    int hooIA = corpus.getElemOfNamespace(hoons, "IA");
    Assert.assertEquals(fooIA, corpus.getSubstOfElem(hooIA));
  }

  /**
   * Always use bundled XBRL schemas instead of those specified by the
   * schema author, which is no longer the case.
   */
  public void testUseItsOwnXBRLSchemas() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/xbrlBareBone.xsd", getClass());

        /**
     * XMLSchema, XMLSchema-instance, urn:foo, xbrli
     */
    Assert.assertEquals(4, corpus.getNamespaceCountOfSchema());
  }

  /**
   * Test global element's serial numbers.
   */
  public void testSerailOfElem() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/xbrlBareBone.xsd", getClass());

    int len = corpus.getElemCountOfSchema();
    Assert.assertTrue(len > 0);

    for (int i = 0; i < len; i++) {
      int elem = corpus.getElemOfSchema(i);
      Assert.assertEquals(i, corpus.getSerialOfElem(elem));
    }

    int type = corpus.getTypeOfElem(corpus.getElemOfSchema(0));
    Assert.assertTrue(EXISchema.NIL_NODE != type);
    try {
      corpus.getSerialOfElem(type);
    }
    catch (EXISchemaRuntimeException scre) {
      Assert.assertEquals(EXISchemaRuntimeException.NOT_ELEMENT, scre.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * An element that has a unknown type is recognized as having ur-type.
   */
  public void testTypeDefNotAvailable() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/typeNotDefined01.xsd",
                                             getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    Assert.assertNotNull(m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0]);

    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    int foo_A = corpus.getElemOfNamespace(foons, "A");
    Assert.assertTrue(!corpus.isSimpleTypedElement(foo_A));
    Assert.assertTrue(corpus.isUrTypedElement(foo_A));
  }

  public void testIncludeSchema01() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/verySimpleIncluding.xsd",
                                               getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    
    // foo (9 + 2) + goo 4 = 15
    Assert.assertEquals(15, corpus.getElemCountOfSchema());
    Assert.assertEquals(15, corpus.getTotalElemCount());

    // foo (1 + 1) + goo 1 + xsi 4 = 7
    Assert.assertEquals(7, corpus.getAttrCountOfSchema());
    // foo (1 + 3) + goo 1 + xsi 4 = 9
    Assert.assertEquals(9, corpus.getTotalAttrCount());

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertTrue(foons != EXISchema.NIL_NODE);
    Assert.assertEquals(11, corpus.getElemCountOfNamespace(foons));
    Assert.assertEquals(2, corpus.getAttrCountOfNamespace(foons));

    int goons = corpus.getNamespaceOfSchema("urn:goo");
    Assert.assertTrue(goons != EXISchema.NIL_NODE);
    Assert.assertEquals(4, corpus.getElemCountOfNamespace(goons));
    Assert.assertEquals(1, corpus.getAttrCountOfNamespace(goons));
  }

  /**
   *  Including chameleon schema.
   */
  public void testIncludeSchemaChameleon_01() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/chameleonIncluding_01.xsd",
                                               getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

        
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertTrue(foons != EXISchema.NIL_NODE);
    
    int unionedString = corpus.getTypeOfNamespace(foons, "unionedString");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(unionedString));
    Assert.assertEquals(2, corpus.getMemberTypesCountOfSimpleType(unionedString));

    int string10 = corpus.getMemberTypeOfSimpleType(unionedString, 0);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(string10));
    Assert.assertEquals("string10", corpus.getNameOfType(string10));
    Assert.assertEquals("urn:foo", corpus.getTargetNamespaceNameOfType(string10));
    
    int nmtoken = corpus.getMemberTypeOfSimpleType(unionedString, 1);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(nmtoken));
    Assert.assertEquals("NMTOKEN", corpus.getNameOfType(nmtoken));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", corpus.getTargetNamespaceNameOfType(nmtoken));
  }
  
  /**
   * Test empty group.
   */
  public void testEmptyGroup01() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/emptyGroup.xsd",
                                               getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int[] nodes = corpus.getNodes();
    int list;
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertTrue(foons != EXISchema.NIL_NODE);

    int emptyCT = corpus.getTypeOfNamespace(foons, "emptyCT");
    Assert.assertTrue(emptyCT != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE,
                      corpus.getNodeType(emptyCT));

    int inlineGroup = corpus.getParticleTermOfComplexType(emptyCT);
    Assert.assertTrue(inlineGroup != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(inlineGroup));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE,
                      corpus.getCompositorOfGroup(inlineGroup));
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(inlineGroup));
    list = corpus.getHeadSubstanceListOfGroup(inlineGroup);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list]);
    Assert.assertEquals(1, nodes[list + 1]);
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(inlineGroup));

    int particle = corpus.getParticleOfGroup(inlineGroup, 0);
    Assert.assertTrue(particle != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.PARTICLE_NODE,
                      corpus.getNodeType(particle));
    Assert.assertEquals(0, corpus.getHeadSubstanceCountOfParticle(particle));
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getHeadSubstanceListOfParticle(particle));

    int emptyGroup = corpus.getTermOfParticle(particle);
    Assert.assertTrue(emptyGroup != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(emptyGroup));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE,
                      corpus.getCompositorOfGroup(emptyGroup));
    Assert.assertEquals(1, corpus.getHeadSubstanceCountOfGroup(emptyGroup));
    list = corpus.getHeadSubstanceListOfGroup(emptyGroup);
    Assert.assertEquals(EXISchema.NIL_NODE, nodes[list]);
    Assert.assertEquals(0, nodes[list + 1]);
    Assert.assertEquals(0, corpus.getParticleCountOfGroup(emptyGroup));
  }

  /**
   * Currently, length facet value "100000000000000000000000000" results in
   * an error in XMLSchema processor.
   * <xsd:length value="100000000000000000000000000"/>
   */
  public void testLengthTooLarge() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/lengthTooLarge.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException[] speList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, speList.length);
    XMLParseException spe = (XMLParseException)speList[0].getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-invalid-value:"));
  }

  /**
   * Make sure mutual import between two schemas does not end up entering
   * an infinite loop.
   */
  public void testCircularImport() throws Exception {

    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/circularSchemaImport_1.xsd",
                                             getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int default3 = corpus.getNamespaceOfSchema("http://www.fujitsu.com/xbrl/taxeditor/default3");
    Assert.assertTrue(default3 != EXISchema.NIL_NODE);
    int default2 = corpus.getNamespaceOfSchema("http://www.fujitsu.com/xbrl/taxeditor/default2");
    Assert.assertTrue(default2 != EXISchema.NIL_NODE);
  }

  /**
   * Make sure mutual import between two schemas does not end up with
   * a stack overflow exception.
   */
  public void testMutualImportWithHub() throws Exception {

    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/mutualImport/hub.xsd",
                                             getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int hubns = corpus.getNamespaceOfSchema("urn:hub");
    Assert.assertTrue(hubns != EXISchema.NIL_NODE);

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertTrue(foons != EXISchema.NIL_NODE);

    int goons = corpus.getNamespaceOfSchema("urn:goo");
    Assert.assertTrue(goons != EXISchema.NIL_NODE);

    int zoons = corpus.getNamespaceOfSchema("urn:zoo");
    Assert.assertTrue(zoons != EXISchema.NIL_NODE);
  }

  /**
   * There are two declarations of element "C" in namespace "urn:foo".
   */
  public void testDuplicateImport01() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/duplicateImport/zoo1.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException[] speList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, speList.length);
    XMLParseException spe = (XMLParseException)speList[0].getException();
    Assert.assertEquals(14, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("sch-props-correct.2"));

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int groupOfA = corpus.getGroupOfElem(eA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(groupOfA));
    Assert.assertEquals(1, corpus.getMemberSubstanceCountOfGroup(groupOfA));

    int eB = corpus.getElemOfNamespace(foons, "B");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eB));
    int groupOfB = corpus.getGroupOfElem(eB);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(groupOfB));
    Assert.assertEquals(1, corpus.getMemberSubstanceCountOfGroup(groupOfB));

    int pC1 = corpus.getMemberSubstanceOfGroup(groupOfA, 0); 
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC1));
    int eC1 = corpus.getTermOfParticle(pC1);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eC1));
    Assert.assertEquals("urn:foo", corpus.getTargetNamespaceNameOfElem(eC1));
    Assert.assertEquals("C", corpus.getNameOfElem(eC1));
    
    int pC2 = corpus.getMemberSubstanceOfGroup(groupOfB, 0); 
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pC2));
    int eC2 = corpus.getTermOfParticle(pC2);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eC2));
    Assert.assertEquals("urn:foo", corpus.getTargetNamespaceNameOfElem(eC2));
    Assert.assertEquals("C", corpus.getNameOfElem(eC2));

    Assert.assertEquals(eC1, eC2);
  }
  
  /**
   * There are 3 schemas.
   * schema_a, schema_b and schema_c of which schema_a is the root.
   * schema_b includes schema_c.
   * schema_a includes both schema_c and schema_b.
   */
  public void testDuplicateSchemaInclude1() throws Exception {

    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/duplicateSchemaInclude1_a.xsd",
                                             getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

        // urn:foo plus xsd, XMLSchema-instance.xsd
    Assert.assertEquals(3, corpus.getNamespaceCountOfSchema());

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals("urn:foo", corpus.getNameOfNamespace(foons));

    Assert.assertEquals(2, corpus.getElemCountOfNamespace(foons));
    Assert.assertEquals(1, corpus.getTypeCountOfNamespace(foons));

    int ea = corpus.getElemOfNamespace(foons, "ea");
    Assert.assertEquals("ea", corpus.getNameOfElem(ea));
    int eb = corpus.getElemOfNamespace(foons, "eb");
    Assert.assertEquals("eb", corpus.getNameOfElem(eb));
    int tc = corpus.getTypeOfNamespace(foons, "tc");
    Assert.assertEquals("tc", corpus.getNameOfType(tc));

    Assert.assertEquals(tc, corpus.getTypeOfElem(ea));
    Assert.assertEquals(eb, corpus.getSubstOfElem(ea));
    Assert.assertEquals(tc, corpus.getTypeOfElem(eb));
  }

  /**
   * There are 4 schemas.
   * schema_a, schema_b, schema_c and schema_d of which schema_a is the root.
   * - namespace urn:foo consists of schema_a and schema_b.
   * - namespace urn:goo consists of schema_c and schema_d.
   * - schema_a includes schema_b and imports schema_c.
   * - schema_b imports schema_d.
   * - schema_c includes schema_d.
   */
  public void testDuplicateSchemaIncludeImport1() throws Exception {

    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/duplicateSchemaIncludeImport1_a.xsd",
                                             getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

        // urn:foo and urn:goo plus xsd, XMLSchema-instance.xsd
    Assert.assertEquals(4, corpus.getNamespaceCountOfSchema());

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals("urn:foo", corpus.getNameOfNamespace(foons));
    int goons = corpus.getNamespaceOfSchema("urn:goo");

    Assert.assertEquals(1, corpus.getElemCountOfNamespace(foons));
    Assert.assertEquals(1, corpus.getTypeCountOfNamespace(foons));
    Assert.assertEquals(1, corpus.getElemCountOfNamespace(goons));
    Assert.assertEquals(1, corpus.getTypeCountOfNamespace(goons));

    int ea = corpus.getElemOfNamespace(foons, "ea");
    Assert.assertEquals("ea", corpus.getNameOfElem(ea));
    int tb = corpus.getTypeOfNamespace(foons, "tb");
    Assert.assertEquals("tb", corpus.getNameOfType(tb));
    int ec = corpus.getElemOfNamespace(goons, "ec");
    Assert.assertEquals("ec", corpus.getNameOfElem(ec));
    int td = corpus.getTypeOfNamespace(goons, "td");
    Assert.assertEquals("td", corpus.getNameOfType(td));

    Assert.assertEquals(tb, corpus.getTypeOfElem(ea));
    Assert.assertEquals(ec, corpus.getSubstOfElem(ea));
    Assert.assertEquals(td, corpus.getBaseTypeOfType(tb));
    Assert.assertEquals(td, corpus.getTypeOfElem(ec));
  }

  /**
   * Circular inclusion. (element declaration)
   */
  public void testCircularSchemaInclude0() throws Exception {

    String[] files = {
        "/includeReverseReference/goo.xsd", // mutual inclusion
        "/includeReverseReference/foo.xsd"  // uni-directional inclusion
    };

    for (int i = 0; i < files.length; i++) {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
          files[i], getClass(), m_compilerErrorHandler);

      Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

            // urn:foo plus xsd, XMLSchema-instance.xsd
      Assert.assertEquals(3, corpus.getNamespaceCountOfSchema());

      int foons = corpus.getNamespaceOfSchema("urn:foo");
      Assert.assertEquals("urn:foo", corpus.getNameOfNamespace(foons));

      Assert.assertEquals(2, corpus.getElemCountOfNamespace(foons));
      Assert.assertEquals(1, corpus.getTypeCountOfNamespace(foons));

      int eItem = corpus.getElemOfNamespace(foons, "MyItem");
      Assert.assertEquals("MyItem", corpus.getNameOfElem(eItem));
      int eTuple = corpus.getElemOfNamespace(foons, "MyTuple");
      Assert.assertEquals("MyTuple", corpus.getNameOfElem(eTuple));
      int tTuple = corpus.getTypeOfNamespace(foons, "MyTupleType");
      Assert.assertEquals("MyTupleType", corpus.getNameOfType(tTuple));

      Assert.assertEquals(tTuple, corpus.getTypeOfElem(eTuple));
      int gc = corpus.getParticleTermOfComplexType(tTuple);
      Assert.assertEquals(EXISchema.GROUP_SEQUENCE,
                        corpus.getCompositorOfGroup(gc));
      Assert.assertEquals(1, corpus.getParticleCountOfGroup(gc));
      Assert.assertEquals(
          eItem, corpus.getTermOfParticle(corpus.getParticleOfGroup(gc, 0)));
    }
    /**
     * EXISchemaFactoryException[] exceptions = m_compilerErrorHandler.getErrors(
     *     EXISchemaFactoryException.SCHEMAPARSE_ERROR);
     * Assert.assertEquals(1, exceptions.length);
     *
     * EXISchemaFactoryException sce = (EXISchemaFactoryException)exceptions[0];
     * SAXParseException spe = (SAXParseException)sce.getException();
     * Assert.assertEquals(8, spe.getLineNumber());
     * Assert.assertTrue(spe.getSystemId().endsWith("foo2.xsd"));
     *
     * FjParseException fpe = (FjParseException)spe.getException();
     * Assert.assertEquals(XmlExceptionCode.IDS_SCHEMA_PARSE_ERROR_ELEMDECL_REF_QNAME_UNSOLVED,
     *                   fpe.getCode());
     */
  }

  /**
   * Circular inclusion. (type declaration)
   */
  public void testCircularSchemaInclude1() throws Exception {

    String[] files = {
        "/circularSchemaInclude/circularSchemaInclude1_a.xsd",
        "/includeReverseReference/includeBackReference1_a.xsd"
    };

    for (int i = 0; i < files.length; i++) {
      EXISchema corpus =
          EXISchemaFactoryTestUtil.getEXISchema(
          files[i], getClass(), m_compilerErrorHandler);

      Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

            // urn:foo plus xsd, XMLSchema-instance.xsd
      Assert.assertEquals(3, corpus.getNamespaceCountOfSchema());

      int foons = corpus.getNamespaceOfSchema("urn:foo");
      Assert.assertEquals("urn:foo", corpus.getNameOfNamespace(foons));

      Assert.assertEquals(2, corpus.getElemCountOfNamespace(foons));
      Assert.assertEquals(2, corpus.getTypeCountOfNamespace(foons));

      int ea = corpus.getElemOfNamespace(foons, "ea");
      Assert.assertEquals("ea", corpus.getNameOfElem(ea));
      int ta = corpus.getTypeOfNamespace(foons, "ta");
      Assert.assertEquals("ta", corpus.getNameOfType(ta));
      int eb = corpus.getElemOfNamespace(foons, "eb");
      Assert.assertEquals("eb", corpus.getNameOfElem(eb));
      int tc = corpus.getTypeOfNamespace(foons, "tc");
      Assert.assertEquals("tc", corpus.getNameOfType(tc));

      Assert.assertEquals(tc, corpus.getTypeOfElem(ea));
      Assert.assertEquals(eb, corpus.getSubstOfElem(ea));
      Assert.assertEquals(ta, corpus.getTypeOfElem(eb));
      Assert.assertEquals(ta, corpus.getBaseTypeOfType(tc));
    }
  }

  /**
   * Circular inclusion. (attribute declaration)
   */
  public void testCircularSchemaInclude2() throws Exception {

    String[] files = {
        "/circularSchemaInclude/circularSchemaInclude2_a.xsd",
        "/includeReverseReference/includeBackReference2_a.xsd"
    };

    for (int i = 0; i < files.length; i++) {
      EXISchema corpus =
          EXISchemaFactoryTestUtil.getEXISchema(
          files[i], getClass(), m_compilerErrorHandler);

      Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

            // urn:foo plus xsd, XMLSchema-instance.xsd
      Assert.assertEquals(3, corpus.getNamespaceCountOfSchema());

      int foons = corpus.getNamespaceOfSchema("urn:foo");
      Assert.assertEquals("urn:foo", corpus.getNameOfNamespace(foons));

      Assert.assertEquals(2, corpus.getElemCountOfNamespace(foons));
      Assert.assertEquals(1, corpus.getTypeCountOfNamespace(foons));
      Assert.assertEquals(1, corpus.getAttrCountOfNamespace(foons));

      int ea = corpus.getElemOfNamespace(foons, "ea");
      Assert.assertEquals("ea", corpus.getNameOfElem(ea));
      int aa = corpus.getAttrOfNamespace(foons, "aa");
      Assert.assertEquals("aa", corpus.getNameOfAttr(aa));
      int eb = corpus.getElemOfNamespace(foons, "eb");
      Assert.assertEquals("eb", corpus.getNameOfElem(eb));
      int tc = corpus.getTypeOfNamespace(foons, "tc");
      Assert.assertEquals("tc", corpus.getNameOfType(tc));

      Assert.assertEquals(tc, corpus.getTypeOfElem(ea));
      Assert.assertEquals(1, corpus.getAttrUseCountOfElem(eb));
      Assert.assertEquals(
          aa, corpus.getAttrOfAttrUse(corpus.getAttrUseOfElem(eb, 0)));
      Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(tc));
      Assert.assertEquals(
          aa, corpus.getAttrOfAttrUse(corpus.getAttrUseOfComplexType(tc, 0)));
    }
  }

  /**
   * Circular inclusion. (model group)
   */
  public void testCircularSchemaInclude3() throws Exception {

    String[] files = {
        "/circularSchemaInclude/circularSchemaInclude3_a.xsd",
        "/includeReverseReference/includeBackReference3_a.xsd"
    };

    for (int i = 0; i < files.length; i++) {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
          files[i], getClass(), m_compilerErrorHandler);

      Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

            // urn:foo plus xsd, XMLSchema-instance.xsd
      Assert.assertEquals(3, corpus.getNamespaceCountOfSchema());

      int foons = corpus.getNamespaceOfSchema("urn:foo");
      Assert.assertEquals("urn:foo", corpus.getNameOfNamespace(foons));

      Assert.assertEquals(1, corpus.getElemCountOfNamespace(foons));
      Assert.assertEquals(1, corpus.getTypeCountOfNamespace(foons));

      int ea = corpus.getElemOfNamespace(foons, "ea");
      Assert.assertEquals("ea", corpus.getNameOfElem(ea));
      int tc = corpus.getTypeOfNamespace(foons, "tc");
      Assert.assertEquals("tc", corpus.getNameOfType(tc));

      Assert.assertEquals(tc, corpus.getTypeOfElem(ea));
      int gc = corpus.getParticleTermOfComplexType(tc);
      Assert.assertEquals(EXISchema.GROUP_SEQUENCE,
                        corpus.getCompositorOfGroup(gc));
      Assert.assertEquals(2, corpus.getParticleCountOfGroup(gc));
      int ga = corpus.getTermOfParticle(corpus.getParticleOfGroup(gc, 0));
      Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(ga));
      Assert.assertEquals(EXISchema.GROUP_SEQUENCE,
                        corpus.getCompositorOfGroup(ga));
      Assert.assertEquals(1, corpus.getParticleCountOfGroup(ga));
      int ea_local = corpus.getTermOfParticle(corpus.getParticleOfGroup(ga, 0));
      Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(ea_local));
      int ec = corpus.getTermOfParticle(corpus.getParticleOfGroup(gc, 1));
      Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(ec));
    }
  }

  /**
   * Circular inclusion. (attribute group)
   */
  public void testCircularSchemaInclude4() throws Exception {

    String[] files = {
        "/circularSchemaInclude/circularSchemaInclude4_a.xsd",
        "/includeReverseReference/includeBackReference4_a.xsd"
    };

    for (int i = 0; i < files.length; i++) {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
          files[i], getClass(), m_compilerErrorHandler);

      Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

            // urn:foo plus xsd, XMLSchema-instance.xsd
      Assert.assertEquals(3, corpus.getNamespaceCountOfSchema());

      int foons = corpus.getNamespaceOfSchema("urn:foo");
      Assert.assertEquals("urn:foo", corpus.getNameOfNamespace(foons));

      Assert.assertEquals(1, corpus.getElemCountOfNamespace(foons));
      Assert.assertEquals(1, corpus.getTypeCountOfNamespace(foons));

      int ea = corpus.getElemOfNamespace(foons, "ea");
      Assert.assertEquals("ea", corpus.getNameOfElem(ea));
      int tc = corpus.getTypeOfNamespace(foons, "tc");
      Assert.assertEquals("tc", corpus.getNameOfType(tc));

      Assert.assertEquals(tc, corpus.getTypeOfElem(ea));
      Assert.assertEquals(2, corpus.getAttrUseCountOfComplexType(tc));
      String a1 = corpus.getNameOfAttrUse(corpus.getAttrUseOfComplexType(tc, 0));
      String a2 = corpus.getNameOfAttrUse(corpus.getAttrUseOfComplexType(tc, 1));
      Assert.assertTrue("aa".equals(a1) && "ac".equals(a2) ||
                      "ac".equals(a1) && "aa".equals(a2));
    }
  }

  /**
   * A schema includes another that has different target namespace from
   * that of the including schema.
   */
  public void testIncludeSchemaUnmatchedNamespaces() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/includeUnmatchedNamespaces1_1.xsd",
        getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    // REVISIT: it should be an error instead of a warning.
    EXISchemaFactoryException[] sceList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, sceList.length);

    EXISchemaFactoryException sce = sceList[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertNotNull(se);
    Assert.assertEquals(3, sce.getLocator().getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("src-include.2.1:"));
  }
  
  /**
   * A schema imports another by specifying like this:
   * <import namespace="urn:none" schemaLocation="..."/>
   * However, the other schema actually has targetNamespace="urn:goo"
   */
  public void testImportSchemaUnmatchedNamespaces() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/importUnmatchedNamespaces1_1.xsd",
        getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException[] sceList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, sceList.length);

    EXISchemaFactoryException sce = sceList[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertNotNull(se);
    Assert.assertEquals(3, sce.getLocator().getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("src-import.3.1:"));
  }

  /**
   * foo.xsd uses a type in namespace urn:zoo without importing zoo.xsd.
   * foo.xsd imports goo.xsd which in turn imports zoo.xsd.
   * It should be handled as an error.
   */
  public void testIndirectSchemaImport() throws Exception {

    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/indirectImport/foo.xsd",
                                             getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException[] sceList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, sceList.length);
    XMLParseException spe = (XMLParseException)sceList[0].getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("src-resolve.4.2"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertTrue(foons != EXISchema.NIL_NODE);
    
    int fooTuple = corpus.getElemOfNamespace(foons, "MyTuple");
    Assert.assertTrue(fooTuple != EXISchema.NIL_NODE);
    
    int fooTupleGroup = corpus.getGroupOfElem(fooTuple);
    Assert.assertTrue(fooTupleGroup != EXISchema.NIL_NODE);
    
    Assert.assertEquals(2, corpus.getMemberSubstanceCountOfGroup(fooTupleGroup));
    int fooItem = corpus.getTermOfParticle(corpus.getMemberSubstanceOfGroup(fooTupleGroup, 0));
    Assert.assertEquals("MyItem", corpus.getNameOfElem(fooItem));
    Assert.assertEquals("urn:foo", corpus.getTargetNamespaceNameOfElem(fooItem));
    int zooItem = corpus.getTermOfParticle(corpus.getMemberSubstanceOfGroup(fooTupleGroup, 1));
    Assert.assertEquals("MyItem", corpus.getNameOfElem(zooItem));
    Assert.assertEquals("urn:zoo", corpus.getTargetNamespaceNameOfElem(zooItem));

    int goons = corpus.getNamespaceOfSchema("urn:goo");
    Assert.assertTrue(goons != EXISchema.NIL_NODE);

    int zoons = corpus.getNamespaceOfSchema("urn:zoo");
    Assert.assertTrue(zoons != EXISchema.NIL_NODE);
  }

  /**
   * Use of attribute group in the context of schema inclusion. 
   */
  public void testIncludeAttrGroup01() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/includeAttrGroup/foo.xsd",
                                               getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));

    int fooElem = corpus.getElemOfNamespace(foons, "Foo");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(fooElem));
    
    int fooA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(fooA));

    int fooB = corpus.getElemOfNamespace(foons, "B");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(fooB));
    
    int fooGroup = corpus.getGroupOfElem(fooElem);
    Assert.assertEquals(fooA, corpus.getTermOfParticle(corpus.getMemberSubstanceOfGroup(fooGroup, 0)));
    Assert.assertEquals(fooB, corpus.getTermOfParticle(corpus.getMemberSubstanceOfGroup(fooGroup, 1)));
    
    int fooTypeC = corpus.getTypeOfNamespace(foons, "C");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(fooTypeC));
    Assert.assertEquals(fooTypeC, corpus.getTypeOfElem(fooA));
    Assert.assertEquals(fooTypeC, corpus.getTypeOfElem(fooB));

    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(fooTypeC));
    int attrUse = corpus.getAttrUseOfComplexType(fooTypeC, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(attrUse));
    int dateAttr = corpus.getAttrOfAttrUse(attrUse);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(dateAttr));
    Assert.assertEquals("", corpus.getTargetNamespaceNameOfAttr(dateAttr));
    Assert.assertEquals("date", corpus.getNameOfAttr(dateAttr));
  }
  
  /**
   * Compiling a schema by its windows-like relative path.
   * The schema imports another in the same directory.
   * It is testing whether the imported schema is resolved successfully.
   */
  public void testWindowsRelativePath() throws Exception {
    EXISchemaFactory schemaCompiler = new EXISchemaFactory();
    schemaCompiler.setCompilerErrorHandler(m_compilerErrorHandler);

    String origCurDir = System.getProperty("user.dir");

    try {
      final String relativePath = "opengis\\openGis.xsd";
      
      URL url = getClass().getResource(("\\" + relativePath).replace('\\', '/'));
      String dirPath = new File(url.getFile()).getParentFile().getParent();

      System.setProperty("user.dir", dirPath);

      InputSource inputSource = new InputSource(relativePath);

      EXISchema corpus = schemaCompiler.compile(inputSource);

      Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
      
      int xsdns = corpus.getNamespaceOfSchema(XMLSCHEMA_URI);
      Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(xsdns));
    }
    finally {
      // restore "user.dir" system property
      System.setProperty("user.dir", origCurDir);
    }
  }

  /**
   * Element declaration needs a name.
   */
  public void testElementWithNoName() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/elementNoName.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    EXISchemaFactoryException[] sceList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, sceList.length);
    XMLParseException spe = (XMLParseException)sceList[0].getException();
    Assert.assertEquals(9, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-must-appear:"));
  }

  /**
   * Same name is used for two element declarations.
   */
  public void testElementSameName() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/elementSameName.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    EXISchemaFactoryException[] sceList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, sceList.length);
    XMLParseException spe = (XMLParseException)sceList[0].getException();
    Assert.assertEquals(9, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("sch-props-correct.2:"));
  }
  
  /**
   * Complex type declaration needs a name.
   */
  public void testComplexTypeWithNoName() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeNoNameComplex.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    EXISchemaFactoryException[] sceList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, sceList.length);
    XMLParseException spe = (XMLParseException)sceList[0].getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-must-appear:"));
  }

  /**
   * Complex content must have either <restriction> or <extension>. 
   */
  public void testComplexContentWithNoDefinition() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/complexContentNG01.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    EXISchemaFactoryException[] sceList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, sceList.length);
    XMLParseException spe = (XMLParseException)sceList[0].getException();
    Assert.assertEquals(6, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-invalid-content.2:"));
  }

  /**
   * Invalid element default value per length facet constraint. 
   */
  public void testElementDefaultLength_01() throws Exception {
    EXISchema corpus;
    int foons, eA;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/elementDefaultOK01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    foons = corpus.getNamespaceOfSchema("urn:foo");

    eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.CONSTRAINT_DEFAULT, corpus.getConstraintOfElem(eA));
    int default_eA = corpus.getConstraintValueOfElem(eA);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(default_eA));
    Assert.assertEquals("X", corpus.getStringValueOfVariant(default_eA));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/elementDefaultNG01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce.getCode());
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2:"));

    foons = corpus.getNamespaceOfSchema("urn:foo");

    eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE, corpus.getConstraintOfElem(eA));
  }
  
  /**
   * Invalid element default value per length facet constraint 
   * with the use of characters in SIP (Supplementary Ideographic Plane).
   */
  public void testElementDefaultLength_CharacterInSIP_01() throws Exception {
    EXISchema corpus;
    int foons, eA;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/elementDefaultOK02.xsd", getClass(), m_compilerErrorHandler);
    
    // REVISIT: surrogate pairs are counted as 2 characters each, not one.
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    XMLParseException spe = (XMLParseException)m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0].getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2:"));
    
    foons = corpus.getNamespaceOfSchema("urn:foo");

    eA = corpus.getElemOfNamespace(foons, "A");
    // REVISIT: it should really be CONSTRAINT_DEFAULT.
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE, corpus.getConstraintOfElem(eA));
    /**
     * Assert.assertEquals(EXISchema.CONSTRAINT_DEFAULT, corpus.getConstraintOfElem(eA));
     * // single character in SIP (U+2000B)
     * int default_eA = corpus.getConstraintValueOfElem(eA);
     * Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(default_eA));
     * Assert.assertEquals("\uD840\uDC0B", corpus.getStringValueOfVariant(default_eA));
     */

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/elementDefaultNG02.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce.getCode());
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2"));

    foons = corpus.getNamespaceOfSchema("urn:foo");

    eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE, corpus.getConstraintOfElem(eA));
  }
  
  /**
   * Invalid element fixed value per length facet constraint. 
   */
  public void testElementFixedLength_01() throws Exception {
    EXISchema corpus;
    int foons, eA;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/elementFixedOK01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    foons = corpus.getNamespaceOfSchema("urn:foo");

    eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.CONSTRAINT_FIXED, corpus.getConstraintOfElem(eA));
    int fixed_eA = corpus.getConstraintValueOfElem(eA);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(fixed_eA));
    Assert.assertEquals("X", corpus.getStringValueOfVariant(fixed_eA));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/elementFixedNG01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce.getCode());
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2:"));
    
    foons = corpus.getNamespaceOfSchema("urn:foo");

    eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE, corpus.getConstraintOfElem(eA));
  }
  
  /**
   * Invalid element fixed value per length facet constraint 
   * with the use of characters in SIP (Supplementary Ideographic Plane).
   */
  public void testElementFixedLength_CharacterInSIP_01() throws Exception {
    EXISchema corpus;
    int foons, eA;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/elementFixedOK02.xsd", getClass(), m_compilerErrorHandler);
    
    // REVISIT: surrogate pairs are counted as 2 characters each, not one.
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    XMLParseException spe = (XMLParseException)m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0].getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2:"));

    foons = corpus.getNamespaceOfSchema("urn:foo");

    eA = corpus.getElemOfNamespace(foons, "A");
    // REVISIT: it should really be CONSTRAINT_NONE.
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE, corpus.getConstraintOfElem(eA));
    /** 
     * Assert.assertEquals(EXISchema.CONSTRAINT_FIXED, corpus.getConstraintOfElem(eA));
     * // single character in SIP (U+2000B)
     * int fixed_eA = corpus.getConstraintValueOfElem(eA);
     * Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(fixed_eA));
     * Assert.assertEquals("\uD840\uDC0B", corpus.getStringValueOfVariant(fixed_eA));
     */

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/elementFixedNG02.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2:"));

    foons = corpus.getNamespaceOfSchema("urn:foo");

    eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE, corpus.getConstraintOfElem(eA));
  }
  
  /**
   * Element default value of type QName where the value is valid. 
   */
  public void testElementDefaultQNameValueOK01a() throws Exception {
    EXISchema corpus;
    int foons, eA;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/elementDefaultOK03a.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    foons = corpus.getNamespaceOfSchema("urn:foo");
    
    eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.CONSTRAINT_DEFAULT, corpus.getConstraintOfElem(eA));
    int default_eA = corpus.getConstraintValueOfElem(eA);
    Assert.assertEquals(EXISchema.VARIANT_QNAME, corpus.getTypeOfVariant(default_eA));
    int qname = corpus.getQNameValueOfVariant(default_eA);
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfQName(qname));
    Assert.assertEquals("test", corpus.getNameOfQName(qname));
  }
  
  /**
   * Element default value of type QName where the value is valid. 
   */
  public void testElementDefaultQNameValueOK01b() throws Exception {
    EXISchema corpus;
    int foons, eA;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/elementDefaultOK03b.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    foons = corpus.getNamespaceOfSchema("urn:foo");
    
    eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.CONSTRAINT_DEFAULT, corpus.getConstraintOfElem(eA));
    int default_eA = corpus.getConstraintValueOfElem(eA);
    Assert.assertEquals(EXISchema.VARIANT_QNAME, corpus.getTypeOfVariant(default_eA));
    int qname = corpus.getQNameValueOfVariant(default_eA);
    Assert.assertEquals("", corpus.getNamespaceNameOfQName(qname));
    Assert.assertEquals("test", corpus.getNameOfQName(qname));
  }
  
  /**
   * Element default value of type QName where the value is *not* valid. 
   */
  public void testElementDefaultQNameValueNG01() throws Exception {
    EXISchema corpus;
    int foons, eA;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/elementDefaultNG03.xsd", getClass(), m_compilerErrorHandler);

    EXISchemaFactoryException sce;
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    Assert.assertEquals(1, m_compilerErrorHandler.getErrorCount(EXISchemaFactoryException.SCHEMAPARSE_ERROR));
    sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(5, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2:"));
    
    foons = corpus.getNamespaceOfSchema("urn:foo");
    
    eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE, corpus.getConstraintOfElem(eA));
  }
  
  /**
   * Element fixed value of type QName where the value is valid. 
   */
  public void testElementFixedQNameValueOK01() throws Exception {
    EXISchema corpus;
    int foons, eA;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/elementFixedOK03.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    foons = corpus.getNamespaceOfSchema("urn:foo");
    
    eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.CONSTRAINT_FIXED, corpus.getConstraintOfElem(eA));
    int default_eA = corpus.getConstraintValueOfElem(eA);
    Assert.assertEquals(EXISchema.VARIANT_QNAME, corpus.getTypeOfVariant(default_eA));
    int qname = corpus.getQNameValueOfVariant(default_eA);
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfQName(qname));
    Assert.assertEquals("test", corpus.getNameOfQName(qname));
  }
  
  /**
   * Element fixed value of type QName where the value is *not* valid. 
   */
  public void testElementFixedQNameValueNG01() throws Exception {
    EXISchema corpus;
    int foons, eA;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/elementFixedNG03.xsd", getClass(), m_compilerErrorHandler);
    
    EXISchemaFactoryException sce;
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    Assert.assertEquals(1, m_compilerErrorHandler.getErrorCount(EXISchemaFactoryException.SCHEMAPARSE_ERROR));
    sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(5, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2:"));
    
    foons = corpus.getNamespaceOfSchema("urn:foo");
    
    eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.CONSTRAINT_NONE, corpus.getConstraintOfElem(eA));
  }
  
  /**
   * Same name is used for two complex type declarations.
   */
  public void testComplexTypeSameName() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeSameNameComplex.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("sch-props-correct.2:"));
  }

  /**
   * Simple type declaration needs a name.
   */
  public void testSimpleTypeWithNoName() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeNoNameSimple.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-must-appear:"));
  }

  /**
   * Same name is used for two simple type declarations.
   */
  public void testSimpleTypeSameName() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeSameNameSimple.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("sch-props-correct.2:"));
  }
  
  /**
   * Simple type facet "length" and "minLength" cannot occur together.
   */
  public void testSimpleTypeFacetCoOccurrenceNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetCoOccurrenceNG1.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("length-minLength-maxLength.2.2.a:"));
  }
  
  /**
   * Simple type facet "maxInclusive" and "maxExclusive" cannot occur together.
   */
  public void testSimpleTypeFacetCoOccurrenceNG02() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetCoOccurrenceNG2.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("maxInclusive-maxExclusive:"));
  }

  /**
   * Simple type facet "minInclusive" and "minExclusive" cannot occur together.
   */
  public void testSimpleTypeFacetCoOccurrenceNG03() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetCoOccurrenceNG3.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("minInclusive-minExclusive:"));
  }

  /**
   * Facet affinity error. "fractionDigits" does not apply to "float".
   */
  public void testSimpleTypeFacetAffinityNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetAffinityNG01.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-applicable-facets:"));
  }

  /**
   * Facet affinity error. "length" does not apply to "float".
   */
  public void testSimpleTypeFacetAffinityNG02() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetAffinityNG02.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-applicable-facets:"));
  }
  
  /**
   * Facet affinity error. "maxInclusive" does not apply to union simple type.
   */
  public void testSimpleTypeFacetAffinityNG03() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetAffinityNG03.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(12, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-applicable-facets:"));
  }

  /**
   * Facet affinity error. "minExclusive" does not apply to "string".
   */
  public void testSimpleTypeFacetAffinityNG04() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetAffinityNG04.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-applicable-facets:"));
  }

  /**
   * Facet validity error. Enumerated value "123456789" does not satisfy
   * length facet constraint of the base type.
   */
  public void testSimpleTypeFacetValidityNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetValidityNG01.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(14, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("enumeration-valid-restriction:"));
  }

  /**
   * Facet validity error. fractionDigits facet value cannot be greater
   * than totalDigits facet value of the base type.
   */
  public void testSimpleTypeFacetValidityNG02() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetValidityNG02.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(14, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("fractionDigits-totalDigits:"));
  }

  /**
   * Facet validity error.
   * length facet value must be same as that of the base type.
   */
  public void testSimpleTypeFacetValidityNG03() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetValidityNG03.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(14, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("length-valid-restriction:"));
  }

  /**
   * Facet validity error.
   * maxExclusive facet value must be a valid instance of the base type.
   */
  public void testSimpleTypeFacetValidityNG04() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetValidityNG04.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cvc-datatype-valid.1.2.1:"));
  }

  /**
   * Facet validity error.
   * maxExclusive facet value must be a valid instance of the base type.
   */
  public void testSimpleTypeFacetValidityNG05() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetValidityNG05.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cvc-datatype-valid.1.2.1:"));
  }

  /**
   * Facet validity error.
   * maxExclusive facet value must be a valid instance of the base type.
   */
  public void testSimpleTypeFacetValidityNG06() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetValidityNG06.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(14, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cvc-maxExclusive-valid:"));
  }

  /**
   * Facet validity error.
   * whiteSpace facet value "replace" cannot be used to restrict
   * base type that has whiteSpace facet value "collapse".
   */
  public void testSimpleTypeFacetValidityNG07() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetValidityNG07.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(14, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("whiteSpace-valid-restriction.1"));
  }

  /**
   * Facet validity error.
   * whiteSpace facet value "preserve" cannot be used to restrict
   * base type that has whiteSpace facet value "replace".
   */
  public void testSimpleTypeFacetValidityNG08() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetValidityNG08.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(14, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("whiteSpace-valid-restriction.2:"));
  }

  /**
   * Pattern Facet validity error.
   * "[A-N}*" is not a valid pattern.
   */
  public void testSimpleTypePatternFacetValidityNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typePatternFacetValidityNG01.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("InvalidRegex:"));
  }

  /**
   * Pattern Facet validity error.
   * "[!jf$%**3" is not a valid pattern.
   */
  public void testSimpleTypePatternFacetValidityNG02() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typePatternFacetValidityNG02.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("InvalidRegex:"));
  }
  
  /**
   * Simple type facet "length" does not permit a zero-length value.
   */
  public void testSimpleTypeFacetNoValueNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetNoValue1.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(9, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-invalid-value:"));
  }
  
  /**
   * Simple type facet "length" needs a value.
   */
  public void testSimpleTypeFacetNoValueNG02() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetNoValue2.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount()); 
  }

  /**
   * Multiple facets of the same kind are not permitted. 
   */
  public void testSimpleTypeFacetSameKindNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetSameKindNG01.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("src-single-facet-value:"));
  }

  /**
   * minLength facet value cannot exceed maxLength facet value.
   */
  public void testSimpleTypeFacetRelativityNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetRelativityNG01.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("minLength-less-than-equal-to-maxLength:"));
  }

  /**
   * fractionDigits facet value cannot exceed totalDigits facet value.
   */
  public void testSimpleTypeFacetRelativityNG02() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetRelativityNG02.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("fractionDigits-totalDigits:"));
  }

  /**
   * minExclusive facet value cannot exceed maxExclusive facet value.
   */
  public void testSimpleTypeFacetRelativityNG03() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetRelativityNG03.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("minExclusive-less-than-equal-to-maxExclusive:"));
  }
  
  /**
   * minInclusive facet value cannot exceed maxInclusive facet value.
   */
  public void testSimpleTypeFacetRelativityNG04() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetRelativityNG04.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("minInclusive-less-than-equal-to-maxInclusive:"));
  }

  /**
   * minExclusive facet value cannot be equal or greater than maxInclusive facet value.
   */
  public void testSimpleTypeFacetRelativityNG05() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetRelativityNG05.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("minExclusive-less-than-maxInclusive:"));
  }

  /**
   * minInclusive facet value cannot be equal or greater than maxExclusive facet value.
   */
  public void testSimpleTypeFacetRelativityNG06() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetRelativityNG06.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("minInclusive-less-than-maxExclusive:"));
  }
  
  /**
   * Complex type "restriction" needs a "base" attribute value.
   */
  public void testComplexTypeNoBaseNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeNoBaseComplex1.xsd",
                                           getClass(), m_compilerErrorHandler);

    // REVISIT: xerces reports two similar, essentially same errors.
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(2, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(9, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-invalid-value:"));
  }

  /**
   * Complex type "restriction" needs to have "base" attribute.
   */
  public void testComplexTypeNoBaseNG02() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeNoBaseComplex2.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(9, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-must-appear:"));
  }

  /**
   * Simple type "restriction" needs a "base" attribute value.
   */
  public void testSimpleTypeNoBaseNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeNoBaseSimple1.xsd",
                                           getClass(), m_compilerErrorHandler);

    // REVISIT: xerces reports two similar, essentially same errors.
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(2, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-invalid-value:"));
  }

  /**
   * Simple type "restriction" needs to have "base" attribute.
   */
  public void testSimpleTypeNoBaseNG02() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeNoBaseSimple2.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("src-simple-type.2.b:"));
  }
  
  /**
   * Global element declarations are not permitted to have "form" attribute.
   */
  public void testGlobalElementWithForm() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/elementFormNG1.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-not-allowed:"));
  }

  /**
   * Global element declarations are not permitted to have "minOccurs" attribute.
   */
  public void testGlobalElementWithMinOccurs() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/elementMinOccursNG1.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-not-allowed:"));
  }

  /**
   * Global element declarations are not permitted to have "maxOccurs" attribute.
   */
  public void testGlobalElementWithMaxOccurs() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/elementMaxOccursNG1.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-not-allowed:"));
  }

  /**
   * Global element declarations are not permitted to have "ref" attribute.
   */
  public void testGlobalElementWithRef() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/elementRefNG1.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-not-allowed:"));
  }
  
  /**
   * minOccurs must not be larger than maxOccurs.
   */
  public void testMinOccursLargerThanMaxOccurs() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/minOccursMaxOccursNG01.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("p-props-correct.2.1:"));
  }

  /**
   * minOccurs must be a non-negative integer.
   */
  public void testMinOccursNegative() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/minOccursMaxOccursNG02.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-invalid-value:"));
  }

  /**
   * maxOccurs must be a non-negative integer.
   */
  public void testMaxOccursNegative() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/minOccursMaxOccursNG03.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-invalid-value:"));
  }
  
  /**
   * Attribute use must have "name" or "ref" attribute. 
   */
  public void testAttributeUseWithoutNameOrRefNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/attributesNameOrRefNG01.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(12, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("src-attribute.3.1:"));
  }

  /**
   * Global attribute declaration must have "name" attribute. 
   */
  public void testAttributeWithoutNameNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/attributesNameNG01.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-must-appear:"));
  }

  /**
   * Same name is used for two attribute declarations. 
   */
  public void testAttributeSameNameNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/attributesSameName.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(9, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("sch-props-correct.2:"));
  }
  
  /**
   * The same attribute is used twice in a simple-content complex type
   * definition that is derived from another by extension. 
   */
  public void testAttributeSameUse01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/attributesSameUse01.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("ct-props-correct.4:"));
  }
  
  /**
   * The same global attribute is used twice in a simple-content complex type
   * definition that is derived from another by restriction.  
   */
  public void testAttributeSameUse02a() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/attributesSameUse02a.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(22, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("ct-props-correct.4:"));
  }
  
  /**
   * Two local attributes of the same name is used in a simple-content complex type
   * definition that is derived from another by restriction.  
   */
  public void testAttributeSameUse02b() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/attributesSameUse02b.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(20, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("ct-props-correct.4:"));
  }
  
  /**
   * The same global attribute is used twice in a complex type
   * definition that is derived implicitly from anyType by restriction.  
   */
  public void testAttributeSameUse03() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/attributesSameUse03.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("ct-props-correct.4:"));
  }
  
  /**
   * Currently "all" group with minOccurs value "0" results in
   * an error (IDS_SCHEMA_PARSE_ERROR_MODELGRP_ALL_MINOCCURS_ILLEGAL)
   * in XMLSchema processor, which is no longer the case as of 2004-11-18.
   */
  public void testAllGroupWithMinOccursZero() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/allGroupWithMinOccursZero.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
  }

  /**
   * "all" group can only participate in a particle of maxOccurs="1".
   * <xsd:all maxOccurs="2"> is not permitted.
   */
  public void testAllGroupMaxOccursNG01() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupMaxOccursNG01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(9, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-invalid-value:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int _all = corpus.getGroupOfElem(eA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_all));
    Assert.assertEquals(EXISchema.GROUP_ALL, corpus.getCompositorOfGroup(_all));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(_all));
    int pB = corpus.getParticleOfGroup(_all, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(pB)); // corrected to "1" from "2"
    int eB = corpus.getTermOfParticle(pB);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eB));
    Assert.assertEquals("B", corpus.getNameOfElem(eB));
    Assert.assertEquals("", corpus.getTargetNamespaceNameOfElem(eB));
  }

  /**
   * "all" group can only participate in a particle of maxOccurs="1".
   * <xsd:all maxOccurs="1"> is OK.
   */
  public void testAllGroupMaxOccursOK01() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupMaxOccursOK01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int _all = corpus.getGroupOfElem(eA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_all));
    Assert.assertEquals(EXISchema.GROUP_ALL, corpus.getCompositorOfGroup(_all));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(_all));
    int pB = corpus.getParticleOfGroup(_all, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(pB));
    int eB = corpus.getTermOfParticle(pB);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eB));
    Assert.assertEquals("B", corpus.getNameOfElem(eB));
    Assert.assertEquals("", corpus.getTargetNamespaceNameOfElem(eB));
  }

  /**
   * Particles in "all" group can have maxOccurs of value "0" or "1".
   */
  public void testAllGroupMaxOccursNG02() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupMaxOccursNG02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(9, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-all-limited.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int _all = corpus.getGroupOfElem(eA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_all));
    Assert.assertEquals(EXISchema.GROUP_ALL, corpus.getCompositorOfGroup(_all));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(_all));
    int pB = corpus.getParticleOfGroup(_all, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    // maxOccurs has been corrected to 1 by schema processor.
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(pB));
    int eB = corpus.getTermOfParticle(pB);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eB));
    Assert.assertEquals("B", corpus.getNameOfElem(eB));
    Assert.assertEquals("", corpus.getTargetNamespaceNameOfElem(eB));
  }

  /**
   * Particles in "all" group can have maxOccurs of value "0" or "1".
   */
  public void testAllGroupMaxOccursNG03() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupMaxOccursNG03.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-all-limited.1.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int _all = corpus.getGroupOfElem(eA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_all));
    Assert.assertEquals(EXISchema.GROUP_ALL, corpus.getCompositorOfGroup(_all));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(_all));
    int pB = corpus.getParticleOfGroup(_all, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(pB)); // corrected to "1" from "2"
    int eB = corpus.getTermOfParticle(pB);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eB));
    Assert.assertEquals("B", corpus.getNameOfElem(eB));
    Assert.assertEquals("", corpus.getTargetNamespaceNameOfElem(eB));
  }

  /**
   * The terms of member particles of an "all" group must be elements.
   * Groups and wildcards are not permitted.
   * <xsd:sequence> appears as child of <xsd:all>, which is an error.
   */
  public void testAllGroupMemberParticleNG01() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupMemberParticleNG01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int typeOfA = corpus.getTypeOfElem(eA);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeOfA));
    Assert.assertEquals(EXISchema.CONTENT_ELEMENT_ONLY, corpus.getContentClassOfComplexType(typeOfA));
    int _all = corpus.getParticleTermOfComplexType(typeOfA); 
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_all));
    Assert.assertEquals(EXISchema.GROUP_ALL, corpus.getCompositorOfGroup(_all));
    Assert.assertEquals(0, corpus.getParticleCountOfGroup(_all));
  }

  /**
   * The terms of member particles of an "all" group must be elements.
   * Groups and wildcards are not permitted.
   * <xsd:choice> appears as child of <xsd:all>, which is an error.
   */
  public void testAllGroupMemberParticleNG02() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupMemberParticleNG02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int typeOfA = corpus.getTypeOfElem(eA);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeOfA));
    Assert.assertEquals(EXISchema.CONTENT_ELEMENT_ONLY, corpus.getContentClassOfComplexType(typeOfA));
    int _all = corpus.getParticleTermOfComplexType(typeOfA); 
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_all));
    Assert.assertEquals(EXISchema.GROUP_ALL, corpus.getCompositorOfGroup(_all));
    Assert.assertEquals(0, corpus.getParticleCountOfGroup(_all));
  }

  /**
   * The terms of member particles of an "all" group must be elements.
   * Groups and wildcards are not permitted.
   * <xsd:all> appears as child of <xsd:all>, which is an error.
   */
  public void testAllGroupMemberParticleNG03() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupMemberParticleNG03.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int typeOfA = corpus.getTypeOfElem(eA);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeOfA));
    Assert.assertEquals(EXISchema.CONTENT_ELEMENT_ONLY, corpus.getContentClassOfComplexType(typeOfA));
    int _all = corpus.getParticleTermOfComplexType(typeOfA); 
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_all));
    Assert.assertEquals(EXISchema.GROUP_ALL, corpus.getCompositorOfGroup(_all));
    Assert.assertEquals(0, corpus.getParticleCountOfGroup(_all));
  }

  /**
   * The terms of member particles of an "all" group must be elements.
   * Groups and wildcards are not permitted.
   * <xsd:group> appears as child of <xsd:all>, which is an error.
   */
  public void testAllGroupMemberParticleNG04() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupMemberParticleNG04.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int typeOfA = corpus.getTypeOfElem(eA);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeOfA));
    Assert.assertEquals(EXISchema.CONTENT_ELEMENT_ONLY, corpus.getContentClassOfComplexType(typeOfA));
    int _all = corpus.getParticleTermOfComplexType(typeOfA); 
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_all));
    Assert.assertEquals(EXISchema.GROUP_ALL, corpus.getCompositorOfGroup(_all));
    Assert.assertEquals(0, corpus.getParticleCountOfGroup(_all));
  }

  /**
   * The terms of member particles of an "all" group must be elements.
   * Groups and wildcards are not permitted.
   * <xsd:any> appears as child of <xsd:all>, which is an error.
   */
  public void testAllGroupMemberParticleNG05() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupMemberParticleNG05.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int typeOfA = corpus.getTypeOfElem(eA);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeOfA));
    Assert.assertEquals(EXISchema.CONTENT_ELEMENT_ONLY, corpus.getContentClassOfComplexType(typeOfA));
    int _all = corpus.getParticleTermOfComplexType(typeOfA); 
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_all));
    Assert.assertEquals(EXISchema.GROUP_ALL, corpus.getCompositorOfGroup(_all));
    Assert.assertEquals(0, corpus.getParticleCountOfGroup(_all));
  }

  /**
   * The terms of member particles of an "all" group must be elements.
   * Groups and wildcards are not permitted.
   * <xsd:element> appears as child of <xsd:all>, which is OK.
   */
  public void testAllGroupMemberParticleOK01() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupMemberParticleOK01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int _all = corpus.getGroupOfElem(eA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_all));
    Assert.assertEquals(EXISchema.GROUP_ALL, corpus.getCompositorOfGroup(_all));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(_all));
    int pB = corpus.getParticleOfGroup(_all, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    int eB = corpus.getTermOfParticle(pB);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eB));
    Assert.assertEquals("B", corpus.getNameOfElem(eB));
    Assert.assertEquals("", corpus.getTargetNamespaceNameOfElem(eB));
  }

  /**
   * "all" group can only be used in a content model particle of a complex type.
   * <xsd:all> appears as child of <xsd:sequence>, which is an error.
   */
  public void testAllGroupChildOfGroupNG01() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupChildOfGroupNG01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int typeOfA = corpus.getTypeOfElem(eA);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeOfA));
    Assert.assertEquals(EXISchema.CONTENT_ELEMENT_ONLY, corpus.getContentClassOfComplexType(typeOfA));
    int _sequence = corpus.getParticleTermOfComplexType(typeOfA); 
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_sequence));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(_sequence));
    Assert.assertEquals(0, corpus.getParticleCountOfGroup(_sequence));
  }

  /**
   * "all" group can only be used in a content model particle of a complex type.
   * <xsd:all> appears as child of <xsd:choice>, which is an error.
   */
  public void testAllGroupChildOfGroupNG02() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupChildOfGroupNG02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int typeOfA = corpus.getTypeOfElem(eA);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeOfA));
    Assert.assertEquals(EXISchema.CONTENT_ELEMENT_ONLY, corpus.getContentClassOfComplexType(typeOfA));
    int pChoice = corpus.getContentTypeOfComplexType(typeOfA); 
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pChoice));
    int _choice = corpus.getTermOfParticle(pChoice);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_choice));
    Assert.assertEquals(EXISchema.GROUP_CHOICE, corpus.getCompositorOfGroup(_choice));
    Assert.assertEquals(0, corpus.getParticleCountOfGroup(_choice));
  }

  /**
   * "all" group can only be used in a content model particle of a complex type.
   * <xsd:all> appears as child of named <xsd:group>, then used from within
   * a sequence group, which is an error.
   */
  public void testAllGroupChildOfGroupNG03() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupChildOfGroupNG03.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-all-limited.1.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int typeOfA = corpus.getTypeOfElem(eA);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeOfA));
    Assert.assertEquals(EXISchema.CONTENT_ELEMENT_ONLY, corpus.getContentClassOfComplexType(typeOfA));
    int pSequence = corpus.getContentTypeOfComplexType(typeOfA); 
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pSequence));
    int _sequence = corpus.getTermOfParticle(pSequence);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_sequence));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(_sequence));
    Assert.assertEquals(0, corpus.getParticleCountOfGroup(_sequence));
  }

  /**
   * "all" group can only be used in a content model particle of a complex type.
   * <xsd:all> appears as child of named <xsd:group>, then used from within
   * a choice group, which is an error.
   */
  public void testAllGroupChildOfGroupNG04() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupChildOfGroupNG04.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-all-limited.1.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int typeOfA = corpus.getTypeOfElem(eA);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeOfA));
    Assert.assertEquals(EXISchema.CONTENT_ELEMENT_ONLY, corpus.getContentClassOfComplexType(typeOfA));
    int pChoice = corpus.getContentTypeOfComplexType(typeOfA); 
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pChoice));
    int _choice = corpus.getTermOfParticle(pChoice);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_choice));
    Assert.assertEquals(EXISchema.GROUP_CHOICE, corpus.getCompositorOfGroup(_choice));
    Assert.assertEquals(0, corpus.getParticleCountOfGroup(_choice));
  }

  /**
   * "all" group can only be used in a content model particle of a complex type.
   * <xsd:all> appears as child of named <xsd:group>, then used from within
   * a "all" group, which is an error.
   */
  public void testAllGroupChildOfGroupNG05() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupChildOfGroupNG05.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int typeOfA = corpus.getTypeOfElem(eA);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeOfA));
    Assert.assertEquals(EXISchema.CONTENT_ELEMENT_ONLY, corpus.getContentClassOfComplexType(typeOfA));
    int _all = corpus.getParticleTermOfComplexType(typeOfA); 
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_all));
    Assert.assertEquals(EXISchema.GROUP_ALL, corpus.getCompositorOfGroup(_all));
    Assert.assertEquals(0, corpus.getParticleCountOfGroup(_all));
  }

  /**
   * "all" group can only be used in a content model particle of a complex type.
   * <xsd:all> appears as child of named <xsd:group>, then used from within
   * complex type definition, which is OK.
   */
  public void testAllGroupChildOfGroupOK01() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/allGroupChildOfGroupOK01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    int _all = corpus.getGroupOfElem(eA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(_all));
    Assert.assertEquals(EXISchema.GROUP_ALL, corpus.getCompositorOfGroup(_all));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(_all));
    int pB = corpus.getParticleOfGroup(_all, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pB));
    int eB = corpus.getTermOfParticle(pB);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eB));
    Assert.assertEquals("B", corpus.getNameOfElem(eB));
    Assert.assertEquals("", corpus.getTargetNamespaceNameOfElem(eB));
  }

  /**
   * "abstract" attribute of an element declaration has to have a value of
   * either "true" or "false".
   */
  public void testElementAbstractNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/elementAbstractNG1.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-invalid-value:"));
  }

  /**
   * "nillable" attribute of an element declaration has to have a value of
   * either "true" or "false".
   */
  public void testElementNillableNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/elementNillableNG1.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-invalid-value:"));
  }

  /**
   * Element reference does not resolve.
   */
  public void testDanglingElementRef() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/elementRefNG2.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(9, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("src-resolve:"));
  }

  /**
   * Element of type ID.
   * <xsd:element name="B" type="xsd:ID"/>
   */
  public void testIdElement() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/idElement.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalErrorCount());
    /**
     * REVISIT: xerces does not report a warning for this issue.
     * Assert.assertEquals(1, m_compilerErrorHandler.getWarningCount());
     * 
     * EXISchemaFactoryException[] warningList =
     *     m_compilerErrorHandler.getWarnings(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
     * Assert.assertEquals(1, warningList.length);
     * 
     * SAXParseException spe = (SAXParseException)warningList[0].getException();
     * FjParseException fpe = (FjParseException)spe.getException();
     * Assert.assertEquals(XmlExceptionCode.IDS_SCHEMA_PARSE_ERROR_ELEMDECL_SIMPLE_IDTYPE_ILLEGAL, fpe.getCode());
     */
    Assert.assertEquals(0, m_compilerErrorHandler.getWarningCount());
  }
  
  /**
   * osgb.xsd includes osgbIncluded1.xsd and osgbIncluded2.xsd
   * both of which in turn imports gml.xsd.
   */
  public void testIncludeMultiple() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/includeMultiple/osgb.xsd", getClass());

    int gmlns = corpus.getNamespaceOfSchema("urn:gml");
    Assert.assertTrue(gmlns != EXISchema.NIL_NODE);
    int osgbns = corpus.getNamespaceOfSchema("urn:osgb");
    Assert.assertTrue(osgbns != EXISchema.NIL_NODE);
    Assert.assertEquals(2, corpus.getTypeCountOfNamespace(osgbns));

    // Inspect typeA
    int typeA = corpus.getTypeOfNamespace(osgbns, "typeA");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeA));
    Assert.assertEquals(EXISchema.CONTENT_SIMPLE, corpus.getContentClassOfComplexType(typeA));
    int simpleTypeOfA = corpus.getContentTypeOfComplexType(typeA);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(simpleTypeOfA));
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(typeA));
    int attruseOfA = corpus.getAttrUseOfComplexType(typeA, 0);
    Assert.assertFalse(corpus.isRequiredAttrUse(attruseOfA));
    int attrOfA = corpus.getAttrOfAttrUse(attruseOfA);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(attrOfA));
    
    // Inspect typeB
    int typeB = corpus.getTypeOfNamespace(osgbns, "typeB");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeB));
    Assert.assertEquals(EXISchema.CONTENT_SIMPLE, corpus.getContentClassOfComplexType(typeB));
    int simpleTypeOfB = corpus.getContentTypeOfComplexType(typeB);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(simpleTypeOfB));
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(typeB));
    int attruseOfB = corpus.getAttrUseOfComplexType(typeB, 0);
    Assert.assertFalse(corpus.isRequiredAttrUse(attruseOfB));
    int attrOfB = corpus.getAttrOfAttrUse(attruseOfB);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(attrOfB));

    // attribute "remoteSchema" in "urn:gml" are used both by "typeA" and "typeB"
    Assert.assertTrue(attruseOfA != attruseOfB);
    Assert.assertEquals(attrOfA, attrOfB);
    int remoteSchema = corpus.getAttrOfNamespace(gmlns, "remoteSchema");
    Assert.assertEquals("urn:gml", corpus.getTargetNamespaceNameOfAttr(attrOfA));
    Assert.assertEquals("remoteSchema", corpus.getNameOfAttr(attrOfA));
    Assert.assertEquals(remoteSchema, attrOfA);

    Assert.assertEquals(simpleTypeOfA, simpleTypeOfB);
    int abcdType = corpus.getTypeOfNamespace(gmlns, "abcdType");
    Assert.assertEquals("urn:gml", corpus.getTargetNamespaceNameOfAttr(attrOfA));
    Assert.assertEquals("abcdType", corpus.getNameOfType(abcdType));
    Assert.assertEquals(abcdType, simpleTypeOfA);
    
    // Inspect osgbElem
    int osgbElem = corpus.getElemOfNamespace(osgbns, "osgbElem");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(osgbElem));
    int groupOfOsgb = corpus.getGroupOfElem(osgbElem);
    Assert.assertEquals(2, corpus.getMemberSubstanceCountOfGroup(groupOfOsgb));
    int particleEA = corpus.getMemberSubstanceOfGroup(groupOfOsgb, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particleEA));
    int eA = corpus.getTermOfParticle(particleEA);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));
    Assert.assertEquals(typeA, corpus.getTypeOfElem(eA));
    int particleEB = corpus.getMemberSubstanceOfGroup(groupOfOsgb, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particleEB));
    int eB = corpus.getTermOfParticle(particleEB);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eB));
    Assert.assertEquals(typeB, corpus.getTypeOfElem(eB));
  }

  /**
   * Docbook 4.3 schema
   */
  public void testDocbook43Schema() throws Exception {

    EXISchema corpus = Docbook43Schema.getEXISchema();
    
    // sparse inspection for now
    
    int dbns = corpus.getNamespaceOfSchema("");
    
    // calstblx.xsd
    int _yesorno = corpus.getTypeOfNamespace(dbns, "yesorno");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_yesorno));

    // dbhierx.xsd
    int appendixClass = corpus.getElemOfNamespace(dbns, "appendix.class");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(appendixClass));

    int chapterClass = corpus.getElemOfNamespace(dbns, "chapter.class");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(chapterClass));
    int _chapterClass = corpus.getTypeOfElem(chapterClass);
    
    int chapter = corpus.getElemOfNamespace(dbns, "chapter");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(chapter));
    Assert.assertEquals(chapterClass, corpus.getSubstOfElem(chapter));
    int _chapter = corpus.getTypeOfElem(chapter);
    Assert.assertEquals(_chapterClass, _chapter);

    // dbnotnx.xsd
    int _notationClass = corpus.getTypeOfNamespace(dbns, "notation.class");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_notationClass));
    Assert.assertEquals(29, corpus.getEnumerationFacetCountOfSimpleType(_notationClass));

    // dbpoolx.xsd
    int ndxtermClass = corpus.getElemOfNamespace(dbns, "ndxterm.class");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(ndxtermClass));

    // htmltblx.xsd
    int colgroup = corpus.getElemOfNamespace(dbns, "colgroup");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(colgroup));

    int book = corpus.getElemOfNamespace(dbns, "book");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(book));
  }
  
  /**
   * REVISIT: This test case does not work!
   * 
   * Compile IPO schema (International Purchase Order) schema
   * excerpted from XML Schema Part 0 (Primer Second Edition).
   */
  public void testPrimerIPOSchemas() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/primerIPO.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ipons = corpus.getNamespaceOfSchema("http://www.example.com/IPO");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(ipons));
    
    int _address = corpus.getTypeOfNamespace(ipons, "Address");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(_address));
    int _usAddress = corpus.getTypeOfNamespace(ipons, "Address");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(_usAddress));
    // REVISIT: It does not work! FIX.
    // Assert.assertEquals(_address, corpus.getBaseTypeOfType(_usAddress));
    int _ukAddress = corpus.getTypeOfNamespace(ipons, "Address");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(_ukAddress));
    // REVISIT: It does not work! FIX.
    // Assert.assertEquals(_usAddress, corpus.getBaseTypeOfType(_ukAddress));
  }

  /**
   * Make sure that reference to model group definitions are resolved. 
   */
  public void testModelGroupDefinitionsOK01() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/modelGroupDefinitions.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int group, particle, elem;
    int typeB = corpus.getTypeOfNamespace(foons, "typeB");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeB));
    group = corpus.getParticleTermOfComplexType(typeB);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(group));
    Assert.assertEquals(2, corpus.getMemberSubstanceCountOfGroup(group));
    particle = corpus.getMemberSubstanceOfGroup(group, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));
    elem = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elem));
    Assert.assertEquals("A", corpus.getNameOfElem(elem));
    particle = corpus.getMemberSubstanceOfGroup(group, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));
    elem = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elem));
    Assert.assertEquals("B", corpus.getNameOfElem(elem));
  }

  /**
   * Named groups become a distinct node for each use. 
   */
  public void testModelGroupDefinitionsOK02() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/modelGroupMultiUse01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int particle;
    
    int typeA = corpus.getTypeOfNamespace(foons, "typeA");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeA));
    int contentA = corpus.getContentTypeOfComplexType(typeA);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(contentA));
    Assert.assertEquals(0, corpus.getMinOccursOfParticle(contentA));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(contentA));
    int groupA1 = corpus.getTermOfParticle(contentA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(groupA1));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(groupA1));
    particle = corpus.getParticleOfGroup(groupA1, 0);
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particle));
    int elemA1 = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elemA1));
    Assert.assertEquals("A", corpus.getNameOfElem(elemA1));
    particle = corpus.getParticleOfGroup(groupA1, 1);
    int groupB1 = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(groupB1));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(groupB1));
    particle = corpus.getParticleOfGroup(groupB1, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particle));
    int elemB1 = corpus.getTermOfParticle(particle) ;
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elemB1));
    Assert.assertEquals("B", corpus.getNameOfElem(elemB1));
    
    int typeB = corpus.getTypeOfNamespace(foons, "typeB");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeB));
    int contentB = corpus.getContentTypeOfComplexType(typeB);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(contentB));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(contentB));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(contentB));
    int group = corpus.getTermOfParticle(contentB);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(group));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(group));
    particle = corpus.getParticleOfGroup(group, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particle));
    int groupA2 = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(groupA2));
    Assert.assertTrue(groupA1 != groupA2);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(groupA2));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(groupA2));
    particle = corpus.getParticleOfGroup(groupA2, 0);
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particle));
    int elemA2 = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elemA2));
    Assert.assertEquals("A", corpus.getNameOfElem(elemA2));
    Assert.assertEquals(elemA1, elemA2);
    particle = corpus.getParticleOfGroup(groupA2, 1);
    int groupB2 = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(groupB2));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(groupB2));
    particle = corpus.getParticleOfGroup(groupB2, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particle));
    int elemB2 = corpus.getTermOfParticle(particle) ;
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elemB2));
    Assert.assertEquals("B", corpus.getNameOfElem(elemB2));
    Assert.assertEquals(elemB1, elemB2);
    particle = corpus.getParticleOfGroup(group, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));
    Assert.assertEquals(0, corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particle));
    int groupB3 = corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(groupB3));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(groupB3));
    Assert.assertTrue(groupB1 != groupB3 && groupB2 != groupB3);
    particle = corpus.getParticleOfGroup(groupB3, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(particle));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particle));
    int elemB3 = corpus.getTermOfParticle(particle) ;
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elemB3));
    Assert.assertEquals("B", corpus.getNameOfElem(elemB3));
    Assert.assertEquals(elemB2, elemB3);
  }

  /**
   * typeC derives from typeA by restriction.
   * Both typeA and typeC refers to an attribute group definition.
   * One of the attribute defined in the group has its own type definition.
   * 
   * Make sure the type does not get cloned, which results in being
   * invalidated since there's no derivation relationship between the
   * two cloned types. 
   */
  public void testAttrGroupDefinitionsOK01() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attrGroupDefinitions.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int atuse;
    
    int typeA = corpus.getTypeOfNamespace(foons, "typeA");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeA));
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(typeA));
    atuse = corpus.getAttrUseOfComplexType(typeA, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(atuse));
    int versionA = corpus.getAttrOfAttrUse(atuse);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(versionA));
    
    int typeC = corpus.getTypeOfNamespace(foons, "typeC");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeC));
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(typeC));
    atuse = corpus.getAttrUseOfComplexType(typeC, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(atuse));
    int versionC = corpus.getAttrOfAttrUse(atuse);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(versionC));

    Assert.assertEquals(versionA, versionC);
  }

  /**
   * CBMS (Convergence of Broadcast and Mobile Services) schema
   */
  public void testCBMSSchema() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/cbms/dvb_ipdc_esg.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
        
    int esgns = corpus.getNamespaceOfSchema("urn:dvb:ipdc:esg:2005");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(esgns));
    
    int tvans = corpus.getNamespaceOfSchema("urn:tva:metadata:2005");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(tvans));

    int mpeg7ns = corpus.getNamespaceOfSchema("urn:mpeg:mpeg7:schema:2001");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(mpeg7ns));

    // Inspect tvans:synopsisType
    int synopsisType = corpus.getTypeOfNamespace(tvans, "SynopsisType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(synopsisType));
    Assert.assertEquals(EXISchema.CONTENT_SIMPLE, corpus.getContentClassOfComplexType(synopsisType));
    Assert.assertEquals(corpus.getBuiltinTypeOfSchema(EXISchemaConst.STRING_TYPE),
        corpus.getContentTypeOfComplexType(synopsisType));
    int textualType = corpus.getTypeOfNamespace(mpeg7ns, "TextualType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(textualType));
    Assert.assertEquals(textualType, corpus.getBaseTypeOfType(synopsisType));

    // Inspect mpeg7ns:TitleType
    int titleType = corpus.getTypeOfNamespace(mpeg7ns, "TitleType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(titleType));
    Assert.assertEquals(EXISchema.CONTENT_SIMPLE, corpus.getContentClassOfComplexType(titleType));
    Assert.assertEquals(corpus.getBuiltinTypeOfSchema(EXISchemaConst.STRING_TYPE),
        corpus.getContentTypeOfComplexType(titleType));
    Assert.assertEquals(4, corpus.getAttrUseCountOfComplexType(titleType));
    int typeAttrOfTitleType = EXISchema.NIL_NODE;
    for (int i = 0; i < 4; i++) {
      int attrUse = corpus.getAttrUseOfComplexType(titleType, i);
      Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(attrUse));
      if ("type".equals(corpus.getNameOfAttrUse(attrUse))) {
        typeAttrOfTitleType = corpus.getAttrOfAttrUse(attrUse);
        Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(typeAttrOfTitleType));
        break;
      }
    }
    Assert.assertTrue(typeAttrOfTitleType != EXISchema.NIL_NODE);
    int unionOfUnion = corpus.getTypeOfAttr(typeAttrOfTitleType); 
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(unionOfUnion));
    Assert.assertEquals(3, corpus.getMemberTypesCountOfSimpleType(unionOfUnion));
    int m1 = corpus.getMemberTypeOfSimpleType(unionOfUnion, 0);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(m1));
    Assert.assertEquals(corpus.getBuiltinTypeOfSchema(EXISchemaConst.NMTOKEN_TYPE), corpus.getBaseTypeOfType(m1));
    Assert.assertEquals(10, corpus.getEnumerationFacetCountOfSimpleType(m1));
    int e1 = corpus.getEnumerationFacetOfSimpleType(m1, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e1));
    Assert.assertEquals("main", corpus.getStringValueOfVariant(e1));
    int e2 = corpus.getEnumerationFacetOfSimpleType(m1, 3);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e2));
    Assert.assertEquals("original", corpus.getStringValueOfVariant(e2));
    int e3 = corpus.getEnumerationFacetOfSimpleType(m1, 6);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e3));
    Assert.assertEquals("songTitle", corpus.getStringValueOfVariant(e3));
    int e4 = corpus.getEnumerationFacetOfSimpleType(m1, 9);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e4));
    Assert.assertEquals("episodeTitle", corpus.getStringValueOfVariant(e4));
    int m2 = corpus.getMemberTypeOfSimpleType(unionOfUnion, 1);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(m2));
    Assert.assertEquals(corpus.getBuiltinTypeOfSchema(EXISchemaConst.NMTOKEN_TYPE), corpus.getBaseTypeOfType(m2));
    int m3 = corpus.getMemberTypeOfSimpleType(unionOfUnion, 2);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(m3));
    Assert.assertEquals(corpus.getBuiltinTypeOfSchema(EXISchemaConst.ANYURI_TYPE), corpus.getBaseTypeOfType(m3));

    // Inspect mpeg7ns:TermDefinitionType
    // Note that TermDefinitionType recurse in its definition.
    int termDefinitionType = corpus.getTypeOfNamespace(mpeg7ns, "TermDefinitionType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(termDefinitionType));
    int groupOfTermDefinitionType = corpus.getParticleTermOfComplexType(termDefinitionType);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(groupOfTermDefinitionType));
    Assert.assertEquals(4, corpus.getMemberSubstanceCountOfGroup(groupOfTermDefinitionType));
    int p1a = corpus.getMemberSubstanceOfGroup(groupOfTermDefinitionType, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(p1a));
    int Header = corpus.getTermOfParticle(p1a);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(Header));
    Assert.assertEquals("Header", corpus.getNameOfElem(Header));
    int p2a = corpus.getMemberSubstanceOfGroup(groupOfTermDefinitionType, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(p2a));
    int Name = corpus.getTermOfParticle(p2a);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(Name));
    Assert.assertEquals("Name", corpus.getNameOfElem(Name));
    int p3a = corpus.getMemberSubstanceOfGroup(groupOfTermDefinitionType, 2);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(p3a));
    int Definition = corpus.getTermOfParticle(p3a);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(Definition));
    Assert.assertEquals("Definition", corpus.getNameOfElem(Definition));
    int p4a = corpus.getMemberSubstanceOfGroup(groupOfTermDefinitionType, 3);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(p4a));
    int Term = corpus.getTermOfParticle(p4a);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(Term));
    Assert.assertEquals("Term", corpus.getNameOfElem(Term));
    int typeOfTerm = corpus.getTypeOfElem(Term);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeOfTerm));
    Assert.assertEquals(termDefinitionType, corpus.getBaseTypeOfType(typeOfTerm));
    int groupOfTerm = corpus.getGroupOfElem(Term);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(groupOfTerm));
    Assert.assertEquals(4, corpus.getMemberSubstanceCountOfGroup(groupOfTerm));
    int p1b = corpus.getMemberSubstanceOfGroup(groupOfTerm, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(p1b));
    int p2b = corpus.getMemberSubstanceOfGroup(groupOfTerm, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(p2b));
    int p3b = corpus.getMemberSubstanceOfGroup(groupOfTerm, 2);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(p3b));
    int p4b = corpus.getMemberSubstanceOfGroup(groupOfTerm, 3);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(p4b));
    Assert.assertEquals(Term, corpus.getTermOfParticle(p4b));
  }

  /**
   * Web Services Addressing + SOAP envelope schemas
   */
  public void testSOAPAddressing() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/soap-addr-2005-08.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
        
    int adrns = corpus.getNamespaceOfSchema("http://www.w3.org/2005/08/addressing");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(adrns));

    int envns = corpus.getNamespaceOfSchema("http://www.w3.org/2003/05/soap-envelope");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(envns));
    
    // Inspect envns:Subcode
    int faultcodeType = corpus.getTypeOfNamespace(envns, "faultcode");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(faultcodeType));
    int groupOfFaultcodeType = corpus.getParticleTermOfComplexType(faultcodeType);
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(groupOfFaultcodeType));
    int pSubcode1 = corpus.getParticleOfGroup(groupOfFaultcodeType, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pSubcode1));
    int Subcode1 = corpus.getTermOfParticle(pSubcode1);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(Subcode1));
    Assert.assertEquals("Subcode", corpus.getNameOfElem(Subcode1));
    Assert.assertEquals(1, corpus.getGroupHeadInstanceCountOfElem(Subcode1));
    int subcodeType = corpus.getTypeOfElem(Subcode1);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(subcodeType));
    int groupOfSubcodeType = corpus.getParticleTermOfComplexType(subcodeType);
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(groupOfSubcodeType));
    int pValue = corpus.getParticleOfGroup(groupOfSubcodeType, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pValue));
    int Value = corpus.getTermOfParticle(pValue);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(Value));
    int pSubcode2 = corpus.getParticleOfGroup(groupOfSubcodeType, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pSubcode2));
    int Subcode2 = corpus.getTermOfParticle(pSubcode2);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(Subcode2));
    Assert.assertEquals("Subcode", corpus.getNameOfElem(Subcode2));
    Assert.assertEquals(subcodeType, corpus.getTypeOfElem(Subcode2));
    Assert.assertEquals(1, corpus.getGroupHeadInstanceCountOfElem(Subcode2));
    int headInstances;
    headInstances = corpus.getGroupHeadInstanceListOfElem(Subcode1);
    Assert.assertEquals(pValue, corpus.getNodes()[headInstances]);
    headInstances = corpus.getGroupHeadInstanceListOfElem(Subcode2);
    Assert.assertEquals(pValue, corpus.getNodes()[headInstances]);
  }

  /**
   * One of the Namespace-prefix bindings gets overridden within a schema.
   * This test case makes sure that redefined bindings take effect for
   * evaluating QName attribute values. 
   */
  public void testRedefinedPrefixOK() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/redefinedPrefix.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));

    int fooString = corpus.getTypeOfNamespace(foons, "string");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(fooString));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));

    int typeOfA = corpus.getTypeOfElem(eA);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(typeOfA));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, corpus.getSerialOfType(typeOfA));
    
    int eB = corpus.getElemOfNamespace(foons, "B");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eB));

    int typeOfB = corpus.getTypeOfElem(eB);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(typeOfB));
    Assert.assertEquals(fooString, typeOfB);
  }

  /**
   * GMTI (The NATO Ground Moving Target Indicator Format) Schema
   */
  public void testGMTISchema() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/exi/GMTI/gmti.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    /**
     * Sparse inspection for now.
     * Traverse from complex type "TargetReportType" to element "High-ResolutionLatitude"
     */
    int natons = corpus.getNamespaceOfSchema("http://www.gmti.mil/STANAG/4607/NATO");
    Assert.assertTrue(natons != EXISchema.NIL_NODE);
    
    int targetReportType = corpus.getTypeOfNamespace(natons, "TargetReportType");
    Assert.assertTrue(targetReportType != EXISchema.NIL_NODE);

    int seq1 = corpus.getParticleTermOfComplexType(targetReportType);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seq1));
    Assert.assertEquals(9, corpus.getParticleCountOfGroup(seq1));
    
    int pTargetLocation = corpus.getParticleOfGroup(seq1, 1);
    Assert.assertTrue(pTargetLocation != EXISchema.NIL_NODE);
    Assert.assertEquals(0, corpus.getMinOccursOfParticle(pTargetLocation));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(pTargetLocation));
    
    int targetLocation = corpus.getTermOfParticle(pTargetLocation);
    Assert.assertTrue(targetLocation != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(targetLocation));
    
    int seq2 = corpus.getGroupOfElem(targetLocation);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seq2));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(seq2));
    
    int pCho1 = corpus.getParticleOfGroup(seq2, 0);
    int cho1 = corpus.getTermOfParticle(pCho1);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(cho1));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(cho1));
    
    int pHighResolutionLatitude = corpus.getMemberSubstanceOfGroup(cho1, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, corpus.getNodeType(pHighResolutionLatitude));
    Assert.assertEquals(0, corpus.getMinOccursOfParticle(pHighResolutionLatitude));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(pHighResolutionLatitude));
    
    int highResolutionLatitude = corpus.getTermOfParticle(pHighResolutionLatitude);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(highResolutionLatitude));
    Assert.assertEquals("High-ResolutionLatitude", corpus.getNameOfElem(highResolutionLatitude));
  }
  
  /**
   * FPML 4.0 schema.
   */
  public void testFpmlSchema01() throws Exception {

    EXISchema m_corpus = EXISchemaFactoryTestUtil.getEXISchema("/fpml-4.0/fpml-main-4-0.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(m_corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    // sparse inspection for now
    int fpmlns = m_corpus.getNamespaceOfSchema("http://www.fpml.org/2003/FpML-4-0");
    Assert.assertEquals(4, m_corpus.getNamespaceCountOfSchema());

    int dsigns = m_corpus.getNamespaceOfSchema("http://www.w3.org/2000/09/xmldsig#");
    int Signature = m_corpus.getElemOfNamespace(dsigns, "Signature");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, m_corpus.getNodeType(Signature));
    
    int elem_FpML = m_corpus.getElemOfNamespace(fpmlns, "FpML");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, m_corpus.getNodeType(elem_FpML));
    int type_Document = m_corpus.getTypeOfNamespace(fpmlns, "Document");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, m_corpus.getNodeType(type_Document));
    Assert.assertEquals(type_Document, m_corpus.getTypeOfElem(elem_FpML));
    int Message = m_corpus.getTypeOfNamespace(fpmlns, "Message");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, m_corpus.getNodeType(Message));
    Assert.assertEquals(type_Document, m_corpus.getBaseTypeOfType(Message));

    int group, particle;
    
    int type_EquityPaymentDates = m_corpus.getTypeOfNamespace(fpmlns, "EquityPaymentDates");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, m_corpus.getNodeType(type_EquityPaymentDates));
    group = m_corpus.getParticleTermOfComplexType(type_EquityPaymentDates);
    Assert.assertEquals(EXISchema.GROUP_NODE, m_corpus.getNodeType(group));
    particle = m_corpus.getMemberSubstanceOfGroup(group, 0);
    Assert.assertEquals(0, m_corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, m_corpus.getMaxOccursOfParticle(particle));
    Assert.assertEquals(EXISchema.PARTICLE_NODE, m_corpus.getNodeType(particle));
    int elem_equityPaymentDatesInterim = m_corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, m_corpus.getNodeType(elem_equityPaymentDatesInterim));
    int type_AdjustableOrRelativeDates = m_corpus.getTypeOfNamespace(fpmlns, "AdjustableOrRelativeDates");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, m_corpus.getNodeType(type_AdjustableOrRelativeDates));
    Assert.assertEquals(type_AdjustableOrRelativeDates, m_corpus.getTypeOfElem(elem_equityPaymentDatesInterim));
    group = m_corpus.getGroupOfElem(elem_equityPaymentDatesInterim);
    Assert.assertEquals(EXISchema.GROUP_NODE, m_corpus.getNodeType(group));
    particle = m_corpus.getMemberSubstanceOfGroup(group, 0);
    int elem_adjustableDates = m_corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, m_corpus.getNodeType(elem_adjustableDates));
    Assert.assertEquals("adjustableDates", m_corpus.getNameOfElem(elem_adjustableDates));
    particle = m_corpus.getMemberSubstanceOfGroup(group, 1);
    int elem_relativeDates = m_corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, m_corpus.getNodeType(elem_relativeDates));
    Assert.assertEquals("relativeDates", m_corpus.getNameOfElem(elem_relativeDates));
    int type_RelativeDates = m_corpus.getTypeOfNamespace(fpmlns, "RelativeDates");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, m_corpus.getNodeType(type_RelativeDates));
    Assert.assertEquals(type_RelativeDates, m_corpus.getTypeOfElem(elem_relativeDates));
    int type_RelativeDateOffset = m_corpus.getTypeOfNamespace(fpmlns, "RelativeDateOffset");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, m_corpus.getNodeType(type_RelativeDateOffset));
    Assert.assertEquals(type_RelativeDateOffset, m_corpus.getBaseTypeOfType(type_RelativeDates));
    int type_Offset = m_corpus.getTypeOfNamespace(fpmlns, "Offset");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, m_corpus.getNodeType(type_Offset));
    Assert.assertEquals(type_Offset, m_corpus.getBaseTypeOfType(type_RelativeDateOffset));
    int type_Interval = m_corpus.getTypeOfNamespace(fpmlns, "Interval");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, m_corpus.getNodeType(type_Interval));
    Assert.assertEquals(type_Interval, m_corpus.getBaseTypeOfType(type_Offset));

    group = m_corpus.getGroupOfElem(elem_relativeDates);
    Assert.assertEquals(EXISchema.GROUP_NODE, m_corpus.getNodeType(group));
    Assert.assertEquals(9, m_corpus.getMemberSubstanceCountOfGroup(group));
    
    int elem;
    particle = m_corpus.getMemberSubstanceOfGroup(group, 0);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, m_corpus.getNodeType(particle));
    Assert.assertEquals(1, m_corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, m_corpus.getMaxOccursOfParticle(particle));
    elem = m_corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, m_corpus.getNodeType(elem));
    Assert.assertEquals("periodMultiplier", m_corpus.getNameOfElem(elem));
    particle = m_corpus.getMemberSubstanceOfGroup(group, 1);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, m_corpus.getNodeType(particle));
    Assert.assertEquals(1, m_corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, m_corpus.getMaxOccursOfParticle(particle));
    elem = m_corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, m_corpus.getNodeType(elem));
    Assert.assertEquals("period", m_corpus.getNameOfElem(elem));
    particle = m_corpus.getMemberSubstanceOfGroup(group, 2);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, m_corpus.getNodeType(particle));
    Assert.assertEquals(0, m_corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, m_corpus.getMaxOccursOfParticle(particle));
    elem = m_corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, m_corpus.getNodeType(elem));
    Assert.assertEquals("dayType", m_corpus.getNameOfElem(elem));
    particle = m_corpus.getMemberSubstanceOfGroup(group, 3);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, m_corpus.getNodeType(particle));
    Assert.assertEquals(1, m_corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, m_corpus.getMaxOccursOfParticle(particle));
    elem = m_corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, m_corpus.getNodeType(elem));
    Assert.assertEquals("businessDayConvention", m_corpus.getNameOfElem(elem));
    particle = m_corpus.getMemberSubstanceOfGroup(group, 4);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, m_corpus.getNodeType(particle));
    Assert.assertEquals(1, m_corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, m_corpus.getMaxOccursOfParticle(particle));
    elem = m_corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, m_corpus.getNodeType(elem));
    Assert.assertEquals("businessCentersReference", m_corpus.getNameOfElem(elem));
    particle = m_corpus.getMemberSubstanceOfGroup(group, 5);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, m_corpus.getNodeType(particle));
    Assert.assertEquals(1, m_corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, m_corpus.getMaxOccursOfParticle(particle));
    elem = m_corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, m_corpus.getNodeType(elem));
    Assert.assertEquals("businessCenters", m_corpus.getNameOfElem(elem));
    particle = m_corpus.getMemberSubstanceOfGroup(group, 6);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, m_corpus.getNodeType(particle));
    Assert.assertEquals(1, m_corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, m_corpus.getMaxOccursOfParticle(particle));
    elem = m_corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, m_corpus.getNodeType(elem));
    Assert.assertEquals("dateRelativeTo", m_corpus.getNameOfElem(elem));
    particle = m_corpus.getMemberSubstanceOfGroup(group, 7);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, m_corpus.getNodeType(particle));
    Assert.assertEquals(0, m_corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, m_corpus.getMaxOccursOfParticle(particle));
    elem = m_corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, m_corpus.getNodeType(elem));
    Assert.assertEquals("periodSkip", m_corpus.getNameOfElem(elem));
    particle = m_corpus.getMemberSubstanceOfGroup(group, 8);
    Assert.assertEquals(EXISchema.PARTICLE_NODE, m_corpus.getNodeType(particle));
    Assert.assertEquals(0, m_corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, m_corpus.getMaxOccursOfParticle(particle));
    elem = m_corpus.getTermOfParticle(particle);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, m_corpus.getNodeType(elem));
    Assert.assertEquals("scheduleBounds", m_corpus.getNameOfElem(elem));
  }

  /**
   * OPENGIS schema.
   */
  public void testOpenGisExample01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/opengis/openGis.xsd", getClass());

    int wfsns = corpus.getNamespaceOfSchema("http://www.opengis.net/wfs");
    Assert.assertTrue(wfsns != EXISchema.NIL_NODE);
    int featureCollection = corpus.getElemOfNamespace(wfsns, "FeatureCollection");
    Assert.assertTrue(featureCollection != EXISchema.NIL_NODE);

    int gmlns = corpus.getNamespaceOfSchema("http://www.opengis.net/gml");
    Assert.assertTrue(gmlns != EXISchema.NIL_NODE);
    int featureMember = corpus.getElemOfNamespace(gmlns, "featureMember");
    Assert.assertTrue(featureMember != EXISchema.NIL_NODE);
    int _Feature = corpus.getElemOfNamespace(gmlns, "_Feature");
    Assert.assertTrue(_Feature != EXISchema.NIL_NODE);

    int osgbns = corpus.getNamespaceOfSchema("http://www.ordnancesurvey.co.uk/xml/namespaces/osgb");
    Assert.assertTrue(osgbns != EXISchema.NIL_NODE);
    int TopographicPoint = corpus.getElemOfNamespace(osgbns, "TopographicPoint");
    Assert.assertTrue(TopographicPoint != EXISchema.NIL_NODE);
    int _TopographicFeature = corpus.getElemOfNamespace(osgbns, "_TopographicFeature");
    Assert.assertTrue(_TopographicFeature != EXISchema.NIL_NODE);
    Assert.assertEquals(_TopographicFeature, corpus.getSubstOfElem(TopographicPoint));
    Assert.assertEquals(_Feature, corpus.getSubstOfElem(_TopographicFeature));
  }
  
  /**
   * A schema with no target namespace can import another that has namespace name.
   * Similarly, a schema that has target namespace can import another that has
   * no target namespace.
   */
  public void testImportNoNamespace_01() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/importNoNamespace/noTargetNamespace.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int nons = corpus.getNamespaceOfSchema("");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(nons));

    int person = corpus.getElemOfNamespace(nons, "person");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(person));

    int name = corpus.getElemOfNamespace(nons, "name");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(name));

    int family = corpus.getElemOfNamespace(nons, "family");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(family));

    int given = corpus.getElemOfNamespace(nons, "given");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(given));

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));

    int fooPerson = corpus.getElemOfNamespace(foons, "person");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(fooPerson));
    Assert.assertTrue(fooPerson != person);

    int fooName = corpus.getElemOfNamespace(foons, "name");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(fooName));
    Assert.assertTrue(fooName != person);

    int fooFamily = corpus.getElemOfNamespace(foons, "family");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(fooFamily));
    Assert.assertTrue(fooFamily != person);

    int sequence, choice, choice1, choice2, seq1, seq2;
    
    // Inspect person
    sequence = corpus.getGroupOfElem(person);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(sequence));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(sequence));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(sequence));
    choice = corpus.getTermOfParticle(corpus.getParticleOfGroup(sequence, 0));
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(choice));
    Assert.assertEquals(EXISchema.GROUP_CHOICE, corpus.getCompositorOfGroup(choice));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(choice));
    choice1 = corpus.getParticleOfGroup(choice, 0);
    Assert.assertEquals(name, corpus.getTermOfParticle(choice1));
    choice2 = corpus.getParticleOfGroup(choice, 1);
    Assert.assertEquals(fooName, corpus.getTermOfParticle(choice2));

    // Inspect name
    sequence = corpus.getGroupOfElem(name);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(sequence));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(sequence));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(sequence));
    seq1 = corpus.getParticleOfGroup(sequence, 0);
    Assert.assertEquals(fooFamily, corpus.getTermOfParticle(seq1));
    seq2 = corpus.getParticleOfGroup(sequence, 1);
    Assert.assertEquals(given, corpus.getTermOfParticle(seq2));
    
    // Inspect foo:personnel
    int fooPersonnel = corpus.getElemOfNamespace(foons, "personnel");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(fooPersonnel));
    
    choice = corpus.getGroupOfElem(fooPersonnel);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(choice));
    Assert.assertEquals(EXISchema.GROUP_CHOICE, corpus.getCompositorOfGroup(choice));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(choice));
    choice1 = corpus.getParticleOfGroup(choice, 0);
    Assert.assertEquals(person, corpus.getTermOfParticle(choice1));
    choice2 = corpus.getParticleOfGroup(choice, 1);
    Assert.assertEquals(fooPerson, corpus.getTermOfParticle(choice2));

    // Inspect foo:person
    sequence = corpus.getGroupOfElem(fooPerson);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(sequence));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(sequence));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(sequence));
    Assert.assertEquals(fooName, corpus.getTermOfParticle(corpus.getParticleOfGroup(sequence, 0)));
    
    // Inspect foo:name
    sequence = corpus.getGroupOfElem(fooName);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(sequence));
    Assert.assertEquals(EXISchema.GROUP_SEQUENCE, corpus.getCompositorOfGroup(sequence));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(sequence));
    seq1 = corpus.getParticleOfGroup(sequence, 0);
    Assert.assertEquals(family, corpus.getTermOfParticle(seq1));
    seq2 = corpus.getParticleOfGroup(sequence, 1);
    Assert.assertEquals(given, corpus.getTermOfParticle(seq2));
  }

  /**
   * Unresolvable QName reference results in an exception
   * of IDS_VALIDATION_ERROR_PREFIX_NOT_DECLARED_IN_NAMESPACE
   */
  public void testUnsolvedElemQName_01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/unresolvedElemQName01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(11, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-invalid-value:"));
  }

  /**
   * UPA violation in choice group.
   */
  public void testUPA_Choice_NG01() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/choiceUPA_NG01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];

    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation in choice group.
   */
  public void testUPA_Choice_NG02() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/choiceUPA_NG02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];

    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation in choice group.
   */
  public void testUPA_Choice_NG03() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/choiceUPA_OK03.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/choiceUPA_NG03.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }
  
  /**
   * UPA violation in choice group.
   */
  public void testUPA_Choice_NG04() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/choiceUPA_OK04.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/choiceUPA_NG04.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation in choice group.
   */
  public void testUPA_Choice_NG05() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/choiceUPA_NG05.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation in choice group.
   */
  public void testUPA_Choice_OK06() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/upa/choiceUPA_OK06.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
  }

  /**
   * UPA violation in all group.
   */
  public void testUPA_All_NG01() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/allUPA_OK01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/allUPA_NG01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation in all group.
   */
  public void testUPA_All_NG02() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/allUPA_OK02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/allUPA_NG02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation in all group.
   */
  public void testUPA_All_NG03() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/allUPA_NG03.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation in sequence group.
   */
  public void testUPA_Sequence_01() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_OK01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_NG01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation in sequence group.
   */
  public void testUPA_Sequence_02() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_OK02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_NG02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }
  
  /**
   * UPA violation in sequence group.
   */
  public void testUPA_Sequence_03() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_OK03.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_NG03.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation in sequence group.
   */
  public void testUPA_Sequence_04() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_OK04.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_NG04.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation in sequence group.
   */
  public void testUPA_Sequence_05() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_OK05.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_NG05.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation in sequence group.
   */
  public void testUPA_Sequence_06() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_OK06.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_NG06.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }
  
  /**
   * UPA violation in sequence group.
   */
  public void testUPA_Sequence_07() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_OK07.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_NG07.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }
  
  /**
   * UPA violation in sequence group.
   */
  public void testUPA_Sequence_08() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_OK08.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/sequenceUPA_NG08.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }
  
  /**
   * UPA violation between two wildcards.
   * ##any and ##any
   */
  public void testUPA_Wildcard_01() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_OK01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_NG01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }
  
  /**
   * UPA violation between two wildcards.
   * ##any and namespaces
   */
  public void testUPA_Wildcard_02() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_OK02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_NG02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }
  
  /**
   * UPA violation between two wildcards.
   * namespaces and ##any
   */
  public void testUPA_Wildcard_03() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_OK03.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_NG03.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation between two wildcards.
   * ##other and ##other
   */
  public void testUPA_Wildcard_04() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_OK04.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_NG04.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation between two wildcards.
   * ##other and namespaces
   */
  public void testUPA_Wildcard_05() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_OK05.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_NG05.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation between two wildcards.
   * namespaces and ##other
   */
  public void testUPA_Wildcard_06() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_OK06.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_NG06.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation between two wildcards.
   * namespaces and namespaces
   */
  public void testUPA_Wildcard_07() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_OK07.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_NG07.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation an element and a wildcard.
   */
  public void testUPA_Wildcard_08() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_OK08.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_NG08.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }
  
  /**
   * UPA violation an element and a wildcard.
   */
  public void testUPA_Wildcard_09() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_OK09.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_NG09.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * UPA violation an element and a wildcard.
   */
  public void testUPA_Wildcard_10() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_OK10.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_NG10.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }
  
  /**
   * UPA violation an element and a wildcard.
   */
  public void testUPA_Wildcard_11() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_OK11.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/wildcardUPA_NG11.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * Derivation by restriction. (complex type with simple content) 
   * Attribute uses are preserved, but attribute wildcard is not preserved.
   */
  public void testAttributesDerivation_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int restrictedStringItemType = corpus.getTypeOfNamespace(foons, "restrictedStringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedStringItemType));

    // Attribute uses are preserved even by derivation-by-restriction.
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(restrictedStringItemType));
    int useOfId = corpus.getAttrUseOfComplexType(restrictedStringItemType, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfId));
    int id = corpus.getAttrOfAttrUse(useOfId);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
    Assert.assertEquals("id", corpus.getNameOfAttr(id));
    
    // Attribute wildcard is *not* preserved by derivation-by-restriction.
    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedStringItemType);
    Assert.assertEquals(EXISchema.NIL_NODE, attrwc);
  }
  
  /**
   * Derivation by restriction. (complex type with complex content) 
   * Attribute uses are preserved, but attribute wildcard is not preserved.
   */
  public void testAttributesDerivation_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int restrictedAnySequenceType = corpus.getTypeOfNamespace(foons, "restrictedAnySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedAnySequenceType));
    
    // Attribute uses are preserved even by derivation-by-restriction.
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(restrictedAnySequenceType));
    int useOfId = corpus.getAttrUseOfComplexType(restrictedAnySequenceType, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfId));
    int id = corpus.getAttrOfAttrUse(useOfId);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
    Assert.assertEquals("id", corpus.getNameOfAttr(id));
    
    // Attribute wildcard is *not* preserved by derivation-by-restriction.
    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedAnySequenceType);
    Assert.assertEquals(EXISchema.NIL_NODE, attrwc);
  }
  
  /**
   * Attribute wildcard satisfies that of the base type. (simpleContent)
   * base: namespace="##targetNamespace urn:goo" processContents="lax"
   * derived: namespace="urn:goo" processContents="lax"
   */
  public void testAttributesDerivation_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int stringItemType = corpus.getTypeOfNamespace(foons, "stringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(stringItemType));

    int basewc = corpus.getAttrWildcardOfComplexType(stringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_NAMESPACES, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(2, corpus.getNamespaceCountOfWildcard(basewc));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfWildcard(basewc, 0));
    Assert.assertEquals("urn:goo", corpus.getNamespaceNameOfWildcard(basewc, 1));
    
    int restrictedStringItemType = corpus.getTypeOfNamespace(foons, "restrictedStringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedStringItemType));

    // Attribute uses are preserved even by derivation-by-restriction.
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(restrictedStringItemType));
    int useOfId = corpus.getAttrUseOfComplexType(restrictedStringItemType, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfId));
    int id = corpus.getAttrOfAttrUse(useOfId);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
    Assert.assertEquals("id", corpus.getNameOfAttr(id));
    
    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedStringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_NAMESPACES, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(attrwc));
    Assert.assertEquals(1, corpus.getNamespaceCountOfWildcard(attrwc));
    Assert.assertEquals("urn:goo", corpus.getNamespaceNameOfWildcard(attrwc, 0));
  }
  
  /**
   * Attribute wildcard satisfies that of the base type. (complexContent)
   * base: namespace="##targetNamespace urn:goo" processContents="lax"
   * derived: namespace="urn:goo" processContents="lax"
   */
  public void testAttributesDerivation_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK04.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int AnySequenceType = corpus.getTypeOfNamespace(foons, "anySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(AnySequenceType));

    int basewc = corpus.getAttrWildcardOfComplexType(AnySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_NAMESPACES, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(2, corpus.getNamespaceCountOfWildcard(basewc));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfWildcard(basewc, 0));
    Assert.assertEquals("urn:goo", corpus.getNamespaceNameOfWildcard(basewc, 1));
    
    int restrictedAnySequenceType = corpus.getTypeOfNamespace(foons, "restrictedAnySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedAnySequenceType));

    // Attribute uses are preserved even by derivation-by-restriction.
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(restrictedAnySequenceType));
    int useOfId = corpus.getAttrUseOfComplexType(restrictedAnySequenceType, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfId));
    int id = corpus.getAttrOfAttrUse(useOfId);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
    Assert.assertEquals("id", corpus.getNameOfAttr(id));
    
    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedAnySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_NAMESPACES, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(attrwc));
    Assert.assertEquals(1, corpus.getNamespaceCountOfWildcard(attrwc));
    Assert.assertEquals("urn:goo", corpus.getNamespaceNameOfWildcard(attrwc, 0));
  }
  
  /**
   * attribute wildcard is *not* subset of that of the base type. (simpleContent by restriction)
   * base: namespace="urn:foo urn:goo" processContents="lax"
   * derived: namespace="urn:hoo" processContents="lax"
   */
  public void testAttributesDerivation_05() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationNG01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(20, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.4.2:"));
    
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int stringItemType = corpus.getTypeOfNamespace(foons, "stringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(stringItemType));

    int basewc = corpus.getAttrWildcardOfComplexType(stringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_NAMESPACES, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(2, corpus.getNamespaceCountOfWildcard(basewc));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfWildcard(basewc, 0));
    Assert.assertEquals("urn:goo", corpus.getNamespaceNameOfWildcard(basewc, 1));
    
    int restrictedStringItemType = corpus.getTypeOfNamespace(foons, "restrictedStringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedStringItemType));

    // Attribute uses are preserved even by derivation-by-restriction.
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(restrictedStringItemType));
    int useOfId = corpus.getAttrUseOfComplexType(restrictedStringItemType, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfId));
    int id = corpus.getAttrOfAttrUse(useOfId);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
    Assert.assertEquals("id", corpus.getNameOfAttr(id));
    
    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedStringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_SKIP, corpus.getProcessContentsOfWildcard(attrwc));
  }
  
  /**
   * attribute wildcard is *not* subset of that of the base type. (complexContent by restriction)
   * base: namespace="urn:foo urn:goo" processContents="lax"
   * derived: namespace="urn:hoo" processContents="lax"
   */
  public void testAttributesDerivation_06() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationNG02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(18, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.4.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int anySequenceType = corpus.getTypeOfNamespace(foons, "anySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(anySequenceType));

    int basewc = corpus.getAttrWildcardOfComplexType(anySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_NAMESPACES, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(2, corpus.getNamespaceCountOfWildcard(basewc));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfWildcard(basewc, 0));
    Assert.assertEquals("urn:goo", corpus.getNamespaceNameOfWildcard(basewc, 1));
    
    int restrictedAnySequenceType = corpus.getTypeOfNamespace(foons, "restrictedAnySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedAnySequenceType));

    // Attribute uses are preserved even by derivation-by-restriction.
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(restrictedAnySequenceType));
    int useOfId = corpus.getAttrUseOfComplexType(restrictedAnySequenceType, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfId));
    int id = corpus.getAttrOfAttrUse(useOfId);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
    Assert.assertEquals("id", corpus.getNameOfAttr(id));
    
    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedAnySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_SKIP, corpus.getProcessContentsOfWildcard(attrwc));
  }
  
  /**
   * attribute wildcard is *not* superset of that of the base type. (simpleContent by extension)
   * base: namespace="urn:hoo" processContents="lax"
   * derived: namespace="urn:foo urn:goo" processContents="lax"
   * "urn:hoo" is to be merged into "urn:foo urn:goo".
   */
  public void testAttributesDerivation_07() throws Exception {
    /*
     * base: namespace="urn:goo"
     * derived: namespace="urn:foo urn:goo"
     */
    EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK05.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    /*
     * base: namespace="urn:hoo"
     * derived: namespace="urn:foo urn:goo"
     * "urn:hoo" is to be merged into "urn:foo urn:goo".
     */
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationNG03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int stringItemType = corpus.getTypeOfNamespace(foons, "stringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(stringItemType));

    int basewc = corpus.getAttrWildcardOfComplexType(stringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_NAMESPACES, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(1, corpus.getNamespaceCountOfWildcard(basewc));
    Assert.assertEquals("urn:hoo", corpus.getNamespaceNameOfWildcard(basewc, 0));
    
    int extendedStringItemType = corpus.getTypeOfNamespace(foons, "extendedStringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(extendedStringItemType));

    // Attribute uses are preserved even by derivation-by-restriction.
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(extendedStringItemType));
    int useOfId = corpus.getAttrUseOfComplexType(extendedStringItemType, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfId));
    int id = corpus.getAttrOfAttrUse(useOfId);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
    Assert.assertEquals("id", corpus.getNameOfAttr(id));
    
    int attrwc = corpus.getAttrWildcardOfComplexType(extendedStringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_NAMESPACES, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(attrwc));
    Assert.assertEquals(3, corpus.getNamespaceCountOfWildcard(attrwc));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfWildcard(attrwc, 0));
    Assert.assertEquals("urn:goo", corpus.getNamespaceNameOfWildcard(attrwc, 1));
    Assert.assertEquals("urn:hoo", corpus.getNamespaceNameOfWildcard(attrwc, 2));
  }
  
  /**
   * attribute wildcard is *not* superset of that of the base type. (complexContent by extension)
   * base: namespace="urn:hoo" processContents="lax"
   * derived: namespace="urn:foo urn:goo" processContents="lax"
   * "urn:hoo" is to be merged into "urn:foo urn:goo".
   */
  public void testAttributesDerivation_08() throws Exception {
    /*
     * base: namespace="urn:goo"
     * derived: namespace="urn:foo urn:goo"
     */
    EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK06.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    /*
     * base: namespace="urn:hoo"
     * derived: namespace="urn:foo urn:goo"
     * "urn:hoo" is to be merged into "urn:foo urn:goo".
     */
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationNG04.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int anySequenceType = corpus.getTypeOfNamespace(foons, "anySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(anySequenceType));

    int basewc = corpus.getAttrWildcardOfComplexType(anySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_NAMESPACES, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(1, corpus.getNamespaceCountOfWildcard(basewc));
    Assert.assertEquals("urn:hoo", corpus.getNamespaceNameOfWildcard(basewc, 0));
    
    int extendedAnySequenceType = corpus.getTypeOfNamespace(foons, "extendedAnySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(extendedAnySequenceType));

    // Attribute uses are preserved even by derivation-by-restriction.
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(extendedAnySequenceType));
    int useOfId = corpus.getAttrUseOfComplexType(extendedAnySequenceType, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfId));
    int id = corpus.getAttrOfAttrUse(useOfId);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
    Assert.assertEquals("id", corpus.getNameOfAttr(id));
    
    int attrwc = corpus.getAttrWildcardOfComplexType(extendedAnySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_NAMESPACES, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(attrwc));
    Assert.assertEquals(3, corpus.getNamespaceCountOfWildcard(attrwc));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfWildcard(attrwc, 0));
    Assert.assertEquals("urn:goo", corpus.getNamespaceNameOfWildcard(attrwc, 1));
    Assert.assertEquals("urn:hoo", corpus.getNamespaceNameOfWildcard(attrwc, 2));
  }
  
  /**
   * Derivation by restriction. (complex type with simple content)
   * Attributes can be mapped to the attribute wildcard in the base type. 
   */
  public void testAttributesDerivation_09() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK07.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int stringItemType = corpus.getTypeOfNamespace(foons, "stringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(stringItemType));

    int basewc = corpus.getAttrWildcardOfComplexType(stringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_NOT, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(2, corpus.getNamespaceCountOfWildcard(basewc));
    Assert.assertEquals("", corpus.getNamespaceNameOfWildcard(basewc, 0));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfWildcard(basewc, 1));
    
    int restrictedStringItemType = corpus.getTypeOfNamespace(foons, "restrictedStringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedStringItemType));

    // Attribute uses are preserved even by derivation-by-restriction.
    Assert.assertEquals(2, corpus.getAttrUseCountOfComplexType(restrictedStringItemType));
    int useOfId = corpus.getAttrUseOfComplexType(restrictedStringItemType, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfId));
    int id = corpus.getAttrOfAttrUse(useOfId);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
    Assert.assertEquals("id", corpus.getNameOfAttr(id));
    int useOfLang = corpus.getAttrUseOfComplexType(restrictedStringItemType, 1);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfLang));
    int lang = corpus.getAttrOfAttrUse(useOfLang);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(lang));
    Assert.assertEquals("lang", corpus.getNameOfAttr(lang));
  }
  
  /**
   * Derivation by restriction. (complex type with simple content)
   * The attribute "lang" does not map into the attribute wildcard
   * in the base type.
   */
  public void testAttributesDerivation_10() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationNG05.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(20, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.2.2.b:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int stringItemType = corpus.getTypeOfNamespace(foons, "stringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(stringItemType));

    int basewc = corpus.getAttrWildcardOfComplexType(stringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_NOT, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(2, corpus.getNamespaceCountOfWildcard(basewc));
    Assert.assertEquals("", corpus.getNamespaceNameOfWildcard(basewc, 0));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfWildcard(basewc, 1));
    
    int restrictedStringItemType = corpus.getTypeOfNamespace(foons, "restrictedStringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedStringItemType));

    // Attribute uses are preserved even by derivation-by-restriction.
    Assert.assertEquals(2, corpus.getAttrUseCountOfComplexType(restrictedStringItemType));
    int useOfId = corpus.getAttrUseOfComplexType(restrictedStringItemType, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfId));
    int id = corpus.getAttrOfAttrUse(useOfId);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
    Assert.assertEquals("id", corpus.getNameOfAttr(id));
    int useOfLang = corpus.getAttrUseOfComplexType(restrictedStringItemType, 1);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfLang));
    int lang = corpus.getAttrOfAttrUse(useOfLang);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(lang));
    Assert.assertEquals("lang", corpus.getNameOfAttr(lang));
  }

  /**
   * Derivation by restriction. (complex type with complex content)
   * Attributes can be mapped to the attribute wildcard in the base type. 
   */
  public void testAttributesDerivation_11() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK08.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int AnySequenceType = corpus.getTypeOfNamespace(foons, "anySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(AnySequenceType));

    int basewc = corpus.getAttrWildcardOfComplexType(AnySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_NOT, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(2, corpus.getNamespaceCountOfWildcard(basewc));
    Assert.assertEquals("", corpus.getNamespaceNameOfWildcard(basewc, 0));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfWildcard(basewc, 1));
    
    int restrictedAnySequenceType = corpus.getTypeOfNamespace(foons, "restrictedAnySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedAnySequenceType));

    // Attribute uses are preserved even by derivation-by-restriction.
    Assert.assertEquals(2, corpus.getAttrUseCountOfComplexType(restrictedAnySequenceType));
    int useOfId = corpus.getAttrUseOfComplexType(restrictedAnySequenceType, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfId));
    int id = corpus.getAttrOfAttrUse(useOfId);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
    Assert.assertEquals("id", corpus.getNameOfAttr(id));
    int useOfLang = corpus.getAttrUseOfComplexType(restrictedAnySequenceType, 1);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfLang));
    int lang = corpus.getAttrOfAttrUse(useOfLang);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(lang));
    Assert.assertEquals("lang", corpus.getNameOfAttr(lang));
  }
  
  /**
   * Derivation by restriction. (complex type with complex content)
   * The attribute "lang" does not map into the attribute wildcard
   * in the base type.
   */
  public void testAttributesDerivation_12() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationNG06.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(18, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.2.2.b:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int AnySequenceType = corpus.getTypeOfNamespace(foons, "anySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(AnySequenceType));

    int basewc = corpus.getAttrWildcardOfComplexType(AnySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_NOT, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(2, corpus.getNamespaceCountOfWildcard(basewc));
    Assert.assertEquals("", corpus.getNamespaceNameOfWildcard(basewc, 0));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfWildcard(basewc, 1));
    
    int restrictedAnySequenceType = corpus.getTypeOfNamespace(foons, "restrictedAnySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedAnySequenceType));

    // Attribute uses are preserved even by derivation-by-restriction.
    Assert.assertEquals(2, corpus.getAttrUseCountOfComplexType(restrictedAnySequenceType));
    int useOfId = corpus.getAttrUseOfComplexType(restrictedAnySequenceType, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfId));
    int id = corpus.getAttrOfAttrUse(useOfId);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
    Assert.assertEquals("id", corpus.getNameOfAttr(id));
    int useOfLang = corpus.getAttrUseOfComplexType(restrictedAnySequenceType, 1);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfLang));
    int lang = corpus.getAttrOfAttrUse(useOfLang);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(lang));
    Assert.assertEquals("lang", corpus.getNameOfAttr(lang));
  }

  /**
   * attribute wildcard is *not* subset of that of the base type. (simpleContent by restriction)
   * Note, however, it is a subset according to the spec.
   * base: namespace="##other" processContents="lax"
   * derived: namespace="##local" processContents="lax"
   */
  public void testAttributesDerivation_13() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK09.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(20, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.4.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int stringItemType = corpus.getTypeOfNamespace(foons, "stringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(stringItemType));

    int basewc = corpus.getAttrWildcardOfComplexType(stringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_NOT, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(2, corpus.getNamespaceCountOfWildcard(basewc));
    Assert.assertEquals("", corpus.getNamespaceNameOfWildcard(basewc, 0));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfWildcard(basewc, 1));
    
    int restrictedStringItemType = corpus.getTypeOfNamespace(foons, "restrictedStringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedStringItemType));

    // Attribute uses are preserved even by derivation-by-restriction.
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(restrictedStringItemType));
    int useOfId = corpus.getAttrUseOfComplexType(restrictedStringItemType, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfId));
    int id = corpus.getAttrOfAttrUse(useOfId);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
    Assert.assertEquals("id", corpus.getNameOfAttr(id));
    
    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedStringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_SKIP, corpus.getProcessContentsOfWildcard(attrwc));
  }

  /**
   * attribute wildcard is not subset of that of the base type. (complexContent by restriction)
   * Note, however, it is a subset according to the spec.
   * base: namespace="##other" processContents="lax"
   * derived: namespace="##local" processContents="lax"
   */
  public void testAttributesDerivation_14() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK10.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(18, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.4.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int anySequenceType = corpus.getTypeOfNamespace(foons, "anySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(anySequenceType));

    int basewc = corpus.getAttrWildcardOfComplexType(anySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_NOT, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(2, corpus.getNamespaceCountOfWildcard(basewc));
    Assert.assertEquals("", corpus.getNamespaceNameOfWildcard(basewc, 0));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfWildcard(basewc, 1));
    
    int restrictedAnySequenceType = corpus.getTypeOfNamespace(foons, "restrictedAnySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedAnySequenceType));

    // Attribute uses are preserved even by derivation-by-restriction.
    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(restrictedAnySequenceType));
    int useOfId = corpus.getAttrUseOfComplexType(restrictedAnySequenceType, 0);
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useOfId));
    int id = corpus.getAttrOfAttrUse(useOfId);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(id));
    Assert.assertEquals("id", corpus.getNameOfAttr(id));
    
    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedAnySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_SKIP, corpus.getProcessContentsOfWildcard(attrwc));
  }

  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "strict" is stronger than "lax". 
   */
  public void testAttributesDerivation_15() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK11.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int anySequenceType = corpus.getTypeOfNamespace(foons, "anySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(anySequenceType));

    int basewc = corpus.getAttrWildcardOfComplexType(anySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(basewc));
    
    int restrictedAnySequenceType = corpus.getTypeOfNamespace(foons, "restrictedAnySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedAnySequenceType));

    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedAnySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_STRICT, corpus.getProcessContentsOfWildcard(attrwc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(attrwc));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "skip" is weaker than "lax". 
   */
  public void testAttributesDerivation_16() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationNG07.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(17, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.4.3:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int anySequenceType = corpus.getTypeOfNamespace(foons, "anySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(anySequenceType));

    int basewc = corpus.getAttrWildcardOfComplexType(anySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(basewc));
    
    int restrictedAnySequenceType = corpus.getTypeOfNamespace(foons, "restrictedAnySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedAnySequenceType));

    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedAnySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_SKIP, corpus.getProcessContentsOfWildcard(attrwc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(attrwc));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "lax" is not weaker than "lax". 
   */
  public void testAttributesDerivation_17() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK12.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int anySequenceType = corpus.getTypeOfNamespace(foons, "anySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(anySequenceType));

    int basewc = corpus.getAttrWildcardOfComplexType(anySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(basewc));
    
    int restrictedAnySequenceType = corpus.getTypeOfNamespace(foons, "restrictedAnySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedAnySequenceType));

    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedAnySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(attrwc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(attrwc));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "skip" is weaker than "lax". However, because the base
   * type is ur-type, this constraint is exonerated. 
   */
  public void testAttributesDerivation_18() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK13.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int anySequenceType = corpus.getTypeOfNamespace(foons, "anySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(anySequenceType));

    int basewc = corpus.getAttrWildcardOfComplexType(anySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_SKIP, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(basewc));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "lax" is stronger than "skip". 
   */
  public void testAttributesDerivation_19() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK14.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int anySequenceType = corpus.getTypeOfNamespace(foons, "anySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(anySequenceType));

    int basewc = corpus.getAttrWildcardOfComplexType(anySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_SKIP, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(basewc));
    
    int restrictedAnySequenceType = corpus.getTypeOfNamespace(foons, "restrictedAnySequenceType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedAnySequenceType));

    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedAnySequenceType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(attrwc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(attrwc));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "strict" is stronger than "lax". 
   */
  public void testAttributesDerivation_20() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK15.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int stringItemType = corpus.getTypeOfNamespace(foons, "stringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(stringItemType));

    int basewc = corpus.getAttrWildcardOfComplexType(stringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(basewc));
    
    int restrictedStringItemType = corpus.getTypeOfNamespace(foons, "restrictedStringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedStringItemType));

    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedStringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_STRICT, corpus.getProcessContentsOfWildcard(attrwc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(attrwc));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "skip" is weaker than "lax". 
   */
  public void testAttributesDerivation_21() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationNG08.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(18, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.4.3:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int stringItemType = corpus.getTypeOfNamespace(foons, "stringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(stringItemType));

    int basewc = corpus.getAttrWildcardOfComplexType(stringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(basewc));
    
    int restrictedStringItemType = corpus.getTypeOfNamespace(foons, "restrictedStringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedStringItemType));

    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedStringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_SKIP, corpus.getProcessContentsOfWildcard(attrwc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(attrwc));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "lax" is not weaker than "lax". 
   */
  public void testAttributesDerivation_22() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK16.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int stringItemType = corpus.getTypeOfNamespace(foons, "stringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(stringItemType));

    int basewc = corpus.getAttrWildcardOfComplexType(stringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(basewc));
    
    int restrictedStringItemType = corpus.getTypeOfNamespace(foons, "restrictedStringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedStringItemType));

    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedStringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(attrwc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(attrwc));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "lax" is stronger than "skip". 
   */
  public void testAttributesDerivation_23() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK17.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int stringItemType = corpus.getTypeOfNamespace(foons, "stringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(stringItemType));

    int basewc = corpus.getAttrWildcardOfComplexType(stringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(basewc));
    Assert.assertEquals(EXISchema.WC_PROCESS_SKIP, corpus.getProcessContentsOfWildcard(basewc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(basewc));
    
    int restrictedStringItemType = corpus.getTypeOfNamespace(foons, "restrictedStringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedStringItemType));

    int attrwc = corpus.getAttrWildcardOfComplexType(restrictedStringItemType);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(attrwc));
    Assert.assertEquals(0, corpus.getNamespaceCountOfWildcard(attrwc));
  }
  
  /**
   * Attributes that are *required* in the base type cannot be "prohibited"
   * in the derived type. 
   */
  public void testAttributesDerivation_24() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationOK18.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int foons, stringItemType, attruse, ref, restrictedStringItemType;
    
    foons = corpus.getNamespaceOfSchema("urn:foo");

    stringItemType = corpus.getTypeOfNamespace(foons, "stringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(stringItemType));

    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(stringItemType));
    attruse = corpus.getAttrUseOfComplexType(stringItemType, 0);
    Assert.assertTrue(corpus.isRequiredAttrUse(attruse));
    ref = corpus.getAttrOfAttrUse(attruse);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(ref));
    Assert.assertEquals("", corpus.getTargetNamespaceNameOfAttr(ref));
    Assert.assertEquals("ref", corpus.getNameOfAttr(ref));
    
    restrictedStringItemType = corpus.getTypeOfNamespace(foons, "restrictedStringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedStringItemType));

    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(restrictedStringItemType));
    attruse = corpus.getAttrUseOfComplexType(restrictedStringItemType, 0);
    Assert.assertTrue(corpus.isRequiredAttrUse(attruse));
    ref = corpus.getAttrOfAttrUse(attruse);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(ref));
    Assert.assertEquals("", corpus.getTargetNamespaceNameOfAttr(ref));
    Assert.assertEquals("ref", corpus.getNameOfAttr(ref));
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributesDerivationNG09.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(19, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.3:"));
    
    foons = corpus.getNamespaceOfSchema("urn:foo");

    stringItemType = corpus.getTypeOfNamespace(foons, "stringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(stringItemType));

    Assert.assertEquals(1, corpus.getAttrUseCountOfComplexType(stringItemType));
    attruse = corpus.getAttrUseOfComplexType(stringItemType, 0);
    Assert.assertTrue(corpus.isRequiredAttrUse(attruse));
    ref = corpus.getAttrOfAttrUse(attruse);
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, corpus.getNodeType(ref));
    Assert.assertEquals("", corpus.getTargetNamespaceNameOfAttr(ref));
    Assert.assertEquals("ref", corpus.getNameOfAttr(ref));
    
    restrictedStringItemType = corpus.getTypeOfNamespace(foons, "restrictedStringItemType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(restrictedStringItemType));

    Assert.assertEquals(0, corpus.getAttrUseCountOfComplexType(restrictedStringItemType));
  }
  
  /**
   * There is a particle in sequence B that does not map into sequence A.
   */
  public void testSequenceParticleRestriction_01() throws Exception {
    
    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionSequenceOK01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionSequenceNG01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-Recurse.2:"));

    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int typeA = corpus.getTypeOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeA));

    int seqA = corpus.getParticleTermOfComplexType(typeA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqA));
    Assert.assertEquals(3, corpus.getParticleCountOfGroup(seqA));
    
    int typeB = corpus.getTypeOfNamespace(foons, "B");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeB));
    
    int seqB = corpus.getParticleTermOfComplexType(typeB);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqB));
    Assert.assertEquals(4, corpus.getParticleCountOfGroup(seqB));
  }
  
  /**
   * There is a particle in sequence A that was not mapped to by
   * any particles in sequence B.
   */
  public void testSequenceParticleRestriction_02() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionSequenceOK02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionSequenceNG02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-Recurse.2:"));

    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int typeA = corpus.getTypeOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeA));

    int seqA = corpus.getParticleTermOfComplexType(typeA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqA));
    Assert.assertEquals(3, corpus.getParticleCountOfGroup(seqA));
    
    int typeB = corpus.getTypeOfNamespace(foons, "B");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeB));
    
    int seqB = corpus.getParticleTermOfComplexType(typeB);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqB));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(seqB));
  }

  /**
   * There is a particle in sequence A that was not mapped to by
   * any particles in sequence B. The unmapped particle locates
   * the last among the particles of the sequence A.
   */
  public void testSequenceParticleRestriction_03() throws Exception {
    
    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionSequenceOK03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionSequenceNG03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-Recurse.2:"));
    
    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int typeA = corpus.getTypeOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeA));

    int seqA = corpus.getParticleTermOfComplexType(typeA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqA));
    Assert.assertEquals(3, corpus.getParticleCountOfGroup(seqA));
    
    int typeB = corpus.getTypeOfNamespace(foons, "B");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeB));
    
    int seqB = corpus.getParticleTermOfComplexType(typeB);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqB));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(seqB));
  }

  /**
   * There is a particle in "all" group B that does not map into "all" group A.
   */
  public void testAllParticleRestriction_01() throws Exception {
    
    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAllOK01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAllNG01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-Recurse.2:"));

    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int typeA = corpus.getTypeOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeA));

    int seqA = corpus.getParticleTermOfComplexType(typeA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqA));
    Assert.assertEquals(3, corpus.getParticleCountOfGroup(seqA));
    
    int typeB = corpus.getTypeOfNamespace(foons, "B");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeB));
    
    int seqB = corpus.getParticleTermOfComplexType(typeB);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqB));
    Assert.assertEquals(4, corpus.getParticleCountOfGroup(seqB));
  }

  /**
   * There is a particle in "all" group A that was not mapped to by
   * any particles in "all" group B.
   */
  public void testAllParticleRestriction_02() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAllOK02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAllNG02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-Recurse.2:"));

    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int typeA = corpus.getTypeOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeA));

    int seqA = corpus.getParticleTermOfComplexType(typeA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqA));
    Assert.assertEquals(3, corpus.getParticleCountOfGroup(seqA));
    
    int typeB = corpus.getTypeOfNamespace(foons, "B");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeB));
    
    int seqB = corpus.getParticleTermOfComplexType(typeB);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqB));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(seqB));
  }

  /**
   * There is a particle in "all" group A that was not mapped to by
   * any particles in "all" group B. The unmapped particle locates
   * the last among the particles of the "all" group A.
   */
  public void testAllParticleRestriction_03() throws Exception {
    
    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAllOK03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAllNG03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-Recurse.2:"));

    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int typeA = corpus.getTypeOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeA));

    int seqA = corpus.getParticleTermOfComplexType(typeA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqA));
    Assert.assertEquals(3, corpus.getParticleCountOfGroup(seqA));
    
    int typeB = corpus.getTypeOfNamespace(foons, "B");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeB));
    
    int seqB = corpus.getParticleTermOfComplexType(typeB);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqB));
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(seqB));
  }

  /**
   * Constraint on processContent of two wildcards, one derived by
   * restriction from another. "skip" is not permitted because it is
   * weaker than "lax".
   */
  public void testAnyParticleRestriction_01() throws Exception {
    
    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAnyOK01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAnyNG01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-NSSubset.3:"));
    
    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int typeA = corpus.getTypeOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeA));

    int seqA = corpus.getParticleTermOfComplexType(typeA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqA));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(seqA));
    int p_anyA = corpus.getParticleOfGroup(seqA, 0);
    int anyA = corpus.getTermOfParticle(p_anyA);
    Assert.assertEquals(EXISchema.WILDCARD_NODE, corpus.getNodeType(anyA));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(anyA));
    
    int typeB = corpus.getTypeOfNamespace(foons, "B");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeB));
    
    int seqB = corpus.getParticleTermOfComplexType(typeB);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqB));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(seqB));
    int p_anyB = corpus.getParticleOfGroup(seqB, 0);
    int anyB = corpus.getTermOfParticle(p_anyB);
    Assert.assertEquals(EXISchema.WILDCARD_NODE, corpus.getNodeType(anyB));
    Assert.assertEquals(EXISchema.WC_PROCESS_SKIP, corpus.getProcessContentsOfWildcard(anyB));
  }

  /**
   * Constraint on processContent of two wildcards, one derived by
   * restriction from another. "lax" is not permitted because it is
   * weaker than "strict".
   */
  public void testAnyParticleRestriction_02() throws Exception {
    
    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAnyOK02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAnyNG02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-NSSubset.3:"));
    
    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int typeA = corpus.getTypeOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeA));

    int seqA = corpus.getParticleTermOfComplexType(typeA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqA));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(seqA));
    int p_anyA = corpus.getParticleOfGroup(seqA, 0);
    int anyA = corpus.getTermOfParticle(p_anyA);
    Assert.assertEquals(EXISchema.WILDCARD_NODE, corpus.getNodeType(anyA));
    Assert.assertEquals(EXISchema.WC_PROCESS_STRICT, corpus.getProcessContentsOfWildcard(anyA));
    
    int typeB = corpus.getTypeOfNamespace(foons, "B");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeB));
    
    int seqB = corpus.getParticleTermOfComplexType(typeB);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqB));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(seqB));
    int p_anyB = corpus.getParticleOfGroup(seqB, 0);
    int anyB = corpus.getTermOfParticle(p_anyB);
    Assert.assertEquals(EXISchema.WILDCARD_NODE, corpus.getNodeType(anyB));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(anyB));
  }
 
  /**
   * Use of XML Schema for schema.
   * Load a schema that uses "Schema for schema".
   * The import statement has a valid schemaLocation. 
   */
  public void testUseXMLSchema4Schema_02() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/useSchema4Schema.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(eA));

    int seqA = corpus.getGroupOfElem(eA);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(seqA));
    Assert.assertEquals(4, corpus.getParticleCountOfGroup(seqA));
    int p_element = corpus.getParticleOfGroup(seqA, 0);
    int element = corpus.getTermOfParticle(p_element);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(element));
    Assert.assertEquals("element", corpus.getNameOfElem(element));
    Assert.assertEquals(XMLSCHEMA_URI, corpus.getTargetNamespaceNameOfElem(element));
    int p_attribute = corpus.getParticleOfGroup(seqA, 1);
    int attribute = corpus.getTermOfParticle(p_attribute);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(attribute));
    Assert.assertEquals("attribute", corpus.getNameOfElem(attribute));
    Assert.assertEquals(XMLSCHEMA_URI, corpus.getTargetNamespaceNameOfElem(attribute));
    int p_simpleType = corpus.getParticleOfGroup(seqA, 2);
    int simpleType = corpus.getTermOfParticle(p_simpleType);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(simpleType));
    Assert.assertEquals("simpleType", corpus.getNameOfElem(simpleType));
    Assert.assertEquals(XMLSCHEMA_URI, corpus.getTargetNamespaceNameOfElem(simpleType));
    int p_complexType = corpus.getParticleOfGroup(seqA, 3);
    int complexType = corpus.getTermOfParticle(p_complexType);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(complexType));
    Assert.assertEquals("complexType", corpus.getNameOfElem(complexType));
    Assert.assertEquals(XMLSCHEMA_URI, corpus.getTargetNamespaceNameOfElem(complexType));
  }

  /**
   * SimpleContent cannot extend EMPTY content.
   * Use of these types in substitutionGroup relationship resulted in
   * an infinite loop. [soft-xwand2-dev:01675]
   */
  public void testSimpleContentExtensionNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/simpleContentExtensionNG01.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.SCHEMAPARSE_ERROR);
    Assert.assertEquals(2, errorList.length);

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(9, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("src-ct.2.1:"));
    
    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(24, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.4:"));
  }

  /**
   * Non-deterministic context of a particle.
   * Inner loop is stateful.
   */
  public void testAmbiguousParticleContext01() throws Exception {
    try {
      EXISchemaFactoryTestUtil.getEXISchema("/ambiguousParticleContext01.xsd",
                                             getClass(), m_compilerErrorHandler);
    }
    catch (EXISchemaFactoryException esfe) {
      GrammarRuntimeException gre = (GrammarRuntimeException)esfe.getException();
      XSParticle particle = (XSParticle)gre.getObject();
      Assert.assertEquals("Street4b", particle.getTerm().getName());
      return;
    }
    Assert.fail();
  }

  /**
   * Non-deterministic context of a particle.
   * Outer loop is stateful.
   */
  public void testAmbiguousParticleContext02() throws Exception {
    try {
      EXISchemaFactoryTestUtil.getEXISchema("/ambiguousParticleContext02.xsd",
                                             getClass(), m_compilerErrorHandler);
    }
    catch (EXISchemaFactoryException esfe) {
      GrammarRuntimeException gre = (GrammarRuntimeException)esfe.getException();
      XSParticle particle = (XSParticle)gre.getObject();
      Assert.assertEquals("Street5", particle.getTerm().getName());
      return;
    }
    Assert.fail();
  }

  /**
   * Tests integral types that have non-negative ranges.
   */
  public void testIntegralRangeNonNegative_01() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/decimalRange.xsd", getClass());

    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
    
    int nonNegativeInteger = corpus.getTypeOfNamespace(xsdns, "nonNegativeInteger");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeInteger));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeInteger));
    
    int unsignedLong = corpus.getTypeOfNamespace(xsdns, "unsignedLong");
    Assert.assertTrue(corpus.isIntegralSimpleType(unsignedLong));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(unsignedLong));

    int positiveInteger = corpus.getTypeOfNamespace(xsdns, "positiveInteger");
    Assert.assertTrue(corpus.isIntegralSimpleType(positiveInteger));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(positiveInteger));

    int unsignedInt = corpus.getTypeOfNamespace(xsdns, "unsignedInt");
    Assert.assertTrue(corpus.isIntegralSimpleType(unsignedInt));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(unsignedInt));

    int unsignedShort = corpus.getTypeOfNamespace(xsdns, "unsignedShort");
    Assert.assertTrue(corpus.isIntegralSimpleType(unsignedShort));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(unsignedShort));

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int nonNegativeInt_a = corpus.getTypeOfNamespace(foons, "nonNegativeInt_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeInt_a));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeInt_a));

    int nonNegativeInt_b = corpus.getTypeOfNamespace(foons, "nonNegativeInt_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeInt_b));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeInt_b));
    
    int nonNegativeLong_a = corpus.getTypeOfNamespace(foons, "nonNegativeLong_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeLong_a));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeLong_a));
    
    int nonNegativeLong_b = corpus.getTypeOfNamespace(foons, "nonNegativeLong_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeLong_b));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeLong_b));

    int nonNegativeLong_c = corpus.getTypeOfNamespace(foons, "nonNegativeLong_c");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeLong_c));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeLong_c));
    
    int nonNegativeInteger_a = corpus.getTypeOfNamespace(foons, "nonNegativeInteger_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeInteger_a));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeInteger_a));
    
    int nonNegativeInteger_b = corpus.getTypeOfNamespace(foons, "nonNegativeInteger_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeInteger_b));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeInteger_b));

    int nonNegativeInteger_c = corpus.getTypeOfNamespace(foons, "nonNegativeInteger_c");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeInteger_c));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeInteger_c));
  }

  /**
   * Tests integral types that have no significant ranges.
   */
  public void testIntegralRangeNone() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/decimalRange.xsd", getClass());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int noRangeInt_a = corpus.getTypeOfNamespace(foons, "noRangeInt_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(noRangeInt_a));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_DEFAULT, 
        corpus.getWidthOfIntegralSimpleType(noRangeInt_a));

    int noRangeInt_b = corpus.getTypeOfNamespace(foons, "noRangeInt_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(noRangeInt_b));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_DEFAULT, 
        corpus.getWidthOfIntegralSimpleType(noRangeInt_b));
    
    int noRangeLong_a = corpus.getTypeOfNamespace(foons, "noRangeLong_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(noRangeLong_a));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_DEFAULT, 
        corpus.getWidthOfIntegralSimpleType(noRangeLong_a));

    int noRangeLong_b = corpus.getTypeOfNamespace(foons, "noRangeLong_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(noRangeLong_b));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_DEFAULT, 
        corpus.getWidthOfIntegralSimpleType(noRangeLong_b));
    
    int noRangeInteger_a = corpus.getTypeOfNamespace(foons, "noRangeInteger_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(noRangeInteger_a));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_DEFAULT, 
        corpus.getWidthOfIntegralSimpleType(noRangeInteger_a));

    int noRangeInteger_b = corpus.getTypeOfNamespace(foons, "noRangeInteger_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(noRangeInteger_b));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_DEFAULT, 
        corpus.getWidthOfIntegralSimpleType(noRangeInteger_b));
  }

  /**
   * Tests integral types that ends up having n-bit ranges.
   */
  public void testIntegralRangeNbits() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/decimalRange.xsd", getClass());

    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

    int unsignedByte = corpus.getTypeOfNamespace(xsdns, "unsignedByte");
    Assert.assertTrue(corpus.isIntegralSimpleType(unsignedByte));
    Assert.assertEquals(8, corpus.getWidthOfIntegralSimpleType(unsignedByte));

    int _byte = corpus.getTypeOfNamespace(xsdns, "byte");
    Assert.assertTrue(corpus.isIntegralSimpleType(_byte));
    Assert.assertEquals(8, corpus.getWidthOfIntegralSimpleType(_byte));

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    int nbitsRangeInt_a = corpus.getTypeOfNamespace(foons, "nbitsRangeInt_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInt_a));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeInt_a));
    
    int nbitsRangeInt_b = corpus.getTypeOfNamespace(foons, "nbitsRangeInt_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInt_b));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeInt_b));
    
    int nbitsRangeInt_c = corpus.getTypeOfNamespace(foons, "nbitsRangeInt_c");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInt_c));
    Assert.assertEquals(11, corpus.getWidthOfIntegralSimpleType(nbitsRangeInt_c));
    
    int nbitsRangeInt_d = corpus.getTypeOfNamespace(foons, "nbitsRangeInt_d");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInt_d));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeInt_d));
    
    int nbitsRangeInt_e = corpus.getTypeOfNamespace(foons, "nbitsRangeInt_e");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInt_e));
    Assert.assertEquals(1, corpus.getWidthOfIntegralSimpleType(nbitsRangeInt_e));
    
    int nbitsRangeInt_f = corpus.getTypeOfNamespace(foons, "nbitsRangeInt_f");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInt_f));
    Assert.assertEquals(0, corpus.getWidthOfIntegralSimpleType(nbitsRangeInt_f));

    int nbitsRangeLong_a = corpus.getTypeOfNamespace(foons, "nbitsRangeLong_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeLong_a));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeLong_a));
    
    int nbitsRangeLong_b = corpus.getTypeOfNamespace(foons, "nbitsRangeLong_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeLong_b));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeLong_b));
    
    int nbitsRangeLong_c = corpus.getTypeOfNamespace(foons, "nbitsRangeLong_c");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeLong_c));
    Assert.assertEquals(11, corpus.getWidthOfIntegralSimpleType(nbitsRangeLong_c));
    
    int nbitsRangeLong_d = corpus.getTypeOfNamespace(foons, "nbitsRangeLong_d");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeLong_d));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeLong_d));
    
    int nbitsRangeLong_e = corpus.getTypeOfNamespace(foons, "nbitsRangeLong_e");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeLong_e));
    Assert.assertEquals(1, corpus.getWidthOfIntegralSimpleType(nbitsRangeLong_e));
    
    int nbitsRangeLong_f = corpus.getTypeOfNamespace(foons, "nbitsRangeLong_f");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeLong_f));
    Assert.assertEquals(0, corpus.getWidthOfIntegralSimpleType(nbitsRangeLong_f));
    
    int nbitsRangeLong_g = corpus.getTypeOfNamespace(foons, "nbitsRangeLong_g");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeLong_g));
    Assert.assertEquals(9, corpus.getWidthOfIntegralSimpleType(nbitsRangeLong_g));

    int nbitsRangeInteger_a = corpus.getTypeOfNamespace(foons, "nbitsRangeInteger_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInteger_a));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeInteger_a));
    
    int nbitsRangeInteger_b = corpus.getTypeOfNamespace(foons, "nbitsRangeInteger_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInteger_b));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeInteger_b));
    
    int nbitsRangeInteger_c = corpus.getTypeOfNamespace(foons, "nbitsRangeInteger_c");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInteger_c));
    Assert.assertEquals(11, corpus.getWidthOfIntegralSimpleType(nbitsRangeInteger_c));
    
    int nbitsRangeInteger_d = corpus.getTypeOfNamespace(foons, "nbitsRangeInteger_d");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInteger_d));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeInteger_d));
    
    int nbitsRangeInteger_e = corpus.getTypeOfNamespace(foons, "nbitsRangeInteger_e");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInteger_e));
    Assert.assertEquals(1, corpus.getWidthOfIntegralSimpleType(nbitsRangeInteger_e));
    
    int nbitsRangeInteger_f = corpus.getTypeOfNamespace(foons, "nbitsRangeInteger_f");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInteger_f));
    Assert.assertEquals(0, corpus.getWidthOfIntegralSimpleType(nbitsRangeInteger_f));
    
    int nbitsRangeInteger_g = corpus.getTypeOfNamespace(foons, "nbitsRangeInteger_g");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInteger_g));
    Assert.assertEquals(8, corpus.getWidthOfIntegralSimpleType(nbitsRangeInteger_g));
  }

  /**
   * Test INODE_ISSPECIFIC_IN_FRAGMENT_MASK flag of elements and attributes.
   */
  public void testFragment_01() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/fragment_01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
        
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int[] fragmentINodes = corpus.getFragmentINodes();
    Assert.assertEquals(10, fragmentINodes.length);
    Assert.assertEquals(7, corpus.getFragmentElemCount());

    int[] nodes = corpus.getNodes();
    String[] names = corpus.getNames();

    int fragmentINode;
    int ind = 0;

    fragmentINode = fragmentINodes[ind++];
    Assert.assertEquals(EXISchema.ELEMENT_NODE, nodes[fragmentINode]);
    Assert.assertEquals("A", EXISchema._getNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertEquals("urn:goo", EXISchema._getTargetNamespaceNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertTrue(EXISchema._isSpecificINodeInFragment(fragmentINode, nodes));

    fragmentINode = fragmentINodes[ind++];
    Assert.assertEquals(EXISchema.ELEMENT_NODE, nodes[fragmentINode]);
    Assert.assertEquals("A_", EXISchema._getNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertEquals("urn:foo", EXISchema._getTargetNamespaceNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertTrue(EXISchema._isSpecificINodeInFragment(fragmentINode, nodes));

    fragmentINode = fragmentINodes[ind++];
    Assert.assertEquals(EXISchema.ELEMENT_NODE, nodes[fragmentINode]);
    Assert.assertEquals("A_", EXISchema._getNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertEquals("urn:goo", EXISchema._getTargetNamespaceNameOfElem(fragmentINode, nodes, names)); 
    // Definitions of element "A" in "urn:goo" are the same, but they independently defines their types.
    Assert.assertFalse(EXISchema._isSpecificINodeInFragment(fragmentINode, nodes));
    
    fragmentINode = fragmentINodes[ind++];
    Assert.assertEquals(EXISchema.ELEMENT_NODE, nodes[fragmentINode]);
    Assert.assertEquals("A__", EXISchema._getNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertEquals("urn:goo", EXISchema._getTargetNamespaceNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertTrue(EXISchema._isSpecificINodeInFragment(fragmentINode, nodes));

    fragmentINode = fragmentINodes[ind++];
    Assert.assertEquals(EXISchema.ELEMENT_NODE, nodes[fragmentINode]);
    Assert.assertEquals("B", EXISchema._getNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertEquals("urn:foo", EXISchema._getTargetNamespaceNameOfElem(fragmentINode, nodes, names)); 
    // two "B" declarations use different nillable values.
    Assert.assertFalse(EXISchema._isSpecificINodeInFragment(fragmentINode, nodes));

    fragmentINode = fragmentINodes[ind++];
    Assert.assertEquals(EXISchema.ELEMENT_NODE, nodes[fragmentINode]);
    Assert.assertEquals("Z", EXISchema._getNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertEquals("urn:foo", EXISchema._getTargetNamespaceNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertFalse(EXISchema._isSpecificINodeInFragment(fragmentINode, nodes));

    fragmentINode = fragmentINodes[ind++];
    Assert.assertEquals(EXISchema.ELEMENT_NODE, nodes[fragmentINode]);
    Assert.assertEquals("Z", EXISchema._getNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertEquals("urn:goo", EXISchema._getTargetNamespaceNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertTrue(EXISchema._isSpecificINodeInFragment(fragmentINode, nodes));

    fragmentINode = fragmentINodes[ind++];
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, nodes[fragmentINode]);
    Assert.assertEquals("a", EXISchema._getNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertEquals("urn:goo", EXISchema._getTargetNamespaceNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertFalse(EXISchema._isSpecificINodeInFragment(fragmentINode, nodes));

    fragmentINode = fragmentINodes[ind++];
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, nodes[fragmentINode]);
    Assert.assertEquals("b", EXISchema._getNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertEquals("urn:foo", EXISchema._getTargetNamespaceNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertTrue(EXISchema._isSpecificINodeInFragment(fragmentINode, nodes));

    fragmentINode = fragmentINodes[ind++];
    Assert.assertEquals(EXISchema.ATTRIBUTE_NODE, nodes[fragmentINode]);
    Assert.assertEquals("c", EXISchema._getNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertEquals("urn:goo", EXISchema._getTargetNamespaceNameOfElem(fragmentINode, nodes, names)); 
    Assert.assertTrue(EXISchema._isSpecificINodeInFragment(fragmentINode, nodes));
  }
  
}
