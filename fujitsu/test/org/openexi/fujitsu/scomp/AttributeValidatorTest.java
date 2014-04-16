package org.openexi.fujitsu.scomp;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.PrefixUriBindings;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;
import org.openexi.fujitsu.schema.SimpleTypeValidationInfo;

/**
 * Attribute validation test cases.
 */
public class AttributeValidatorTest extends TestCase {

  public AttributeValidatorTest(String name) {
    super(name);
    emptyPrefixUriBindings = new PrefixUriBindings();
  }

  private final PrefixUriBindings emptyPrefixUriBindings;

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Attribute declaration of type "xsd:decimal" where the attribute use
   * specifies the fixed value "1". 
   */
  public void testAttrDecimalFixed_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixed.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int decimals_A = corpus.getTypeOfNamespace(foons, "decimal");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(decimals_A));
    
    int useDecimal = corpus.getAttrUseOfComplexType(decimals_A, "urn:foo", "decimal");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useDecimal));
    
    stv.validateAttrValue("1.2345", useDecimal, new SimpleTypeValidationInfo());
    stv.validateAttrValue("1.23450", useDecimal, new SimpleTypeValidationInfo());
    
    try {
      stv.validateAttrValue("1.23456", useDecimal, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }
  
  /**
   * Attribute declaration of type "xsd:string" where the attribute use
   * specifies the fixed value "1". 
   */
  public void testAttrStringFixed_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixed.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "string");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useString = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "string");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useString));
    
    stv.validateAttrValue("abc", useString, new SimpleTypeValidationInfo());
    
    try {
      stv.validateAttrValue("xyz", useString, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Attribute declaration of type "xsd:anySimpleType" where the attribute use
   * specifies the fixed value "1". 
   */
  public void testAttrAnySimpleTypeFixed_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixed.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "anySimpleType");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useAnySimpleType = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "anySimpleType");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useAnySimpleType));
    
    stv.validateAttrValue("xyz", useAnySimpleType, new SimpleTypeValidationInfo());
    
    try {
      stv.validateAttrValue("abc", useAnySimpleType, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Attribute declaration of type "xsd:anyURI" where the attribute use
   * specifies the fixed value "1". 
   */
  public void testAttrAnyURIFixed_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixed.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "anyURI");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useAnyURI = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "anyURI");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useAnyURI));
    
    stv.validateAttrValue("urn:foo", useAnyURI, new SimpleTypeValidationInfo());
    
    try {
      stv.validateAttrValue("urn:goo", useAnyURI, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Attribute declaration of type "xsd:QName" where the attribute use
   * specifies the fixed value "goo:xyz". 
   */
  public void testAttrQNameFixed_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixed.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "QName");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useQName = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "QName");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useQName));
    
    PrefixUriBindings nspm = new PrefixUriBindings();
    stv.validateAttrValue("p0:xyz", useQName, new SimpleTypeValidationInfo(), nspm.bind("p0", "urn:goo"));
    stv.validateAttrValue("xyz", useQName, new SimpleTypeValidationInfo(), nspm.bindDefault("urn:goo"));
    
    boolean caught = false;
    try {
      stv.validateAttrValue("1.0", useQName, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_QNAME, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("goo:abc", useQName, new SimpleTypeValidationInfo(), nspm.bind("goo", "urn:goo"));
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
  }
  
  /**
   * Attribute declaration of type "xsd:float" where the attribute use
   * specifies the fixed value "1.23". 
   */
  public void testAttrFloatFixed_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixed.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "float");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useFloat = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "float");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useFloat));
    
    stv.validateAttrValue("1.23", useFloat, new SimpleTypeValidationInfo());
    stv.validateAttrValue("1.230", useFloat, new SimpleTypeValidationInfo());
    
    try {
      stv.validateAttrValue("1.2", useFloat, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }
  
  /**
   * Attribute declaration of type "xsd:double" where the attribute use
   * specifies the fixed value "1.23". 
   */
  public void testAttrDoubleFixed_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixed.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "double");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useDouble = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "double");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useDouble));
    
    stv.validateAttrValue("1.34", useDouble, new SimpleTypeValidationInfo());
    stv.validateAttrValue("1.340", useDouble, new SimpleTypeValidationInfo());
    
    try {
      stv.validateAttrValue("1.3", useDouble, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }
  
  /**
   * Attribute declaration of type "xsd:dateTime" where the attribute use
   * specifies the fixed value "2007-07-11T21:51:43Z". 
   */
  public void testAttrDateTimeFixed_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixed.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "dateTime");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useDateTime = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "dateTime");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useDateTime));

    // REVISIT:
    // stv.validateAttrValue("2007-07-11T12:51:43Z", useDateTime, new SimpleTypeValidationInfo());
    stv.validateAttrValue("2007-07-11T21:51:43+09:00", useDateTime, new SimpleTypeValidationInfo());
    
    boolean caught = false;
    try {
      stv.validateAttrValue("2007-07-12T21:51:43Z", useDateTime, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("2007-07-11T21:51:43-05:00", useDateTime, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
  }
  
  /**
   * Attribute declaration of type "xsd:duration" where the attribute use
   * specifies the fixed value "P3DT10H30M". 
   */
  public void testAttrDurationFixed_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixed.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "duration");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useDuration = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "duration");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useDuration));
    
    stv.validateAttrValue("P3DT10H30M", useDuration, new SimpleTypeValidationInfo());
    // REVISIT:
    // stv.validateAttrValue("P3DT9H90M", useDuration, new SimpleTypeValidationInfo());
    
    try {
      stv.validateAttrValue("P3DT9H30M", useDuration, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }
  
  /**
   * Attribute declaration of type "xsd:base64Binary" where the attribute use
   * specifies the fixed value "QUJDREVGR0hJSg==". 
   */
  public void testAttrBase64BinaryFixed_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixed.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "base64Binary");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useBase64Binary = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "base64Binary");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useBase64Binary));
    
    stv.validateAttrValue("QUJDREVGR0hJSg==", useBase64Binary, new SimpleTypeValidationInfo());
    
    try {
      stv.validateAttrValue("QUJDREVGR0hJ", useBase64Binary, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }
  
  /**
   * Attribute declaration of type "xsd:hexBinary" where the attribute use
   * specifies the fixed value "4142434445464748494A". 
   */
  public void testAttrHexBinaryFixed_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixed.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "hexBinary");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useHexBinary = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "hexBinary");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useHexBinary));
    
    stv.validateAttrValue("4142434445464748494A", useHexBinary, new SimpleTypeValidationInfo());
    
    try {
      stv.validateAttrValue("4142434445464748494A4B", useHexBinary, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }
  
  /**
   * Attribute declaration of "xsd:decimal" list type where the attribute use
   * specifies the fixed value "1 2 3". 
   */
  public void testAttrDecimalFixedList_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedList.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "C01");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useDecimalList = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "decimalList");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useDecimalList));
    
    stv.validateAttrValue("1 2 3", useDecimalList, new SimpleTypeValidationInfo());
    stv.validateAttrValue("1.0 2.00 3.000", useDecimalList, new SimpleTypeValidationInfo());

    boolean caught = false;
    try {
      stv.validateAttrValue("1", useDecimalList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("1 2", useDecimalList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("1 2 3 4", useDecimalList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
  }
  
  /**
   * Attribute declaration of "xsd:string" list type where the attribute use
   * specifies the fixed value "1 2 3". 
   */
  public void testAttrStringFixedList_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedList.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "C02");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useStringList = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "stringList");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useStringList));
    
    stv.validateAttrValue("1 2 3", useStringList, new SimpleTypeValidationInfo());

    boolean caught = false;
    try {
      stv.validateAttrValue("1.0 2.00 3.000", useStringList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("1 2", useStringList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("1 2 3 4", useStringList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
  }
  
  /**
   * Attribute declaration of "xsd:anyURI" list type where the attribute use
   * specifies the fixed value "1 2 3". 
   */
  public void testAttrAnyURIFixedList_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedList.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "C04");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useAnyURIList = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "anyURIList");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useAnyURIList));
    
    stv.validateAttrValue("1 2 3", useAnyURIList, new SimpleTypeValidationInfo());

    boolean caught = false;
    try {
      stv.validateAttrValue("1.0 2.00 3.000", useAnyURIList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("1 2", useAnyURIList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("1 2 3 4", useAnyURIList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
  }
  
  /**
   * Attribute declaration of "xsd:float" list type where the attribute use
   * specifies the fixed value "1.23 2.34 3.45". 
   */
  public void testAttrFloatFixedList_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedList.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "C06");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useFloatList = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "floatList");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useFloatList));
    
    stv.validateAttrValue("1.23 2.34 3.45", useFloatList, new SimpleTypeValidationInfo());
    stv.validateAttrValue("1.230 2.340 3.450", useFloatList, new SimpleTypeValidationInfo());

    boolean caught = false;
    try {
      stv.validateAttrValue("1.0 2.00 3.000", useFloatList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("1.23 2.34", useFloatList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("1.23 2.34 3.45 4.56", useFloatList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
  }
  
  /**
   * Attribute declaration of "xsd:double" list type where the attribute use
   * specifies the fixed value "1.23 2.34 3.45". 
   */
  public void testAttrDoubleFixedList_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedList.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "C07");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useDoubleList = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "doubleList");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useDoubleList));
    
    stv.validateAttrValue("1.23 2.34 3.45", useDoubleList, new SimpleTypeValidationInfo());
    stv.validateAttrValue("1.230 2.340 3.450", useDoubleList, new SimpleTypeValidationInfo());

    boolean caught = false;
    try {
      stv.validateAttrValue("1.0 2.00 3.000", useDoubleList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("1.23 2.34", useDoubleList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("1.23 2.34 3.45 4.56", useDoubleList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
  }
  
  /**
   * Attribute declaration of "xsd:date" list type where the attribute use
   * specifies the fixed value "2007-07-11 2008-08-12". 
   */
  public void testAttrDateFixedList_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedList.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "C08");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useDateList = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "dateList");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useDateList));
    
    stv.validateAttrValue("2007-07-11 2008-08-12", useDateList, new SimpleTypeValidationInfo());

    boolean caught = false;
    try {
      stv.validateAttrValue("2008-08-12 2007-07-11", useDateList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("2007-07-11", useDateList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("2007-07-11 2008-08-12 2009-09-13", useDateList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
  }

  /**
   * Attribute declaration of "xsd:duration" list type where the attribute use
   * specifies the fixed value "P3DT10H30M P4DT11H31M". 
   */
  public void testAttrDurationFixedList_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedList.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "C09");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useDurationList = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "durationList");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useDurationList));
    
    stv.validateAttrValue("P3DT10H30M P4DT11H31M", useDurationList, new SimpleTypeValidationInfo());

    boolean caught = false;
    try {
      stv.validateAttrValue("P4DT11H31M P3DT10H30M", useDurationList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("P3DT10H30M", useDurationList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("P3DT10H30M P4DT11H31M P5DT12H32M", useDurationList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
  }
  
  /**
   * Attribute declaration of "xsd:base64Binary" list type where the attribute use
   * specifies the fixed value "QUJDREVGR0hJSg== QUJDREVGR0hJ". 
   */
  public void testAttrBase64BinaryFixedList_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedList.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "C10");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useBase64BinaryList = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "base64BinaryList");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useBase64BinaryList));
    
    stv.validateAttrValue("QUJDREVGR0hJSg== QUJDREVGR0hJ", useBase64BinaryList, new SimpleTypeValidationInfo());

    boolean caught = false;
    try {
      stv.validateAttrValue("QUJDREVGR0hJ QUJDREVGR0hJSg==", useBase64BinaryList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("QUJDREVGR0hJSg==", useBase64BinaryList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("QUJDREVGR0hJSg== QUJDREVGR0hJ QUJDREVGR0hJ", useBase64BinaryList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
  }
  
  /**
   * Attribute declaration of "xsd:hexBinary" list type where the attribute use
   * specifies the fixed value "4142434445464748494A 4142434445464748494A4B". 
   */
  public void testAttrHexBinaryFixedList_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedList.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "C11");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useHexBinaryList = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "hexBinaryList");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useHexBinaryList));
    
    stv.validateAttrValue("4142434445464748494A 4142434445464748494A4B", useHexBinaryList, new SimpleTypeValidationInfo());

    boolean caught = false;
    try {
      stv.validateAttrValue("4142434445464748494A4B 4142434445464748494A", useHexBinaryList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("4142434445464748494A", useHexBinaryList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("4142434445464748494A 4142434445464748494A4B 4142434445464748494A4B", useHexBinaryList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
  }

  /**
   * Attribute declaration of "xsd:boolean" list type where the attribute use
   * specifies the fixed value "false true true". 
   */
  public void testAttrBooleanFixedList_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedList.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "C12");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useBooleanList = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "booleanList");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useBooleanList));
    
    stv.validateAttrValue("false true true", useBooleanList, new SimpleTypeValidationInfo());
    stv.validateAttrValue("0 1 1", useBooleanList, new SimpleTypeValidationInfo());

    boolean caught = false;
    try {
      stv.validateAttrValue("0", useBooleanList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("0 1", useBooleanList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("0 1 1 0", useBooleanList, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
  }
  
  /**
   * Attribute declaration of "xsd:QName" list type where the attribute use
   * specifies the fixed value "foo:dog goo:cat foo:pig". 
   */
  public void testAttrQNameFixedList_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixedList.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "C13");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));
    
    int useQNameList = corpus.getAttrUseOfComplexType(complexType, "urn:foo", "qnameList");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useQNameList));
    
    PrefixUriBindings nspm;
    nspm = emptyPrefixUriBindings; 
    nspm = nspm.bind("foo", "urn:foo");
    nspm = nspm.bind("goo", "urn:goo");
    nspm = nspm.bind("zoo", "urn:zoo");
    
    stv.validateAttrValue("foo:dog goo:cat zoo:pig", useQNameList, new SimpleTypeValidationInfo(), nspm);
    
    nspm = emptyPrefixUriBindings; 
    nspm = nspm.bind("Foo", "urn:foo");
    nspm = nspm.bind("Goo", "urn:goo");
    nspm = nspm.bind("Zoo", "urn:zoo");
    
    stv.validateAttrValue("Foo:dog Goo:cat Zoo:pig", useQNameList, new SimpleTypeValidationInfo(), nspm);

    nspm = emptyPrefixUriBindings; 
    nspm = nspm.bind("foo", "urn:foo");
    nspm = nspm.bind("goo", "urn:goo");
    nspm = nspm.bind("zoo", "urn:zoo");
    
    boolean caught = false;
    try {
      stv.validateAttrValue("foo:dog", useQNameList, new SimpleTypeValidationInfo(), nspm);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("foo:dog goo:cat", useQNameList, new SimpleTypeValidationInfo(), nspm);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    caught = false;
    try {
      stv.validateAttrValue("foo:dog goo:cat zoo:pig foo:rat", useQNameList, new SimpleTypeValidationInfo(), nspm);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
  }
  
  /**
   * Attribute declaration of union (decimal and restricted string) type
   * where the attribute use specifies a fixed value. 
   */
  public void testAttrUnionFixed_01() throws Exception {
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/attributeFixed.xsd", getClass());

    SimpleTypeValidator stv = new SimpleTypeValidator(corpus);
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    int complexType = corpus.getTypeOfNamespace(foons, "decimalOrINF");
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(complexType));

    boolean caught;
    
    int useUnioned4 = corpus.getAttrUseOfComplexType(complexType, "", "decimal");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useUnioned4));
    
    stv.validateAttrValue("4", useUnioned4, new SimpleTypeValidationInfo());
    stv.validateAttrValue("4.0", useUnioned4, new SimpleTypeValidationInfo());
    
    caught = false;
    try {
      stv.validateAttrValue("INF", useUnioned4, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    
    int useUnionedINF = corpus.getAttrUseOfComplexType(complexType, "", "inf");
    Assert.assertEquals(EXISchema.ATTRIBUTE_USE_NODE, corpus.getNodeType(useUnionedINF));
    
    stv.validateAttrValue("INF", useUnionedINF, new SimpleTypeValidationInfo());
    
    caught = false;
    try {
      stv.validateAttrValue("4", useUnionedINF, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
    
    caught = false;
    try {
      stv.validateAttrValue("IMF", useUnionedINF, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_UNION, sve.getCode());
      caught = true;
    }
    Assert.assertTrue(caught);
  }
}