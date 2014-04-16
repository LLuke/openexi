package org.openexi.fujitsu.scomp;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;

public class DateTimeValidatorTest extends TestCase {

  public DateTimeValidatorTest(String name) {
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
  * Test dateTime syntax parser.
  */
 public void testDateTimeSyntax() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                              getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int dateTime = corpus.getTypeOfNamespace(xsdns, "dateTime");
   
   // leading spaces removed during validation.
   Assert.assertEquals(
        "2003-04-25T11:41:30.45+09:00",
        validator.validate("   2003-04-25T11:41:30.45+09:00  ", dateTime));
   validator.validate("2003-04-25T11:41:30.45+14:00", dateTime);
   validator.validate("1997-07-16T19:20:30.45-12:00", dateTime);
   validator.validate("-0601-07-16T19:20:30.45-05:00", dateTime);
   validator.validate("1997-07-16T19:20:30.45Z", dateTime);
   validator.validate("1999-12-31T24:00:00", dateTime);
   
   try {
     caught = false;
     // "" is not a legitimate dateTime
     validator.validate("", dateTime);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_DATETIME, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

    try {
      caught = false;
      // year field has to match the pattern "CCYY".
      validator.validate("-601-07-16T19:20:30.45-05:00", dateTime);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_DATETIME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
      
   try {
     caught = false;
     // "J" is not a valid timezone indicator
     validator.validate("1997-07-16T19:20:30.45J", dateTime);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_DATETIME, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
   
   try {
     caught = false;
     // hour 24 is permitted only if minute and second parts are both zero. 
     validator.validate("1999-09-16T24:01:00", dateTime);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_DATETIME, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
 }

 /**
  * Test dateTime minInclusive facet.
  */
 public void testDateTimeMinInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minInclusive: 2003-03-19T13:20:00-05:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "dateTimeDerived");

   validator.validate("2003-04-25T11:41:30.45+09:00", minInclusive);
   validator.validate("2003-03-19T13:20:00-05:00", minInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("1997-07-16T19:20:30.45Z", minInclusive);
//   try {
//     caught = false;
//     validator.validate("1997-07-16T19:20:30.45Z", minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03-19T13:20:00", minInclusive);
//   try {
//     caught = false;
//     // order equivocalness
//     validator.validate("2003-03-19T13:20:00", minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.ORDER_EQUIVOCAL, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test dateTime maxInclusive facet.
  */
 public void testDateTimeMaxInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxInclusive: 2003-03-19T13:20:00-05:00
   int maxInclusive = corpus.getTypeOfNamespace(foons, "dateTimeDerived");

   validator.validate("1995-04-25T11:41:30.45+09:00", maxInclusive);
   validator.validate("2003-03-19T13:20:00-05:00", maxInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-07-16T19:20:30.45Z", maxInclusive);
//   try {
//     caught = false;
//     validator.validate("2003-07-16T19:20:30.45Z", maxInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03-19T13:20:00", maxInclusive);
//   try {
//     caught = false;
//     // order equivocalness
//     validator.validate("2003-03-19T13:20:00", maxInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.ORDER_EQUIVOCAL, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test dateTime minExclusive facet.
  */
 public void testDateTimeMinExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minExclusive: 2003-03-19T13:20:00-05:00
   int minExclusive = corpus.getTypeOfNamespace(foons, "dateTimeDerived");

   validator.validate("2003-04-25T11:41:30.45+09:00", minExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03-19T13:20:00-05:00", minExclusive);
//   try {
//     caught = false;
//     // same dateTime is excluded.
//     validator.validate("2003-03-19T13:20:00-05:00", minExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_EXCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03-19T13:20:00", minExclusive);
//   try {
//     caught = false;
//     // order equivocalness
//     validator.validate("2003-03-19T13:20:00", minExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.ORDER_EQUIVOCAL, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test dateTime maxExclusive facet.
  */
 public void testDateTimeMaxExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxExclusive: 2003-03-19T13:20:00-05:00
   int maxExclusive = corpus.getTypeOfNamespace(foons, "dateTimeDerived");

   validator.validate("2001-04-25T11:41:30.45+09:00", maxExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03-19T13:20:00-05:00", maxExclusive);
//   try {
//     caught = false;
//     // same dateTime is excluded.
//     validator.validate("2003-03-19T13:20:00-05:00", maxExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_EXCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03-19T13:20:00", maxExclusive);
//   try {
//     caught = false;
//     // order equivocalness
//     validator.validate("2003-03-19T13:20:00", maxExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.ORDER_EQUIVOCAL, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test dateTime maxInclusive facet with value.
  */
 public void testDateTimeMaxInclusiveWithValue() throws Exception {

   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   // maxInclusive: 2003-03-19T13:20:00-05:00
   int maxInclusive = corpus.getTypeOfNamespace(foons, "dateTimeDerived");

   XMLGregorianCalendar dateTimeValue;
   
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("1995-04-25T11:41:30.45+09:00"); 
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, maxInclusive);
   
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("2003-03-19T13:20:00-05:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, maxInclusive);

//   boolean caught = false;

   // REVISIT-XSOM: order relation not enforced.
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("2003-07-16T19:20:30.45Z");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, maxInclusive);
//   caught = false;
//   try {
//     dateTimeValue = new XSDateTimeParser().parseDateTime("2003-07-16T19:20:30.45Z");
//     validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, maxInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }

   // REVISIT-XSOM: order relation not enforced.
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("2003-03-19T13:20:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, maxInclusive);
//   caught = false;
//   try {
//     // order equivocalness
//     dateTimeValue = new XSDateTimeParser().parseDateTime("2003-03-19T13:20:00");
//     validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, maxInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.ORDER_EQUIVOCAL, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }
 
 /**
  * Test date syntax parser.
  */
 public void testDateSyntax() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                              getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int dateType = corpus.getTypeOfNamespace(xsdns, "date");
   
   // leading spaces removed during validation.
   Assert.assertEquals(
        "2003-04-25+09:00",
        validator.validate("   2003-04-25+09:00  ", dateType));
   validator.validate("-0601-07-16-05:00", dateType);
   validator.validate("1997-07-16", dateType);
   validator.validate("1997-07-16Z", dateType);

   try {
     caught = false;
     // "" is not a legitimate date
     validator.validate("", dateType);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_DATE, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     // day field is missing.
     validator.validate("1997-07T19:20:30", dateType);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_DATE, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
 }

 /**
  * Test date minInclusive facet.
  */
 public void testDateMinInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minInclusive: 2003-03-19-05:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "dateDerived");

   validator.validate("2003-04-25", minInclusive);
   validator.validate("2003-04-25+09:00", minInclusive);
   validator.validate("2003-03-19-05:00", minInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03-18-05:00", minInclusive);
//   try {
//     caught = false;
//     validator.validate("2003-03-18-05:00", minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03-19", minInclusive);
//   try {
//     caught = false;
//     // order equivocalness
//     validator.validate("2003-03-19", minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.ORDER_EQUIVOCAL, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test date maxInclusive facet.
  */
 public void testDateMaxInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxInclusive: 2003-03-19-05:00
   int maxInclusive = corpus.getTypeOfNamespace(foons, "dateDerived");

   validator.validate("2003-03-15", maxInclusive);
   validator.validate("2003-03-15+09:00", maxInclusive);
   validator.validate("2003-03-19-05:00", maxInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03-20-05:00", maxInclusive);
//   try {
//     caught = false;
//     validator.validate("2003-03-20-05:00", maxInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03-19", maxInclusive);
//   try {
//     caught = false;
//     // order equivocalness
//     validator.validate("2003-03-19", maxInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.ORDER_EQUIVOCAL, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test date minExclusive facet.
  */
 public void testDateMinExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minExclusive: 2003-03-19-05:00
   int minExclusive = corpus.getTypeOfNamespace(foons, "dateDerived");

   validator.validate("2003-03-21", minExclusive);
   validator.validate("2003-03-21+09:00", minExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03-19-05:00", minExclusive);
//   try {
//     caught = false;
//     // same date is excluded.
//     validator.validate("2003-03-19-05:00", minExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MIN_EXCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03-19", minExclusive);
//   try {
//     caught = false;
//     // order equivocalness
//     validator.validate("2003-03-19", minExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.ORDER_EQUIVOCAL, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test date maxExclusive facet.
  */
 public void testDateMaxExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxExclusive: 2003-03-19-05:00
   int maxExclusive = corpus.getTypeOfNamespace(foons, "dateDerived");

   validator.validate("2003-03-17", maxExclusive);
   validator.validate("2003-03-17+09:00", maxExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03-19-05:00", maxExclusive);
//   try {
//     caught = false;
//     // same date is excluded.
//     validator.validate("2003-03-19-05:00", maxExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.MAX_EXCLUSIVE_INVALID, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("2003-03-19", maxExclusive);
//   try {
//     caught = false;
//     // order equivocalness
//     validator.validate("2003-03-19", maxExclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.ORDER_EQUIVOCAL, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }

 /**
  * Test date minInclusive facet with value.
  */
 public void testDateMinInclusiveWithValue() throws Exception {

   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   // minInclusive: 2003-03-19-05:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "dateDerived");

   XMLGregorianCalendar dateTimeValue;
   
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("2003-04-25");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("2003-04-25+09:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("2003-03-19-05:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

//   boolean caught = false;

   // REVISIT-XSOM: order relation not enforced.
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("2003-03-18-05:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);
//   caught = false;
//   try {
//     dateTimeValue = new XSDateTimeParser().parseDate("2003-03-18-05:00");
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

   // REVISIT-XSOM: order relation not enforced.
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("2003-03-19");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);
//   caught = false;
//   try {
//     // order equivocalness
//     dateTimeValue = new XSDateTimeParser().parseDate("2003-03-19");
//     validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);
//   }
//   catch (SchemaValidatorException sve) {
//     caught = true;
//     Assert.assertEquals(SchemaValidatorException.ORDER_EQUIVOCAL, sve.getCode());
//   }
//   finally {
//     if (!caught)
//       Assert.fail("The operation should have resulted in an exception.");
//   }
 }
 
 /**
  * Test time syntax parser.
  */
 public void testTimeSyntax() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                              getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int timeType = corpus.getTypeOfNamespace(xsdns, "time");
   
   // leading spaces removed during validation.
   Assert.assertEquals(
        "13:20:00+09:00",
        validator.validate("   13:20:00+09:00  ", timeType));
   validator.validate("13:20:00", timeType);
   validator.validate("13:20:00Z", timeType);

   try {
     caught = false;
     // "" is not a legitimate time
     validator.validate("", timeType);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_TIME, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     // minute field is missing.
     validator.validate("19:", timeType);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_TIME, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
 }

 /**
  * Test time minInclusive facet.
  */
 public void testTimeMinInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minInclusive: 13:20:00-05:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "timeDerived");

   validator.validate("13:20:01-05:00", minInclusive);
   validator.validate("13:20:00-05:00", minInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("13:19:00-05:00", minInclusive);
//   try {
//     caught = false;
//     validator.validate("13:19:00-05:00", minInclusive);
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
  * Test time maxInclusive facet.
  */
 public void testTimeMaxInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxInclusive: 13:20:00-05:00
   int maxInclusive = corpus.getTypeOfNamespace(foons, "timeDerived");

   validator.validate("13:19:00-05:00", maxInclusive);
   validator.validate("13:20:00-05:00", maxInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("13:21:00-05:00", maxInclusive);
//   try {
//     caught = false;
//     validator.validate("13:21:00-05:00", maxInclusive);
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
  * Test time minExclusive facet.
  */
 public void testTimeMinExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minExclusive: 13:20:00-05:00
   int minExclusive = corpus.getTypeOfNamespace(foons, "timeDerived");

   validator.validate("13:20:01-05:00", minExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("13:20:00-05:00", minExclusive);
//   try {
//     caught = false;
//     // same time is excluded.
//     validator.validate("13:20:00-05:00", minExclusive);
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
  * Test time maxExclusive facet.
  */
 public void testTimeMaxExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxExclusive: 13:20:00-05:00
   int maxExclusive = corpus.getTypeOfNamespace(foons, "timeDerived");

   validator.validate("13:19:01-05:00", maxExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("13:20:00-05:00", maxExclusive);
//   try {
//     caught = false;
//     // same time is excluded.
//     validator.validate("13:20:00-05:00", maxExclusive);
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
  * Test time minInclusive facet with value.
  */
 public void testTimeMinInclusiveWithValue() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   // minInclusive: 13:20:00-05:00
   int minInclusive = corpus.getTypeOfNamespace(foons, "timeDerived");

   XMLGregorianCalendar dateTimeValue;
   
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("13:20:01-05:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("13:20:00-05:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);

//   boolean caught = false;

   // REVISIT-XSOM: order relation not enforced.
   dateTimeValue = m_datatypeFactory.newXMLGregorianCalendar("13:19:00-05:00");
   validator.validateAtomicValue(dateTimeValue.toString(), dateTimeValue, minInclusive);
//   caught = false;
//   try {
//     dateTimeValue = new XSDateTimeParser().parseTime("13:19:00-05:00");
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
