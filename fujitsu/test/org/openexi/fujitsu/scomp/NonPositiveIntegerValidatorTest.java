package org.openexi.fujitsu.scomp;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;

public class NonPositiveIntegerValidatorTest extends TestCase {

  public NonPositiveIntegerValidatorTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

 /**
  * Test nonPositiveInteger value space.
  */
 public void testNonPositiveIntegerValueSpace() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/decimalRange.xsd", getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator =  new SimpleTypeValidator(corpus);

   boolean caught = false;

   int nonPositiveInteger = corpus.getTypeOfNamespace(xsdns, "nonPositiveInteger");

   validator.validate("0", nonPositiveInteger);
   validator.validate("-92233720368547758080000000000", nonPositiveInteger);

   try {
     caught = false;
     validator.validate("-12.0", nonPositiveInteger);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_INTEGER, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     validator.validate("1", nonPositiveInteger);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
 }

 /**
  * Test negativeInteger value space.
  */
 public void testNegativeIntegerValueSpace() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/decimalRange.xsd", getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int negativeInteger = corpus.getTypeOfNamespace(xsdns, "negativeInteger");

   validator.validate("-1", negativeInteger);
   validator.validate("-92233720368547758080000000000", negativeInteger);

   try {
     caught = false;
     validator.validate("-12.0", negativeInteger);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_INTEGER, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     validator.validate("0", negativeInteger);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.MAX_INCLUSIVE_INVALID, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
 }

}
