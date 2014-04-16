package org.openexi.fujitsu.scomp;

import java.math.BigDecimal;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.AtomicTypedValue;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;
import org.openexi.fujitsu.schema.SimpleTypeValidationInfo;
import org.openexi.fujitsu.schema.XSDecimal;

public class DecimalValidatorTest extends TestCase {

  public DecimalValidatorTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

 /**
  * Test decimal syntax parser.
  */
 public void testDecimalSyntax() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/decimalDigits.xsd", getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int decimal = corpus.getTypeOfNamespace(xsdns, "decimal");
   validator.validate("0123456789", decimal);
   validator.validate("0123456789.", decimal);
   validator.validate(".0123456789", decimal);
   validator.validate("+0123456789", decimal);
   validator.validate("-0123456789", decimal);
   validator.validate("-.0123456789", decimal);

   validator.validate(".000", decimal);
   validator.validate("+.000", decimal);
   validator.validate("-.000", decimal);

   try {
     caught = false;
     validator.validate("", decimal);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_DECIMAL, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     validator.validate("+", decimal);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_DECIMAL, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     validator.validate("0123456789A", decimal);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_DECIMAL, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     validator.validate("@0123456789", decimal);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_DECIMAL, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     validator.validate("0123456789..0", decimal);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_DECIMAL, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     // zero-length decimal
     validator.validate("-.", decimal);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_DECIMAL, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
 }

 /**
  * Test decimal total digits.
  */
 public void testDecimalTotalDigits() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/decimalDigits.xsd", getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int totalDigits6 = corpus.getTypeOfNamespace(foons, "totalDigits6");

   validator.validate("123456", totalDigits6);
   validator.validate("0123456", totalDigits6);
   validator.validate("00123456", totalDigits6);

   validator.validate("12345.6", totalDigits6);

   try {
     caught = false;
     validator.validate("1234567", totalDigits6);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.TOTAL_DIGITS_INVALID, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     validator.validate("1230456", totalDigits6);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.TOTAL_DIGITS_INVALID,
                       sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   validator.validate("12345.60", totalDigits6);
   /*
    * NOTE: trailing zeros in fraction are no longer considered significant.
    * try {
    *   caught = false;
    *   validator.validate("12345.60", totalDigits6);
    * }
    * catch (SchemaValidatorException sve) {
    *   caught = true;
    *   Assert.assertEquals(SchemaValidatorException.TOTAL_DIGITS_INVALID,
    *                     sve.getCode());
    * }
    * finally {
    *   if (!caught)
    *     Assert.fail("The operation should have resulted in an exception.");
    * }
    */

   validator.validate("123456.0", totalDigits6);
   /*
    * NOTE: trailing zeros in fraction are no longer considered significant.
    * try {
    *   caught = false;
    *   validator.validate("123456.0", totalDigits6);
    * }
    * catch (SchemaValidatorException sve) {
    *   caught = true;
    *   Assert.assertEquals(SchemaValidatorException.TOTAL_DIGITS_INVALID,
    *                     sve.getCode());
    * }
    * finally {
    *   if (!caught)
    *     Assert.fail("The operation should have resulted in an exception.");
    * }
    */

   validator.validate("123456.", totalDigits6);
   /*
    * NOTE: trailing zeros in fraction are no longer considered significant.
    * try {
    *   caught = false;
    *   validator.validate("123456.", totalDigits6);
    * }
    * catch (SchemaValidatorException sve) {
    *   caught = true;
    *   Assert.assertEquals(SchemaValidatorException.TOTAL_DIGITS_INVALID,
    *                     sve.getCode());
    * }
    * finally {
    *   if (!caught)
    *     Assert.fail("The operation should have resulted in an exception.");
    * }
    */
 }

 /**
  * Test decimal fraction digits.
  */
 public void testDecimalFractionDigits() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/decimalDigits.xsd", getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int fractionDigits5 = corpus.getTypeOfNamespace(foons, "fractionDigits5");

   validator.validate("987.12345", fractionDigits5);

   try {
     caught = false;
     validator.validate("987.123456", fractionDigits5);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.FRACTION_DIGITS_INVALID, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   validator.validate("987.123450", fractionDigits5);
   /*
    * NOTE: trailing zeros in fraction are no longer considered significant.
    * try {
    *   caught = false;
    *   validator.validate("987.123450", fractionDigits5);
    * }
    * catch (SchemaValidatorException sve) {
    *   caught = true;
    *   Assert.assertEquals(SchemaValidatorException.FRACTION_DIGITS_INVALID, sve.getCode());
    * }
    * finally {
    *   if (!caught)
    *     Assert.fail("The operation should have resulted in an exception.");
    * }
    */

   SimpleTypeValidationInfo stvi;
   XSDecimal dec;
   
   stvi = new SimpleTypeValidationInfo();
   validator.validate("0.0000", fractionDigits5, stvi);
   
   dec = (XSDecimal)((AtomicTypedValue)stvi.getTypedValue()).getValue();
   Assert.assertEquals(0, dec.getFractionDigits());
   Assert.assertEquals(4, dec.getTrailingZeros());
   Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());

