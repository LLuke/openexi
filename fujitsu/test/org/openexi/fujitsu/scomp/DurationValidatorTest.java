package org.openexi.fujitsu.scomp;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;

public class DurationValidatorTest extends TestCase {

  public DurationValidatorTest(String name) {
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
  * Test duration syntax parser.
  */
 public void testDurationSyntax() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                              getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int duration = corpus.getTypeOfNamespace(xsdns, "duration");

   // leading spaces removed during validation.
   Assert.assertEquals("P1Y2M3DT10H30M", validator.validate("   P1Y2M3DT10H30M  ", duration));
   validator.validate("-P1Y2M3DT10H30M", duration);
   validator.validate("P1Y2M3DT10H30M20.01S", duration);

   try {
     caught = false;
     // "" is not a legitimate duration
     validator.validate("", duration);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_DURATION, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     // P1Y2M3DT10I30M is not valid. Use 'H' instead of 'I' to represent hour.
     validator.validate("P1Y2M3DT10I30M", duration);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_DURATION, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
 }

 /**
  * Test duration minInclusive facet.
  */
 public void testDurationMinInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minInclusive: P1Y2M3DT10H30M
   int minInclusive = corpus.getTypeOfNamespace(foons, "durationDerived");

   validator.validate("P1Y2M3DT10H31M", minInclusive);
   validator.validate("P1Y2M3DT10H30M", minInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("P1Y2M3DT10H29M", minInclusive);
//   try {
//     caught = false;
//     validator.validate("P1Y2M3DT10H29M", minInclusive);
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
  * Test duration minExclusive facet.
  */
 public void testDurationMinExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // minExclusive: P1Y2M3DT10H30M
   int minExclusive = corpus.getTypeOfNamespace(foons, "durationDerived");

   validator.validate("P1Y2M3DT10H31M", minExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("P1Y2M3DT10H30M", minExclusive);
//   try {
//     caught = false;
//     validator.validate("P1Y2M3DT10H30M", minExclusive);
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
  * Test duration maxInclusive facet.
  */
 public void testDurationMaxInclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxInclusive: P1Y2M3DT10H30M
   int maxInclusive = corpus.getTypeOfNamespace(foons, "durationDerived");

   validator.validate("P1Y2M3DT10H29M", maxInclusive);
   validator.validate("P1Y2M3DT10H30M", maxInclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("P1Y2M3DT10H31M", maxInclusive);
//   try {
//     caught = false;
//     validator.validate("P1Y2M3DT10H31M", maxInclusive);
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
  * Test duration maxExclusive facet.
  */
 public void testDurationMaxExclusive() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/maxExclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

//   boolean caught = false;

   // maxExclusive: P1Y2M3DT10H30M
   int maxExclusive = corpus.getTypeOfNamespace(foons, "durationDerived");

   validator.validate("P1Y2M3DT10H29M", maxExclusive);

   // REVISIT-XSOM: order relation not enforced.
   validator.validate("P1Y2M3DT10H30M", maxExclusive);
//   try {
//     caught = false;
//     validator.validate("P1Y2M3DT10H30M", maxExclusive);
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
  * Test duration minInclusive facet with value.
  */
 public void testDurationMinInclusiveWithValue() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/minInclusive.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   // minInclusive: P1Y2M3DT10H30M
   int minInclusive = corpus.getTypeOfNamespace(foons, "durationDerived");

   Duration durationValue;
   
   durationValue = m_datatypeFactory.newDuration("P1Y2M3DT10H31M"); 
   validator.validateAtomicValue(durationValue.toString(), durationValue, minInclusive);

   durationValue = m_datatypeFactory.newDuration("P1Y2M3DT10H30M");
   validator.validateAtomicValue(durationValue.toString(), durationValue, minInclusive);

//   boolean caught = false;

   // REVISIT-XSOM: order relation not enforced.
   durationValue = m_datatypeFactory.newDuration("P1Y2M3DT10H29M");
   validator.validateAtomicValue(durationValue.toString(), durationValue, minInclusive);
//   caught = false;
//   try {
//     durationValue = new XSDuration("P1Y2M3DT10H29M");
//     validator.validateAtomicValue(durationValue.toString(), durationValue, minInclusive);
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
