package org.openexi.fujitsu.scomp;
 
import junit.framework.Assert;
import junit.framework.TestCase;
 
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;

public class TokenValidatorTest extends TestCase {
 
  public TokenValidatorTest(String name) {
    super(name);
  }
 
  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////
 
  /**
   * Test token syntax parser.
   */
  public void testTokenSyntax() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                               getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    int token = corpus.getTypeOfNamespace(xsdns, "token");
    validator.validate("FUJITSU LIMITED", token);
 
    // internal sequence of two or more spaces gets collapsed
    Assert.assertEquals(
         "FUJITSU LIMITED",
         validator.validate("FUJITSU  LIMITED", token));
    // leading space removed
    Assert.assertEquals(
         "FUJITSU", validator.validate(" FUJITSU", token));
    // trailing space
    Assert.assertEquals(
         "FUJITSU", validator.validate("FUJITSU ", token));
 
    /**
     * tab '\011' becomes a space during validation.
     * This behaviour inherits from normalizedString.
     */
    Assert.assertEquals(
         "FUJITSU LIMITED",
         validator.validate("FUJITSU\011LIMITED", token));
  }
 
  /**
   * Test NMTOKEN syntax parser.
   */
  public void testNMTOKENSyntax() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                               getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int nmtoken = corpus.getTypeOfNamespace(xsdns, "NMTOKEN");
    validator.validate("_.-_:AZ", nmtoken);
    validator.validate(":.-_:AZ", nmtoken);
    validator.validate(".-_:AZ", nmtoken);
    validator.validate("-_:AZ", nmtoken);
    validator.validate("9.-_:AZ", nmtoken);
    validator.validate("\u3042.-_:\u3042\u3093", nmtoken); // Hiragana A and N
    validator.validate("\u4E0A\u8C37\u5353\u5DF1", nmtoken); // Kamiya Takuki
 
    /*
     * leading spaces removed during validation.
     * This behaviour inherits from token.
     */
    Assert.assertEquals(
         "en-US", validator.validate(" en-US", nmtoken));
 
    try {
      caught = false;
      // spaces are not allowed in NMTOKEN.
      validator.validate("en\011-US", nmtoken);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NMTOKEN, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("A\u005BZ", nmtoken);
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
 
}
