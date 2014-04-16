package org.openexi.fujitsu.scomp;
 
import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;

public class BooleanValidatorTest extends TestCase {
 
  public BooleanValidatorTest(String name) {
    super(name);
  }
 
  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////
 
  /**
   * Test boolean value range.
   */
  public void testBooleanValueRange() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsd", getClass());
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int booleanType = corpus.getTypeOfNamespace(xsdns, "boolean");
 
    validator.validate("true", booleanType);
    validator.validate("false", booleanType);
    validator.validate("1", booleanType);
    validator.validate("0", booleanType);
 
    try {
      caught = false;
      validator.validate("yes", booleanType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_BOOLEAN, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("no", booleanType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_BOOLEAN, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("TRUE", booleanType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_BOOLEAN, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("FALSE", booleanType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_BOOLEAN, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
 
  /**
   * Test boolean pattern.
   */
  public void testBooleanPattern() throws Exception {
    
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    int trueType = corpus.getTypeOfNamespace(foons, "trueType");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    validator.validate("true", trueType);
 
    validator.validate("1", trueType); // pattern applies to lexical space
    /**
     * REVISIT: consider pattern-based validation
     * caught = false;
     * try {
     *   validator.validate("1", trueType); // pattern applies to lexical space
     * }
     * catch (SchemaValidatorException sve) {
     *   caught = true;
     *   Assert.assertEquals(SchemaValidatorException.INVALID_PATTERN, sve.getCode());
     * }
     * finally {
     *   if (!caught)
     *     Assert.fail("The operation should have resulted in an exception.");
     * }
     */
 
    validator.validate("false", trueType);
    /**
     * REVISIT: consider pattern-based validation
     * caught = false;
     * try {
     *   validator.validate("false", trueType);
     * }
     * catch (SchemaValidatorException sve) {
     *   caught = true;
     *   Assert.assertEquals(SchemaValidatorException.INVALID_PATTERN, sve.getCode());
     * }
     * finally {
     *   if (!caught)
     *     Assert.fail("The operation should have resulted in an exception.");
     * }
     */
 
    validator.validate("0", trueType);
    /**
     * REVISIT: consider pattern-based validation
     * caught = false;
     * try {
     *   validator.validate("0", trueType);
     * }
     * catch (SchemaValidatorException sve) {
     *   caught = true;
     *   Assert.assertEquals(SchemaValidatorException.INVALID_PATTERN, sve.getCode());
     * }
     * finally {
     *   if (!caught)
     *     Assert.fail("The operation should have resulted in an exception.");
     * }
     */
  }
 
  /**
   * Test boolean pattern with value.
   */
  public void testBooleanPatternWithValue() throws Exception {
    
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/boolean.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    int trueType = corpus.getTypeOfNamespace(foons, "trueType");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    validator.validateAtomicValue("true", Boolean.TRUE, trueType);
 
    validator.validateAtomicValue("1", Boolean.TRUE, trueType); // pattern applies to lexical space
    /**
     * caught = false;
     * REVISIT: consider pattern-based validation
     * try {
     *   validator.validateAtomicValue("1", Boolean.TRUE, trueType); // pattern applies to lexical space
     * }
     * catch (SchemaValidatorException sve) {
     *   caught = true;
     *   Assert.assertEquals(SchemaValidatorException.INVALID_PATTERN, sve.getCode());
     * }
     * finally {
     *   if (!caught)
     *     Assert.fail("The operation should have resulted in an exception.");
     * }
     */
 
    validator.validateAtomicValue("false", Boolean.FALSE, trueType);
    /**
     * caught = false;
     * REVISIT: consider pattern-based validation
     * try {
     *   validator.validateAtomicValue("false", Boolean.FALSE, trueType);
     * }
     * catch (SchemaValidatorException sve) {
     *   caught = true;
     *   Assert.assertEquals(SchemaValidatorException.INVALID_PATTERN, sve.getCode());
     * }
     * finally {
     *   if (!caught)
     *     Assert.fail("The operation should have resulted in an exception.");
     * }
     */
 
    validator.validateAtomicValue("0", Boolean.FALSE, trueType);
    /**
     * caught = false;
     * REVISIT: consider pattern-based validation
     * try {
     *   validator.validateAtomicValue("0", Boolean.FALSE, trueType);
     * }
     * catch (SchemaValidatorException sve) {
     *   caught = true;
     *   Assert.assertEquals(SchemaValidatorException.INVALID_PATTERN, sve.getCode());
     * }
     * finally {
     *   if (!caught)
     *     Assert.fail("The operation should have resulted in an exception.");
     * }
     */
  }
  
}
