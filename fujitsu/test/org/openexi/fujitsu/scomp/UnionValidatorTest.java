package org.openexi.fujitsu.scomp;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;

public class UnionValidatorTest extends TestCase {

  public UnionValidatorTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

 /**
  * Test union of xsd:integer, xsd:NMTOKEN and xsd:string
  */
 public void testUnionOfIntegerNMTOKEN() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/union.xsd",
                                              getClass());

   int foons = corpus.getNamespaceOfSchema("urn:foo");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int unioned = corpus.getTypeOfNamespace(foons, "unionOf_Integer-NMTOKEN-string");

   // leading spaces removed when validated against integer
   Assert.assertEquals("3.1415926", validator.validate("   3.1415926  ", unioned));
   // leading spaces removed when validated against NMTOKEN
   Assert.assertEquals("FUJITSU", validator.validate("   FUJITSU  ", unioned));
   // leading spaces *not* removed when validated against string
   Assert.assertEquals("   FUJITSU LIMITED ",
                     validator.validate("   FUJITSU LIMITED ", unioned));

   try {
     caught = false;
     // "de facto" does not parse as any of the member types
     validator.validate("de facto", unioned);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_UNION, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
 }

}
