package org.openexi.scomp;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import org.apache.xerces.xni.parser.XMLParseException;

import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.EXISchemaLayout;
import org.openexi.schema.EXISchemaUtil;

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
    m_stringBuilder = new StringBuilder();
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
  
  private final StringBuilder m_stringBuilder;

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
    Assert.assertNull(schemaCompiler.compile(is));

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(-1, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("schema_reference.4:"));
  }

  /**
   * Compilation succeeds even for empty list of schemas.
   */
  public void testCompileEmptyListOfSchemas() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(getClass(), m_compilerErrorHandler);
    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    Assert.assertEquals(4, corpus.uris.length);
    Assert.assertEquals("", corpus.uris[0]);
    Assert.assertEquals("http://www.w3.org/XML/1998/namespace", corpus.uris[1]);
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", corpus.uris[2]);
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", corpus.uris[3]);
  }

  /**
   * Test corpus methods
   */
  public void testCorpus() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/verySimple.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    // Check grand total number of elements and attributes
    Assert.assertEquals(19, EXISchemaUtil.countElemsOfSchema(corpus));
    // 3 in verySimple.xsd, 1 in verySimpleImported.xsd
    Assert.assertEquals(4, EXISchemaUtil.countAttrsOfSchema(corpus));
  }

  /**
   * Test schema methods
   */
  public void testSchema() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/verySimple.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    Assert.assertEquals(19, corpus.getGlobalElemCountOfSchema());
    // 1 in verySimple.xsd, 1 in verySimpleImported.xsd
    Assert.assertEquals(2, corpus.getGlobalAttrCountOfSchema());
    // 1 in verySimple.xsd, 2 in verySimpleImported.xsd,
    Assert.assertEquals(3 + EXISchemaConst.N_BUILTIN_TYPES, EXISchemaUtil.countTypesOfSchema(corpus, true));
    Assert.assertEquals(3 + (EXISchemaConst.N_BUILTIN_TYPES - 1), EXISchemaUtil.countLinkedSimpleTypesOfSchema(corpus));
    Assert.assertEquals(3 + (EXISchemaConst.N_BUILTIN_TYPES - 1), corpus.getTotalSimpleTypeCount()); 
    Assert.assertEquals(6, corpus.uris.length);
    Assert.assertEquals("", corpus.uris[0]);
    Assert.assertEquals("http://www.w3.org/XML/1998/namespace", corpus.uris[1]);
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", corpus.uris[2]);
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", corpus.uris[3]);
    Assert.assertEquals("urn:foo", corpus.uris[4]);
    Assert.assertEquals("urn:goo", corpus.uris[5]);
  }

  /**
   * Test XMLSchema-instance.xsd
   */
  public void testXMLSchemaInstanceSchema() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/verySimple.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    Assert.assertEquals(XMLSCHEMA_URI, corpus.uris[3]); 
    int booleanType = corpus.getTypeOfSchema(XMLSCHEMA_URI, "boolean"); 
    Assert.assertTrue(EXISchema.NIL_NODE != booleanType);
    int qnameType = corpus.getTypeOfSchema(XMLSCHEMA_URI, "QName"); 
    Assert.assertTrue(EXISchema.NIL_NODE != qnameType);
    int anyURIType = corpus.getTypeOfSchema(XMLSCHEMA_URI, "anyURI");
    Assert.assertTrue(EXISchema.NIL_NODE != anyURIType);

    Assert.assertEquals(XMLSCHEMA_INSTANCE_URI, corpus.uris[2]); 
    Assert.assertEquals(0, EXISchemaUtil.getTypeCountOfSchema(XMLSCHEMA_INSTANCE_URI, corpus));

    int a_nil = corpus.getGlobalElemOfSchema(XMLSCHEMA_INSTANCE_URI, "nil");
    Assert.assertEquals(EXISchema.NIL_NODE, a_nil);

    int a_type = corpus.getGlobalAttrOfSchema(XMLSCHEMA_INSTANCE_URI, "type");
    Assert.assertEquals(EXISchema.NIL_NODE, a_type);

    int a_schemaLocation = corpus.getGlobalAttrOfSchema(XMLSCHEMA_INSTANCE_URI, "schemaLocation");
    Assert.assertEquals(EXISchema.NIL_NODE, a_schemaLocation);

    int a_noNamespaceSchemaLocation = corpus.getGlobalAttrOfSchema(XMLSCHEMA_INSTANCE_URI, "noNamespaceSchemaLocation");
    Assert.assertEquals(EXISchema.NIL_NODE, a_noNamespaceSchemaLocation);
  }

  /**
   * Test schema methods against a schema for default namespace
   */
  public void testSchemaDefault() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/verySimpleDefault.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    Assert.assertEquals(4, corpus.getGlobalElemCountOfSchema());
    Assert.assertEquals(4, EXISchemaUtil.countElemsOfSchema(corpus));
    // 4 in XMLSchema-instance.xsd
    Assert.assertEquals(0, corpus.getGlobalAttrCountOfSchema());
    Assert.assertEquals(0, EXISchemaUtil.countAttrsOfSchema(corpus));
    Assert.assertEquals(EXISchemaConst.N_BUILTIN_TYPES, EXISchemaUtil.countTypesOfSchema(corpus, true)); 
    Assert.assertEquals(EXISchemaConst.N_BUILTIN_TYPES - 1, EXISchemaUtil.countLinkedSimpleTypesOfSchema(corpus)); 
    Assert.assertEquals(EXISchemaConst.N_BUILTIN_TYPES - 1, corpus.getTotalSimpleTypeCount()); 
    Assert.assertEquals(4, corpus.uris.length);
  }

  /**
   * Test namespace methods
   */
  public void testNamespace() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/verySimple.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    Assert.assertEquals("urn:foo", corpus.uris[4]);
    
    Assert.assertEquals(1, EXISchemaUtil.getTypeCountOfSchema("urn:foo", corpus));

    int elemc = corpus.getGlobalElemOfSchema("urn:foo", "C");
    Assert.assertTrue(elemc != EXISchema.NIL_NODE);
    Assert.assertEquals(4, corpus.getUriOfElem(elemc));
    Assert.assertEquals("C", corpus.getNameOfElem(elemc));

    int str10 = corpus.getTypeOfSchema("urn:foo", "string10");
    Assert.assertTrue(corpus.isSimpleType(str10));

    int id = corpus.getGlobalAttrOfSchema("urn:foo", "id");
    Assert.assertTrue(id != EXISchema.NIL_NODE);
    Assert.assertEquals(4, corpus.getUriOfAttr(id));
    Assert.assertEquals("id", corpus.localNames[4][corpus.getLocalNameOfAttr(id)]);
  }

  /**
   * Test element methods
   */
  public void testElement() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/verySimple.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    Assert.assertEquals(6, corpus.uris.length);
    Assert.assertEquals("urn:foo", corpus.uris[4]);
    Assert.assertEquals("urn:goo", corpus.uris[5]);
    
    int goo_b = corpus.getGlobalElemOfSchema("urn:goo", "b");
    Assert.assertEquals("b", corpus.getNameOfElem(goo_b));
    Assert.assertEquals("urn:goo", corpus.uris[corpus.getUriOfElem(goo_b)]);
  }

  /**
   * Test element's IsGlobalElem() method.
   */
  public void testIsGlobalElement() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexType.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    final int[] elems = corpus.getElems();
    
    final Map<String,Integer> elemSet = new HashMap<String,Integer>();
    for (int pos = 0; pos < elems.length; pos += EXISchemaLayout.SZ_ELEM) {
      elemSet.put(corpus.getNameOfElem(pos), pos);
    }
  
    final int _String = elemSet.get("String");
    Assert.assertTrue(corpus.isGlobalElem(_String));
  
    final int Street = elemSet.get("Street");
    Assert.assertTrue(!corpus.isGlobalAttr(Street));
  }

  /**
   * Test type methods against a simple type.
   */
  public void testTypeSimpleType() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/simpleType.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    Assert.assertEquals(5, corpus.uris.length);
    Assert.assertEquals(XMLSCHEMA_URI, corpus.uris[3]);
    Assert.assertEquals("urn:foo", corpus.uris[4]);
    
    int anySimpleType = corpus.getTypeOfSchema(XMLSCHEMA_URI, "anySimpleType");
    Assert.assertTrue(anySimpleType != EXISchema.NIL_NODE);
    
    int _string = corpus.getTypeOfSchema(XMLSCHEMA_URI, "string");
    Assert.assertTrue(_string != EXISchema.NIL_NODE);
        
    int str10type = corpus.getTypeOfSchema("urn:foo", "string10");
    Assert.assertEquals("string10", corpus.getNameOfType(str10type));
    Assert.assertEquals("urn:foo", EXISchemaUtil.getTargetNamespaceNameOfType(str10type, corpus));
    Assert.assertEquals(_string, corpus.getBaseTypeOfSimpleType(str10type));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(str10type));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(str10type));
    Assert.assertEquals(EXISchema.WHITESPACE_PRESERVE,
                      corpus.getWhitespaceFacetValueOfStringSimpleType(str10type));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, corpus.ancestryIds[corpus.getSerialOfType(str10type)]);

    int str16type = corpus.getTypeOfSchema("urn:foo", "string16");
    Assert.assertEquals(_string, corpus.getBaseTypeOfSimpleType(str16type));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, corpus.ancestryIds[corpus.getSerialOfType(str16type)]);

    int str3_10type = corpus.getTypeOfSchema("urn:foo", "string3_10");
    int _str10type = corpus.getBaseTypeOfSimpleType(str3_10type);
    Assert.assertEquals(str10type, _str10type);
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, corpus.ancestryIds[corpus.getSerialOfType(str3_10type)]);

    int str16list = corpus.getTypeOfSchema("urn:foo", "strings16");
    Assert.assertEquals(anySimpleType, corpus.getBaseTypeOfSimpleType(str16list));
    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(str16list));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(str16list));
    Assert.assertEquals(str16type, corpus.getItemTypeOfListSimpleType(str16list));

    int length10 = corpus.getTypeOfSchema("urn:foo", "length10");
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfStringSimpleType(length10));

    int pos;
    int[] types = corpus.getTypes();

    // pattern "(\d|\s)*"
    int digits10 = corpus.getTypeOfSchema("urn:foo", "digits10");
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfStringSimpleType(digits10));

    // pattern "[0-5\\s]*"
    int strictDigits8 = corpus.getTypeOfSchema("urn:foo", "strictDigits8");
    Assert.assertEquals(10, corpus.getRestrictedCharacterCountOfStringSimpleType(strictDigits8));
    pos = corpus.getRestrictedCharacterOfSimpleType(strictDigits8);
    Assert.assertEquals('\t', types[pos + 0]);
    Assert.assertEquals('\n', types[pos + 1]);
    Assert.assertEquals('\r', types[pos + 2]);
    Assert.assertEquals(' ', types[pos + 3]);
    Assert.assertEquals('0', types[pos + 4]);
    Assert.assertEquals('1', types[pos + 5]);
    Assert.assertEquals('2', types[pos + 6]);
    Assert.assertEquals('3', types[pos + 7]);
    Assert.assertEquals('4', types[pos + 8]);
    Assert.assertEquals('5', types[pos + 9]);

    // pattern "[01]*|[34]*"
    int strictDigits6 = corpus.getTypeOfSchema("urn:foo", "strictDigits6");
    Assert.assertEquals(4, corpus.getRestrictedCharacterCountOfStringSimpleType(strictDigits6));
    pos = corpus.getRestrictedCharacterOfSimpleType(strictDigits6);
    Assert.assertEquals('0', types[pos + 0]);
    Assert.assertEquals('1', types[pos + 1]);
    Assert.assertEquals('3', types[pos + 2]);
    Assert.assertEquals('4', types[pos + 3]);

    // pattern "[03]*|[14]*"
    int strictDigits4 = corpus.getTypeOfSchema("urn:foo", "strictDigits4");
    Assert.assertEquals(4, corpus.getRestrictedCharacterCountOfStringSimpleType(strictDigits4));
    pos = corpus.getRestrictedCharacterOfSimpleType(strictDigits4);
    Assert.assertEquals('0', types[pos + 0]);
    Assert.assertEquals('1', types[pos + 1]);
    Assert.assertEquals('3', types[pos + 2]);
    Assert.assertEquals('4', types[pos + 3]);

    // pattern "[04]*|[13]*"
    int strictDigits2 = corpus.getTypeOfSchema("urn:foo", "strictDigits2");
    Assert.assertEquals(4, corpus.getRestrictedCharacterCountOfStringSimpleType(strictDigits2));
    pos = corpus.getRestrictedCharacterOfSimpleType(strictDigits2);
    Assert.assertEquals('0', types[pos + 0]);
    Assert.assertEquals('1', types[pos + 1]);
    Assert.assertEquals('3', types[pos + 2]);
    Assert.assertEquals('4', types[pos + 3]);

    // pattern "[03]*|[14]*"
    int strictDigits3 = corpus.getTypeOfSchema("urn:foo", "strictDigits3");
    Assert.assertEquals(4, corpus.getRestrictedCharacterCountOfStringSimpleType(strictDigits3));
    pos = corpus.getRestrictedCharacterOfSimpleType(strictDigits3);
    Assert.assertEquals('0', types[pos + 0]);
    Assert.assertEquals('1', types[pos + 1]);
    Assert.assertEquals('3', types[pos + 2]);
    Assert.assertEquals('4', types[pos + 3]);

    // pattern "[03]*|[14]*"
    int strictDigits1 = corpus.getTypeOfSchema("urn:foo", "strictDigits1");
    Assert.assertEquals(4, corpus.getRestrictedCharacterCountOfStringSimpleType(strictDigits1));
    pos = corpus.getRestrictedCharacterOfSimpleType(strictDigits1);
    Assert.assertEquals('0', types[pos + 0]);
    Assert.assertEquals('1', types[pos + 1]);
    Assert.assertEquals('3', types[pos + 2]);
    Assert.assertEquals('4', types[pos + 3]);
    
    int preservedString = corpus.getTypeOfSchema("urn:foo", "preservedString");
    Assert.assertEquals(EXISchema.WHITESPACE_PRESERVE,
        corpus.getWhitespaceFacetValueOfStringSimpleType(preservedString));
    int e1 = corpus.getEnumerationFacetOfAtomicSimpleType(preservedString, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e1));
    Assert.assertEquals("\n\r\t ", corpus.getStringValueOfVariant(e1));

    int replaceString = corpus.getTypeOfSchema("urn:foo", "replaceString");
    Assert.assertEquals(EXISchema.WHITESPACE_REPLACE,
                      corpus.getWhitespaceFacetValueOfStringSimpleType(replaceString));

    int replaceString2 = corpus.getTypeOfSchema("urn:foo", "replaceString2");
    Assert.assertEquals(EXISchema.WHITESPACE_REPLACE,
                      corpus.getWhitespaceFacetValueOfStringSimpleType(replaceString2));
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfAtomicSimpleType(replaceString2));
    e1 = corpus.getEnumerationFacetOfAtomicSimpleType(replaceString2, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e1));
    Assert.assertEquals("    ", corpus.getStringValueOfVariant(e1));

    int collapsedString = corpus.getTypeOfSchema("urn:foo", "collapsedString");
    Assert.assertEquals(EXISchema.WHITESPACE_COLLAPSE,
                      corpus.getWhitespaceFacetValueOfStringSimpleType(collapsedString));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, corpus.ancestryIds[corpus.getSerialOfType(collapsedString)]);
    
    int collapsedString2 = corpus.getTypeOfSchema("urn:foo", "collapsedString2");
    Assert.assertEquals(EXISchema.WHITESPACE_COLLAPSE,
        corpus.getWhitespaceFacetValueOfStringSimpleType(collapsedString));
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfAtomicSimpleType(collapsedString2));
    e1 = corpus.getEnumerationFacetOfAtomicSimpleType(collapsedString2, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e1));
    Assert.assertEquals("A B", corpus.getStringValueOfVariant(e1));
  }

  /**
   * Test type methods against a complex type.
   */
  public void testTypeComplexType() throws Exception {
    EXISchema corpus =  EXISchemaFactoryTestUtil.getEXISchema("/complexType.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    // 46 (built-in) + 20 (global types, and content of "datedStringLength10")
    Assert.assertEquals(67, EXISchemaUtil.countTypesOfSchema(corpus, false));
    // 45 (built-in) + 2 ("string10", and content of "datedStringLength10") 
    Assert.assertEquals(47, corpus.getTotalSimpleTypeCount());
    
    Assert.assertEquals(5, corpus.uris.length);
    Assert.assertEquals("urn:foo", corpus.uris[4]); 

    int datedString = corpus.getTypeOfSchema("urn:foo", "datedString");
    Assert.assertFalse(corpus.isSimpleType(datedString));
    Assert.assertEquals("datedString", corpus.getNameOfType(datedString));
    Assert.assertEquals("urn:foo", EXISchemaUtil.getTargetNamespaceNameOfType(datedString, corpus));

    /*
     * <complexContent>/<extension> is permitted to derive from simple-content
     * complex-type if the content of the derived type is empty. 
     */
    int datedString3 = corpus.getTypeOfSchema("urn:foo", "datedString3");
    Assert.assertFalse(corpus.isSimpleType(datedString3));
    
    int dateTimedString = corpus.getTypeOfSchema("urn:foo", "dateTimedString");
    Assert.assertFalse(corpus.isSimpleType(dateTimedString));

    int namedString = corpus.getTypeOfSchema("urn:foo", "namedString");
    Assert.assertFalse(corpus.isSimpleType(namedString));

    int choiceOccursZero = corpus.getTypeOfSchema("urn:foo", "choiceOccursZero");
    Assert.assertFalse(corpus.isSimpleType(choiceOccursZero));
  }

  /**
   * Invalid attribute default value "x y z" per NCName constraint. 
   */
  public void testAttributeDefaultNCName_01() throws Exception {
    EXISchema corpus;
    int attrA, elemA;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributeDefaultOK03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    Assert.assertEquals("urn:foo", corpus.uris[4]);
    
    attrA = corpus.getGlobalAttrOfSchema("urn:foo", "a");
    Assert.assertEquals(4, corpus.getUriOfAttr(attrA));
    Assert.assertEquals("a", corpus.localNames[4][corpus.getLocalNameOfAttr(attrA)]);
    elemA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertTrue(elemA != EXISchema.NIL_NODE);
    Assert.assertEquals(4, corpus.getUriOfElem(elemA));
    Assert.assertEquals("A", corpus.getNameOfElem(elemA));
    
    m_compilerErrorHandler.clear();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributeDefaultNG03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce1, sce2;
    XMLParseException se;
    int index;
    String message;
    sce1 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    sce2 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce1.getCode());
    Assert.assertEquals(9, sce1.getLocator().getLineNumber());
    se = (XMLParseException)sce1.getException();
    Assert.assertEquals(9, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("cvc-datatype-valid.1.2.1:"));
    
    message = sce1.getMessage();
    Assert.assertTrue((index = message.indexOf("'x y z'")) > 0);
    Assert.assertTrue(message.indexOf("NCName") > index);

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce2.getCode());
    Assert.assertEquals(9, sce2.getLocator().getLineNumber());
    se = (XMLParseException)sce2.getException();
    Assert.assertEquals(9, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("a-props-correct.2:"));
    
    message = sce2.getMessage();
    Assert.assertTrue((index = message.indexOf("'x y z'")) > 0);
    Assert.assertTrue(message.indexOf("'a'") > index);

    Assert.assertEquals("urn:foo", corpus.uris[4]);

    attrA = corpus.getGlobalAttrOfSchema("urn:foo", "a");
    Assert.assertEquals(4, corpus.getUriOfAttr(attrA));
    Assert.assertEquals("a", corpus.localNames[4][corpus.getLocalNameOfAttr(attrA)]);
    elemA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertTrue(elemA != EXISchema.NIL_NODE);
    Assert.assertEquals(4, corpus.getUriOfElem(elemA));
    Assert.assertEquals("A", corpus.getNameOfElem(elemA));
  }

  /**
   * Invalid attribute default value per length facet constraint. 
   */
  public void testAttributeDefaultLength_01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/attributeDefaultOK01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    EXISchemaFactoryTestUtil.getEXISchema("/attributeDefaultNG01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce1, sce2;
    XMLParseException se;
    
    sce1 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    sce2 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce1.getCode());
    Assert.assertEquals(8, sce1.getLocator().getLineNumber());
    se = (XMLParseException)sce1.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("cvc-length-valid:"));

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce2.getCode());
    Assert.assertEquals(8, sce2.getLocator().getLineNumber());
    se = (XMLParseException)sce2.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("a-props-correct.2:"));
  }

  /**
   * Invalid attribute default value per length facet constraint 
   * with the use of characters in SIP (Supplementary Ideographic Plane).
   */
  public void testAttributeDefaultLength_CharacterInSIP_01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/attributeDefaultOK02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    EXISchemaFactoryTestUtil.getEXISchema("/attributeDefaultNG02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce.getCode());
    Assert.assertEquals(8, sce.getLocator().getLineNumber());
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("cvc-length-valid:"));
  }

  /**
   * Invalid attribute fixed value per length facet constraint. 
   */
  public void testAttributeFixedLength_01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedOK01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedNG01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce1, sce2;
    XMLParseException se;
    sce1 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    sce2 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce1.getCode());
    Assert.assertEquals(8, sce1.getLocator().getLineNumber());
    se = (XMLParseException)sce1.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("cvc-length-valid:"));

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce2.getCode());
    Assert.assertEquals(8, sce2.getLocator().getLineNumber());
    se = (XMLParseException)sce2.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("a-props-correct.2:"));
  }

  /**
   * Invalid attribute fixed value per length facet constraint 
   * with the use of characters in SIP (Supplementary Ideographic Plane).
   */
  public void testAttributeFixedLength_CharacterInSIP_01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedOK02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedNG02.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce1, sce2;
    XMLParseException se;
    sce1 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    sce2 = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce1.getCode());
    Assert.assertEquals(8, sce1.getLocator().getLineNumber());
    se = (XMLParseException)sce1.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("cvc-length-valid:"));

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce2.getCode());
    Assert.assertEquals(8, sce2.getLocator().getLineNumber());
    se = (XMLParseException)sce2.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("a-props-correct.2:"));
  }

  /**
   * Test Attribute node
   */
  public void testAttribute() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexType.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int name10 = corpus.getGlobalAttrOfSchema("urn:foo", "name10");
    Assert.assertEquals("name10", EXISchemaUtil.getNameOfAttr(name10, corpus));
    Assert.assertEquals("urn:foo", corpus.uris[corpus.getUriOfAttr(name10)]);
    int string10 = corpus.getTypeOfAttr(name10);
    Assert.assertTrue(corpus.isSimpleType(string10));
    int string10Global = corpus.getTypeOfSchema("urn:foo", "string10");
    Assert.assertEquals(string10Global, string10);
  }
  
  /**
   * Test attribute's IsGlobalAttr() method.
   */
  public void testIsGlobalAttribute() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/verySimple.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    final int[] attrs = EXISchemaUtil.getAttrs(corpus);
    
    final Map<String,Integer> attrSet = new HashMap<String,Integer>();
    for (int pos = 0; pos < attrs.length; pos += EXISchemaLayout.SZ_ATTR) {
      attrSet.put(EXISchemaUtil.getNameOfAttr(pos, corpus), pos);
    }

    Assert.assertEquals(4, attrSet.size());

    final int A = attrSet.get("A");
    Assert.assertTrue(corpus.isGlobalAttr(A));

    final int date = attrSet.get("date");
    Assert.assertTrue(!corpus.isGlobalAttr(date));

    final int id = attrSet.get("id");
    Assert.assertTrue(corpus.isGlobalAttr(id));

    final int time = attrSet.get("time");
    Assert.assertTrue(!corpus.isGlobalAttr(time));
  }

  /**
   * Test minExclusive variant values
   */
  public void testMinExclusive() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/minExclusive.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int minInclusiveFacet;

    int intDerived = corpus.getTypeOfSchema("urn:foo", "intDerived");
    Assert.assertEquals(EXISchema.NIL_VALUE, corpus.getMinInclusiveFacetOfIntegerSimpleType(intDerived));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, corpus.getWidthOfIntegralSimpleType(intDerived));
    
    int intDerived2 = corpus.getTypeOfSchema("urn:foo", "intDerived2");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfIntegerSimpleType(intDerived2);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertEquals(104, corpus.getIntValueOfVariant(minInclusiveFacet));
    Assert.assertEquals(2, corpus.getWidthOfIntegralSimpleType(intDerived2));
    
    int longDerived = corpus.getTypeOfSchema("urn:foo", "longDerived");
    Assert.assertEquals(EXISchema.NIL_VALUE, corpus.getMinInclusiveFacetOfIntegerSimpleType(longDerived));

    int longDerived2 = corpus.getTypeOfSchema("urn:foo", "longDerived2");
    Assert.assertEquals(EXISchema.NIL_VALUE, corpus.getMinInclusiveFacetOfIntegerSimpleType(longDerived2));

    int longDerived3 = corpus.getTypeOfSchema("urn:foo", "longDerived3");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfIntegerSimpleType(longDerived3);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertEquals(105, corpus.getIntValueOfVariant(minInclusiveFacet));
    Assert.assertEquals(3, corpus.getWidthOfIntegralSimpleType(longDerived3));
  }

  /**
   * Test minInclusive variant values
   */
  public void testMinInclusive() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/minInclusive.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int minInclusiveFacet;

    int intDerived = corpus.getTypeOfSchema("urn:foo", "intDerived");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfIntegerSimpleType(intDerived);
    Assert.assertEquals(EXISchema.NIL_VALUE, minInclusiveFacet);

    int intDerived2 = corpus.getTypeOfSchema("urn:foo", "intDerived2");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfIntegerSimpleType(intDerived2);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT,
                      corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertEquals(103, corpus.getIntValueOfVariant(minInclusiveFacet));
    Assert.assertEquals(2, corpus.getWidthOfIntegralSimpleType(intDerived2));

    int longDerived = corpus.getTypeOfSchema("urn:foo", "longDerived");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfIntegerSimpleType(longDerived);
    Assert.assertEquals(EXISchema.NIL_VALUE, minInclusiveFacet);

    int longDerived2 = corpus.getTypeOfSchema("urn:foo", "longDerived2");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfIntegerSimpleType(longDerived2);
    Assert.assertEquals(EXISchema.NIL_VALUE, minInclusiveFacet);

    int longDerived3 = corpus.getTypeOfSchema("urn:foo", "longDerived3");
    minInclusiveFacet = corpus.getMinInclusiveFacetOfIntegerSimpleType(longDerived3);
    Assert.assertTrue(EXISchema.NIL_VALUE != minInclusiveFacet);
    Assert.assertEquals(EXISchema.VARIANT_INT,
                      corpus.getTypeOfVariant(minInclusiveFacet));
    Assert.assertEquals(104, corpus.getIntValueOfVariant(minInclusiveFacet));
    Assert.assertEquals(3, corpus.getWidthOfIntegralSimpleType(longDerived3));
  }

  /**
   * Test enumeration variant values
   */
  public void testEnumeration() throws Exception {
    EXISchema corpus;
    try {
      corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass(), 
          new EXISchemaFactoryTestUtilContext(m_compilerErrorHandler, m_stringBuilder));
    }
    finally {
      //System.out.println(m_stringBuilder.toString());
    }
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    final int[] types = corpus.getTypes();
    
    int e, e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11;

    int stringDerived = corpus.getTypeOfSchema("urn:foo", "stringDerived");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfAtomicSimpleType(stringDerived));
    e1 = corpus.getEnumerationFacetOfAtomicSimpleType(stringDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e1));
    Assert.assertEquals("Tokyo", corpus.getStringValueOfVariant(e1));
    e2 = corpus.getEnumerationFacetOfAtomicSimpleType(stringDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e2));
    Assert.assertEquals("Osaka", corpus.getStringValueOfVariant(e2));
    e3 = corpus.getEnumerationFacetOfAtomicSimpleType(stringDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e3));
    Assert.assertEquals("Nagoya", corpus.getStringValueOfVariant(e3));

    int stringDerived3 = corpus.getTypeOfSchema("urn:foo", "stringDerived3");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfAtomicSimpleType(stringDerived3));
    e1 = corpus.getEnumerationFacetOfAtomicSimpleType(stringDerived3, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e1));
    Assert.assertEquals("Snowden", corpus.getStringValueOfVariant(e1));
    e2 = corpus.getEnumerationFacetOfAtomicSimpleType(stringDerived3, 1);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e2));
    Assert.assertEquals("Assange", corpus.getStringValueOfVariant(e2));
    e3 = corpus.getEnumerationFacetOfAtomicSimpleType(stringDerived3, 2);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e3));
    Assert.assertEquals("Manning", corpus.getStringValueOfVariant(e3));
    Assert.assertEquals(52, corpus.getRestrictedCharacterCountOfStringSimpleType(stringDerived3));
    int pos = corpus.getRestrictedCharacterOfSimpleType(stringDerived3);
    Assert.assertEquals('A', types[pos + 0]);
    Assert.assertEquals('M', types[pos + 12]);
    Assert.assertEquals('N', types[pos + 13]);
    Assert.assertEquals('Z', types[pos + 25]);
    Assert.assertEquals('a', types[pos + 26]);
    Assert.assertEquals('m', types[pos + 38]);
    Assert.assertEquals('n', types[pos + 39]);
    Assert.assertEquals('z', types[pos + 51]);

    int qNameDerived = corpus.getTypeOfSchema("urn:foo", "qNameDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(qNameDerived));

    int notationDerived = corpus.getTypeOfSchema("urn:foo", "notationDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(notationDerived));
    
    int decimalDerived = corpus.getTypeOfSchema("urn:foo", "decimalDerived");
    Assert.assertEquals(4, corpus.getEnumerationFacetCountOfAtomicSimpleType(decimalDerived));
    e1 = corpus.getEnumerationFacetOfAtomicSimpleType(decimalDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(e1));
    Assert.assertEquals("100.1234567", corpus.getVariantCharacters(e1).makeString());
    e2 = corpus.getEnumerationFacetOfAtomicSimpleType(decimalDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(e2));
    Assert.assertEquals("101.2345678", corpus.getVariantCharacters(e2).makeString());
    e3 = corpus.getEnumerationFacetOfAtomicSimpleType(decimalDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(e3));
    Assert.assertEquals("102.3456789", corpus.getVariantCharacters(e3).makeString());
    e4 = corpus.getEnumerationFacetOfAtomicSimpleType(decimalDerived, 3);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(e4));
    Assert.assertEquals("0", corpus.getVariantCharacters(e4).makeString());

    int floatDerived = corpus.getTypeOfSchema("urn:foo", "floatDerived");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfAtomicSimpleType(floatDerived));
    e1 = corpus.getEnumerationFacetOfAtomicSimpleType(floatDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e1));
    Assert.assertEquals("10301E-2", corpus.getVariantCharacters(e1).makeString());
    e2 = corpus.getEnumerationFacetOfAtomicSimpleType(floatDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e2));
    Assert.assertEquals("10501E-2", corpus.getVariantCharacters(e2).makeString());
    e3 = corpus.getEnumerationFacetOfAtomicSimpleType(floatDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e3));
    Assert.assertEquals("10701E-2", corpus.getVariantCharacters(e3).makeString());

    int doubleDerived = corpus.getTypeOfSchema("urn:foo", "doubleDerived");
    Assert.assertEquals(11, corpus.getEnumerationFacetCountOfAtomicSimpleType(doubleDerived));
    e1 = corpus.getEnumerationFacetOfAtomicSimpleType(doubleDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e1));
    Assert.assertEquals("-1E4", corpus.getVariantCharacters(e1).makeString());
    e2 = corpus.getEnumerationFacetOfAtomicSimpleType(doubleDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e2));
    Assert.assertEquals("126743233E7", corpus.getVariantCharacters(e2).makeString());
    e3 = corpus.getEnumerationFacetOfAtomicSimpleType(doubleDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e3));
    Assert.assertEquals("1278E-4", corpus.getVariantCharacters(e3).makeString());
    e4 = corpus.getEnumerationFacetOfAtomicSimpleType(doubleDerived, 3);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e4));
    Assert.assertEquals("12", corpus.getVariantCharacters(e4).makeString());
    e5 = corpus.getEnumerationFacetOfAtomicSimpleType(doubleDerived, 4);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e5));
    Assert.assertEquals("12E2", corpus.getVariantCharacters(e5).makeString());
    e6 = corpus.getEnumerationFacetOfAtomicSimpleType(doubleDerived, 5);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e6));
    Assert.assertEquals("0", corpus.getVariantCharacters(e6).makeString());
    e7 = corpus.getEnumerationFacetOfAtomicSimpleType(doubleDerived, 6);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e7));
    Assert.assertEquals("0", corpus.getVariantCharacters(e7).makeString());
    e8 = corpus.getEnumerationFacetOfAtomicSimpleType(doubleDerived, 7);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e8));
    Assert.assertEquals("INF", corpus.getVariantCharacters(e8).makeString());
    e9 = corpus.getEnumerationFacetOfAtomicSimpleType(doubleDerived, 8);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e9));
    Assert.assertEquals("-INF", corpus.getVariantCharacters(e9).makeString());
    e10 = corpus.getEnumerationFacetOfAtomicSimpleType(doubleDerived, 9);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e10));
    Assert.assertEquals("NaN", corpus.getVariantCharacters(e10).makeString());
    e11 = corpus.getEnumerationFacetOfAtomicSimpleType(doubleDerived, 10);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e11));
    Assert.assertEquals("0", corpus.getVariantCharacters(e11).makeString());

    int intDerived = corpus.getTypeOfSchema("urn:foo", "intDerived");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfAtomicSimpleType(intDerived));
    e1 = corpus.getEnumerationFacetOfAtomicSimpleType(intDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(e1));
    Assert.assertEquals((int)109, corpus.getIntValueOfVariant(e1));
    e2 = corpus.getEnumerationFacetOfAtomicSimpleType(intDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(e2));
    Assert.assertEquals((int)110, corpus.getIntValueOfVariant(e2));
    e3 = corpus.getEnumerationFacetOfAtomicSimpleType(intDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(e3));
    Assert.assertEquals((int)111, corpus.getIntValueOfVariant(e3));

    int longDerived = corpus.getTypeOfSchema("urn:foo", "longDerived");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfAtomicSimpleType(longDerived));
    e1 = corpus.getEnumerationFacetOfAtomicSimpleType(longDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(e1));
    Assert.assertEquals(112, corpus.getIntValueOfVariant(e1));
    e2 = corpus.getEnumerationFacetOfAtomicSimpleType(longDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(e2));
    Assert.assertEquals(113, corpus.getIntValueOfVariant(e2));
    e3 = corpus.getEnumerationFacetOfAtomicSimpleType(longDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(e3));
    Assert.assertEquals(114, corpus.getIntValueOfVariant(e3));

    int integerDerived = corpus.getTypeOfSchema("urn:foo", "integerDerived");
    Assert.assertEquals(6, corpus.getEnumerationFacetCountOfAtomicSimpleType(integerDerived));
    e1 = corpus.getEnumerationFacetOfAtomicSimpleType(integerDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(e1));
    Assert.assertEquals(115, corpus.getIntValueOfVariant(e1));
    e2 = corpus.getEnumerationFacetOfAtomicSimpleType(integerDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(e2));
    Assert.assertEquals(9223372036854775807L, corpus.getLongValueOfVariant(e2));
    e3 = corpus.getEnumerationFacetOfAtomicSimpleType(integerDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(e3));
    Assert.assertEquals(-9223372036854775808L, corpus.getLongValueOfVariant(e3));
    e4 = corpus.getEnumerationFacetOfAtomicSimpleType(integerDerived, 3);
    Assert.assertEquals(EXISchema.VARIANT_INTEGER, corpus.getTypeOfVariant(e4));
    Assert.assertEquals("98765432109876543210", corpus.getIntegerValueOfVariant(e4).toString());
    e5 = corpus.getEnumerationFacetOfAtomicSimpleType(integerDerived, 4);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(e5));
    Assert.assertEquals(987654321098765432L, corpus.getLongValueOfVariant(e5));
    e6 = corpus.getEnumerationFacetOfAtomicSimpleType(integerDerived, 5);
    Assert.assertEquals(EXISchema.VARIANT_LONG, corpus.getTypeOfVariant(e6));
    Assert.assertEquals(-987654321098765432L, corpus.getLongValueOfVariant(e6));

    int dateTimeDerived = corpus.getTypeOfSchema("urn:foo", "dateTimeDerived");
    Assert.assertEquals(6, corpus.getEnumerationFacetCountOfAtomicSimpleType(dateTimeDerived));
    e = corpus.getEnumerationFacetOfAtomicSimpleType(dateTimeDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(e));
    Assert.assertEquals("2003-03-19T13:20:00-05:00", corpus.getDateTimeValueOfVariant(e).toString());
    e = corpus.getEnumerationFacetOfAtomicSimpleType(dateTimeDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(e));
    Assert.assertEquals("2003-03-20T13:20:00-05:00", corpus.getDateTimeValueOfVariant(e).toString());
    e = corpus.getEnumerationFacetOfAtomicSimpleType(dateTimeDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(e));
    Assert.assertEquals("2003-03-21T13:20:00-05:00", corpus.getDateTimeValueOfVariant(e).toString());
    e = corpus.getEnumerationFacetOfAtomicSimpleType(dateTimeDerived, 3);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(e));
    Assert.assertEquals("2013-06-03T24:00:00-05:00", corpus.getDateTimeValueOfVariant(e).toString());
    e = corpus.getEnumerationFacetOfAtomicSimpleType(dateTimeDerived, 4);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(e));
    Assert.assertEquals("2013-06-04T06:00:00Z", corpus.getDateTimeValueOfVariant(e).toString());
    e = corpus.getEnumerationFacetOfAtomicSimpleType(dateTimeDerived, 5);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(e));
    Assert.assertEquals("2012-07-01T00:00:00Z", corpus.getDateTimeValueOfVariant(e).toString());

    int durationDerived = corpus.getTypeOfSchema("urn:foo", "durationDerived");
    Assert.assertEquals(3, corpus.getEnumerationFacetCountOfAtomicSimpleType(durationDerived));
    e1 = corpus.getEnumerationFacetOfAtomicSimpleType(durationDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_DURATION, corpus.getTypeOfVariant(e1));
    Assert.assertTrue(m_datatypeFactory.newDuration("P1Y2M3DT10H30M").equals(
                      corpus.getDurationValueOfVariant(e1)));
    e2 = corpus.getEnumerationFacetOfAtomicSimpleType(durationDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_DURATION, corpus.getTypeOfVariant(e2));
    Assert.assertTrue(m_datatypeFactory.newDuration("P1Y2M4DT10H30M").equals(
                      corpus.getDurationValueOfVariant(e2)));
    e3 = corpus.getEnumerationFacetOfAtomicSimpleType(durationDerived, 2);
    Assert.assertEquals(EXISchema.VARIANT_DURATION, corpus.getTypeOfVariant(e3));
    Assert.assertTrue(m_datatypeFactory.newDuration("P1Y2M5DT10H30M").equals(
                      corpus.getDurationValueOfVariant(e3)));
  }
  
  /**
   * Test the use of itemType in list simple types.
   */
  public void testListItemType() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/list.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int anySimpleType = corpus.getTypeOfSchema(XMLSCHEMA_URI, "anySimpleType");
    Assert.assertTrue(anySimpleType != EXISchema.NIL_NODE);
    
    int _decimal = corpus.getTypeOfSchema(XMLSCHEMA_URI, "decimal");
    Assert.assertTrue(_decimal != EXISchema.NIL_NODE);
    
    int decimal8 = corpus.getTypeOfSchema("urn:goo", "decimal8");
    Assert.assertEquals(_decimal, corpus.getBaseTypeOfSimpleType(decimal8));

    int decimal4 = corpus.getTypeOfSchema("urn:foo", "decimal4");
    Assert.assertEquals(decimal8, corpus.getBaseTypeOfSimpleType(decimal4));

    int listOfDecimal8 = corpus.getTypeOfSchema("urn:foo", "listOfDecimal8");
    Assert.assertEquals(anySimpleType, corpus.getBaseTypeOfSimpleType(listOfDecimal8));
    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(listOfDecimal8));
    Assert.assertEquals(decimal8, corpus.getItemTypeOfListSimpleType(listOfDecimal8));

    int listOfDecimal8Len4 = corpus.getTypeOfSchema("urn:foo", "listOfDecimal8Len4");
    Assert.assertEquals(listOfDecimal8, corpus.getBaseTypeOfSimpleType(listOfDecimal8Len4));
    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(listOfDecimal8Len4));
    Assert.assertEquals(decimal8, corpus.getItemTypeOfListSimpleType(listOfDecimal8Len4));

    int listOfDecimal4 = corpus.getTypeOfSchema("urn:goo", "listOfDecimal4");
    Assert.assertEquals(anySimpleType, corpus.getBaseTypeOfSimpleType(listOfDecimal4));
    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(listOfDecimal4));
    Assert.assertEquals(decimal4, corpus.getItemTypeOfListSimpleType(listOfDecimal4));
  }
  
  /**
   * This is a test to confirm that list of a list is not a valid form of
   * list datatype definition.
   */
  public void testListOfList() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/listOfList.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException schemaFactoryException;
    schemaFactoryException = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException xmlParseException = (XMLParseException)schemaFactoryException.getException();
    Assert.assertEquals(16, xmlParseException.getLineNumber());
    Assert.assertTrue(xmlParseException.getMessage().startsWith("cos-st-restricts.2.1"));
  }
  
  /**
   * Test union simple type
   */
  public void testUnion() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/union.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int _anySimpleType = corpus.getTypeOfSchema(XMLSCHEMA_URI, "anySimpleType");
    Assert.assertTrue(_anySimpleType != EXISchema.NIL_NODE);

    int unionedEnum = corpus.getTypeOfSchema("urn:foo", "unionedEnum");
    Assert.assertEquals(EXISchema.UNION_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(unionedEnum));

    int parentType = corpus.getBaseTypeOfSimpleType(unionedEnum);
    Assert.assertNull(EXISchemaUtil.getTargetNamespaceNameOfType(parentType, corpus));
    Assert.assertEquals("", corpus.getNameOfType(parentType));
    Assert.assertEquals(EXISchema.UNION_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(parentType));
    
    int grandeParentType = corpus.getBaseTypeOfSimpleType(parentType);
    Assert.assertEquals(_anySimpleType, grandeParentType);
  }

  /**
   * Test union of union simple type
   */
  public void testUnionOfUnion() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/union.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int _anySimpleType = corpus.getTypeOfSchema(XMLSCHEMA_URI, "anySimpleType");
    Assert.assertTrue(_anySimpleType != EXISchema.NIL_NODE);

    int unionOfUnion = corpus.getTypeOfSchema("urn:foo", "unionOfUnion");
    Assert.assertTrue(corpus.isSimpleType(unionOfUnion));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(unionOfUnion));
    Assert.assertEquals(EXISchema.UNION_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(unionOfUnion));
  }

  /**
   * Test list of union simple type
   */
  public void testListOfUnion() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/list.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int listOfUnion = corpus.getTypeOfSchema("urn:foo", "listOfUnion");
    Assert.assertTrue(corpus.isSimpleType(listOfUnion));

    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(listOfUnion));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(listOfUnion));
