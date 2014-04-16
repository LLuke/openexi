package org.openexi.fujitsu.scomp;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;

public class BinaryValidatorTest extends TestCase {

  public BinaryValidatorTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Test base64Binary syntax parser.
   */
  public void testBase64BinarySyntax() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                               getClass());

    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int base64Binary = corpus.getTypeOfNamespace(xsdns, "base64Binary");

    // leading spaces removed during validation.
    Assert.assertEquals("QUJDREVGR0hJSg==", // "ABCDEFGHIJ"
                      validator.validate("   QUJDREVGR0hJSg==  ", base64Binary));
    Assert.assertEquals("", validator.validate("", base64Binary));

    try {
      caught = false;
      // '\u3042' (= Hiragana "A") is not legitimate in encoded string
      validator.validate("QUJDR\u3042VGR0hJSg==", base64Binary);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_BASE64_BINARY,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      // '=' cannot appear as the first in the four.
      validator.validate("=A==", base64Binary);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_BASE64_BINARY,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      // '=' cannot appear as the second in the four.
      validator.validate("A===", base64Binary);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_BASE64_BINARY,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      // '=' cannot appear as the third in the four unless the fourth is '='.
      validator.validate("AB=D", base64Binary);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_BASE64_BINARY,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      // The length must be multiple of 4.
      validator.validate("ABCDE", base64Binary);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_BASE64_BINARY,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      // The last 'Z' is extraneous
      validator.validate("QUJDREVGR0hJSg==Z", base64Binary);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_BASE64_BINARY,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test base64Binary length.
   */
  public void testBase64BinaryLength() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/length.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int base64Binary10 = corpus.getTypeOfNamespace(foons, "base64Binary10");

    validator.validate("QUJDREVGR0hJSg==", base64Binary10); // "ABCDEFGHIJ"

    try {
      caught = false;
      // "ABCDEFGHI" (length=9) which is too short
      validator.validate("QUJDREVGR0hJ", base64Binary10);
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
      // "ABCDEFGHIJK" (length=11) which is too long
      validator.validate("QUJDREVGR0hJSks=", base64Binary10);
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
   * Test base64Binary minLength.
   */
  public void testBase64BinaryMinLength() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/length.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int base64BinaryMin10 = corpus.getTypeOfNamespace(foons,
        "base64BinaryMin10");

    validator.validate("QUJDREVGR0hJSg==", base64BinaryMin10); // "ABCDEFGHIJ"
    validator.validate("QUJDREVGR0hJSks=", base64BinaryMin10); // "ABCDEFGHIJK"

    try {
      caught = false;
      // "ABCDEFGHI" (length=9) which is too short
      validator.validate("QUJDREVGR0hJ", base64BinaryMin10);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_LENGTH_INVALID,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test base64Binary maxLength.
   */
  public void testBase64BinaryMaxLength() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/length.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int base64BinaryMax10 = corpus.getTypeOfNamespace(foons,
        "base64BinaryMax10");

    validator.validate("QUJDREVGR0hJSg==", base64BinaryMax10); // "ABCDEFGHIJ"
    validator.validate("QUJDREVGR0hJ", base64BinaryMax10); // "ABCDEFGHI"

    try {
      caught = false;
      // "ABCDEFGHIJK" (length=11) which is too long
      validator.validate("QUJDREVGR0hJSks=", base64BinaryMax10); // "ABCDEFGHIJK"
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_LENGTH_INVALID,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test base64Binary length with value.
   */
  public void testBase64BinaryLengthWithValue() throws Exception {

    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/length.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    int base64Binary10 = corpus.getTypeOfNamespace(foons, "base64Binary10");
    
    byte[] val;
    
    val = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
    
    validator.validateAtomicValue(
        SimpleTypeValidator.encodeBinaryByBase64(val), val, base64Binary10);

    boolean caught = false;

    caught = false;
    try {
      val = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }; // too short
      validator.validateAtomicValue(
          SimpleTypeValidator.encodeBinaryByBase64(val), val, base64Binary10);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    caught = false;
    try {
      val = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 }; // too long
      validator.validateAtomicValue(
          SimpleTypeValidator.encodeBinaryByBase64(val), val, base64Binary10);
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
   * Test hexBinary syntax parser.
   */
  public void testHexBinarySyntax() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                               getClass());

    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int hexBinary = corpus.getTypeOfNamespace(xsdns, "hexBinary");

    // leading spaces removed during validation.
    Assert.assertEquals("0FB7",
                      validator.validate("   0FB7  ", hexBinary));
    Assert.assertEquals("", validator.validate("", hexBinary));

    // whitespaces are just being ignored.
    validator.validate("0\tF B7", hexBinary);

    validator.validate("0123456789ABCDEFabcdef", hexBinary);

    try {
      caught = false;
      // 'g' is not valid in the context of hexBinary
      validator.validate("0gB7", hexBinary);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_HEX_BINARY,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      // '/' is not valid in the context of hexBinary
      validator.validate("0/B7", hexBinary);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_HEX_BINARY,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      // '@' is not valid in the context of hexBinary
      validator.validate("0@B7", hexBinary);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_HEX_BINARY,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      // 'G' is not valid in the context of hexBinary
      validator.validate("0GB7", hexBinary);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_HEX_BINARY,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      // The length has to be even number.
      validator.validate("0FB7A", hexBinary);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_HEX_BINARY,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      // "0F\u3042B7" is not a legitimate hexBinary
      validator.validate("0F\u3042B7", hexBinary); // \u3042 = Hiragana "A"
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_HEX_BINARY,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test hexBinary length.
   */
  public void testHexBinaryLength() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/length.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int hexBinary10 = corpus.getTypeOfNamespace(foons, "hexBinary10");

    validator.validate("4142434445464748494A", hexBinary10); // "ABCDEFGHIJ"

    try {
      caught = false;
      // "ABCDEFGHI" (length=9) which is too short
      validator.validate("414243444546474849", hexBinary10);
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
      // "ABCDEFGHIJK" (length=11) which is too long
      validator.validate("4142434445464748494A4B", hexBinary10);
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
   * Test hexBinary minLength.
   */
  public void testHexBinaryMinLength() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/length.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int hexBinaryMin10 = corpus.getTypeOfNamespace(foons, "hexBinaryMin10");

    validator.validate("4142434445464748494A", hexBinaryMin10); // "ABCDEFGHIJ"
    validator.validate("4142434445464748494A4B", hexBinaryMin10); // "ABCDEFGHIJK"

    try {
      caught = false;
      // "ABCDEFGHI" (length=9) which is too short
      validator.validate("414243444546474849", hexBinaryMin10);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_LENGTH_INVALID,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test hexBinary maxLength.
   */
  public void testHexBinaryMaxLength() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/length.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int hexBinaryMax10 = corpus.getTypeOfNamespace(foons, "hexBinaryMax10");

    validator.validate("4142434445464748494A", hexBinaryMax10); // "ABCDEFGHIJ"
    validator.validate("414243444546474849", hexBinaryMax10); // "ABCDEFGHI"

    try {
      caught = false;
      // "ABCDEFGHIJK" (length=11) which is too long
      validator.validate("4142434445464748494A4B", hexBinaryMax10); //
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_LENGTH_INVALID,
                        sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test hexBinary length with value.
   */
  public void testHexBinaryLengthWithValue() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/length.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    int hexBinary10 = corpus.getTypeOfNamespace(foons, "hexBinary10");

    byte[] val;
    
    val = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    
    validator.validateAtomicValue(
        SimpleTypeValidator.encodeBinaryByHexBin(val), val, hexBinary10);

    boolean caught = false;

    caught = false;
    try {
      val = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }; // too short
      validator.validateAtomicValue(
          SimpleTypeValidator.encodeBinaryByHexBin(val), val, hexBinary10);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.LENGTH_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    caught = false;
    try {
      val = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 }; // too long
      validator.validateAtomicValue(
          SimpleTypeValidator.encodeBinaryByHexBin(val), val, hexBinary10);
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
