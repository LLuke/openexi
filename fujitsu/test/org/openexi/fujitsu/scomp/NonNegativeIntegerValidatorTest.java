package org.openexi.fujitsu.scomp;
 
import junit.framework.Assert;
import junit.framework.TestCase;
 
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;

public class NonNegativeIntegerValidatorTest extends TestCase {
 
  public NonNegativeIntegerValidatorTest(String name) {
    super(name);
  }
 
  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////
 
  /**
   * Test nonNegativeInteger value space.
   */
  public void testNonNegativeIntegerValueSpace() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/decimalRange.xsd", getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int nonNegativeInteger = corpus.getTypeOfNamespace(xsdns, "nonNegativeInteger");
 
    validator.validate("92233720368547758070000000000", nonNegativeInteger);
    validator.validate("0", nonNegativeInteger);
 
    try {
      caught = false;
      validator.validate("12.0", nonNegativeInteger);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_INTEGER, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("-1", nonNegativeInteger);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
 
  /**
   * Test positiveInteger value space.
   */
  public void testPositiveIntegerValueSpace() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/decimalRange.xsd", getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int positiveInteger = corpus.getTypeOfNamespace(xsdns, "positiveInteger");
 
    validator.validate("92233720368547758070000000000", positiveInteger);
    validator.validate("1", positiveInteger);
 
    try {
      caught = false;
      validator.validate("12.0", positiveInteger);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_INTEGER, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("0", positiveInteger);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
 
  /**
   * Test unsignedLong value space.
   */
  public void testUnsignedLongValueSpace() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/decimalRange.xsd", getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int unsignedLong = corpus.getTypeOfNamespace(xsdns, "unsignedLong");
 
    validator.validate("18446744073709551615", unsignedLong);
    validator.validate("0", unsignedLong);
 
    try {
      caught = false;
      validator.validate("12.0", unsignedLong);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_INTEGER, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("18446744073709551616", unsignedLong);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("-1", unsignedLong);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
 
  /**
   * Test unsignedInt value space.
   */
  public void testUnsignedIntValueSpace() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/decimalRange.xsd", getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int unsignedInt = corpus.getTypeOfNamespace(xsdns, "unsignedInt");
 
    validator.validate("4294967295", unsignedInt);
    validator.validate("0", unsignedInt);
 
    try {
      caught = false;
      validator.validate("12.0", unsignedInt);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_INTEGER, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("4294967296", unsignedInt);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("-1", unsignedInt);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
 
  /**
   * Test unsignedShort value space.
   */
  public void testUnsignedShortValueSpace() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/decimalRange.xsd", getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int unsignedShort = corpus.getTypeOfNamespace(xsdns, "unsignedShort");
 
    validator.validate("65535", unsignedShort);
    validator.validate("0", unsignedShort);
 
    try {
      caught = false;
      validator.validate("12.0", unsignedShort);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_INTEGER, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("65536", unsignedShort);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("-1", unsignedShort);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
 
  /**
   * Test unsignedByte value space.
   */
  public void testUnsignedByteValueSpace() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/decimalRange.xsd", getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int unsignedByte = corpus.getTypeOfNamespace(xsdns, "unsignedByte");
 
    validator.validate("255", unsignedByte);
    validator.validate("0", unsignedByte);
 
    try {
      caught = false;
      validator.validate("12.0", unsignedByte);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_INTEGER, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("256", unsignedByte);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("-1", unsignedByte);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
 
}