//    Assert.assertEquals(EXISchema.WHITESPACE_COLLAPSE,
//          corpus.getWhitespaceFacetValueOfAtomicSimpleType(listOfUnion));
    
    int union = corpus.getItemTypeOfListSimpleType(listOfUnion);
    Assert.assertTrue(corpus.isSimpleType(union));
    Assert.assertEquals(EXISchema.UNION_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(union));
  }

  /**
   * Test list of union simple type with length facet
   */
  public void testList4OfUnion() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/list.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int list4OfUnion = corpus.getTypeOfSchema("urn:foo", "list4OfUnion");
    Assert.assertTrue(corpus.isSimpleType(list4OfUnion));
    
    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(list4OfUnion));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(list4OfUnion));
//    Assert.assertEquals(EXISchema.WHITESPACE_COLLAPSE,
//          corpus.getWhitespaceFacetValueOfAtomicSimpleType(list4OfUnion));
    
    int union = corpus.getItemTypeOfListSimpleType(list4OfUnion);
    Assert.assertTrue(corpus.isSimpleType(union));
    Assert.assertEquals(EXISchema.UNION_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(union));
  }
  
  public void testNestedGroup01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/nestedGroup01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce;
    sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce.getCode());
    Assert.assertEquals(12, sce.getLocator().getLineNumber());
    
    int datedAddress = corpus.getTypeOfSchema("urn:foo", "datedAddress");
    Assert.assertFalse(corpus.isSimpleType(datedAddress));
  }

  public void testNestedGroup02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/nestedGroup02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce;
    sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce.getCode());
    Assert.assertEquals(23, sce.getLocator().getLineNumber());

    int datedAddress = corpus.getTypeOfSchema("urn:foo", "datedAddress");
    Assert.assertFalse(corpus.isSimpleType(datedAddress));
  }

  /**
   * A model group definition ("group") is used by a complex type
   * definition ("tuple"). The model group definition ("group") in turn
   * references another model group definition ("hoge").
   */
  public void testNestedGroupReference_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/nestedGroupReference.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int item = corpus.getGlobalElemOfSchema("urn:foo", "item");
    Assert.assertEquals("item", corpus.getNameOfElem(item));

    int tuple = corpus.getGlobalElemOfSchema("urn:foo", "tuple");
    Assert.assertEquals("tuple", corpus.getNameOfElem(tuple));
  }
  
  /**
   */
  public void testParticleSerial() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/modelGroupMultiUse02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int typeA = corpus.getTypeOfSchema("urn:foo", "typeA");
    Assert.assertFalse(corpus.isSimpleType(typeA));
    
    int typeB = corpus.getTypeOfSchema("urn:foo", "typeB");
    Assert.assertFalse(corpus.isSimpleType(typeB));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "s0");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "s1");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "s2");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "s3");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "s4");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "s5");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "c0");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "c1");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "c2");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "c3");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "c4");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "c5");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "c6");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "a0");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "a1");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "a2");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "a3");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "a4");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "a5");
    Assert.assertFalse(corpus.isSimpleType(ctype));
  }
  
  /**
   * No particle in a complex type. (empty content)
   * <xsd:complexType name="e1" />
   */
  public void testComplexTypeParticles_Empty_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "e1");
    Assert.assertFalse(corpus.isSimpleType(ctype));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/complexTypeParticles.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int ctype = corpus.getTypeOfSchema("urn:foo", "st1");
    Assert.assertFalse(corpus.isSimpleType(ctype));
  }
  
  /**
   * Extend an empty content model that was defined so implicitly
   * <xsd:complexType name="empty"/>
   */
  public void testExtendEmptyContent_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/extendEmptyContent01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }
  
  /**
   * Extend an empty content model that was explicitly derived from xsd:anyType
   * by <xsd:restriction base="xsd:anyType"/>
   */
  public void testExtendEmptyContent_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/extendEmptyContent02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int empty = corpus.getTypeOfSchema("urn:foo", "empty");
    Assert.assertFalse(corpus.isSimpleType(empty));

    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }
  
  /**
   * A seemingly empty content model actually end up having a particle
   * if it was defined to be of mixed content.
   */
  public void testExtendEmptyContent_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/extendEmptyContent03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int empty = corpus.getTypeOfSchema("urn:foo", "empty");
    Assert.assertFalse(corpus.isSimpleType(empty));

    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }
  
  /**
   * The schema document is not well-formed.
   */
  public void testSAXParseException01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/saxParseException01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce1;
    EXISchemaFactoryException[] sceList = m_compilerErrorHandler.getFatalErrors();
    sce1 = sceList[0];
    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce1.getCode());
    Assert.assertEquals(9, sce1.getLocator().getLineNumber());
  }

  /**
   * nillable="OK", where "true" or "false" is expected.
   */
  public void testSAXParseException02_no_ErrorHandler() throws Exception {
    EXISchema corpus = null;
    try {
      corpus = EXISchemaFactoryTestUtil.getEXISchema(
          "/saxParseException02.xsd", getClass(), (EXISchemaFactoryErrorHandler)null);
    }
    catch (EXISchemaFactoryException sce) {
      Locator locator = sce.getLocator();
      Assert.assertNotNull(locator);
      Assert.assertTrue(locator.getSystemId().endsWith("saxParseException02.xsd"));
      Assert.assertEquals(7, locator.getLineNumber());
    }
    Assert.assertNull(corpus);
    // Simply because we did not set an error handler.
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
  }

  /**
   * nillable="OK", where "true" or "false" is expected.
   */
  public void testSAXParseException02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/saxParseException02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    Assert.assertNotNull(sce);
    Locator locator = sce.getLocator();
    Assert.assertNotNull(locator);
    Assert.assertTrue(locator.getSystemId().endsWith("saxParseException02.xsd"));
    Assert.assertEquals(7, locator.getLineNumber());
    
    int elemA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(elemA));
  }

  /**
   * Importing the same schema multiple times.
   * Two schemas extending the same one.
   */
  public void testMultiSchemaParallel() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/xbrlBareBoneIoo.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    /**
     * "" [empty string]
     * http://www.w3.org/XML/1998/namespace
     * http://www.w3.org/2001/XMLSchema-instance
     * http://www.w3.org/2001/XMLSchema
     * http://www.xbrl.org/2003/instance
     * urn:foo
     * urn:goo
     * urn:hoo
     * urn:ioo
     */
    Assert.assertEquals(9, corpus.uris.length);

    Assert.assertEquals("", corpus.uris[0]);
    Assert.assertEquals("http://www.w3.org/XML/1998/namespace", corpus.uris[1]);
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", corpus.uris[2]);
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", corpus.uris[3]);
    Assert.assertEquals("http://www.xbrl.org/2003/instance", corpus.uris[4]);
    Assert.assertEquals("urn:foo", corpus.uris[5]);
    Assert.assertEquals("urn:goo", corpus.uris[6]);
    Assert.assertEquals("urn:hoo", corpus.uris[7]);
    Assert.assertEquals("urn:ioo", corpus.uris[8]);
    
    int fooIA = corpus.getGlobalElemOfSchema("urn:foo", "IA");
    Assert.assertTrue(fooIA != EXISchema.NIL_NODE);
    Assert.assertEquals(5, corpus.getUriOfElem(fooIA));
    Assert.assertEquals("IA", corpus.getNameOfElem(fooIA));
  }

  /**
   * Always use bundled XBRL schemas instead of those specified by the
   * schema author, which is no longer the case.
   */
  public void testUseItsOwnXBRLSchemas() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/xbrlBareBone.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    /**
     * initial entries plus urn:foo, xbrli
     */
    Assert.assertEquals(6, corpus.uris.length);
  }

  /**
   * Element's serial numbers no longer tells anything about the element. (2013-04-26)
   * 
   * Test global element's serial numbers.
   * 
   * public void testSerailOfElem() throws Exception {
   *   EXISchema corpus =
   *     EXISchemaFactoryTestUtil.getEXISchema("/xbrlBareBone.xsd", getClass());
   * 
   *   int len = corpus.getGlobalElemCountOfSchema();
   *   Assert.assertTrue(len > 0);
   * 
   *   for (int i = 0; i < len; i++) {
   *     int elem = corpus.getGlobalElemOfSchema(i);
   *     Assert.assertEquals(i, corpus.getSerialOfElem(elem));
   *   }
   * }
   */

  public void testIncludeSchema01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/verySimpleIncluding.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    // foo (9 + 2) + goo 10 = 21
    Assert.assertEquals(21, corpus.getGlobalElemCountOfSchema());
    Assert.assertEquals(21, EXISchemaUtil.countElemsOfSchema(corpus));

    // foo (1 + 1) + goo 1 = 7
    Assert.assertEquals(3, corpus.getGlobalAttrCountOfSchema());
    // foo (1 + 3) + goo 1 = 5
    Assert.assertEquals(5, EXISchemaUtil.countAttrsOfSchema(corpus));

    Assert.assertEquals(6, corpus.uris.length);
    Assert.assertEquals("urn:foo", corpus.uris[4]); 
    Assert.assertEquals("urn:goo", corpus.uris[5]); 
  }

  /**
   *  Including chameleon schema.
   */
  public void testIncludeSchemaChameleon_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/chameleonIncluding_01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
        
    int unionedString = corpus.getTypeOfSchema("urn:foo", "unionedString");
    Assert.assertTrue(corpus.isSimpleType(unionedString));
    Assert.assertEquals(EXISchema.UNION_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(unionedString));
  }
  
  /**
   * Test empty group.
   */
  public void testEmptyGroup01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/emptyGroup.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int emptyCT = corpus.getTypeOfSchema("urn:foo", "emptyCT");
    Assert.assertTrue(emptyCT != EXISchema.NIL_NODE);
    Assert.assertFalse(corpus.isSimpleType(emptyCT));
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/circularSchemaImport_1.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    Assert.assertEquals(6, corpus.uris.length);
    Assert.assertEquals("http://www.fujitsu.com/xbrl/taxeditor/default2", corpus.uris[4]);
    Assert.assertEquals("http://www.fujitsu.com/xbrl/taxeditor/default3", corpus.uris[5]);
  }

  /**
   * Make sure mutual import between two schemas does not end up with
   * a stack overflow exception.
   */
  public void testMutualImportWithHub() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/mutualImport/hub.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    Assert.assertEquals(8, corpus.uris.length);
    Assert.assertEquals("urn:foo", corpus.uris[4]);
    Assert.assertEquals("urn:goo", corpus.uris[5]);
    Assert.assertEquals("urn:hub", corpus.uris[6]);
    Assert.assertEquals("urn:zoo", corpus.uris[7]);
  }

  /**
   * There are two declarations of element "C" in namespace "urn:foo".
   */
  public void testDuplicateImport01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/duplicateImport/zoo1.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException[] speList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, speList.length);
    XMLParseException spe = (XMLParseException)speList[0].getException();
    Assert.assertEquals(14, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("sch-props-correct.2"));

    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));

    int eB = corpus.getGlobalElemOfSchema("urn:foo", "B");
    Assert.assertEquals("B", corpus.getNameOfElem(eB));
  }
  
  /**
   * Directly importing two separate schemas of the same target namespace is
   * permitted as long as the two imported schemas do not overlap each other.
   */
  public void testDuplicateImport02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/duplicateImport2/goo.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int tA = corpus.getTypeOfSchema("urn:foo", "A");
    Assert.assertFalse(corpus.isSimpleType(tA));
    int tB = corpus.getTypeOfSchema("urn:foo", "B");
    Assert.assertFalse(corpus.isSimpleType(tB));

    int eX = corpus.getGlobalElemOfSchema("urn:goo", "X");
    Assert.assertEquals("X", corpus.getNameOfElem(eX));
    int eY = corpus.getGlobalElemOfSchema("urn:goo", "Y");
    Assert.assertEquals("Y", corpus.getNameOfElem(eY));
  }
  
  /**
   * There are 3 schemas.
   * schema_a, schema_b and schema_c of which schema_a is the root.
   * schema_b includes schema_c.
   * schema_a includes both schema_c and schema_b.
   */
  public void testDuplicateSchemaInclude1() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/duplicateSchemaInclude1_a.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    // urn:foo plus initial entries
    Assert.assertEquals(5, corpus.uris.length);

    Assert.assertEquals(1, EXISchemaUtil.getTypeCountOfSchema("urn:foo", corpus));

    int ea = corpus.getGlobalElemOfSchema("urn:foo", "ea");
    Assert.assertEquals("ea", corpus.getNameOfElem(ea));
    int eb = corpus.getGlobalElemOfSchema("urn:foo", "eb");
    Assert.assertEquals("eb", corpus.getNameOfElem(eb));
    int tc = corpus.getTypeOfSchema("urn:foo", "tc");
    Assert.assertEquals("tc", corpus.getNameOfType(tc));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/duplicateSchemaIncludeImport1_a.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    // urn:foo and urn:goo plus initial entries
    Assert.assertEquals(6, corpus.uris.length);

    Assert.assertEquals(1, EXISchemaUtil.getTypeCountOfSchema("urn:foo", corpus));
    Assert.assertEquals(1, EXISchemaUtil.getTypeCountOfSchema("urn:goo", corpus));

    int ea = corpus.getGlobalElemOfSchema("urn:foo", "ea");
    Assert.assertEquals("ea", corpus.getNameOfElem(ea));
    int tb = corpus.getTypeOfSchema("urn:foo", "tb");
    Assert.assertEquals("tb", corpus.getNameOfType(tb));
    int ec = corpus.getGlobalElemOfSchema("urn:goo", "ec");
    Assert.assertEquals("ec", corpus.getNameOfElem(ec));
    int td = corpus.getTypeOfSchema("urn:goo", "td");
    Assert.assertEquals("td", corpus.getNameOfType(td));
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

      // urn:foo plus initial entries
      Assert.assertEquals(5, corpus.uris.length);

      Assert.assertEquals(1, EXISchemaUtil.getTypeCountOfSchema("urn:foo", corpus));

      int eItem = corpus.getGlobalElemOfSchema("urn:foo", "MyItem");
      Assert.assertEquals("MyItem", corpus.getNameOfElem(eItem));
      int eTuple = corpus.getGlobalElemOfSchema("urn:foo", "MyTuple");
      Assert.assertEquals("MyTuple", corpus.getNameOfElem(eTuple));
      int tTuple = corpus.getTypeOfSchema("urn:foo", "MyTupleType");
      Assert.assertEquals("MyTupleType", corpus.getNameOfType(tTuple));
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
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
          files[i], getClass(), m_compilerErrorHandler);
      Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

       // urn:foo plus initial entries
      Assert.assertEquals(5, corpus.uris.length);

      Assert.assertEquals(2, EXISchemaUtil.getTypeCountOfSchema("urn:foo", corpus));

      int ea = corpus.getGlobalElemOfSchema("urn:foo", "ea");
      Assert.assertEquals("ea", corpus.getNameOfElem(ea));
      int ta = corpus.getTypeOfSchema("urn:foo", "ta");
      Assert.assertEquals("ta", corpus.getNameOfType(ta));
      int eb = corpus.getGlobalElemOfSchema("urn:foo", "eb");
      Assert.assertEquals("eb", corpus.getNameOfElem(eb));
      int tc = corpus.getTypeOfSchema("urn:foo", "tc");
      Assert.assertEquals("tc", corpus.getNameOfType(tc));
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

      // urn:foo plus initial entries
      Assert.assertEquals(5, corpus.uris.length);

      Assert.assertEquals(1, EXISchemaUtil.getTypeCountOfSchema("urn:foo", corpus));

      int ea = corpus.getGlobalElemOfSchema("urn:foo", "ea");
      Assert.assertEquals("ea", corpus.getNameOfElem(ea));
      int aa = corpus.getGlobalAttrOfSchema("urn:foo", "aa");
      Assert.assertEquals("aa", EXISchemaUtil.getNameOfAttr(aa, corpus));
      int eb = corpus.getGlobalElemOfSchema("urn:foo", "eb");
      Assert.assertEquals("eb", corpus.getNameOfElem(eb));
      int tc = corpus.getTypeOfSchema("urn:foo", "tc");
      Assert.assertEquals("tc", corpus.getNameOfType(tc));
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

      // urn:foo plus initial entries
      Assert.assertEquals(5, corpus.uris.length);

      Assert.assertEquals(1, EXISchemaUtil.getTypeCountOfSchema("urn:foo", corpus));

      int ea = corpus.getGlobalElemOfSchema("urn:foo", "ea");
      Assert.assertEquals("ea", corpus.getNameOfElem(ea));
      int tc = corpus.getTypeOfSchema("urn:foo", "tc");
      Assert.assertEquals("tc", corpus.getNameOfType(tc));
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

      // urn:foo plus initial entries
      Assert.assertEquals(5, corpus.uris.length);

      Assert.assertEquals(1, EXISchemaUtil.getTypeCountOfSchema("urn:foo", corpus));

      int ea = corpus.getGlobalElemOfSchema("urn:foo", "ea");
      Assert.assertEquals("ea", corpus.getNameOfElem(ea));
      int tc = corpus.getTypeOfSchema("urn:foo", "tc");
      Assert.assertEquals("tc", corpus.getNameOfType(tc));
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/indirectImport/foo.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException[] sceList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, sceList.length);
    XMLParseException spe = (XMLParseException)sceList[0].getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("src-resolve.4.2"));
    
    Assert.assertEquals(7, corpus.uris.length);
    Assert.assertEquals("urn:foo", corpus.uris[4]); 
    Assert.assertEquals("urn:goo", corpus.uris[5]); 
    Assert.assertEquals("urn:zoo", corpus.uris[6]); 
  }

  /**
   * Use of attribute group in the context of schema inclusion. 
   */
  public void testIncludeAttrGroup01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/includeAttrGroup/foo.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int fooElem = corpus.getGlobalElemOfSchema("urn:foo", "Foo");
    Assert.assertEquals("Foo", corpus.getNameOfElem(fooElem));
    
    int fooA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(fooA));

    int fooB = corpus.getGlobalElemOfSchema("urn:foo", "B");
    Assert.assertEquals("B", corpus.getNameOfElem(fooB));
    
    int fooTypeC = corpus.getTypeOfSchema("urn:foo", "C");
    Assert.assertFalse(corpus.isSimpleType(fooTypeC));
  }
  
  /**
   * Compiling a schema by its windows-like relative path.
   * The schema imports another in the same directory.
   * It is testing whether the imported schema is resolved successfully.
   */
  public void testWindowsRelativePath() throws Exception {
    if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
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
        
        Assert.assertEquals(XMLSCHEMA_URI, corpus.uris[3]); 
      }
      finally {
        // restore "user.dir" system property
        System.setProperty("user.dir", origCurDir);
      }
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);

    EXISchemaFactoryException[] sceList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    EXISchemaFactoryException[] sceList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    EXISchemaFactoryException[] sceList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    EXISchemaFactoryException[] sceList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, sceList.length);
    XMLParseException spe = (XMLParseException)sceList[0].getException();
    Assert.assertEquals(6, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-invalid-content.2:"));
  }

  /**
   * Invalid element default value per length facet constraint. 
   */
  public void testElementDefaultLength_01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/elementDefaultOK01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    EXISchemaFactoryTestUtil.getEXISchema("/elementDefaultNG01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce.getCode());
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2:"));
  }
  
  /**
   * Invalid element default value per length facet constraint 
   * with the use of characters in SIP (Supplementary Ideographic Plane).
   */
  public void testElementDefaultLength_CharacterInSIP_01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/elementDefaultOK02.xsd", getClass(), m_compilerErrorHandler);
    
    // REVISIT: surrogate pairs are counted as 2 characters each, not one.
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    XMLParseException spe = (XMLParseException)m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0].getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2:"));
    
    /**
     * Assert.assertEquals(EXISchema.CONSTRAINT_DEFAULT, corpus.getConstraintOfElem(eA));
     * // single character in SIP (U+2000B)
     * int default_eA = corpus.getConstraintValueOfElem(eA);
     * Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(default_eA));
     * Assert.assertEquals("\uD840\uDC0B", corpus.getStringValueOfVariant(default_eA));
     */

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    EXISchemaFactoryTestUtil.getEXISchema("/elementDefaultNG02.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce.getCode());
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2"));
  }
  
  /**
   * Invalid element fixed value per length facet constraint. 
   */
  public void testElementFixedLength_01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/elementFixedOK01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    EXISchemaFactoryTestUtil.getEXISchema("/elementFixedNG01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce.getCode());
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2:"));
  }
  
  /**
   * Invalid element fixed value per length facet constraint 
   * with the use of characters in SIP (Supplementary Ideographic Plane).
   */
  public void testElementFixedLength_CharacterInSIP_01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/elementFixedOK02.xsd", getClass(), m_compilerErrorHandler);
    
    // REVISIT: surrogate pairs are counted as 2 characters each, not one.
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    XMLParseException spe = (XMLParseException)m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0].getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2:"));

    /** 
     * Assert.assertEquals(EXISchema.CONSTRAINT_FIXED, corpus.getConstraintOfElem(eA));
     * // single character in SIP (U+2000B)
     * int fixed_eA = corpus.getConstraintValueOfElem(eA);
     * Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(fixed_eA));
     * Assert.assertEquals("\uD840\uDC0B", corpus.getStringValueOfVariant(fixed_eA));
     */

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    EXISchemaFactoryTestUtil.getEXISchema("/elementFixedNG02.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2:"));
  }
  
  /**
   * Element default value of type QName where the value is valid. 
   */
  public void testElementDefaultQNameValueOK01a() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/elementDefaultOK03a.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
  }
  
  /**
   * Element default value of type QName where the value is valid. 
   */
  public void testElementDefaultQNameValueOK01b() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/elementDefaultOK03b.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
  }
  
  /**
   * Element default value of type QName where the value is *not* valid. 
   */
  public void testElementDefaultQNameValueNG01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/elementDefaultNG03.xsd", getClass(), m_compilerErrorHandler);

    EXISchemaFactoryException sce;
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    Assert.assertEquals(1, m_compilerErrorHandler.getErrorCount(EXISchemaFactoryException.XMLSCHEMA_ERROR));
    sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(5, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2:"));
  }
  
  /**
   * Element fixed value of type QName where the value is valid. 
   */
  public void testElementFixedQNameValueOK01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/elementFixedOK03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
  }
  
  /**
   * Element fixed value of type QName where the value is *not* valid. 
   */
  public void testElementFixedQNameValueNG01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/elementFixedNG03.xsd", getClass(), m_compilerErrorHandler);
    
    EXISchemaFactoryException sce;
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    Assert.assertEquals(1, m_compilerErrorHandler.getErrorCount(EXISchemaFactoryException.XMLSCHEMA_ERROR));
    sce = m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(5, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.2:"));
  }
  
  /**
   * Same name is used for two complex type declarations.
   */
  public void testComplexTypeSameName() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeSameNameComplex.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("InvalidRegex:"));
  }
  
  /**
   * Union datatypes can have patterns in schemas, however, those patterns
   * are not taken into account in EXI.
   */
  public void testUnionSimpleTypePatternFacet() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/patterns.xsd",
                                           getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int patternedUnion = corpus.getTypeOfSchema("urn:foo", "patternedUnion");
    Assert.assertEquals("patternedUnion", corpus.getNameOfType(patternedUnion));
    Assert.assertEquals("urn:foo", EXISchemaUtil.getTargetNamespaceNameOfType(patternedUnion, corpus));
    Assert.assertEquals(EXISchema.UNION_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(patternedUnion));
  }
  
  /**
   * Simple type facet "length" does not permit a zero-length value.
   */
  public void testSimpleTypeFacetNoValueNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/typeFacetNoValue1.xsd",
                                           getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("ct-props-correct.4:"));
  }
  
  /**
   * Currently "all" group with minOccurs value "0" must not result in an error.
   */
  public void testAllGroupWithMinOccursZero() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupWithMinOccursZero.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
  }

  /**
   * "all" group can only participate in a particle of maxOccurs="1".
   * <xsd:all maxOccurs="2"> is not permitted.
   */
  public void testAllGroupMaxOccursNG01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupMaxOccursNG01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(9, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-invalid-value:"));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * "all" group can only participate in a particle of maxOccurs="1".
   * <xsd:all maxOccurs="1"> is OK.
   */
  public void testAllGroupMaxOccursOK01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupMaxOccursOK01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * Particles in "all" group can have maxOccurs of value "0" or "1".
   */
  public void testAllGroupMaxOccursNG02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupMaxOccursNG02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(9, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-all-limited.2:"));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * Particles in "all" group can have maxOccurs of value "0" or "1".
   */
  public void testAllGroupMaxOccursNG03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupMaxOccursNG03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-all-limited.1.2:"));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * The terms of member particles of an "all" group must be elements.
   * Groups and wildcards are not permitted.
   * <xsd:sequence> appears as child of <xsd:all>, which is an error.
   */
  public void testAllGroupMemberParticleNG01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupMemberParticleNG01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);

    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));

    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * The terms of member particles of an "all" group must be elements.
   * Groups and wildcards are not permitted.
   * <xsd:choice> appears as child of <xsd:all>, which is an error.
   */
  public void testAllGroupMemberParticleNG02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupMemberParticleNG02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * The terms of member particles of an "all" group must be elements.
   * Groups and wildcards are not permitted.
   * <xsd:all> appears as child of <xsd:all>, which is an error.
   */
  public void testAllGroupMemberParticleNG03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupMemberParticleNG03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * The terms of member particles of an "all" group must be elements.
   * Groups and wildcards are not permitted.
   * <xsd:group> appears as child of <xsd:all>, which is an error.
   */
  public void testAllGroupMemberParticleNG04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupMemberParticleNG04.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * The terms of member particles of an "all" group must be elements.
   * Groups and wildcards are not permitted.
   * <xsd:any> appears as child of <xsd:all>, which is an error.
   */
  public void testAllGroupMemberParticleNG05() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupMemberParticleNG05.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * The terms of member particles of an "all" group must be elements.
   * Groups and wildcards are not permitted.
   * <xsd:element> appears as child of <xsd:all>, which is OK.
   */
  public void testAllGroupMemberParticleOK01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupMemberParticleOK01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * "all" group can only be used in a content model particle of a complex type.
   * <xsd:all> appears as child of <xsd:sequence>, which is an error.
   */
  public void testAllGroupChildOfGroupNG01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupChildOfGroupNG01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * "all" group can only be used in a content model particle of a complex type.
   * <xsd:all> appears as child of <xsd:choice>, which is an error.
   */
  public void testAllGroupChildOfGroupNG02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupChildOfGroupNG02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * "all" group can only be used in a content model particle of a complex type.
   * <xsd:all> appears as child of named <xsd:group>, then used from within
   * a sequence group, which is an error.
   */
  public void testAllGroupChildOfGroupNG03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupChildOfGroupNG03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-all-limited.1.2:"));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * "all" group can only be used in a content model particle of a complex type.
   * <xsd:all> appears as child of named <xsd:group>, then used from within
   * a choice group, which is an error.
   */
  public void testAllGroupChildOfGroupNG04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupChildOfGroupNG04.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-all-limited.1.2:"));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * "all" group can only be used in a content model particle of a complex type.
   * <xsd:all> appears as child of named <xsd:group>, then used from within
   * a "all" group, which is an error.
   */
  public void testAllGroupChildOfGroupNG05() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupChildOfGroupNG05.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList =
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(10, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-elt-must-match.1:"));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
  }

  /**
   * "all" group can only be used in a content model particle of a complex type.
   * <xsd:all> appears as child of named <xsd:group>, then used from within
   * complex type definition, which is OK.
   */
  public void testAllGroupChildOfGroupOK01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/allGroupChildOfGroupOK01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/includeMultiple/osgb.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalErrorCount());

    Assert.assertEquals(1, EXISchemaUtil.getTypeCountOfSchema("urn:gml", corpus));
    Assert.assertEquals(2, EXISchemaUtil.getTypeCountOfSchema("urn:osgb", corpus));

    // Inspect typeA
    int typeA = corpus.getTypeOfSchema("urn:osgb", "typeA");
    Assert.assertFalse(corpus.isSimpleType(typeA));
    
    // Inspect typeB
    int typeB = corpus.getTypeOfSchema("urn:osgb", "typeB");
    Assert.assertFalse(corpus.isSimpleType(typeB));

    int abcdType = corpus.getTypeOfSchema("urn:gml", "abcdType");
    Assert.assertEquals("abcdType", corpus.getNameOfType(abcdType));
    
    // Inspect osgbElem
    int osgbElem = corpus.getGlobalElemOfSchema("urn:osgb", "osgbElem");
    Assert.assertEquals("osgbElem", corpus.getNameOfElem(osgbElem));
  }

  /**
   * Docbook 4.3 schema
   */
  public void testDocbook43Schema() throws Exception {
    EXISchema corpus = Docbook43Schema.getEXISchema();
    
    // NOTE: sparse inspection (for now)
    
    // calstblx.xsd
    int _yesorno = corpus.getTypeOfSchema("", "yesorno");
    Assert.assertTrue(corpus.isSimpleType(_yesorno));

    // dbhierx.xsd
    int appendixClass = corpus.getGlobalElemOfSchema("", "appendix.class");
    Assert.assertEquals("appendix.class", corpus.getNameOfElem(appendixClass));

    int chapterClass = corpus.getGlobalElemOfSchema("", "chapter.class");
    Assert.assertEquals("chapter.class", corpus.getNameOfElem(chapterClass));
    
    int chapter = corpus.getGlobalElemOfSchema("", "chapter");
    Assert.assertEquals("chapter", corpus.getNameOfElem(chapter));

    // dbnotnx.xsd
    int _notationClass = corpus.getTypeOfSchema("", "notation.class");
    Assert.assertTrue(corpus.isSimpleType(_notationClass));
    Assert.assertEquals(29, corpus.getEnumerationFacetCountOfAtomicSimpleType(_notationClass));

    // dbpoolx.xsd
    int ndxtermClass = corpus.getGlobalElemOfSchema("", "ndxterm.class");
    Assert.assertEquals("ndxterm.class", corpus.getNameOfElem(ndxtermClass));

    // htmltblx.xsd
    int colgroup = corpus.getGlobalElemOfSchema("", "colgroup");
    Assert.assertEquals("colgroup", corpus.getNameOfElem(colgroup));

    int book = corpus.getGlobalElemOfSchema("", "book");
    Assert.assertEquals("book", corpus.getNameOfElem(book));
  }
  
  /**
   * REVISIT: This test case does not work!
   * 
   * Compile IPO schema (International Purchase Order) schema
   * excerpted from XML Schema Part 0 (Primer Second Edition).
   */
  public void testPrimerIPOSchemas() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/primerIPO.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int _address = corpus.getTypeOfSchema("http://www.example.com/IPO", "Address");
    Assert.assertFalse(corpus.isSimpleType(_address));
    int _usAddress = corpus.getTypeOfSchema("http://www.example.com/IPO", "Address");
    Assert.assertFalse(corpus.isSimpleType(_usAddress));
    int _ukAddress = corpus.getTypeOfSchema("http://www.example.com/IPO", "Address");
    Assert.assertFalse(corpus.isSimpleType(_ukAddress));
  }

  /**
   * Make sure that reference to model group definitions are resolved. 
   */
  public void testModelGroupDefinitionsOK01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/modelGroupDefinitions.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int typeB = corpus.getTypeOfSchema("urn:foo", "typeB");
    Assert.assertFalse(corpus.isSimpleType(typeB));
  }

  /**
   * Named groups become a distinct node for each use. 
   */
  public void testModelGroupDefinitionsOK02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/modelGroupMultiUse01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int typeA = corpus.getTypeOfSchema("urn:foo", "typeA");
    Assert.assertFalse(corpus.isSimpleType(typeA));
    
    int typeB = corpus.getTypeOfSchema("urn:foo", "typeB");
    Assert.assertFalse(corpus.isSimpleType(typeB));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attrGroupDefinitions.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int typeA = corpus.getTypeOfSchema("urn:foo", "typeA");
    Assert.assertFalse(corpus.isSimpleType(typeA));
    
    int typeC = corpus.getTypeOfSchema("urn:foo", "typeC");
    Assert.assertFalse(corpus.isSimpleType(typeC));
  }

  /**
   * CBMS (Convergence of Broadcast and Mobile Services) schema
   */
  public void testCBMSSchema() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/cbms/dvb_ipdc_esg.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
        
    Assert.assertEquals(7, corpus.uris.length);
    Assert.assertEquals("urn:dvb:ipdc:esg:2005", corpus.uris[4]);
    Assert.assertEquals("urn:mpeg:mpeg7:schema:2001", corpus.uris[5]);
    Assert.assertEquals("urn:tva:metadata:2005", corpus.uris[6]);
    
    // Inspect tvans:synopsisType
    int synopsisType = corpus.getTypeOfSchema("urn:tva:metadata:2005", "SynopsisType");
    Assert.assertFalse(corpus.isSimpleType(synopsisType));
    int textualType = corpus.getTypeOfSchema("urn:mpeg:mpeg7:schema:2001", "TextualType");
    Assert.assertFalse(corpus.isSimpleType(textualType));

    // Inspect mpeg7ns:TitleType
    int titleType = corpus.getTypeOfSchema("urn:mpeg:mpeg7:schema:2001", "TitleType");
    Assert.assertFalse(corpus.isSimpleType(titleType));

    // Inspect mpeg7ns:TermDefinitionType
    // Note that TermDefinitionType recurse in its definition.
    int termDefinitionType = corpus.getTypeOfSchema("urn:mpeg:mpeg7:schema:2001", "TermDefinitionType");
    Assert.assertFalse(corpus.isSimpleType(termDefinitionType));
  }

  /**
   * Web Services Addressing + SOAP envelope schemas
   */
  public void testSOAPAddressing() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/soap-addr-2005-08.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
        
    Assert.assertEquals(6, corpus.uris.length);
    Assert.assertEquals("http://www.w3.org/2003/05/soap-envelope", corpus.uris[4]); 
    Assert.assertEquals("http://www.w3.org/2005/08/addressing", corpus.uris[5]); 
    
    // Inspect envns:Subcode
    int faultcodeType = corpus.getTypeOfSchema("http://www.w3.org/2003/05/soap-envelope", "faultcode");
    Assert.assertFalse(corpus.isSimpleType(faultcodeType));
  }

  /**
   * One of the Namespace-prefix bindings gets overridden within a schema.
   * This test case makes sure that redefined bindings take effect for
   * evaluating QName attribute values. 
   */
  public void testRedefinedPrefixOK() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/redefinedPrefix.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int fooString = corpus.getTypeOfSchema("urn:foo", "string");
    Assert.assertTrue(corpus.isSimpleType(fooString));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));

    int eB = corpus.getGlobalElemOfSchema("urn:foo", "B");
    Assert.assertEquals("B", corpus.getNameOfElem(eB));
  }

  /**
   * GMTI (The NATO Ground Moving Target Indicator Format) Schema
   */
  public void testGMTISchema() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/exi/GMTI/gmti.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    // Sparse inspection for now.
    Assert.assertEquals(6, corpus.uris.length);
    Assert.assertEquals("http://diides.ncr.disa.mil/xmlreg/GEO", corpus.uris[4]); 
    Assert.assertEquals("http://www.gmti.mil/STANAG/4607/NATO", corpus.uris[5]); 
    
    int targetReportType = corpus.getTypeOfSchema("http://www.gmti.mil/STANAG/4607/NATO", "TargetReportType");
    Assert.assertTrue(targetReportType != EXISchema.NIL_NODE);
  }
  
  /**
   * FPML 4.0 schema.
   */
  public void testFpmlSchema01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fpml-4.0/fpml-main-4-0.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    // sparse inspection for now
    
    Assert.assertEquals(6, corpus.uris.length);
    Assert.assertEquals("http://www.fpml.org/2003/FpML-4-0", corpus.uris[4]);
    Assert.assertEquals("http://www.w3.org/2000/09/xmldsig#", corpus.uris[5]); 

    int Signature = corpus.getGlobalElemOfSchema("http://www.w3.org/2000/09/xmldsig#", "Signature");
    Assert.assertEquals("Signature", corpus.getNameOfElem(Signature));
    
    int elem_FpML = corpus.getGlobalElemOfSchema("http://www.fpml.org/2003/FpML-4-0", "FpML");
    Assert.assertEquals("FpML", corpus.getNameOfElem(elem_FpML));
    int type_Document = corpus.getTypeOfSchema("http://www.fpml.org/2003/FpML-4-0", "Document");
    Assert.assertFalse(corpus.isSimpleType(type_Document));
    int Message = corpus.getTypeOfSchema("http://www.fpml.org/2003/FpML-4-0", "Message");
    Assert.assertFalse(corpus.isSimpleType(Message));

    int type_EquityPaymentDates = corpus.getTypeOfSchema("http://www.fpml.org/2003/FpML-4-0", "EquityPaymentDates");
    Assert.assertFalse(corpus.isSimpleType(type_EquityPaymentDates));
    int type_AdjustableOrRelativeDates = corpus.getTypeOfSchema("http://www.fpml.org/2003/FpML-4-0", "AdjustableOrRelativeDates");
    Assert.assertFalse(corpus.isSimpleType(type_AdjustableOrRelativeDates));
  }

  /**
   * OPENGIS schema.
   */
  public void testOpenGisExample01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/opengis/openGis.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    Assert.assertEquals(9, corpus.uris.length);
    Assert.assertEquals("http://www.opengis.net/gml", corpus.uris[4]);
    Assert.assertEquals("http://www.opengis.net/ogc", corpus.uris[5]);
    Assert.assertEquals("http://www.opengis.net/wfs", corpus.uris[6]);
    Assert.assertEquals("http://www.ordnancesurvey.co.uk/xml/namespaces/osgb", corpus.uris[7]); 
    Assert.assertEquals("urn:myhub", corpus.uris[8]); 
    
    int featureCollection = corpus.getGlobalElemOfSchema("http://www.opengis.net/wfs", "FeatureCollection");
    Assert.assertTrue(featureCollection != EXISchema.NIL_NODE);

    int featureMember = corpus.getGlobalElemOfSchema("http://www.opengis.net/gml", "featureMember");
    Assert.assertTrue(featureMember != EXISchema.NIL_NODE);
    int _Feature = corpus.getGlobalElemOfSchema("http://www.opengis.net/gml", "_Feature");
    Assert.assertTrue(_Feature != EXISchema.NIL_NODE);

    int TopographicPoint = corpus.getGlobalElemOfSchema("http://www.ordnancesurvey.co.uk/xml/namespaces/osgb", "TopographicPoint");
    Assert.assertTrue(TopographicPoint != EXISchema.NIL_NODE);
    int _TopographicFeature = corpus.getGlobalElemOfSchema("http://www.ordnancesurvey.co.uk/xml/namespaces/osgb", "_TopographicFeature");
    Assert.assertTrue(_TopographicFeature != EXISchema.NIL_NODE);
    
    int add = corpus.getGlobalElemOfSchema("http://www.opengis.net/ogc", "Add");
    Assert.assertTrue(add != EXISchema.NIL_NODE);
  }
  
  /**
   * A schema with no target namespace can import another that has namespace name.
   * Similarly, a schema that has target namespace can import another that has
   * no target namespace.
   */
  public void testImportNoNamespace_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/importNoNamespace/noTargetNamespace.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int person = corpus.getGlobalElemOfSchema("", "person");
    Assert.assertEquals("person", corpus.getNameOfElem(person));

    int name = corpus.getGlobalElemOfSchema("", "name");
    Assert.assertEquals("name", corpus.getNameOfElem(name));

    int family = corpus.getGlobalElemOfSchema("", "family");
    Assert.assertEquals("family", corpus.getNameOfElem(family));

    int given = corpus.getGlobalElemOfSchema("", "given");
    Assert.assertEquals("given", corpus.getNameOfElem(given));

    int fooPerson = corpus.getGlobalElemOfSchema("urn:foo", "person");
    Assert.assertEquals("person", corpus.getNameOfElem(fooPerson));
    Assert.assertTrue(fooPerson != person);

    int fooName = corpus.getGlobalElemOfSchema("urn:foo", "name");
    Assert.assertEquals("name", corpus.getNameOfElem(fooName));
    Assert.assertTrue(fooName != person);

    int fooFamily = corpus.getGlobalElemOfSchema("urn:foo", "family");
    Assert.assertEquals("family", corpus.getNameOfElem(fooFamily));
    Assert.assertTrue(fooFamily != person);

    // Inspect foo:personnel
    int fooPersonnel = corpus.getGlobalElemOfSchema("urn:foo", "personnel");
    Assert.assertEquals("personnel", corpus.getNameOfElem(fooPersonnel));
  }

  /**
   * Unresolvable QName reference results in an exception
   * of IDS_VALIDATION_ERROR_PREFIX_NOT_DECLARED_IN_NAMESPACE
   */
  public void testUnsolvedElemQName_01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema("/unresolvedElemQName01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(11, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("s4s-att-invalid-value:"));
  }

  /**
   * UPA violation in choice group.
   */
  public void testUPA_Choice_NG01_no_ErrorHandler() throws Exception {
    EXISchema corpus = null;
    try {
      corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/choiceUPA_NG01.xsd", getClass(), 
        (EXISchemaFactoryErrorHandler)null);
    }
    catch (EXISchemaFactoryException sce) {
      XMLParseException spe = (XMLParseException)sce.getException();
      Assert.assertEquals(8, spe.getLineNumber());
      Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
    }
    Assert.assertNull(corpus);
    // Simply because we did not set an error handler.
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
  }

  /**
   * UPA violation in choice group.
   */
  public void testUPA_Choice_NG01() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/upa/choiceUPA_NG01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getFatalErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];

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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];

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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
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
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(8, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("cos-nonambig:"));
  }

  /**
   * Derivation by restriction. (complex type with simple content) 
   * Attribute uses are preserved, but attribute wildcard is not preserved.
   */
  public void testAttributesDerivation_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int restrictedStringItemType = corpus.getTypeOfSchema("urn:foo", "restrictedStringItemType");
    Assert.assertFalse(corpus.isSimpleType(restrictedStringItemType));
  }
  
  /**
   * Derivation by restriction. (complex type with complex content) 
   * Attribute uses are preserved, but attribute wildcard is not preserved.
   */
  public void testAttributesDerivation_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int restrictedAnySequenceType = corpus.getTypeOfSchema("urn:foo", "restrictedAnySequenceType");
    Assert.assertFalse(corpus.isSimpleType(restrictedAnySequenceType));
  }
  
  /**
   * Attribute wildcard satisfies that of the base type. (simpleContent)
   * base: namespace="##targetNamespace urn:goo" processContents="lax"
   * derived: namespace="urn:goo" processContents="lax"
   */
  public void testAttributesDerivation_03() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int stringItemType = corpus.getTypeOfSchema("urn:foo", "stringItemType");
    Assert.assertFalse(corpus.isSimpleType(stringItemType));

    int restrictedStringItemType = corpus.getTypeOfSchema("urn:foo", "restrictedStringItemType");
    Assert.assertFalse(corpus.isSimpleType(restrictedStringItemType));
  }
  
  /**
   * Attribute wildcard satisfies that of the base type. (complexContent)
   * base: namespace="##targetNamespace urn:goo" processContents="lax"
   * derived: namespace="urn:goo" processContents="lax"
   */
  public void testAttributesDerivation_04() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK04.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int AnySequenceType = corpus.getTypeOfSchema("urn:foo", "anySequenceType");
    Assert.assertFalse(corpus.isSimpleType(AnySequenceType));

    int restrictedAnySequenceType = corpus.getTypeOfSchema("urn:foo", "restrictedAnySequenceType");
    Assert.assertFalse(corpus.isSimpleType(restrictedAnySequenceType));
  }
  
  /**
   * attribute wildcard is *not* subset of that of the base type. (simpleContent by restriction)
   * base: namespace="urn:foo urn:goo" processContents="lax"
   * derived: namespace="urn:hoo" processContents="lax"
   */
  public void testAttributesDerivation_05() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationNG01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(20, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.4.2:"));
    
    int stringItemType = corpus.getTypeOfSchema("urn:foo", "stringItemType");
    Assert.assertFalse(corpus.isSimpleType(stringItemType));

    int restrictedStringItemType = corpus.getTypeOfSchema("urn:foo", "restrictedStringItemType");
    Assert.assertFalse(corpus.isSimpleType(restrictedStringItemType));
  }
  
  /**
   * attribute wildcard is *not* subset of that of the base type. (complexContent by restriction)
   * base: namespace="urn:foo urn:goo" processContents="lax"
   * derived: namespace="urn:hoo" processContents="lax"
   */
  public void testAttributesDerivation_06() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationNG02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(18, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.4.2:"));
    
    int anySequenceType = corpus.getTypeOfSchema("urn:foo", "anySequenceType");
    Assert.assertFalse(corpus.isSimpleType(anySequenceType));

    int restrictedAnySequenceType = corpus.getTypeOfSchema("urn:foo", "restrictedAnySequenceType");
    Assert.assertFalse(corpus.isSimpleType(restrictedAnySequenceType));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationNG03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int stringItemType = corpus.getTypeOfSchema("urn:foo", "stringItemType");
    Assert.assertFalse(corpus.isSimpleType(stringItemType));

    int extendedStringItemType = corpus.getTypeOfSchema("urn:foo", "extendedStringItemType");
    Assert.assertFalse(corpus.isSimpleType(extendedStringItemType));
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
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationNG04.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int anySequenceType = corpus.getTypeOfSchema("urn:foo", "anySequenceType");
    Assert.assertFalse(corpus.isSimpleType(anySequenceType));

    int extendedAnySequenceType = corpus.getTypeOfSchema("urn:foo", "extendedAnySequenceType");
    Assert.assertFalse(corpus.isSimpleType(extendedAnySequenceType));
  }
  
  /**
   * Derivation by restriction. (complex type with simple content)
   * Attributes can be mapped to the attribute wildcard in the base type. 
   */
  public void testAttributesDerivation_09() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK07.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int stringItemType = corpus.getTypeOfSchema("urn:foo", "stringItemType");
    Assert.assertFalse(corpus.isSimpleType(stringItemType));

    int restrictedStringItemType = corpus.getTypeOfSchema("urn:foo", "restrictedStringItemType");
    Assert.assertFalse(corpus.isSimpleType(restrictedStringItemType));
  }
  
  /**
   * Derivation by restriction. (complex type with simple content)
   * The attribute "lang" does not map into the attribute wildcard
   * in the base type.
   */
  public void testAttributesDerivation_10() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationNG05.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(20, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.2.2.b:"));
    
    int stringItemType = corpus.getTypeOfSchema("urn:foo", "stringItemType");
    Assert.assertFalse(corpus.isSimpleType(stringItemType));

    int restrictedStringItemType = corpus.getTypeOfSchema("urn:foo", "restrictedStringItemType");
    Assert.assertFalse(corpus.isSimpleType(restrictedStringItemType));
  }

  /**
   * Derivation by restriction. (complex type with complex content)
   * Attributes can be mapped to the attribute wildcard in the base type. 
   */
  public void testAttributesDerivation_11() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK08.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int AnySequenceType = corpus.getTypeOfSchema("urn:foo", "anySequenceType");
    Assert.assertFalse(corpus.isSimpleType(AnySequenceType));

    int restrictedAnySequenceType = corpus.getTypeOfSchema("urn:foo", "restrictedAnySequenceType");
    Assert.assertFalse(corpus.isSimpleType(restrictedAnySequenceType));
  }
  
  /**
   * Derivation by restriction. (complex type with complex content)
   * The attribute "lang" does not map into the attribute wildcard
   * in the base type.
   */
  public void testAttributesDerivation_12() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationNG06.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(18, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.2.2.b:"));
    
    int AnySequenceType = corpus.getTypeOfSchema("urn:foo", "anySequenceType");
    Assert.assertFalse(corpus.isSimpleType(AnySequenceType));

    int restrictedAnySequenceType = corpus.getTypeOfSchema("urn:foo", "restrictedAnySequenceType");
    Assert.assertFalse(corpus.isSimpleType(restrictedAnySequenceType));
  }

  /**
   * attribute wildcard is *not* subset of that of the base type. (simpleContent by restriction)
   * Note, however, it is a subset according to the spec.
   * base: namespace="##other" processContents="lax"
   * derived: namespace="##local" processContents="lax"
   */
  public void testAttributesDerivation_13() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK09.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(20, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.4.2:"));
    
    int stringItemType = corpus.getTypeOfSchema("urn:foo", "stringItemType");
    Assert.assertFalse(corpus.isSimpleType(stringItemType));

    int restrictedStringItemType = corpus.getTypeOfSchema("urn:foo", "restrictedStringItemType");
    Assert.assertFalse(corpus.isSimpleType(restrictedStringItemType));
  }

  /**
   * attribute wildcard is not subset of that of the base type. (complexContent by restriction)
   * Note, however, it is a subset according to the spec.
   * base: namespace="##other" processContents="lax"
   * derived: namespace="##local" processContents="lax"
   */
  public void testAttributesDerivation_14() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK10.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(18, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.4.2:"));
    
    int anySequenceType = corpus.getTypeOfSchema("urn:foo", "anySequenceType");
    Assert.assertFalse(corpus.isSimpleType(anySequenceType));

    int restrictedAnySequenceType = corpus.getTypeOfSchema("urn:foo", "restrictedAnySequenceType");
    Assert.assertFalse(corpus.isSimpleType(restrictedAnySequenceType));
  }

  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "strict" is stronger than "lax". 
   */
  public void testAttributesDerivation_15() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK11.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int anySequenceType = corpus.getTypeOfSchema("urn:foo", "anySequenceType");
    Assert.assertFalse(corpus.isSimpleType(anySequenceType));

    int restrictedAnySequenceType = corpus.getTypeOfSchema("urn:foo", "restrictedAnySequenceType");
    Assert.assertFalse(corpus.isSimpleType(restrictedAnySequenceType));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "skip" is weaker than "lax". 
   */
  public void testAttributesDerivation_16() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationNG07.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(17, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.4.3:"));
    
    int anySequenceType = corpus.getTypeOfSchema("urn:foo", "anySequenceType");
    Assert.assertFalse(corpus.isSimpleType(anySequenceType));

    int restrictedAnySequenceType = corpus.getTypeOfSchema("urn:foo", "restrictedAnySequenceType");
    Assert.assertFalse(corpus.isSimpleType(restrictedAnySequenceType));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "lax" is not weaker than "lax". 
   */
  public void testAttributesDerivation_17() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK12.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int anySequenceType = corpus.getTypeOfSchema("urn:foo", "anySequenceType");
    Assert.assertFalse(corpus.isSimpleType(anySequenceType));

    int restrictedAnySequenceType = corpus.getTypeOfSchema("urn:foo", "restrictedAnySequenceType");
    Assert.assertFalse(corpus.isSimpleType(restrictedAnySequenceType));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "skip" is weaker than "lax". However, because the base
   * type is ur-type, this constraint is exonerated. 
   */
  public void testAttributesDerivation_18() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK13.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int anySequenceType = corpus.getTypeOfSchema("urn:foo", "anySequenceType");
    Assert.assertFalse(corpus.isSimpleType(anySequenceType));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "lax" is stronger than "skip". 
   */
  public void testAttributesDerivation_19() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK14.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int anySequenceType = corpus.getTypeOfSchema("urn:foo", "anySequenceType");
    Assert.assertFalse(corpus.isSimpleType(anySequenceType));

    int restrictedAnySequenceType = corpus.getTypeOfSchema("urn:foo", "restrictedAnySequenceType");
    Assert.assertFalse(corpus.isSimpleType(restrictedAnySequenceType));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "strict" is stronger than "lax". 
   */
  public void testAttributesDerivation_20() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK15.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int stringItemType = corpus.getTypeOfSchema("urn:foo", "stringItemType");
    Assert.assertFalse(corpus.isSimpleType(stringItemType));

    int restrictedStringItemType = corpus.getTypeOfSchema("urn:foo", "restrictedStringItemType");
    Assert.assertFalse(corpus.isSimpleType(restrictedStringItemType));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "skip" is weaker than "lax". 
   */
  public void testAttributesDerivation_21() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationNG08.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(18, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.4.3:"));
    
    int stringItemType = corpus.getTypeOfSchema("urn:foo", "stringItemType");
    Assert.assertFalse(corpus.isSimpleType(stringItemType));

    int restrictedStringItemType = corpus.getTypeOfSchema("urn:foo", "restrictedStringItemType");
    Assert.assertFalse(corpus.isSimpleType(restrictedStringItemType));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "lax" is not weaker than "lax". 
   */
  public void testAttributesDerivation_22() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK16.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int stringItemType = corpus.getTypeOfSchema("urn:foo", "stringItemType");
    Assert.assertFalse(corpus.isSimpleType(stringItemType));

    int restrictedStringItemType = corpus.getTypeOfSchema("urn:foo", "restrictedStringItemType");
    Assert.assertFalse(corpus.isSimpleType(restrictedStringItemType));
  }
  
  /**
   * The processContent of the wildcard in type derived by restriction
   * from another must not be weaker than that of the wildcard in the
   * base type. "lax" is stronger than "skip". 
   */
  public void testAttributesDerivation_23() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK17.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int stringItemType = corpus.getTypeOfSchema("urn:foo", "stringItemType");
    Assert.assertFalse(corpus.isSimpleType(stringItemType));

    int restrictedStringItemType = corpus.getTypeOfSchema("urn:foo", "restrictedStringItemType");
    Assert.assertFalse(corpus.isSimpleType(restrictedStringItemType));
  }
  
  /**
   * Attributes that are *required* in the base type cannot be "prohibited"
   * in the derived type. 
   */
  public void testAttributesDerivation_24() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationOK18.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int stringItemType, restrictedStringItemType;
    
    stringItemType = corpus.getTypeOfSchema("urn:foo", "stringItemType");
    Assert.assertFalse(corpus.isSimpleType(stringItemType));

    restrictedStringItemType = corpus.getTypeOfSchema("urn:foo", "restrictedStringItemType");
    Assert.assertFalse(corpus.isSimpleType(restrictedStringItemType));

    corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/attributesDerivationNG09.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(19, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.3:"));
    
    stringItemType = corpus.getTypeOfSchema("urn:foo", "stringItemType");
    Assert.assertFalse(corpus.isSimpleType(stringItemType));

    restrictedStringItemType = corpus.getTypeOfSchema("urn:foo", "restrictedStringItemType");
    Assert.assertFalse(corpus.isSimpleType(restrictedStringItemType));
  }
  
  /**
   * There is a particle in sequence B that does not map into sequence A.
   */
  public void testSequenceParticleRestriction_01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionSequenceOK01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/particleRestrictionSequenceNG01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-Recurse.2:"));

    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int typeA = corpus.getTypeOfSchema("urn:foo", "A");
    Assert.assertFalse(corpus.isSimpleType(typeA));

    int typeB = corpus.getTypeOfSchema("urn:foo", "B");
    Assert.assertFalse(corpus.isSimpleType(typeB));
  }
  
  /**
   * There is a particle in sequence A that was not mapped to by
   * any particles in sequence B.
   */
  public void testSequenceParticleRestriction_02() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionSequenceOK02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/particleRestrictionSequenceNG02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-Recurse.2:"));

    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int typeA = corpus.getTypeOfSchema("urn:foo", "A");
    Assert.assertFalse(corpus.isSimpleType(typeA));

    int typeB = corpus.getTypeOfSchema("urn:foo", "B");
    Assert.assertFalse(corpus.isSimpleType(typeB));
  }

  /**
   * There is a particle in sequence A that was not mapped to by
   * any particles in sequence B. The unmapped particle locates
   * the last among the particles of the sequence A.
   */
  public void testSequenceParticleRestriction_03() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionSequenceOK03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/particleRestrictionSequenceNG03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-Recurse.2:"));
    
    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int typeA = corpus.getTypeOfSchema("urn:foo", "A");
    Assert.assertFalse(corpus.isSimpleType(typeA));

    int typeB = corpus.getTypeOfSchema("urn:foo", "B");
    Assert.assertFalse(corpus.isSimpleType(typeB));
  }

  /**
   * There is a particle in "all" group B that does not map into "all" group A.
   */
  public void testAllParticleRestriction_01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAllOK01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/particleRestrictionAllNG01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-Recurse.2:"));

    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int typeA = corpus.getTypeOfSchema("urn:foo", "A");
    Assert.assertFalse(corpus.isSimpleType(typeA));

    int typeB = corpus.getTypeOfSchema("urn:foo", "B");
    Assert.assertFalse(corpus.isSimpleType(typeB));
  }

  /**
   * There is a particle in "all" group A that was not mapped to by
   * any particles in "all" group B.
   */
  public void testAllParticleRestriction_02() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAllOK02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/particleRestrictionAllNG02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-Recurse.2:"));

    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int typeA = corpus.getTypeOfSchema("urn:foo", "A");
    Assert.assertFalse(corpus.isSimpleType(typeA));

    int typeB = corpus.getTypeOfSchema("urn:foo", "B");
    Assert.assertFalse(corpus.isSimpleType(typeB));
  }

  /**
   * There is a particle in "all" group A that was not mapped to by
   * any particles in "all" group B. The unmapped particle locates
   * the last among the particles of the "all" group A.
   */
  public void testAllParticleRestriction_03() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAllOK03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/particleRestrictionAllNG03.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-Recurse.2:"));

    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(15, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int typeA = corpus.getTypeOfSchema("urn:foo", "A");
    Assert.assertFalse(corpus.isSimpleType(typeA));

    int typeB = corpus.getTypeOfSchema("urn:foo", "B");
    Assert.assertFalse(corpus.isSimpleType(typeB));
  }

  /**
   * Constraint on processContent of two wildcards, one derived by
   * restriction from another. "skip" is not permitted because it is
   * weaker than "lax".
   */
  public void testAnyParticleRestriction_01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAnyOK01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/particleRestrictionAnyNG01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-NSSubset.3:"));
    
    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int typeA = corpus.getTypeOfSchema("urn:foo", "A");
    Assert.assertFalse(corpus.isSimpleType(typeA));

    int typeB = corpus.getTypeOfSchema("urn:foo", "B");
    Assert.assertFalse(corpus.isSimpleType(typeB));
  }

  /**
   * Constraint on processContent of two wildcards, one derived by
   * restriction from another. "lax" is not permitted because it is
   * weaker than "strict".
   */
  public void testAnyParticleRestriction_02() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/particleRestrictionAnyOK02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/particleRestrictionAnyNG02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("rcase-NSSubset.3:"));
    
    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(13, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));
    
    int typeA = corpus.getTypeOfSchema("urn:foo", "A");
    Assert.assertFalse(corpus.isSimpleType(typeA));

    int typeB = corpus.getTypeOfSchema("urn:foo", "B");
    Assert.assertFalse(corpus.isSimpleType(typeB));
  }
 
  /**
   * Use of XML Schema for schema.
   * Load a schema that includes "Schema for schema".
   */
  public void testUseXMLSchema4Schema_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/useSchema4Schema1.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    Assert.assertEquals(4, corpus.uris.length);
    Assert.assertEquals(XMLSCHEMA_URI, corpus.uris[3]); 
    
    int eA = corpus.getGlobalElemOfSchema(XMLSCHEMA_URI, "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
    
    // REVISIT: It should be 46
    Assert.assertEquals(92, EXISchemaUtil.getTypeCountOfSchema(XMLSCHEMA_URI, corpus));

    int tA = corpus.getTypeOfSchema(XMLSCHEMA_URI, "tA");
    Assert.assertFalse(corpus.isSimpleType(tA));
  }

  /**
   * Use of XML Schema for schema.
   * Load a schema that uses "Schema for schema".
   * The import statement has a valid schemaLocation. 
   */
  public void testUseXMLSchema4Schema_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/useSchema4Schema2.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    Assert.assertEquals(5, corpus.uris.length);
    Assert.assertEquals(XMLSCHEMA_URI, corpus.uris[3]); 
    Assert.assertEquals("urn:foo", corpus.uris[4]); 

    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals("A", corpus.getNameOfElem(eA));
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
      m_compilerErrorHandler.getErrors(EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(2, errorList.length);

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException spe = (XMLParseException)sce.getException();
    Assert.assertEquals(9, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("src-ct.2.1:"));
    
    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];
    spe = (XMLParseException)sce.getException();
    Assert.assertEquals(24, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("e-props-correct.4:"));
  }

  /**
   * SimpleContent restricting another SimpleContent.
   */
  public void testSimpleContentRestriction_01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema(
        "/simpleContentRestriction_01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int stringItemType = schema.getTypeOfSchema("urn:foo", "stringItemType");
    Assert.assertTrue(stringItemType != EXISchema.NIL_NODE);
    
    int explicitDomainType = schema.getTypeOfSchema("urn:foo", "explicitDomainType");
    Assert.assertTrue(explicitDomainType != EXISchema.NIL_NODE);
  }

  /**
   * Non-deterministic context of a particle.
   * Inner loop is stateful.
   */
  public void testAmbiguousParticleContext01() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/ambiguousParticleContext01.xsd",
                                           getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
  }

  /**
   * Non-deterministic context of a particle.
   * Outer loop is stateful.
   */
  public void testAmbiguousParticleContext02() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema("/ambiguousParticleContext02.xsd",
                                           getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
  }

  /**
   * Tests integral types that have non-negative ranges.
   */
  public void testIntegralRangeNonNegative_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/decimalRange.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int nonNegativeInteger = corpus.getTypeOfSchema(XMLSCHEMA_URI, "nonNegativeInteger");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeInteger));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeInteger));
    
    int unsignedLong = corpus.getTypeOfSchema(XMLSCHEMA_URI, "unsignedLong");
    Assert.assertTrue(corpus.isIntegralSimpleType(unsignedLong));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(unsignedLong));

    int positiveInteger = corpus.getTypeOfSchema(XMLSCHEMA_URI, "positiveInteger");
    Assert.assertTrue(corpus.isIntegralSimpleType(positiveInteger));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(positiveInteger));

    int unsignedInt = corpus.getTypeOfSchema(XMLSCHEMA_URI, "unsignedInt");
    Assert.assertTrue(corpus.isIntegralSimpleType(unsignedInt));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(unsignedInt));

    int unsignedShort = corpus.getTypeOfSchema(XMLSCHEMA_URI, "unsignedShort");
    Assert.assertTrue(corpus.isIntegralSimpleType(unsignedShort));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(unsignedShort));

    int nonNegativeInt_a = corpus.getTypeOfSchema("urn:foo", "nonNegativeInt_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeInt_a));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeInt_a));

    int nonNegativeInt_b = corpus.getTypeOfSchema("urn:foo", "nonNegativeInt_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeInt_b));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeInt_b));
    
    int nonNegativeLong_a = corpus.getTypeOfSchema("urn:foo", "nonNegativeLong_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeLong_a));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeLong_a));
    
    int nonNegativeLong_b = corpus.getTypeOfSchema("urn:foo", "nonNegativeLong_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeLong_b));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeLong_b));

    int nonNegativeLong_c = corpus.getTypeOfSchema("urn:foo", "nonNegativeLong_c");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeLong_c));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeLong_c));
    
    int nonNegativeInteger_a = corpus.getTypeOfSchema("urn:foo", "nonNegativeInteger_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeInteger_a));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeInteger_a));
    
    int nonNegativeInteger_b = corpus.getTypeOfSchema("urn:foo", "nonNegativeInteger_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeInteger_b));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeInteger_b));

    int nonNegativeInteger_c = corpus.getTypeOfSchema("urn:foo", "nonNegativeInteger_c");
    Assert.assertTrue(corpus.isIntegralSimpleType(nonNegativeInteger_c));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_NONNEGATIVE, 
        corpus.getWidthOfIntegralSimpleType(nonNegativeInteger_c));
  }

  /**
   * Tests integral types that have no significant ranges.
   */
  public void testIntegralRangeNone() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/decimalRange.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int noRangeInt_a = corpus.getTypeOfSchema("urn:foo", "noRangeInt_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(noRangeInt_a));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_DEFAULT, 
        corpus.getWidthOfIntegralSimpleType(noRangeInt_a));

    int noRangeInt_b = corpus.getTypeOfSchema("urn:foo", "noRangeInt_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(noRangeInt_b));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_DEFAULT, 
        corpus.getWidthOfIntegralSimpleType(noRangeInt_b));
    
    int noRangeLong_a = corpus.getTypeOfSchema("urn:foo", "noRangeLong_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(noRangeLong_a));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_DEFAULT, 
        corpus.getWidthOfIntegralSimpleType(noRangeLong_a));

    int noRangeLong_b = corpus.getTypeOfSchema("urn:foo", "noRangeLong_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(noRangeLong_b));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_DEFAULT, 
        corpus.getWidthOfIntegralSimpleType(noRangeLong_b));
    
    int noRangeInteger_a = corpus.getTypeOfSchema("urn:foo", "noRangeInteger_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(noRangeInteger_a));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_DEFAULT, 
        corpus.getWidthOfIntegralSimpleType(noRangeInteger_a));

    int noRangeInteger_b = corpus.getTypeOfSchema("urn:foo", "noRangeInteger_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(noRangeInteger_b));
    Assert.assertEquals(EXISchema.INTEGER_CODEC_DEFAULT, 
        corpus.getWidthOfIntegralSimpleType(noRangeInteger_b));
  }

  /**
   * Tests integral types that ends up having n-bit ranges.
   */
  public void testIntegralRangeNbits() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/decimalRange.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int unsignedByte = corpus.getTypeOfSchema(XMLSCHEMA_URI, "unsignedByte");
    Assert.assertTrue(corpus.isIntegralSimpleType(unsignedByte));
    Assert.assertEquals(8, corpus.getWidthOfIntegralSimpleType(unsignedByte));

    int _byte = corpus.getTypeOfSchema(XMLSCHEMA_URI, "byte");
    Assert.assertTrue(corpus.isIntegralSimpleType(_byte));
    Assert.assertEquals(8, corpus.getWidthOfIntegralSimpleType(_byte));

    int nbitsRangeInt_a = corpus.getTypeOfSchema("urn:foo", "nbitsRangeInt_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInt_a));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeInt_a));
    
    int nbitsRangeInt_b = corpus.getTypeOfSchema("urn:foo", "nbitsRangeInt_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInt_b));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeInt_b));
    
    int nbitsRangeInt_c = corpus.getTypeOfSchema("urn:foo", "nbitsRangeInt_c");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInt_c));
    Assert.assertEquals(11, corpus.getWidthOfIntegralSimpleType(nbitsRangeInt_c));
    
    int nbitsRangeInt_d = corpus.getTypeOfSchema("urn:foo", "nbitsRangeInt_d");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInt_d));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeInt_d));
    
    int nbitsRangeInt_e = corpus.getTypeOfSchema("urn:foo", "nbitsRangeInt_e");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInt_e));
    Assert.assertEquals(1, corpus.getWidthOfIntegralSimpleType(nbitsRangeInt_e));
    
    int nbitsRangeInt_f = corpus.getTypeOfSchema("urn:foo", "nbitsRangeInt_f");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInt_f));
    Assert.assertEquals(0, corpus.getWidthOfIntegralSimpleType(nbitsRangeInt_f));

    int nbitsRangeLong_a = corpus.getTypeOfSchema("urn:foo", "nbitsRangeLong_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeLong_a));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeLong_a));
    
    int nbitsRangeLong_b = corpus.getTypeOfSchema("urn:foo", "nbitsRangeLong_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeLong_b));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeLong_b));
    
    int nbitsRangeLong_c = corpus.getTypeOfSchema("urn:foo", "nbitsRangeLong_c");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeLong_c));
    Assert.assertEquals(11, corpus.getWidthOfIntegralSimpleType(nbitsRangeLong_c));
    
    int nbitsRangeLong_d = corpus.getTypeOfSchema("urn:foo", "nbitsRangeLong_d");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeLong_d));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeLong_d));
    
    int nbitsRangeLong_e = corpus.getTypeOfSchema("urn:foo", "nbitsRangeLong_e");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeLong_e));
    Assert.assertEquals(1, corpus.getWidthOfIntegralSimpleType(nbitsRangeLong_e));
    
    int nbitsRangeLong_f = corpus.getTypeOfSchema("urn:foo", "nbitsRangeLong_f");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeLong_f));
    Assert.assertEquals(0, corpus.getWidthOfIntegralSimpleType(nbitsRangeLong_f));
    
    int nbitsRangeLong_g = corpus.getTypeOfSchema("urn:foo", "nbitsRangeLong_g");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeLong_g));
    Assert.assertEquals(9, corpus.getWidthOfIntegralSimpleType(nbitsRangeLong_g));

    int nbitsRangeInteger_a = corpus.getTypeOfSchema("urn:foo", "nbitsRangeInteger_a");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInteger_a));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeInteger_a));
    
    int nbitsRangeInteger_b = corpus.getTypeOfSchema("urn:foo", "nbitsRangeInteger_b");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInteger_b));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeInteger_b));
    
    int nbitsRangeInteger_c = corpus.getTypeOfSchema("urn:foo", "nbitsRangeInteger_c");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInteger_c));
    Assert.assertEquals(11, corpus.getWidthOfIntegralSimpleType(nbitsRangeInteger_c));
    
    int nbitsRangeInteger_d = corpus.getTypeOfSchema("urn:foo", "nbitsRangeInteger_d");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInteger_d));
    Assert.assertEquals(12, corpus.getWidthOfIntegralSimpleType(nbitsRangeInteger_d));
    
    int nbitsRangeInteger_e = corpus.getTypeOfSchema("urn:foo", "nbitsRangeInteger_e");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInteger_e));
    Assert.assertEquals(1, corpus.getWidthOfIntegralSimpleType(nbitsRangeInteger_e));
    
    int nbitsRangeInteger_f = corpus.getTypeOfSchema("urn:foo", "nbitsRangeInteger_f");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInteger_f));
    Assert.assertEquals(0, corpus.getWidthOfIntegralSimpleType(nbitsRangeInteger_f));
    
    int nbitsRangeInteger_g = corpus.getTypeOfSchema("urn:foo", "nbitsRangeInteger_g");
    Assert.assertTrue(corpus.isIntegralSimpleType(nbitsRangeInteger_g));
    Assert.assertEquals(8, corpus.getWidthOfIntegralSimpleType(nbitsRangeInteger_g));
  }

  /**
   * Test INODE_ISSPECIFIC_IN_FRAGMENT_MASK flag of elements and attributes.
   */
  public void testFragment_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/fragment_01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
        
    int[] fragmentINodes = corpus.getFragmentINodes();
    Assert.assertEquals(10, fragmentINodes.length);
    Assert.assertEquals(7, corpus.getFragmentElemCount());

    String[] uris = corpus.uris;

    int fragmentINode;
    int ind = 0;
    
    fragmentINode = fragmentINodes[ind++];
    Assert.assertTrue(fragmentINode >= 0);
    Assert.assertEquals("A", corpus.getNameOfElem(fragmentINode));
    Assert.assertEquals("urn:goo", uris[corpus.getUriOfElem(fragmentINode)]); 

    fragmentINode = fragmentINodes[ind++];
    Assert.assertTrue(fragmentINode >= 0);
    Assert.assertEquals("A_", corpus.getNameOfElem(fragmentINode));
    Assert.assertEquals("urn:foo", uris[corpus.getUriOfElem(fragmentINode)]);

    // Definitions of element "A" in "urn:goo" are the same, but they independently defines their types.
    fragmentINode = fragmentINodes[ind++];
    Assert.assertFalse(fragmentINode >= 0);
    fragmentINode = ~fragmentINode;
    Assert.assertEquals("A_", corpus.getNameOfElem(fragmentINode));
    Assert.assertEquals("urn:goo", uris[corpus.getUriOfElem(fragmentINode)]);
    
    fragmentINode = fragmentINodes[ind++];
    Assert.assertTrue(fragmentINode >= 0);
    Assert.assertEquals("A__", corpus.getNameOfElem(fragmentINode));
    Assert.assertEquals("urn:goo", uris[corpus.getUriOfElem(fragmentINode)]);

    // two "B" declarations use different nillable values.
    fragmentINode = fragmentINodes[ind++];
    Assert.assertFalse(fragmentINode >= 0);
    fragmentINode = ~fragmentINode;
    Assert.assertEquals("B", corpus.getNameOfElem(fragmentINode));
    Assert.assertEquals("urn:foo", uris[corpus.getUriOfElem(fragmentINode)]);

    fragmentINode = fragmentINodes[ind++];
    Assert.assertFalse(fragmentINode >= 0);
    fragmentINode = ~fragmentINode;
    Assert.assertEquals("Z", corpus.getNameOfElem(fragmentINode));
    Assert.assertEquals("urn:foo", uris[corpus.getUriOfElem(fragmentINode)]);

    fragmentINode = fragmentINodes[ind++];
    Assert.assertTrue(fragmentINode >= 0);
    Assert.assertEquals("Z", corpus.getNameOfElem(fragmentINode));
    Assert.assertEquals("urn:goo", uris[corpus.getUriOfElem(fragmentINode)]);

    fragmentINode = fragmentINodes[ind++];
    Assert.assertFalse(fragmentINode >= 0);
    fragmentINode = ~fragmentINode;
    Assert.assertEquals("a", EXISchemaUtil.getNameOfAttr(fragmentINode, corpus));
    Assert.assertEquals("urn:goo", uris[corpus.getUriOfAttr(fragmentINode)]); 

    fragmentINode = fragmentINodes[ind++];
    Assert.assertTrue(fragmentINode >= 0);
    Assert.assertEquals("b", EXISchemaUtil.getNameOfAttr(fragmentINode, corpus));
    Assert.assertEquals("urn:foo", uris[corpus.getUriOfAttr(fragmentINode)]); 

    fragmentINode = fragmentINodes[ind++];
    Assert.assertTrue(fragmentINode >= 0);
    Assert.assertEquals("c", EXISchemaUtil.getNameOfAttr(fragmentINode, corpus));
    Assert.assertEquals("urn:goo", uris[corpus.getUriOfAttr(fragmentINode)]); 
  }

  /**
   * schemaLocation is bogus.
   * <xsd:import namespace="urn:goo" schemaLocation="none"/>
   */
  public void testImportReferenceNG_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/importReferenceNG_01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertNotNull(corpus);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    Assert.assertEquals(1, m_compilerErrorHandler.getWarningCount());

    EXISchemaFactoryException[] errorList = m_compilerErrorHandler.getWarnings(
        EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("schema_reference.4:"));

    Assert.assertEquals(5, corpus.uris.length);
    Assert.assertEquals("", corpus.uris[0]); 
    Assert.assertEquals("http://www.w3.org/XML/1998/namespace", corpus.uris[1]); 
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", corpus.uris[2]); 
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", corpus.uris[3]); 
    Assert.assertEquals("urn:foo", corpus.uris[4]); 
  }

  /**
   * Bogus schemaLocation is resolved via EntityResolverEx.
   * <xsd:import namespace="urn:goo" schemaLocation="none"/>
   */
  public void testImportReferenceNG_01_EntityResolver_01() throws Exception {
    final InputSource inputSource = new InputSource();
    
    final EntityResolverEx entityResolver = new EntityResolverEx() {
      public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        Assert.fail();
        return null;
      }
      public InputSource resolveEntity(String publicId, String systemId, String uri) throws SAXException, IOException {
        Assert.assertTrue(systemId.endsWith("/none"));
        Assert.assertEquals("urn:goo", uri);
        URI baseUri;
        try {
          baseUri = new URI(systemId);
        }
        catch (URISyntaxException e) {
          return null;
        }
        URI entityUri = baseUri.resolve("booleanImported.xsd");
        inputSource.setSystemId(entityUri.toString());
        return inputSource;
      }
    };
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/importReferenceNG_01.xsd", 
        getClass(), new EXISchemaFactoryTestUtilContext(m_compilerErrorHandler, entityResolver));

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    Assert.assertTrue(inputSource.getSystemId().endsWith("/booleanImported.xsd"));
    
    Assert.assertEquals(6, corpus.uris.length);
    Assert.assertEquals("", corpus.uris[0]); 
    Assert.assertEquals("http://www.w3.org/XML/1998/namespace", corpus.uris[1]); 
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", corpus.uris[2]); 
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", corpus.uris[3]); 
    Assert.assertEquals("urn:foo", corpus.uris[4]); 
    Assert.assertEquals("urn:goo", corpus.uris[5]); 
  }

  /**
   * Bogus schemaLocation is resolved via EntityResolverEx.
   * <xsd:import namespace="urn:goo" schemaLocation="none"/>
   * 
   * Set a bogus systemId in the returned InputSource.
   */
  public void testImportReferenceNG_01_EntityResolver_02() throws Exception {
    final InputSource inputSource = new InputSource();
    
    final EntityResolverEx entityResolver = new EntityResolverEx() {
      public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        Assert.fail();
        return null;
      }
      public InputSource resolveEntity(String publicId, String systemId, String uri) throws SAXException, IOException {
        Assert.assertTrue(systemId.endsWith("/none"));
        Assert.assertEquals("urn:goo", uri);
        URI baseUri;
        try {
          baseUri = new URI(systemId);
        }
        catch (URISyntaxException e) {
          return null;
        }
        URI entityUri = baseUri.resolve("booleanImported.xsd");
        inputSource.setByteStream(entityUri.toURL().openStream());
        inputSource.setSystemId("http://zzzzzzzzzz/bogus");
        return inputSource;
      }
    };
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/importReferenceNG_01.xsd", 
        getClass(), new EXISchemaFactoryTestUtilContext(m_compilerErrorHandler, entityResolver));

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    Assert.assertEquals("http://zzzzzzzzzz/bogus", inputSource.getSystemId());
    
    Assert.assertEquals(6, corpus.uris.length);
    Assert.assertEquals("", corpus.uris[0]); 
    Assert.assertEquals("http://www.w3.org/XML/1998/namespace", corpus.uris[1]); 
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", corpus.uris[2]); 
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", corpus.uris[3]); 
    Assert.assertEquals("urn:foo", corpus.uris[4]); 
    Assert.assertEquals("urn:goo", corpus.uris[5]); 
  }

  /**
   * schemaLocation is bogus.
   * <xsd:import schemaLocation="none"/>
   */
  public void testImportReferenceNG_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/importReferenceNG_02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    Assert.assertEquals(1, m_compilerErrorHandler.getWarningCount());

    EXISchemaFactoryException[] errorList = m_compilerErrorHandler.getWarnings(
        EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    Assert.assertEquals(7, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("schema_reference.4:"));

    Assert.assertEquals(5, corpus.uris.length);
    Assert.assertEquals("", corpus.uris[0]); 
    Assert.assertEquals("http://www.w3.org/XML/1998/namespace", corpus.uris[1]); 
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", corpus.uris[2]); 
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", corpus.uris[3]); 
    Assert.assertEquals("urn:foo", corpus.uris[4]); 
  }

  /**
   * Bogus schemaLocation is resolved via EntityResolverEx.
   * <xsd:import schemaLocation="none"/>
   */
  public void testImportReferenceNG_02_EntityResolver() throws Exception {
    final InputSource inputSource = new InputSource();
    
    final EntityResolverEx entityResolver = new EntityResolverEx() {
      public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        Assert.fail();
        return null;
      }
      public InputSource resolveEntity(String publicId, String systemId, String uri) throws SAXException, IOException {
        Assert.assertTrue(systemId.endsWith("/none"));
        Assert.assertEquals("", uri);
        URI baseUri;
        try {
          baseUri = new URI(systemId);
        }
        catch (URISyntaxException e) {
          return null;
        }
        URI entityUri = baseUri.resolve("verySimpleDefault.xsd");
        inputSource.setSystemId(entityUri.toString());
        return inputSource;
      }
    };
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/importReferenceNG_02.xsd", 
        getClass(), new EXISchemaFactoryTestUtilContext(m_compilerErrorHandler, entityResolver));

    Assert.assertNotNull(corpus);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    Assert.assertTrue(inputSource.getSystemId().endsWith("/verySimpleDefault.xsd"));
    
    Assert.assertEquals(5, corpus.uris.length);
    Assert.assertEquals("", corpus.uris[0]); 
    Assert.assertEquals("http://www.w3.org/XML/1998/namespace", corpus.uris[1]); 
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema-instance", corpus.uris[2]); 
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", corpus.uris[3]); 
    Assert.assertEquals("urn:foo", corpus.uris[4]); 
  }

  /**
   * Feed multiple schemas into EXISchemaFactory.
   * The second schema appears to get ignored.
   * This is probably because the two schemas are for the same target namespace. 
   */
  public void testConflictingSchemas01() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema(
        new String[] {"/conflictingSchemas/foo1a.xsd", "/conflictingSchemas/foo1b.xsd"}, 
        getClass(), new EXISchemaFactoryTestUtilContext(m_compilerErrorHandler));
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int foo_A = schema.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertTrue(EXISchema.NIL_NODE != foo_A);
    int foo_C = schema.getGlobalElemOfSchema("urn:foo", "C");
    Assert.assertTrue(EXISchema.NIL_NODE != foo_C);
    int foo_B = schema.getGlobalElemOfSchema("urn:foo", "B");
    Assert.assertEquals(EXISchema.NIL_NODE, foo_B);
    
    int type_C = schema.getTypeOfElem(foo_C);
    Assert.assertEquals(EXISchema.NIL_NODE, EXISchemaUtil.getContentDataTypeOfType(type_C, schema)); 
  }

  /**
   * Feed multiple schemas into EXISchemaFactory.
   * The second schema appears to get ignored.
   * This is probably because the two schemas are for the same target namespace. 
   */
  public void testConflictingSchemas02() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema(
        new String[] {"/conflictingSchemas/foo1b.xsd", "/conflictingSchemas/foo1a.xsd"}, 
        getClass(), new EXISchemaFactoryTestUtilContext(m_compilerErrorHandler));
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int foo_B = schema.getGlobalElemOfSchema("urn:foo", "B");
    Assert.assertTrue(EXISchema.NIL_NODE != foo_B);
    int foo_C = schema.getGlobalElemOfSchema("urn:foo", "C");
    Assert.assertTrue(EXISchema.NIL_NODE != foo_C);
    int foo_A = schema.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertEquals(EXISchema.NIL_NODE, foo_A);
    
    int type_C = schema.getTypeOfElem(foo_C);
    Assert.assertTrue(EXISchema.NIL_NODE != EXISchemaUtil.getContentDataTypeOfType(type_C, schema)); 
  }

  /**
   * Feed multiple schemas into EXISchemaFactory.
   * Both schemas appear to be loaded, however, two different declarations for 
   * element "C" are not considered an error.
   */
  public void testConflictingSchemas03() throws Exception {
    EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema(
        new String[] {"/conflictingSchemas/foo1a.xsd", "/conflictingSchemas/foo1b_wrapped.xsd"}, 
        getClass(), new EXISchemaFactoryTestUtilContext(m_compilerErrorHandler));
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int foo_A = schema.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertTrue(EXISchema.NIL_NODE != foo_A);
    int foo_B = schema.getGlobalElemOfSchema("urn:foo", "B");
    Assert.assertTrue(EXISchema.NIL_NODE != foo_B);
    int foo_C = schema.getGlobalElemOfSchema("urn:foo", "C");
    Assert.assertTrue(EXISchema.NIL_NODE != foo_C);
    
    int type_C = schema.getTypeOfElem(foo_C);
    Assert.assertEquals(EXISchema.NIL_NODE, EXISchemaUtil.getContentDataTypeOfType(type_C, schema));
  }

  /**
   * Use hub schema to feed multiple schemas.
   * Two different declarations for element "C" are detected to be an error.
   */
  public void testConflictingSchemas04() throws Exception {
    EXISchemaFactoryTestUtil.getEXISchema(
        "/conflictingSchemas/hub.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException[] errorList = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR);
    Assert.assertEquals(1, errorList.length);
    
    XMLParseException spe = (XMLParseException)errorList[0].getException();
    String systemId = spe.getExpandedSystemId();
    Assert.assertTrue(systemId.endsWith("foo1a.xsd"));
    Assert.assertEquals(14, spe.getLineNumber());
    Assert.assertTrue(spe.getMessage().startsWith("sch-props-correct.2:"));
  }

  /**
   * Complex types with empty content model share the same content grammar.
   */
  public void testEmptyContentGrammar01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/emptyContent01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    Assert.assertEquals(2, EXISchemaUtil.countTypesOfSchema(corpus, true) - EXISchemaConst.N_BUILTIN_TYPES);
    
    int emptyContent1 = corpus.getTypeOfSchema("urn:foo", "emptyContent1");
    Assert.assertTrue(EXISchema.NIL_NODE != emptyContent1);
    int gramEmptyContent1 = corpus.getGrammarOfType(emptyContent1);
    int contentGrammar1 = corpus.getContentGrammarOfGrammar(gramEmptyContent1);
    Assert.assertTrue(EXISchema.NIL_GRAM != contentGrammar1);

    int emptyContent2 = corpus.getTypeOfSchema("urn:foo", "emptyContent2");
    Assert.assertTrue(EXISchema.NIL_NODE != emptyContent2);
    int gramEmptyContent2 = corpus.getGrammarOfType(emptyContent2);
    int contentGrammar2 = corpus.getContentGrammarOfGrammar(gramEmptyContent2);
    Assert.assertTrue(EXISchema.NIL_GRAM != contentGrammar2);

    Assert.assertEquals(contentGrammar1, contentGrammar2);
  }

  /**
   * Complex types with empty content model share the same content grammar.
   */
  public void testEmptyContentGrammar02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/emptyContent02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int tB = corpus.getTypeOfSchema("urn:foo", "B");
    Assert.assertTrue(EXISchema.NIL_NODE != tB);
    int gB = corpus.getGrammarOfType(tB);
    int gB_empty = corpus.getTypeEmptyGrammarOfGrammar(gB);
    Assert.assertTrue(EXISchema.NIL_GRAM != gB_empty);
    Assert.assertEquals(gB_empty, gB);
  }

  /**
   * Complex types with simple content model share the same content grammar.
   */
  public void testSimpleContentGrammar01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/simpleContent.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    Assert.assertEquals(2, EXISchemaUtil.countTypesOfSchema(corpus, true) - EXISchemaConst.N_BUILTIN_TYPES);
    
    int simpleContent1 = corpus.getTypeOfSchema("urn:foo", "simpleContent1");
    Assert.assertTrue(EXISchema.NIL_NODE != simpleContent1);
    int gramSimpleContent1 = corpus.getGrammarOfType(simpleContent1);
    int contentGrammar1 = corpus.getContentGrammarOfGrammar(gramSimpleContent1);
    Assert.assertTrue(EXISchema.NIL_GRAM != contentGrammar1);

    int simpleContent2 = corpus.getTypeOfSchema("urn:foo", "simpleContent2");
    Assert.assertTrue(EXISchema.NIL_NODE != simpleContent2);
    int gramSimpleContent2 = corpus.getGrammarOfType(simpleContent2);
    int contentGrammar2 = corpus.getContentGrammarOfGrammar(gramSimpleContent2);
    Assert.assertTrue(EXISchema.NIL_GRAM != contentGrammar2);

    Assert.assertEquals(contentGrammar1, contentGrammar2);
  }

  /**
   * Grammar optimization for element particle with maxOccurs="unbounded".
   */
  public void testGrammarElementParticleUnbounded01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/elementParticleUnbounded.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int tA = corpus.getTypeOfSchema("", "A");
    Assert.assertTrue(EXISchema.NIL_NODE != tA);
    int g0 = corpus.getGrammarOfType(tA);
    int p0_g0 = corpus.getProductionOfGrammar(g0, 0);
    int event_p0_g0 = corpus.getEventOfProduction(p0_g0);
    Assert.assertEquals(EXISchema.EVENT_TYPE_AT, corpus.getEventType(event_p0_g0));

    // minOccurs is 1
    int g1 = corpus.getGrammarOfProduction(p0_g0);
    int p0_g1 = corpus.getProductionOfGrammar(g1, 0);
    int event_p0_g1 = corpus.getEventOfProduction(p0_g1);
    Assert.assertEquals(EXISchema.EVENT_TYPE_SE, corpus.getEventType(event_p0_g1));
    
    // maxOccurs is unbounded
    int g2 = corpus.getGrammarOfProduction(p0_g1);
    int p0_g2 = corpus.getProductionOfGrammar(g2, 0);
    int event_p0_g2 = corpus.getEventOfProduction(p0_g1);
    Assert.assertEquals(EXISchema.EVENT_TYPE_SE, corpus.getEventType(event_p0_g2));
    // Make sure it comes back to itself.
    Assert.assertEquals(g2, corpus.getGrammarOfProduction(p0_g2));
  }

  /**
   * Grammar optimization by eliminating proxy grammars.
   */
  public void testProxyGrammarElimination01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/sequenceParticleUnbounded.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int tA = corpus.getTypeOfSchema("", "A");
    Assert.assertTrue(EXISchema.NIL_NODE != tA);
    int g0 = corpus.getGrammarOfType(tA);
    int p0_g0 = corpus.getProductionOfGrammar(g0, 0);
    int event_p0_g0 = corpus.getEventOfProduction(p0_g0);
    Assert.assertEquals(EXISchema.EVENT_TYPE_AT, corpus.getEventType(event_p0_g0));

    // minOccurs is 1
    int g1 = corpus.getGrammarOfProduction(p0_g0);
    int p0_g1 = corpus.getProductionOfGrammar(g1, 0);
    int event_p0_g1 = corpus.getEventOfProduction(p0_g1);
    Assert.assertEquals(EXISchema.EVENT_TYPE_SE, corpus.getEventType(event_p0_g1));
    
    // maxOccurs is unbounded
    int g2 = corpus.getGrammarOfProduction(p0_g1);
    int p0_g2 = corpus.getProductionOfGrammar(g2, 0);
    int event_p0_g2 = corpus.getEventOfProduction(p0_g2);
    Assert.assertEquals(EXISchema.EVENT_TYPE_SE, corpus.getEventType(event_p0_g2));
    // Make sure it comes back to itself.
    Assert.assertEquals(g2, corpus.getGrammarOfProduction(p0_g2));
  }

  /**
   * Grammar optimization by sharing a step grammar to the empty content grammar.
   */
  public void testEmptyGrammarSharing01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/emptyGrammarSharing01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int tA = corpus.getTypeOfSchema("", "A");
    Assert.assertTrue(EXISchema.NIL_NODE != tA);
    int g0_A = corpus.getGrammarOfType(tA);
    Assert.assertTrue(corpus.hasEmptyGrammar(g0_A));
    g0_A = corpus.getTypeEmptyGrammarOfGrammar(g0_A);
    Assert.assertEquals(1, corpus.getProductionCountOfGrammar(g0_A));
    int p0_g0_A = corpus.getProductionOfGrammar(g0_A, 0);
    int event_p0_g0_A = corpus.getEventOfProduction(p0_g0_A);
    Assert.assertEquals(EXISchema.EVENT_TYPE_AT, corpus.getEventType(event_p0_g0_A));

    int g1_A = corpus.getGrammarOfProduction(p0_g0_A);
    Assert.assertEquals(0, corpus.getProductionCountOfGrammar(g1_A));
    Assert.assertTrue(corpus.hasEndElement(g1_A));
    Assert.assertTrue(corpus.hasContentGrammar(g1_A));
    Assert.assertEquals(1, corpus.getSerialOfGrammar(corpus.getContentGrammarOfGrammar(g1_A)));

    int tB = corpus.getTypeOfSchema("", "B");
    Assert.assertTrue(EXISchema.NIL_NODE != tB);
    int g0_B = corpus.getGrammarOfType(tB);
    Assert.assertTrue(corpus.hasEmptyGrammar(g0_B));
    g0_B = corpus.getTypeEmptyGrammarOfGrammar(g0_B);
    Assert.assertEquals(1, corpus.getProductionCountOfGrammar(g0_B));
    int p0_g0_B = corpus.getProductionOfGrammar(g0_B, 0);
    int event_p0_g0_B = corpus.getEventOfProduction(p0_g0_B);
    Assert.assertEquals(EXISchema.EVENT_TYPE_AT, corpus.getEventType(event_p0_g0_B));

    int g1_B = corpus.getGrammarOfProduction(p0_g0_B);
    Assert.assertEquals(0, corpus.getProductionCountOfGrammar(g1_B));
    Assert.assertTrue(corpus.hasEndElement(g1_B));
    Assert.assertTrue(corpus.hasContentGrammar(g1_B));
    Assert.assertEquals(1, corpus.getSerialOfGrammar(corpus.getContentGrammarOfGrammar(g1_B)));
    
    Assert.assertTrue(g1_A == g1_B);
  }

}