   stvi = new SimpleTypeValidationInfo();
   validator.validate("-0.00000", fractionDigits5, stvi);
   dec = (XSDecimal)((AtomicTypedValue)stvi.getTypedValue()).getValue();
   Assert.assertEquals(0, dec.getFractionDigits());
   Assert.assertEquals(5, dec.getTrailingZeros());
   Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());
   
   stvi = new SimpleTypeValidationInfo();
   validator.validate("0.", fractionDigits5, stvi);
   dec = (XSDecimal)((AtomicTypedValue)stvi.getTypedValue()).getValue();
   Assert.assertEquals(0, dec.getFractionDigits());
   Assert.assertEquals(1, dec.getTrailingZeros());
   Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());
   
   stvi = new SimpleTypeValidationInfo();
   validator.validate("0", fractionDigits5, stvi);
   dec = (XSDecimal)((AtomicTypedValue)stvi.getTypedValue()).getValue();
   Assert.assertEquals(0, dec.getFractionDigits());
   Assert.assertEquals(0, dec.getTrailingZeros());
   Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());
 }

 /**
  * Test decimal value range.
  */
 public void testDecimalValueRange() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/decimalRange.xsd", getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int minInclusive = corpus.getTypeOfNamespace(foons, "minInclusive");

   validator.validate("987.12345", minInclusive);
   validator.validate("-6.00310", minInclusive);

   try {
     caught = false;
     validator.validate("-987.12345", minInclusive);
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

   validator.validate("0.12345", maxInclusive);
   validator.validate("6.00310", maxInclusive);

   try {
     caught = false;
     validator.validate("10", maxInclusive);
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
     validator.validate("-6.00310", minExclusive);
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

   validator.validate("0.12345", maxExclusive);

   try {
     caught = false;
     validator.validate("6.00310", maxExclusive);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.MAX_EXCLUSIVE_INVALID, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   int minInclusiveInt = corpus.getTypeOfNamespace(foons, "minInclusiveInt");

   validator.validate("10", minInclusiveInt);
   validator.validate("-6", minInclusiveInt);

   try {
     caught = false;
     validator.validate("-10", minInclusiveInt);
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
  * Test decimal value range with value.
  */
 public void testDecimalValueRangeWithValue() throws Exception {

   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/decimalRange.xsd", getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   int minInclusive = corpus.getTypeOfNamespace(foons, "minInclusive");

   String textualDecimal;
   
   textualDecimal = "987.12345";
   validator.validateAtomicValue(textualDecimal, new XSDecimal(new BigDecimal(textualDecimal)), minInclusive);

   textualDecimal = "-6.00310";
   validator.validateAtomicValue(textualDecimal, new XSDecimal(new BigDecimal(textualDecimal)), minInclusive);

   boolean caught = false;

   caught = false;
   try {
     textualDecimal = "-987.12345";
     validator.validateAtomicValue(textualDecimal, new XSDecimal(new BigDecimal(textualDecimal)), minInclusive);
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

   textualDecimal = "0.12345";
   validator.validateAtomicValue(textualDecimal, new XSDecimal(new BigDecimal(textualDecimal)), maxInclusive);

   textualDecimal = "6.00310";
   validator.validateAtomicValue(textualDecimal, new XSDecimal(new BigDecimal(textualDecimal)), maxInclusive);

   caught = false;
   try {
     textualDecimal = "10";
     validator.validateAtomicValue(textualDecimal, new XSDecimal(new BigDecimal(textualDecimal)), maxInclusive);
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

   textualDecimal = "987.12345";
   validator.validateAtomicValue(textualDecimal, new XSDecimal(new BigDecimal(textualDecimal)), minExclusive);

   caught = false;
   try {
     textualDecimal = "-6.00310";
     validator.validateAtomicValue(textualDecimal, new XSDecimal(new BigDecimal(textualDecimal)), minExclusive);
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

   textualDecimal = "0.12345";
   validator.validateAtomicValue(textualDecimal, new XSDecimal(new BigDecimal(textualDecimal)), maxExclusive);

   caught = false;
   try {
     textualDecimal = "6.00310";
     validator.validateAtomicValue(textualDecimal, new XSDecimal(new BigDecimal(textualDecimal)), maxExclusive);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.MAX_EXCLUSIVE_INVALID, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   int minInclusiveInt = corpus.getTypeOfNamespace(foons, "minInclusiveInt");

   textualDecimal = "10";
   validator.validateAtomicValue(textualDecimal, new XSDecimal(new BigDecimal(textualDecimal)), minInclusiveInt);

   textualDecimal = "-6";
   validator.validateAtomicValue(textualDecimal, new XSDecimal(new BigDecimal(textualDecimal)), minInclusiveInt);

   caught = false;
   try {
     textualDecimal = "-10";
     validator.validateAtomicValue(textualDecimal, new XSDecimal(new BigDecimal(textualDecimal)), minInclusiveInt);
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
  * Test Integer value space.
  */
 public void testIntegerValueSpace() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/decimalRange.xsd", getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int longType = corpus.getTypeOfNamespace(xsdns, "integer");

   validator.validate("92233720368547758070000000000", longType);
   validator.validate("-92233720368547758080000000000", longType);

   try {
     caught = false;
     validator.validate("123.0", longType);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_INTEGER, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
 }

}
