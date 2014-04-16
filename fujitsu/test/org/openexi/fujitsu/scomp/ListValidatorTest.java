package org.openexi.fujitsu.scomp;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaConst;
import org.openexi.fujitsu.schema.ListTypedValue;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;
import org.openexi.fujitsu.schema.SimpleTypeValidationInfo;

public class ListValidatorTest extends TestCase {

  public ListValidatorTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
  }

  private static final String XMLSCHEMA_URI = "http://www.w3.org/2001/XMLSchema";
  
  private EXISchemaFactoryErrorMonitor m_compilerErrorHandler;

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Test xsd:NMTOKENS
   */
  public void testBuiltinNMTOKENS() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/list.xsd",
                                               getClass());
 
    int xsdns = corpus.getNamespaceOfSchema(XMLSCHEMA_URI);

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int nmtokens = corpus.getTypeOfNamespace(xsdns, "NMTOKENS");

    Assert.assertEquals("yes no", validator.validate("yes no", nmtokens));
    // leading spaces removed when validated against list
    Assert.assertEquals(".-_:AB .-_:CD .-_:EF",
                      validator.validate("   .-_:AB\t .-_:CD\r\n .-_:EF  ", nmtokens));
    
    caught = false;
    try {
        // no items as opposed to an item of zero length
        validator.validate("", nmtokens);
    }
    catch (SchemaValidatorException sve) {
        caught = true;
        Assert.assertEquals(SchemaValidatorException.MIN_LENGTH_INVALID, sve.getCode());
    }
    if (!caught)
        Assert.fail("The operation should have resulted in an exception.");

    caught = false;
    try {
      validator.validate("A\u005BB C\u005BD E\u005BF", nmtokens);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NMTOKEN, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test xsd:IDREFS
   */
  public void testBuiltinIDREFS() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/list.xsd",
                                               getClass());
 
    int xsdns = corpus.getNamespaceOfSchema(XMLSCHEMA_URI);

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int idrefs = corpus.getTypeOfNamespace(xsdns, "IDREFS");

    // leading spaces removed when validated against list
    Assert.assertEquals("SEC15 SEC16 SEC17",
                      validator.validate("   SEC15\t SEC16\r\n SEC17  ", idrefs));
    
    caught = false;
    try {
        // no items as opposed to an item of zero length
        validator.validate("", idrefs);
    }
    catch (SchemaValidatorException sve) {
        caught = true;
        Assert.assertEquals(SchemaValidatorException.MIN_LENGTH_INVALID, sve.getCode());
    }
    if (!caught)
        Assert.fail("The operation should have resulted in an exception.");

    caught = false;
    try {
      // "3.14" is not a valid IDREF
      validator.validate("3.14 3.15 3.16", idrefs);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test xsd:ENTITIES
   */
  public void testBuiltinENTITIES() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/list.xsd",
                                               getClass());
 
    int xsdns = corpus.getNamespaceOfSchema(XMLSCHEMA_URI);

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int entities = corpus.getTypeOfNamespace(xsdns, "ENTITIES");
    int entity = corpus.getItemTypeOfListSimpleType(entities);
    Assert.assertTrue(entity != EXISchema.NIL_NODE);

    SimpleTypeValidationInfo stvi = new SimpleTypeValidationInfo();
    ListTypedValue listValue;

    // leading spaces removed when validated against list
    Assert.assertEquals("SEC15 SEC16 SEC17",
                      validator.validate("   SEC15\t SEC16\r\n SEC17  ", entities, stvi));
    listValue = (ListTypedValue)stvi.getTypedValue();
    Assert.assertEquals(corpus, listValue.getEXISchema());
    Assert.assertEquals(entities, listValue.getType());
    Assert.assertEquals(3, listValue.getAtomicValueCount());
    Assert.assertEquals("SEC15", listValue.getAtomicValue(0).getValue());
    Assert.assertEquals(corpus, listValue.getAtomicValue(0).getEXISchema());
    Assert.assertEquals(EXISchemaConst.STRING_TYPE,
                      listValue.getAtomicValue(0).getPrimTypeId());
    Assert.assertEquals(entity, listValue.getAtomicValue(0).getType());
    Assert.assertEquals("SEC16", listValue.getAtomicValue(1).getValue());
    Assert.assertEquals(corpus, listValue.getAtomicValue(1).getEXISchema());
    Assert.assertEquals(EXISchemaConst.STRING_TYPE,
                      listValue.getAtomicValue(1).getPrimTypeId());
    Assert.assertEquals(entity, listValue.getAtomicValue(1).getType());
    Assert.assertEquals("SEC17", listValue.getAtomicValue(2).getValue());
    Assert.assertEquals(corpus, listValue.getAtomicValue(2).getEXISchema());
    Assert.assertEquals(EXISchemaConst.STRING_TYPE,
                      listValue.getAtomicValue(2).getPrimTypeId());
    Assert.assertEquals(entity, listValue.getAtomicValue(2).getType());

    stvi = new SimpleTypeValidationInfo();
    caught = false;
    try {
        // no items as opposed to an item of zero length
        validator.validate("", entities, stvi);
    }
    catch (SchemaValidatorException sve) {
        caught = true;
        Assert.assertEquals(SchemaValidatorException.MIN_LENGTH_INVALID, sve.getCode());
    }
    if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    
    stvi = new SimpleTypeValidationInfo();
    try {
      caught = false;
      // "3.14" is not a valid ENTITY
      validator.validate("3.14 3.15 3.16", entities, stvi);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NAME, sve.getCode());
      listValue = (ListTypedValue)stvi.getTypedValue();
      Assert.assertEquals(entities, listValue.getType());
      Assert.assertEquals(0, listValue.getAtomicValueCount());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test list of xsd:ID
   */
  public void testListOfIDs() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/list.xsd",
                                               getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int listOfIDs = corpus.getTypeOfNamespace(foons, "listOfIDs");

    // leading spaces removed when validated against integer
    Assert.assertEquals("SEC15 SEC16 SEC17",
                      validator.validate("   SEC15\t SEC16\r\n SEC17  ", listOfIDs));
    // no items as opposed to an item of zero length
    validator.validate("", listOfIDs);

    try {
      caught = false;
      // "3.14" is not a valid ID
      validator.validate("3.14 3.15 3.16", listOfIDs);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test list of xsd:ID with minLength:3 and maxLength:5
   */
  public void testListOfIDsMin3Max5() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/list.xsd",
                                               getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int listOfIDsMin3Max5 = corpus.getTypeOfNamespace(foons, "listOfIDsMin3Max5");

    validator.validate("SEC15 SEC16 SEC17", listOfIDsMin3Max5);
    validator.validate("SEC15 SEC16 SEC17 SEC18 SEC19", listOfIDsMin3Max5);

    try {
      caught = false;
      // no items where 3 or more items are expected
      validator.validate("", listOfIDsMin3Max5);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      // only 2 items where 3 or more items are expected
      validator.validate("SEC15 SEC16", listOfIDsMin3Max5);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      // too many items where 5 or less items are expected
      validator.validate("SEC15 SEC16 SEC17 SEC18 SEC19 SEC20", listOfIDsMin3Max5);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test list of xsd:ID with length:4
   */
  public void testListOfIDsLen4() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/list.xsd",
                                               getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int listOfIDsLen4 = corpus.getTypeOfNamespace(foons, "listOfIDsLen4");

    validator.validate("SEC15 SEC16 SEC17 SEC18", listOfIDsLen4);

    try {
      caught = false;
      // no items where 4 items are expected
      validator.validate("", listOfIDsLen4);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      // only 3 items where 4 items are expected
      validator.validate("SEC15 SEC16 SEC17", listOfIDsLen4);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      // too many items where 4 items are expected
      validator.validate("SEC15 SEC16 SEC17 SEC18 SEC19", listOfIDsLen4);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   */
  public void testListOfUnion() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/list.xsd",
                                               getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int listOfUnion = corpus.getTypeOfNamespace(foons, "listOfUnion");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(listOfUnion));
    
    // leading spaces removed when validated against integer
    Assert.assertEquals("SEC15 SEC16 SEC17",
                      validator.validate("   SEC15\t SEC16\r\n SEC17  ", listOfUnion));
    // no items as opposed to an item of zero length
    validator.validate("", listOfUnion);

    try {
      caught = false;
      validator.validate("3.14 3.15 3.16", listOfUnion);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_UNION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   */
  public void testList4OfUnion() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/list.xsd",
                                               getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int listOfUnion = corpus.getTypeOfNamespace(foons, "list4OfUnion");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(listOfUnion));
    
    // leading spaces removed when validated against integer
    Assert.assertEquals("SEC15 SEC16 SEC17 SEC18",
                      validator.validate("   SEC15\t SEC16\r\n SEC17  SEC18   ", listOfUnion));
    // no items as opposed to an item of zero length

    try {
      caught = false;
      validator.validate("SEC15 SEC16 SEC17", listOfUnion);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
  
}
