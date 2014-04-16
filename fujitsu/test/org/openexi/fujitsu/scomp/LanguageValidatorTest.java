package org.openexi.fujitsu.scomp;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;

public class LanguageValidatorTest extends TestCase {

  public LanguageValidatorTest(String name) {
    super(name);
  }

  protected void setUp() throws Exception {
    super.setUp();

    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                               getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
    m_validator =  new SimpleTypeValidator(corpus);
    m_language = corpus.getTypeOfNamespace(xsdns, "language");
  }

  private SimpleTypeValidator m_validator;
  private int m_language;

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Test language syntax traits inherited from token.
   */
  public void testLanguageHasTokenTraits() throws Exception {

    m_validator.validate("en-US", m_language);

    /**
     * leading spaces removed during validation.
     * This behaviour inherits from token.
     */
    Assert.assertEquals(
        "en-US", m_validator.validate(" en-US", m_language));

    /**
     * trailing spaces removed during validation.
     * This behaviour inherits from token.
     */
    Assert.assertEquals(
        "en-US", m_validator.validate("en-US\011", m_language));
  }

  /**
   * There can be any number of subtags
   */
  public void testLanguageNumberOfTags() throws Exception {
    m_validator.validate("ja", m_language);
    m_validator.validate("ja-JP", m_language);
    m_validator.validate("ja-JP-US", m_language);
    m_validator.validate("ja-JP-US-JP", m_language);
    m_validator.validate("ja-JP-US-JP-US", m_language);
  }

  /**
   * No tags are available.
   */
  public void testLanguageNoTags() throws Exception {
    try {
      m_validator.validate("", m_language);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE, sve.getCode());
      SchemaValidatorException nested = (SchemaValidatorException)sve.getException();
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE_PRIMARY_TAG,
                        nested.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Primary tags cannot be empty.
   */
  public void testLanguageEmptyPrimaryTag() throws Exception {
    try {
      m_validator.validate("-US", m_language);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE, sve.getCode());
      SchemaValidatorException nested = (SchemaValidatorException)sve.getException();
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE_PRIMARY_TAG,
                        nested.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Primary tags cannot be more than 3 characters.
   */
  public void testLanguagePrimaryTagTooLong() throws Exception {
    m_validator.validate("abc-US", m_language);
    try {
      m_validator.validate("abcd-US", m_language);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE, sve.getCode());
      SchemaValidatorException nested = (SchemaValidatorException)sve.getException();
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE_PRIMARY_TAG,
                        nested.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Primary tag cannot be a single character unless it is "i" or "x".
   */
  public void testLanguagePrimaryTagShort() throws Exception {
    m_validator.validate("i-US", m_language);
    m_validator.validate("x-US", m_language);
    try {
      m_validator.validate("a-US", m_language);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE, sve.getCode());
      SchemaValidatorException nested = (SchemaValidatorException)sve.getException();
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE_PRIMARY_TAG,
                        nested.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Whitespaces are not permitted in primary tag of a language.
   */
  public void testLanguagePrimaryTagSpace() throws Exception {
    try {
      m_validator.validate("en\011-US", m_language);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE, sve.getCode());
      SchemaValidatorException nested = (SchemaValidatorException)sve.getException();
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE_PRIMARY_TAG,
                        nested.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Digits are not permitted in primary tag of a language.
   */
  public void testLanguagePrimaryTagDigits() throws Exception {
    try {
      m_validator.validate("e8-US", m_language);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE, sve.getCode());
      SchemaValidatorException nested = (SchemaValidatorException)sve.getException();
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE_PRIMARY_TAG,
                        nested.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Symbols are not permitted in primary tag of a language.
   */
  public void testLanguagePrimaryTagSymbols() throws Exception {
    try {
      m_validator.validate("e$-US", m_language);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE, sve.getCode());
      SchemaValidatorException nested = (SchemaValidatorException)sve.getException();
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE_PRIMARY_TAG,
                        nested.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Subsequent tags cannot be empty.
   */
  public void testLanguageEmptySubsequentTag() throws Exception {
    try {
      m_validator.validate("en-", m_language);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE, sve.getCode());
      SchemaValidatorException nested = (SchemaValidatorException)sve.getException();
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE_SUBSEQUENT_TAG,
                        nested.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Second tag cannot be more than 8 characters.
   */
  public void testLanguageSecondTagTooLong() throws Exception {
    m_validator.validate("en-ABCDEFGH", m_language);
    try {
      m_validator.validate("en-ABCDEFGHI", m_language);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE, sve.getCode());
      SchemaValidatorException nested = (SchemaValidatorException)sve.getException();
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE_SUBSEQUENT_TAG,
                        nested.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Second tag cannot be a single character.
   */
  public void testLanguageSecondTagTooShort() throws Exception {
    try {
      m_validator.validate("en-a", m_language);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE, sve.getCode());
      SchemaValidatorException nested = (SchemaValidatorException)sve.getException();
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE_SUBSEQUENT_TAG,
                        nested.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Subsequent tag cannot be more than 8 characters.
   */
  public void testLanguageSubsequentTagTooLong() throws Exception {
    m_validator.validate("en-US-ABCDEFGH", m_language);
    try {
      m_validator.validate("en-US-ABCDEFGHI", m_language);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE, sve.getCode());
      SchemaValidatorException nested = (SchemaValidatorException)sve.getException();
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE_SUBSEQUENT_TAG,
                        nested.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Subsequent tag can be a single character.
   */
  public void testLanguageSubsequentTagShort() throws Exception {
    m_validator.validate("en-US-a", m_language);
  }

  /**
   * Whitespaces are not permitted in subsequent tag of a language.
   */
  public void testLanguageSubsequentTagSpace() throws Exception {
    try {
      m_validator.validate("en-\011US", m_language);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE, sve.getCode());
      SchemaValidatorException nested = (SchemaValidatorException)sve.getException();
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE_SUBSEQUENT_TAG,
                        nested.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Symbols are not permitted in subsequent tag of a language.
   */
  public void testLanguageSubsequentTagSymbols() throws Exception {
    try {
      m_validator.validate("en-U$", m_language);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE, sve.getCode());
      SchemaValidatorException nested = (SchemaValidatorException)sve.getException();
      Assert.assertEquals(SchemaValidatorException.INVALID_LANGUAGE_SUBSEQUENT_TAG,
                        nested.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception.");
  }

  /**
   * Digits are permitted in subsequent tag of a language.
   */
  public void testLanguageSubsequentTagDigits() throws Exception {
      m_validator.validate("en-U8", m_language);
      m_validator.validate("en-US-U8", m_language);
  }

}  