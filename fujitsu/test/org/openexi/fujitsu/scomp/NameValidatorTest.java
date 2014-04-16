package org.openexi.fujitsu.scomp;
 
import junit.framework.Assert;
import junit.framework.TestCase;
 
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;

public class NameValidatorTest extends TestCase {
 
  public NameValidatorTest(String name) {
    super(name);
  }
 
  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////
 
  /**
   * Test Name syntax parser.
   */
  public void testNameSyntax() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/Name.xsd",
                                               getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
    int foons = corpus.getNamespaceOfSchema("urn:foo");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int nameType = corpus.getTypeOfNamespace(xsdns, "Name");
    validator.validate("_.-_:AZ", nameType);
    validator.validate(":.-_:AZ", nameType);
    validator.validate("A.-_:AZ", nameType);
    validator.validate("\u3042.-_:\u3042\u3093", nameType); // Hiragana A and N
    validator.validate("\u4E0A\u8C37\u5353\u5DF1", nameType); // Kamiya Takuki
 
    /**
     * leading spaces removed during validation.
     * This behaviour inherits from token.
     */
    Assert.assertEquals(
         "en-US", validator.validate(" en-US", nameType));
 
    try {
      caught = false;
      // spaces are not allowed in Name.
      validator.validate("en\011-US", nameType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      // '-' cannot appear as head.
      validator.validate("-AZ", nameType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      // '.' cannot appear as head.
      validator.validate(".AZ", nameType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      // digits cannot appear as head.
      validator.validate("2AZ", nameType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      // combining char cannot appear as head.
      validator.validate("\u0300AZ", nameType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      // extender cannot appear as head.
      validator.validate("\u00B7AZ", nameType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    // Try user-defined Name-derived types.
    int name10 = corpus.getTypeOfNamespace(foons, "Name10");
 
    try {
      caught = false;
      // length does not match.
      validator.validate("ABCDEFGHIJK", name10);
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
      // '.' cannot appear as head.
      validator.validate(".ABCDEFGHZ", name10);
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
   * Test NCName syntax parser.
   */
  public void testNCNameSyntax() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                               getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int ncName = corpus.getTypeOfNamespace(xsdns, "NCName");
    validator.validate("_.-_AZ", ncName);
    validator.validate("A.-_AZ", ncName);
    validator.validate("\u3042.-_\u3042\u3093", ncName); // Hiragana A and N
    validator.validate("\u4E0A\u8C37\u5353\u5DF1", ncName); // Kamiya Takuki
 
    /**
     * leading spaces removed during validation.
     * This behaviour inherits from token.
     */
    Assert.assertEquals(
         "en-US", validator.validate(" en-US", ncName));
 
    try {
      caught = false;
      // ':' is prohibited in lexical space.
      validator.validate(":.-_:AZ", ncName);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NCNAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      // ':' is prohibited in lexical space.
      validator.validate("_.-_:AZ", ncName);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NCNAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      // spaces are not allowed. This behaviour inherits from Name.
      validator.validate("en\011-US", ncName);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      // '-' cannot appear as head.
      // This behaviour inherits from Name
      validator.validate("-AZ", ncName);
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
   * Test ID syntax parser. ID validation is identical to that of NCName.
   */
  public void testIDSyntax() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                               getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int idType = corpus.getTypeOfNamespace(xsdns, "ID");
    validator.validate("_.-_AZ", idType);
    validator.validate("\u4E0A\u8C37\u5353\u5DF1", idType); // Kamiya Takuki
 
    try {
      caught = false;
      // ':' is prohibited in lexical space.
      // This behaviour inherits from NCName
      validator.validate(":.-_:AZ", idType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NCNAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
 
  /**
   * Test IDREF syntax parser. IDREF validation is identical to that of NCName.
   */
  public void testIDREFSyntax() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                               getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int idrefType = corpus.getTypeOfNamespace(xsdns, "IDREF");
    validator.validate("_.-_AZ", idrefType);
    validator.validate("\u4E0A\u8C37\u5353\u5DF1", idrefType); // Kamiya Takuki
 
    try {
      caught = false;
      // ':' is prohibited in lexical space.
      // This behaviour inherits from NCName
      validator.validate(":.-_:AZ", idrefType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NCNAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
 
  /**
   * Test ENTITY syntax parser. ENTITY validation is identical to that of NCName.
   */
  public void testENTITYSyntax() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                               getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int entityType = corpus.getTypeOfNamespace(xsdns, "ENTITY");
    validator.validate("_.-_AZ", entityType);
    validator.validate("\u4E0A\u8C37\u5353\u5DF1", entityType); // Kamiya Takuki
 
    try {
      caught = false;
      // ':' is prohibited in lexical space.
      // This behaviour inherits from NCName
      validator.validate(":.-_:AZ", entityType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NCNAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
 
}
