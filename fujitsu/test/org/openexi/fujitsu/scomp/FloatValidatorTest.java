package org.openexi.fujitsu.scomp;
 
import junit.framework.Assert;
import junit.framework.TestCase;
 
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;

public class FloatValidatorTest extends TestCase {
 
  public FloatValidatorTest(String name) {
    super(name);
  }
 
  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////
 
  /**
   * Test float syntax.
   */
  public void testFloatSyntax() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/floatRange.xsd", getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int floatType = corpus.getTypeOfNamespace(xsdns, "float");
    validator.validate("-1E4", floatType);
    validator.validate("1267.43233E12", floatType);
    validator.validate("12.78e-2", floatType);
    validator.validate("12", floatType);
    validator.validate("0", floatType);
    validator.validate("-0", floatType);
    validator.validate("INF", floatType);
    validator.validate("-INF", floatType);
    validator.validate("NaN", floatType);
 
    try {
      caught = false;
      validator.validate("", floatType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_FLOAT, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("JNF", floatType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_FLOAT, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("MaN", floatType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_FLOAT, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("FUJITSU", floatType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_FLOAT, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
 
  /**
   * Test float value range.
   */
  public void testFloatValueRange() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/floatRange.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int minInclusive = corpus.getTypeOfNamespace(foons, "minInclusive");
 
    validator.validate("987.12345", minInclusive);
    validator.validate("3.14", minInclusive);
 
    try {
      caught = false;
      validator.validate("3.10", minInclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
    
    int maxInclusive = corpus.getTypeOfNamespace(foons, "maxInclusive");
 
    validator.validate("1.01", maxInclusive);
    validator.validate("3.14", maxInclusive);
    
    try {
      caught = false;
      validator.validate("3.21", maxInclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    int minExclusive = corpus.getTypeOfNamespace(foons, "minExclusive");
 
    validator.validate("987.12345", minExclusive);
 
    try {
      caught = false;
      validator.validate("3.14", minExclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_EXCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    int maxExclusive = corpus.getTypeOfNamespace(foons, "maxExclusive");
 
    validator.validate("1.12345", maxExclusive);
 
    try {
      caught = false;
      validator.validate("3.14", maxExclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_EXCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
    
  }
 
  /**
   * Test float value range with value.
   */
  public void testFloatValueRangeWithValue() throws Exception {
    
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/floatRange.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    int minInclusive = corpus.getTypeOfNamespace(foons, "minInclusive");
 
    String textualFloat;
    
    textualFloat = "987.12345";
    validator.validateAtomicValue(textualFloat, Float.valueOf(textualFloat), minInclusive);
 
    textualFloat = "3.14";
    validator.validateAtomicValue(textualFloat, Float.valueOf(textualFloat), minInclusive);
 
    boolean caught = false;
 
    caught = false;
    try {
      textualFloat = "3.10";
      validator.validateAtomicValue(textualFloat, Float.valueOf(textualFloat), minInclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
    
    int maxInclusive = corpus.getTypeOfNamespace(foons, "maxInclusive");
 
    textualFloat = "1.01";
    validator.validateAtomicValue(textualFloat, Float.valueOf(textualFloat), maxInclusive);
 
    textualFloat = "3.14";
    validator.validateAtomicValue(textualFloat, Float.valueOf(textualFloat), maxInclusive);
    
    caught = false;
    try {
      textualFloat = "3.21";
      validator.validateAtomicValue(textualFloat, Float.valueOf(textualFloat), maxInclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    int minExclusive = corpus.getTypeOfNamespace(foons, "minExclusive");
 
    textualFloat = "987.12345";
    validator.validateAtomicValue(textualFloat, Float.valueOf(textualFloat), minExclusive);
 
    caught = false;
    try {
      textualFloat = "3.14";
      validator.validateAtomicValue(textualFloat, Float.valueOf(textualFloat), minExclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_EXCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    int maxExclusive = corpus.getTypeOfNamespace(foons, "maxExclusive");
 
    textualFloat = "1.12345";
    validator.validateAtomicValue(textualFloat, Float.valueOf(textualFloat), maxExclusive);
 
    caught = false;
    try {
      textualFloat = "3.14";
      validator.validateAtomicValue(textualFloat, Float.valueOf(textualFloat), maxExclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_EXCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
  
  /**
   * Test double syntax.
   */
  public void testDoubleSyntax() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/floatRange.xsd", getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int doubleType = corpus.getTypeOfNamespace(xsdns, "double");
    validator.validate("-1E4", doubleType);
    validator.validate("1267.43233E12", doubleType);
    validator.validate("12.78e-2", doubleType);
    validator.validate("12", doubleType);
    validator.validate("0", doubleType);
    validator.validate("-0", doubleType);
    validator.validate("INF", doubleType);
    validator.validate("-INF", doubleType);
    validator.validate("NaN", doubleType);
 
    try {
      caught = false;
      validator.validate("", doubleType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_DOUBLE, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("JNF", doubleType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_DOUBLE, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("MaN", doubleType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_DOUBLE, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    try {
      caught = false;
      validator.validate("FUJITSU", doubleType);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_DOUBLE, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
   
  /**
   * Test double value range.
   */
  public void testDoubleValueRange() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/doubleRange.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    boolean caught = false;
 
    int minInclusive = corpus.getTypeOfNamespace(foons, "minInclusive");
 
    validator.validate("987.12345", minInclusive);
    validator.validate("3.14", minInclusive);
 
    try {
      caught = false;
      validator.validate("3.10", minInclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
    
    int maxInclusive = corpus.getTypeOfNamespace(foons, "maxInclusive");
 
    validator.validate("1.01", maxInclusive);
    validator.validate("3.14", maxInclusive);
    
    try {
      caught = false;
      validator.validate("3.21", maxInclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    int minExclusive = corpus.getTypeOfNamespace(foons, "minExclusive");
 
    validator.validate("987.12345", minExclusive);
 
    try {
      caught = false;
      validator.validate("3.14", minExclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_EXCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    int maxExclusive = corpus.getTypeOfNamespace(foons, "maxExclusive");
 
    validator.validate("1.12345", maxExclusive);
 
    try {
      caught = false;
      validator.validate("3.14", maxExclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_EXCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
 
  /**
   * Test double value range with value.
   */
  public void testDoubleValueRangeWithValue() throws Exception {
 
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/doubleRange.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");
 
    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
 
    int minInclusive = corpus.getTypeOfNamespace(foons, "minInclusive");
 
    String textualDouble;
    
    textualDouble = "987.12345";
    validator.validateAtomicValue(textualDouble, Double.valueOf(textualDouble), minInclusive);
 
    textualDouble = "3.14";
    validator.validateAtomicValue(textualDouble, Double.valueOf(textualDouble), minInclusive);
 
    boolean caught;
 
    caught = false;
    try {
      textualDouble = "3.10";
      validator.validateAtomicValue(textualDouble, Double.valueOf(textualDouble), minInclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
    
    int maxInclusive = corpus.getTypeOfNamespace(foons, "maxInclusive");
 
    textualDouble = "1.01";
    validator.validateAtomicValue(textualDouble, Double.valueOf(textualDouble), maxInclusive);
 
    textualDouble = "3.14";
    validator.validateAtomicValue(textualDouble, Double.valueOf(textualDouble), maxInclusive);
    
    caught = false;
    try {
      textualDouble = "3.21";
      validator.validateAtomicValue(textualDouble, Double.valueOf(textualDouble), maxInclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    int minExclusive = corpus.getTypeOfNamespace(foons, "minExclusive");
 
    textualDouble = "987.12345";
    validator.validateAtomicValue(textualDouble, Double.valueOf(textualDouble), minExclusive);
 
    caught = false;
    try {
      textualDouble = "3.14";
      validator.validateAtomicValue(textualDouble, Double.valueOf(textualDouble), minExclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MIN_EXCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
 
    int maxExclusive = corpus.getTypeOfNamespace(foons, "maxExclusive");
 
    textualDouble = "1.12345";
    validator.validateAtomicValue(textualDouble, Double.valueOf(textualDouble), maxExclusive);
 
    caught = false;
    try {
      textualDouble = "3.14";
      validator.validateAtomicValue(textualDouble, Double.valueOf(textualDouble), maxExclusive);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.MAX_EXCLUSIVE_INVALID, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }
 
}
