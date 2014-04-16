package org.openexi.fujitsu.scomp;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;

public class GregorianValidatorTest extends TestCase {

  public GregorianValidatorTest(String name) {
    super(name);
    DatatypeFactory datatypeFactory = null;
    try {
      datatypeFactory = DatatypeFactory.newInstance();
    }
    catch(DatatypeConfigurationException dce) {
      throw new RuntimeException(dce);
    }
    finally {
      m_datatypeFactory = datatypeFactory;
    }
  }

  private final DatatypeFactory m_datatypeFactory;

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

 /**
  * Test gYearMonth syntax parser.
  */
 public void testGYearMonthSyntax() throws Exception {

   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                              getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int gYearMonth = corpus.getTypeOfNamespace(xsdns, "gYearMonth");
   
   // leading spaces removed during validation.
   Assert.assertEquals(
        "1999-05", validator.validate("   1999-05  ", gYearMonth));
   validator.validate("1999-05+09:00", gYearMonth);
   validator.validate("1999-05Z", gYearMonth);

   try {
     caught = false;
     // "" is not a legitimate gYearMonth
     validator.validate("", gYearMonth);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_GYEARMONTH, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     // month field is missing.
     validator.validate("1999", gYearMonth);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_GYEARMONTH, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
 }

 /**
  * Test gYearMonth minInclusive facet.
  */
 public void testGYearMonthMinInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minInclusive: 2003-04-05:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "gYearMonthDerived");

