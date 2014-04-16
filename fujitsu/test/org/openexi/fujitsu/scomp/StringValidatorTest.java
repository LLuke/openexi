package org.openexi.fujitsu.scomp;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;
import org.openexi.fujitsu.schema.SimpleTypeValidationInfo;

public class StringValidatorTest extends TestCase {

  public StringValidatorTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Test length validation for string.
   */
  public void testLength_string() throws Exception {
    EXISchema corpus =
         EXISchemaFactoryTestUtil.getEXISchema("/length.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");
  
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
  
    boolean caught = false;
  
    int string10 = corpus.getTypeOfNamespace(foons, "string10");
    validator.validate("1234567890", string10);
    try {
      caught = false;
      validator.validate("123456789", string10);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  
    int string10c = corpus.getTypeOfNamespace(foons, "string10c");
    validator.validate(" 1234567890 ", string10c);
  }
  
  /**
   * Test min/maxLength validation for string.
   */
  public void testMinMaxLength_string() throws Exception {
    EXISchema corpus =
         EXISchemaFactoryTestUtil.getEXISchema("/length.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");
  
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
  
    boolean caught = false;
  
    int stringMin10 = corpus.getTypeOfNamespace(foons, "stringMin10");
    validator.validate("0123456789", stringMin10);
    validator.validate("0123456789A", stringMin10);
    try {
      caught = false;
      validator.validate("012345678", stringMin10);
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
      validator.validate("", stringMin10);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  
    int elemStringMin10 = corpus.getElemOfNamespace(foons, "elemStringMin10");
    validator.validateElemValue("0123456789", elemStringMin10, new SimpleTypeValidationInfo());
    validator.validateElemValue("0123456789A", elemStringMin10, new SimpleTypeValidationInfo());
    try {
      caught = false;
      validator.validateElemValue("012345678", elemStringMin10, new SimpleTypeValidationInfo());
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
      validator.validateElemValue("", elemStringMin10, new SimpleTypeValidationInfo());
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  
    caught = false;
  
    int stringMax10 = corpus.getTypeOfNamespace(foons, "stringMax10");
    validator.validate("0123456789", stringMax10);
    validator.validate("012345678", stringMax10);
    try {
      caught = false;
      validator.validate("0123456789A", stringMax10);
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
   * Test length validation for string with value.
   */
  public void testLengthStringWithValue() throws Exception {
     
    EXISchema corpus =
         EXISchemaFactoryTestUtil.getEXISchema("/length.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");
  
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
  
    int string10 = corpus.getTypeOfNamespace(foons, "string10");
   
    String value;
   
    value = "1234567890";
    validator.validateAtomicValue(value, value, string10);
   
    boolean caught = false;
  
    caught = false;
    try {
      value = "123456789";
      validator.validateAtomicValue(value, value, string10);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  
    int string10c = corpus.getTypeOfNamespace(foons, "string10c");
    value = " 1234567890 ";
      validator.validateAtomicValue(value, value, string10c);
  }
  
  /**
   * Test normalizedString syntax parser.
   */
  public void testNormalizedStringSyntax() throws Exception {
    EXISchema corpus =
         EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                              getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
  
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
  
    int normalizedString = corpus.getTypeOfNamespace(xsdns, "normalizedString");
    Assert.assertEquals(
      "  FUJITSU  LIMITED  ",
      validator.validate("  FUJITSU  LIMITED  ", normalizedString));
    // tab '\011' becomes a space during validation.
    Assert.assertEquals(
      "FUJITSU LIMITED",
      validator.validate("FUJITSU\011LIMITED", normalizedString));
    // lf '\012' becomes a space during validation.
    Assert.assertEquals(
      "FUJITSU LIMITED",
      validator.validate("FUJITSU\012LIMITED", normalizedString));
    // cr '\015' becomes a space during validation.
    Assert.assertEquals(
      "FUJITSU LIMITED",
      validator.validate("FUJITSU\015LIMITED", normalizedString));
  
    // No negative cases provided. normalizedString validation always succeeds.
  }

  /**
   * Test characters in SIP (Supplementary Ideographic Plane).
   */
  public void testSIPCharacters() throws Exception {
    
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/length.xsd", getClass());

 
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
    
    boolean caught = false;
    
    int string1 = corpus.getTypeOfNamespace(foons, "string1");

    // single character in SIP (U+2000B)
    validator.validate("\uD840\uDC0B", string1);

    try {
      caught = false;
      validator.validate("\uD840\uDC0B\uD840\uDC0B", string1);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    // invalid UTF-16 surrogate pair
    try {
      caught = false;
      validator.validate("\uD840\uDC0B\uD840", string1);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_SURROGATE_PAIR, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
    
    // invalid UTF-16 surrogate pair
    try {
      caught = false;
      validator.validate("\uD840\uDC0B\uD840\uD841", string1);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_SURROGATE_PAIR, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
    
    int stringMin1 = corpus.getTypeOfNamespace(foons, "stringMin1");
    validator.validate("\uD840\uDC0B", stringMin1);

    try {
      caught = false;
      validator.validate("", stringMin1);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
    
    int stringMax1 = corpus.getTypeOfNamespace(foons, "stringMax1");
    validator.validate("\uD840\uDC0B", stringMax1);

    try {
      caught = false;
      validator.validate("\uD840\uDC0B\uD840\uDC0B", stringMax1);
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
 
}
