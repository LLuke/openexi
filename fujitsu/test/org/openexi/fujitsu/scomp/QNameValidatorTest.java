package org.openexi.fujitsu.scomp;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.AtomicTypedValue;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaConst;
import org.openexi.fujitsu.schema.PrefixUriBindings;
import org.openexi.fujitsu.schema.QName;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;
import org.openexi.fujitsu.schema.SimpleTypeValidationInfo;

public class QNameValidatorTest extends TestCase {

  public QNameValidatorTest(String name) {
    super(name);
    emptyPrefixUriBindings = new PrefixUriBindings();
  }

  private final PrefixUriBindings emptyPrefixUriBindings;
  
  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

 /**
  * Test QName syntax parser.
  */
 public void testQNameSyntax() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                              getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
   SimpleTypeValidationInfo stvi;
   PrefixUriBindings prefixUriBindings;
   AtomicTypedValue val;

   boolean caught;

   int qname = corpus.getTypeOfNamespace(xsdns, "QName");
   stvi = new SimpleTypeValidationInfo();
   prefixUriBindings = emptyPrefixUriBindings.bind("_.-_AZ", "urn:foo");
   validator.validate("_.-_AZ:A.-_AZ", qname, stvi, prefixUriBindings);
   val = (AtomicTypedValue)stvi.getTypedValue();
   Assert.assertEquals(EXISchemaConst.QNAME_TYPE, val.getPrimTypeId());
   Assert.assertEquals("urn:foo", ((QName)val.getValue()).namespaceName);
   Assert.assertEquals("A.-_AZ", ((QName)val.getValue()).localName);
   Assert.assertEquals("_.-_AZ", ((QName)val.getValue()).prefix);
   // validate again without namespace-prefix map
   caught = false;
   try {
     // namespace-prefix map has not been provided.
     validator.validate("_.-_AZ:A.-_AZ", qname);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_QNAME, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
   
   // Hiragana A and N : Kamiya Takuki
   prefixUriBindings = emptyPrefixUriBindings.bind("\u3042\u3093", "urn:foo");
   validator.validate("\u3042\u3093:\u4E0A\u8C37\u5353\u5DF1", qname, prefixUriBindings);

   // leading spaces removed during validation.
   prefixUriBindings = emptyPrefixUriBindings.bind("foo", "urn:foo");
   Assert.assertEquals(
        "foo:en-US", validator.validate(" foo:en-US", qname, prefixUriBindings));

   caught = false;
   try {
     // "" is not a legitimate QName
     validator.validate("", qname);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_QNAME, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     // there are two ':'
     validator.validate("_.-_AZ:A.:-_AZ", qname);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_QNAME, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     // spaces are not allowed in QName.
     validator.validate("en\011-US", qname);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_QNAME, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }

   try {
     caught = false;
     validator.validate("A\u005BZ", qname);
   }
   catch (SchemaValidatorException sve) {
     caught = true;
     Assert.assertEquals(SchemaValidatorException.INVALID_QNAME, sve.getCode());
   }
   finally {
     if (!caught)
       Assert.fail("The operation should have resulted in an exception.");
   }
 }

 /**
  * Test namespace-prefix binding.
  */
 public void testQNameBinding() throws Exception {
   EXISchema corpus =
       EXISchemaFactoryTestUtil.getEXISchema("/simpleType.xsd",
                                              getClass());

   int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

   SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

   boolean caught = false;

   int qname = corpus.getTypeOfNamespace(xsdns, "QName");

   // with prefix "foo"
   validator.validate("foo:abc", qname, emptyPrefixUriBindings.bind("foo", "urn:foo"));

   try {
     validator.validate("foo:abc", qname, null, emptyPrefixUriBindings);
   }
   catch (SchemaValidatorException sce) {
     caught = true;
   }
   Assert.assertTrue(caught);
   caught = false;

   // Default namespace
   validator.validate("abc", qname);

   // unbound NCName qualifies as a valid QName.
   validator.validate("abc", qname, emptyPrefixUriBindings);
   
   validator.validate("abc", qname, emptyPrefixUriBindings.bindDefault("urn:foo"));
 }

}