   validator.validate("2003-05+09:00", minInclusive);
   validator.validate("2003-04-05:00", minInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03", minInclusive);
//   try {
//     caught = false;
//     validator.validate("2003-03", minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gYearMonth maxInclusive facet.
  */
 public void testGYearMonthMaxInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxInclusive: 2003-04-05:00
   int maxInclusive = corpus.getTypeOfNamespace(foons, "gYearMonthDerived");

   validator.validate("2003-03+09:00", maxInclusive);
   validator.validate("2003-04-05:00", maxInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-06", maxInclusive);
//   try {
//     caught = false;
//     validator.validate("2003-06", maxInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gYearMonth minExclusive facet.
  */
 public void testGYearMonthMinExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minExclusive: 2003-04-05:00
   int minExclusive = corpus.getTypeOfNamespace(foons, "gYearMonthDerived");

   validator.validate("2003-06+09:00", minExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-04-05:00", minExclusive);
//   try {
//     caught = false;
//     validator.validate("2003-04-05:00", minExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_EXCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gYearMonth maxExclusive facet.
  */
 public void testGYearMonthMaxExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxExclusive: 2003-04-05:00
   int maxExclusive = corpus.getTypeOfNamespace(foons, "gYearMonthDerived");

   validator.validate("2003-02+09:00", maxExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-04-05:00", maxExclusive);
//   try {
//     caught = false;
//     validator.validate("2003-04-05:00", maxExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_EXCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gYearMonth minInclusive facet with value.
  */
 public void testGYearMonthMinInclusiveWithValue() throws Exception {

   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   // minInclusive: 2003-04-05:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "gYearMonthDerived");

   XMLGregorianCalendar dateTimeValue;
   
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("2003-05+09:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("2003-04-05:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

//   boolean caught = false;

   // REVISIT-XSOM: order relation not enforced.
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("2003-03");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);
//   caught = false;
//   try {
//     dateTimeValue = new XSDateTimeParser().parseGYearMonth("2003-03");
//     validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }
 
 /**
  * Test gMonthDay syntax parser.
  */
 public void testGMonthDaySyntax() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                              getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int gMonthDay = corpus.getTypeOfNamespace(xsdns, "gMonthDay");
   
   // leading spaces removed during validation.
   Assert.assertEquals(
        "--09-16", validator.validate("   --09-16  ", gMonthDay));
   validator.validate("--09-16+09:00", gMonthDay);
   validator.validate("--09-16Z", gMonthDay);

   try {
     caught = false;
     // "" is not a legitimate gMonthDay
     validator.validate("", gMonthDay);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_GMONTHDAY, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     validator.validate("--09", gMonthDay);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_GMONTHDAY, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
 }

 /**
  * Test gMonthDay minInclusive facet.
  */
 public void testGMonthDayMinInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minInclusive: --09-16+09:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "gMonthDayDerived");

   validator.validate("--09-17+09:00", minInclusive);
   validator.validate("--09-16+09:00", minInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("--09-14+09:00", minInclusive);
//   try {
//     caught = false;
//     validator.validate("--09-14+09:00", minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gMonthDay maxInclusive facet.
  */
 public void testGMonthDayMaxInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxInclusive: --09-16+09:00
   int maxInclusive = corpus.getTypeOfNamespace(foons, "gMonthDayDerived");

   validator.validate("--09-15+09:00", maxInclusive);
   validator.validate("--09-16+09:00", maxInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("--09-17+09:00", maxInclusive);
//   try {
//     caught = false;
//     validator.validate("--09-17+09:00", maxInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gMonthDay minExclusive facet.
  */
 public void testGMonthDayMinExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minExclusive: --09-16+09:00
   int minExclusive = corpus.getTypeOfNamespace(foons, "gMonthDayDerived");

   validator.validate("--09-17+09:00", minExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("--09-16+09:00", minExclusive);
//   try {
//     caught = false;
//     validator.validate("--09-16+09:00", minExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_EXCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gMonthDay maxExclusive facet.
  */
 public void testGMonthDayMaxExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxExclusive: --09-16+09:00
   int maxExclusive = corpus.getTypeOfNamespace(foons, "gMonthDayDerived");

   validator.validate("--09-15+09:00", maxExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("--09-16+09:00", maxExclusive);
//   try {
//     caught = false;
//     validator.validate("--09-16+09:00", maxExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_EXCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gMonthDay minInclusive facet with value.
  */
 public void testGMonthDayMinInclusiveWithValue() throws Exception {

   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   // minInclusive: --09-16+09:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "gMonthDayDerived");

   XMLGregorianCalendar dateTimeValue;
   
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("--09-17+09:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("--09-16+09:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

//   boolean caught = false;

   // REVISIT-XSOM: order relation not enforced.
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("--09-14+09:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);
//   caught = false;
//   try {
//     dateTimeValue = new XSDateTimeParser().parseGMonthDay("--09-14+09:00");
//     validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gYear syntax parser.
  */
 public void testGYearSyntax() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                              getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int gYear = corpus.getTypeOfNamespace(xsdns, "gYear");
   
   // leading spaces removed during validation.
   Assert.assertEquals(
        "1969", validator.validate("   1969  ", gYear));
   validator.validate("1969+09:00", gYear);
   validator.validate("1969Z", gYear);

   try {
     caught = false;
     // "" is not a legitimate gYear
     validator.validate("", gYear);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_GYEAR, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     validator.validate("969", gYear);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_GYEAR, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
 }

 /**
  * Test gYear minInclusive facet.
  */
 public void testGYearMinInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minInclusive: 1969+09:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "gYearDerived");

   validator.validate("1970+09:00", minInclusive);
   validator.validate("1969+09:00", minInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("1968+09:00", minInclusive);
//   try {
//     caught = false;
//     validator.validate("1968+09:00", minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gYear maxInclusive facet.
  */
 public void testGYearMaxInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxInclusive: 1969+09:00
   int maxInclusive = corpus.getTypeOfNamespace(foons, "gYearDerived");

   validator.validate("1968+09:00", maxInclusive);
   validator.validate("1969+09:00", maxInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("1970+09:00", maxInclusive);
//   try {
//     caught = false;
//     validator.validate("1970+09:00", maxInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gYear minExclusive facet.
  */
 public void testGYearMinExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minExclusive: 1969+09:00
   int minExclusive = corpus.getTypeOfNamespace(foons, "gYearDerived");

   validator.validate("1970+09:00", minExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("1969+09:00", minExclusive);
//   try {
//     caught = false;
//     validator.validate("1969+09:00", minExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_EXCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gYear maxExclusive facet.
  */
 public void testGYearMaxExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxExclusive: 1969+09:00
   int maxExclusive = corpus.getTypeOfNamespace(foons, "gYearDerived");

   validator.validate("1968+09:00", maxExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("1969+09:00", maxExclusive);
//   try {
//     caught = false;
//     validator.validate("1969+09:00", maxExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_EXCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gYear minInclusive facet with value.
  */
 public void testGYearMinInclusiveWithValue() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   // minInclusive: 1969+09:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "gYearDerived");

   XMLGregorianCalendar dateTimeValue;
   
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("1970+09:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("1969+09:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

//   boolean caught = false;

   // REVISIT-XSOM: order relation not enforced.
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("1968+09:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);
//   caught = false;
//   try {
//     dateTimeValue = new XSDateTimeParser().parseGYear("1968+09:00");
//     validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gMonth syntax parser.
  */
 public void testGMonthSyntax() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                              getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int gMonth = corpus.getTypeOfNamespace(xsdns, "gMonth");
   
   // leading spaces removed during validation.
   Assert.assertEquals("--09--", validator.validate("   --09--  ", gMonth));
   validator.validate("--09--+09:00", gMonth);
   validator.validate("--09--Z", gMonth);

   try {
     caught = false;
     // "" is not a legitimate gMonth
     validator.validate("", gMonth);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_GMONTH, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     validator.validate("09--", gMonth);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_GMONTH, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   /**
    * REVISIT: restore this test case.
    * 
    * try {
    *   caught = false;
    *   validator.validate("--09--", gMonth);
    * }
    * catch (SchemaValidatorException sve) {
    *   caught = true;
    *   Assert.assertEquals(SchemaValidatorException.INVALID_GMONTH, sve.getCode());
    * }
    * finally {
    *   if (!caught)
    *     Assert.fail("The operation should have resulted in an exception.");
    * }
    */
 }

 /**
  * Test gMonth minInclusive facet.
  */
 public void testGMonthMinInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minInclusive: --09+09:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "gMonthDerived");

   validator.validate("--10--+09:00", minInclusive);
   validator.validate("--09--+09:00", minInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("--08--+09:00", minInclusive);
//   try {
//     caught = false;
//     validator.validate("--08+09:00", minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gMonth maxInclusive facet.
  */
 public void testGMonthMaxInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxInclusive: --09+09:00
   int maxInclusive = corpus.getTypeOfNamespace(foons, "gMonthDerived");

   validator.validate("--08--+09:00", maxInclusive);
   validator.validate("--09--+09:00", maxInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("--10--+09:00", maxInclusive);
//   try {
//     caught = false;
//     validator.validate("--10+09:00", maxInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gMonth minExclusive facet.
  */
 public void testGMonthMinExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minExclusive: --09+09:00
   int minExclusive = corpus.getTypeOfNamespace(foons, "gMonthDerived");

   validator.validate("--10--+09:00", minExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("--09--+09:00", minExclusive);
//   try {
//     caught = false;
//     validator.validate("--09+09:00", minExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_EXCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gMonth maxExclusive facet.
  */
 public void testGMonthMaxExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxExclusive: --09+09:00
   int maxExclusive = corpus.getTypeOfNamespace(foons, "gMonthDerived");

   validator.validate("--08--+09:00", maxExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("--09--+09:00", maxExclusive);
//   try {
//     caught = false;
//     validator.validate("--09+09:00", maxExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_EXCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gMonth minInclusive facet with value.
  */
 public void testGMonthMinInclusiveWithValue() throws Exception {

   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   // minInclusive: --09+09:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "gMonthDerived");

   XMLGregorianCalendar dateTimeValue;
   
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("--10--+09:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("--09--+09:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

//   boolean caught = false;

   // REVISIT-XSOM: order relation not enforced.
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("--08--+09:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);
//   caught = false;
//   try {
//     dateTimeValue = new XSDateTimeParser().parseGMonth("--08+09:00");
//     validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gDay syntax parser.
  */
 public void testGDaySyntax() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                              getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int gDay = corpus.getTypeOfNamespace(xsdns, "gDay");

   // leading spaces removed during validation.
   Assert.assertEquals("---16", validator.validate("   ---16  ", gDay));
   validator.validate("---16+09:00", gDay);
   validator.validate("---16Z", gDay);

   try {
     caught = false;
     // "" is not a legitimate gDay
     validator.validate("", gDay);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_GDAY, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     validator.validate("---6", gDay);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_GDAY, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
 }

 /**
  * Test gDay minInclusive facet.
  */
 public void testGDayMinInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minInclusive: ---16+09:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "gDayDerived");

   validator.validate("---17+09:00", minInclusive);
   validator.validate("---16+09:00", minInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("---15+09:00", minInclusive);
//   try {
//     caught = false;
//     validator.validate("---15+09:00", minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gDay maxInclusive facet.
  */
 public void testGDayMaxInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxInclusive: ---16+09:00
   int maxInclusive = corpus.getTypeOfNamespace(foons, "gDayDerived");

   validator.validate("---15+09:00", maxInclusive);
   validator.validate("---16+09:00", maxInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("---17+09:00", maxInclusive);
//   try {
//     caught = false;
//     validator.validate("---17+09:00", maxInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gDay minExclusive facet.
  */
 public void testGDayMinExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minExclusive: ---16+09:00
   int minExclusive = corpus.getTypeOfNamespace(foons, "gDayDerived");

   validator.validate("---17+09:00", minExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("---16+09:00", minExclusive);
//   try {
//     caught = false;
//     validator.validate("---16+09:00", minExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_EXCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gDay maxExclusive facet.
  */
 public void testGDayMaxExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxExclusive: ---16+09:00
   int maxExclusive = corpus.getTypeOfNamespace(foons, "gDayDerived");

   validator.validate("---15+09:00", maxExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("---16+09:00", maxExclusive);
//   try {
//     caught = false;
//     validator.validate("---16+09:00", maxExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_EXCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test gDay minInclusive facet with value.
  */
 public void testGDayMinInclusiveWithValue() throws Exception {

   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   // minInclusive: ---16+09:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "gDayDerived");

   XMLGregorianCalendar dateTimeValue;
   
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("---17+09:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("---16+09:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

//   boolean caught = false;

   // REVISIT-XSOM: order relation not enforced.
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("---15+09:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);
//   caught = false;
//   try {
//     dateTimeValue = new XSDateTimeParser().parseGDay("---15+09:00");
//     validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

}
