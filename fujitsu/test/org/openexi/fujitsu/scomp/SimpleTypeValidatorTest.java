package org.openexi.fujitsu.scomp;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.AtomicTypedValue;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaRuntimeException;
import org.openexi.fujitsu.schema.PrefixUriBindings;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;
import org.openexi.fujitsu.schema.SimpleTypeValidationInfo;

public class SimpleTypeValidatorTest extends TestCase {

  public SimpleTypeValidatorTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * SimpleTypeValidator#validate method permits SimpleTypeValidationInfo
   * to be null.
   */
  public void testValidateWithVInfoNull() throws Exception {
    EXISchema corpus = new EXISchemaFactory().compile();
 
    int xmlns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
    int decimalType = corpus.getTypeOfNamespace(xmlns, "decimal");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    validator.validate("100", decimalType, (SimpleTypeValidationInfo)null);
    try {
      validator.validate("10a", decimalType, (SimpleTypeValidationInfo)null);
    }
    catch (SchemaValidatorException sve) {
      Assert.assertEquals(SchemaValidatorException.INVALID_DECIMAL,
                        sve.getCode());
      return;
    }
    Assert.fail("The operation should have resulted in an exception");
  }

  /**
   * Test whiteSpace normalization.
   */
  public void testWhiteSpaceNormalization() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/whiteSpace.xsd", getClass());
 
    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    // whiteSpace of _anySimpleType is WHITESPACE_ABSENT
    int _anySimpleType = corpus.getTypeOfNamespace(xsdns, "anySimpleType");
    Assert.assertEquals("  a b\t\rc   ", validator.validate("  a b\t\rc   ", _anySimpleType));

    // whiteSpace of _anySimpleType is WHITESPACE_PRESERVE
    int _string = corpus.getTypeOfNamespace(xsdns, "string");
    Assert.assertEquals("  a b\t\rc   ", validator.validate("  a b\t\rc   ", _string));

    // whiteSpace of _anySimpleType is WHITESPACE_REPLACE
    int _normalizedString = corpus.getTypeOfNamespace(xsdns, "normalizedString");
    Assert.assertEquals("  a b  c   ", validator.validate(" \ta b\n c \r ", _normalizedString));

    // whiteSpace of _collapsedString is WHITESPACE_COLLAPSE
    int _collapsedString = corpus.getTypeOfNamespace(foons, "collapsedString");
    Assert.assertEquals("a b c", validator.validate(" \ta b\n c\r  ", _collapsedString));
 }

 /**
  * Test Base64 decode/encode
  */
  public void testBase64() throws Exception {

    StringBuffer encodingResult;

    encodingResult = new StringBuffer();
    SimpleTypeValidator.Base64.encode(null, encodingResult);
    Assert.assertEquals(0, encodingResult.length());
    Assert.assertNull(SimpleTypeValidator.Base64.decode(null));

    encodingResult = new StringBuffer();
    SimpleTypeValidator.Base64.encode(new byte[0], encodingResult);
    Assert.assertEquals(0, encodingResult.length());
    Assert.assertEquals(0, SimpleTypeValidator.Base64.decode("").length);

    int i;

    byte[] n1 = { -1, 2, 3, 4 }; // 4 % 3 == 1
    encodingResult = new StringBuffer();
    SimpleTypeValidator.Base64.encode(n1, encodingResult);
    String enc_n1 = encodingResult.toString();
    byte[] dec_n1 = SimpleTypeValidator.Base64.decode(enc_n1);
    for (i = 0; i < n1.length; i++) {
      Assert.assertEquals(n1[i], dec_n1[i]);
    }

    byte[] n2 = { 1, -2, 3, 4, 5 }; // 5 % 3 == 2
    encodingResult = new StringBuffer();
    SimpleTypeValidator.Base64.encode(n2, encodingResult);
    String enc_n2 = encodingResult.toString();
    byte[] dec_n2 = SimpleTypeValidator.Base64.decode(enc_n2);
    for (i = 0; i < n2.length; i++) {
      Assert.assertEquals(n2[i], dec_n2[i]);
    }

    byte[] n3 = { 1, 2, -3, 4, 5, 6 }; // 6 % 3 == 0
    encodingResult = new StringBuffer();
    SimpleTypeValidator.Base64.encode(n3, encodingResult);
    String enc_n3 = encodingResult.toString();
    byte[] dec_n3 = SimpleTypeValidator.Base64.decode(enc_n3);
    for (i = 0; i < n3.length; i++) {
      Assert.assertEquals(n3[i], dec_n3[i]);
    }

    String text1 =
    "タレント優香（23）が7日、都内で行われた3Dトレーディングカード写真集「優香」" +
    "（コナミ、9月27日発売）と生ポジ立体写真集「優香　Pure＆Lure」（新潮社、9月26日発売）の" +
    "記者発表に出席した。いずれも特製3Dビュアーを通して、立体的かつ鮮明に優香を見ることができる。" +
    "「2台カメラがあって、あまり動いてはだめと言われたぐらい」と通常の写真集の撮影とほとんど" +
    "変わらなかったという。だが仕上がりは予想外で「あまりにも立体的だし、指のしわまで見えるし、" +
    "すごい」とビックリ。「部屋をのぞいているみたいで恥ずかしい。生きている人形みたいですね」と" +
    "リアルさに驚いていた。（日刊スポーツ）";

    encodingResult = new StringBuffer();
    SimpleTypeValidator.Base64.encode(text1.getBytes("UTF8"), encodingResult);
    String enc1 = encodingResult.toString();
    byte[] dec1 = SimpleTypeValidator.Base64.decode(enc1);
    Assert.assertEquals(text1, new String(dec1, "UTF8"));
    for (i = 0; i < enc1.length() && '\n' != (char)enc1.charAt(i); i++);
    Assert.assertEquals(76, i); // 76 characters per line.

    encodingResult = new StringBuffer();
    SimpleTypeValidator.Base64.encode(text1.getBytes("SJIS"), encodingResult);
    String enc2 = encodingResult.toString();
    byte[] dec2 = SimpleTypeValidator.Base64.decode(enc2);
    Assert.assertEquals(text1, new String(dec2, "SJIS"));

    encodingResult = new StringBuffer();
    SimpleTypeValidator.Base64.encode(text1.getBytes("UTF-16"), encodingResult);
    String enc3 = encodingResult.toString();
    byte[] dec3 = SimpleTypeValidator.Base64.decode(enc3);
    Assert.assertEquals(text1, new String(dec3, "UTF-16"));

    encodingResult = new StringBuffer();
    SimpleTypeValidator.Base64.encode(text1.getBytes("EUC-JP"), encodingResult);
    String enc4 = encodingResult.toString();
    byte[] dec4 = SimpleTypeValidator.Base64.decode(enc4);
    Assert.assertEquals(text1, new String(dec4, "EUC-JP"));
  }

  /**
   * Test HexBin decode/encode
   */
   public void testHexBin() throws Exception {

     StringBuffer encodingResult;

     encodingResult = new StringBuffer();
     SimpleTypeValidator.HexBin.encode(null, encodingResult);
     Assert.assertEquals(0, encodingResult.length());
     Assert.assertNull(SimpleTypeValidator.HexBin.decode(null));

     encodingResult = new StringBuffer();
     SimpleTypeValidator.HexBin.encode(new byte[0], encodingResult);
     Assert.assertEquals(0, encodingResult.length());
     Assert.assertEquals(0, SimpleTypeValidator.HexBin.decode("").length);

     int i;

     byte[] n1 = { -100, 100 };
     encodingResult = new StringBuffer();
     SimpleTypeValidator.HexBin.encode(n1, encodingResult);
     String enc_n1 = encodingResult.toString();
     byte[] dec_n1 = SimpleTypeValidator.HexBin.decode(enc_n1);
     for (i = 0; i < n1.length; i++) {
       Assert.assertEquals(n1[i], dec_n1[i]);
     }

     String text1 =
     "タレント優香（23）が7日、都内で行われた3Dトレーディングカード写真集「優香」" +
     "（コナミ、9月27日発売）と生ポジ立体写真集「優香　Pure＆Lure」（新潮社、9月26日発売）の" +
     "記者発表に出席した。いずれも特製3Dビュアーを通して、立体的かつ鮮明に優香を見ることができる。" +
     "「2台カメラがあって、あまり動いてはだめと言われたぐらい」と通常の写真集の撮影とほとんど" +
     "変わらなかったという。だが仕上がりは予想外で「あまりにも立体的だし、指のしわまで見えるし、" +
     "すごい」とビックリ。「部屋をのぞいているみたいで恥ずかしい。生きている人形みたいですね」と" +
     "リアルさに驚いていた。（日刊スポーツ）";

     encodingResult = new StringBuffer();
     SimpleTypeValidator.HexBin.encode(text1.getBytes("UTF8"), encodingResult);
     String enc1 = encodingResult.toString();
     byte[] dec1 = SimpleTypeValidator.HexBin.decode(enc1);
     Assert.assertEquals(text1, new String(dec1, "UTF8"));

     encodingResult = new StringBuffer();
     SimpleTypeValidator.HexBin.encode(text1.getBytes("SJIS"), encodingResult);
     String enc2 = encodingResult.toString();
     byte[] dec2 = SimpleTypeValidator.HexBin.decode(enc2);
     Assert.assertEquals(text1, new String(dec2, "SJIS"));

     encodingResult = new StringBuffer();
     SimpleTypeValidator.HexBin.encode(text1.getBytes("UTF-16"), encodingResult);
     String enc3 = encodingResult.toString();
     byte[] dec3 = SimpleTypeValidator.HexBin.decode(enc3);
     Assert.assertEquals(text1, new String(dec3, "UTF-16"));

     encodingResult = new StringBuffer();
     SimpleTypeValidator.HexBin.encode(text1.getBytes("EUC-JP"), encodingResult);
     String enc4 = encodingResult.toString();
     byte[] dec4 = SimpleTypeValidator.HexBin.decode(enc4);
     Assert.assertEquals(text1, new String(dec4, "EUC-JP"));
   }

   /**
    * Test attribute / attribute use validation.
    */
   public void testAttributeValidation() throws Exception {
     EXISchema corpus =
         EXISchemaFactoryTestUtil.getEXISchema("/attributes.xsd", getClass());
  
     int foons = corpus.getNamespaceOfSchema("urn:foo");

     SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
     SimpleTypeValidationInfo stvi;

     // Global attribute "aA" declared with fixed value "true".
     int aA = corpus.getAttrOfNamespace(foons, "aA");

     stvi = new SimpleTypeValidationInfo();
     validator.validateAttrValue("true", aA, stvi);
     Assert.assertEquals(Boolean.TRUE, ((AtomicTypedValue)stvi.getTypedValue()).getValue());
     Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());

     stvi = new SimpleTypeValidationInfo();
     try {
       validator.validateAttrValue("yes", aA, stvi);
       Assert.fail("The operation should have resulted in an exception.");
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.INVALID_BOOLEAN, sve.getCode());
       Assert.assertNull(stvi.getTypedValue());
     }

     stvi = new SimpleTypeValidationInfo();
     try {
       validator.validateAttrValue("false", aA, stvi);
       Assert.fail("The operation should have resulted in an exception.");
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED,
                         sve.getCode());
       Assert.assertNotNull(((AtomicTypedValue)stvi.getTypedValue()).getValue());
       Assert.assertEquals(Boolean.FALSE, ((AtomicTypedValue)stvi.getTypedValue()).getValue());
       Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());
     }

     stvi = new SimpleTypeValidationInfo();
     String aA_val = validator.validateAttrValue(null, aA, stvi);
     Assert.assertEquals("true", aA_val);
     Assert.assertEquals(Boolean.TRUE, ((AtomicTypedValue)stvi.getTypedValue()).getValue());
     Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());

     // Global attribute "aB" declared with no fixed value.
     int aB = corpus.getAttrOfNamespace(foons, "aB");

     stvi = new SimpleTypeValidationInfo();
     validator.validateAttrValue("true", aB, stvi);
     Assert.assertEquals(Boolean.TRUE, ((AtomicTypedValue)stvi.getTypedValue()).getValue());
     Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());
     stvi = new SimpleTypeValidationInfo();
     validator.validateAttrValue("false", aB, stvi);
     Assert.assertEquals(Boolean.FALSE, ((AtomicTypedValue)stvi.getTypedValue()).getValue());
     Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());

     stvi = new SimpleTypeValidationInfo();
     try {
       validator.validateAttrValue("yes", aB, stvi);
       Assert.fail("The operation should have resulted in an exception.");
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.INVALID_BOOLEAN, sve.getCode());
       Assert.assertNull(stvi.getTypedValue());
     }

     // Use of attribute "aA"
     int eA = corpus.getElemOfNamespace(foons, "A");
     int uaA = corpus.getAttrUseOfElem(eA, 0);
     Assert.assertTrue(uaA != EXISchema.NIL_NODE);

     stvi = new SimpleTypeValidationInfo();
     validator.validateAttrValue("true", uaA, stvi);

     stvi = new SimpleTypeValidationInfo();
     try {
       validator.validateAttrValue("yes", uaA, stvi);
       Assert.fail("The operation should have resulted in an exception.");
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.INVALID_BOOLEAN, sve.getCode());
       Assert.assertNull(stvi.getTypedValue());
     }

     stvi = new SimpleTypeValidationInfo();
     try {
       validator.validateAttrValue("false", uaA, stvi);
       Assert.fail("The operation should have resulted in an exception.");
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED,
                         sve.getCode());
       Assert.assertNotNull(((AtomicTypedValue)stvi.getTypedValue()).getValue());
       Assert.assertEquals(Boolean.FALSE, ((AtomicTypedValue)stvi.getTypedValue()).getValue());
       Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());
     }

     // Use of attribute "aB" with fixed value "true"
     int eB = corpus.getElemOfNamespace(foons, "B");
     int uaB = corpus.getAttrUseOfElem(eB, 0);
     Assert.assertTrue(uaB != EXISchema.NIL_NODE);

     stvi = new SimpleTypeValidationInfo();
     validator.validateAttrValue("true", uaB, stvi);

     stvi = new SimpleTypeValidationInfo();
     try {
       validator.validateAttrValue("yes", uaB, stvi);
       Assert.fail("The operation should have resulted in an exception.");
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.INVALID_BOOLEAN, sve.getCode());
       Assert.assertNull(stvi.getTypedValue());
     }

     stvi = new SimpleTypeValidationInfo();
     try {
       validator.validateAttrValue("false", uaB, stvi);
       Assert.fail("The operation should have resulted in an exception.");
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ATTRIBUTE_INVALID_PER_FIXED,
                         sve.getCode());
       Assert.assertNotNull(((AtomicTypedValue)stvi.getTypedValue()).getValue());
       Assert.assertEquals(Boolean.FALSE, ((AtomicTypedValue)stvi.getTypedValue()).getValue());
       Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());
     }

     stvi = new SimpleTypeValidationInfo();
     String uaB_val = validator.validateAttrValue(null, uaB, stvi);
     Assert.assertEquals("true", uaB_val);
     Assert.assertEquals(Boolean.TRUE, ((AtomicTypedValue)stvi.getTypedValue()).getValue());
     Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());

     // Element "A" not expected.
     stvi = new SimpleTypeValidationInfo();
     try {
       validator.validateAttrValue("true", eA, stvi);
     }
     catch (EXISchemaRuntimeException scre) {
       Assert.assertEquals(EXISchemaRuntimeException.NOT_ATTRIBUTE_NOR_ATTRIBUTE_USE,
                         scre.getCode());
     }
   }

   /**
    * Test validation of simple content elements.
    */
   public void testSimpleElementValidation() throws Exception {
     EXISchema corpus =
         EXISchemaFactoryTestUtil.getEXISchema("/verySimple.xsd", getClass());
  
     int foons = corpus.getNamespaceOfSchema("urn:foo");

     SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
     SimpleTypeValidationInfo stvi;

     // Element "B" declared with default value "baa".
     int eB = corpus.getElemOfNamespace(foons, "B");

     Assert.assertEquals("abc", validator.validateElemValue("abc", eB, (SimpleTypeValidationInfo)null));
     
     stvi = new SimpleTypeValidationInfo();
     Assert.assertEquals("abc", validator.validateElemValue("abc", eB, stvi));
     Assert.assertEquals("abc", ((AtomicTypedValue)stvi.getTypedValue()).getValue());
     Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());

     stvi = new SimpleTypeValidationInfo();
     Assert.assertEquals("baa", validator.validateElemValue(null, eB, stvi));
     Assert.assertEquals("baa", ((AtomicTypedValue)stvi.getTypedValue()).getValue());
     Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());

     stvi = new SimpleTypeValidationInfo();
     Assert.assertEquals("baa", validator.validateElemValue("", eB, stvi));
     Assert.assertEquals("baa", ((AtomicTypedValue)stvi.getTypedValue()).getValue());
     Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());

     stvi = new SimpleTypeValidationInfo();
     try {
       validator.validateElemValue("abcdefghiji", eB, stvi);
       Assert.fail("The operation should have resulted in an exception.");
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.MAX_LENGTH_INVALID, sve.getCode());
       Assert.assertNotNull(((AtomicTypedValue)stvi.getTypedValue()).getValue());
       Assert.assertEquals("abcdefghiji", ((AtomicTypedValue)stvi.getTypedValue()).getValue());
       Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());
     }

     // Element "C" declared with fixed value "abcdefghij".
     int eC = corpus.getElemOfNamespace(foons, "C");

     stvi = new SimpleTypeValidationInfo();
     Assert.assertEquals("abcdefghij",
                       validator.validateElemValue("abcdefghij", eC, stvi));
     Assert.assertEquals("abcdefghij", ((AtomicTypedValue)stvi.getTypedValue()).getValue());
     Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());

     stvi = new SimpleTypeValidationInfo();
     Assert.assertEquals("abcdefghij", validator.validateElemValue(null, eC, stvi));
     Assert.assertEquals("abcdefghij", ((AtomicTypedValue)stvi.getTypedValue()).getValue());
     Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());

     stvi = new SimpleTypeValidationInfo();
     try {
       validator.validateElemValue("0123456789", eC, stvi);
       Assert.fail("The operation should have resulted in an exception.");
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ELEMENT_INVALID_PER_FIXED,
                         sve.getCode());
       Assert.assertNotNull(((AtomicTypedValue)stvi.getTypedValue()).getValue());
       Assert.assertEquals("0123456789", ((AtomicTypedValue)stvi.getTypedValue()).getValue());
       Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());
     }

     // Global attribute "id" is not apparently an element.
     int aId = corpus.getAttrOfNamespace(foons, "id");
     stvi = new SimpleTypeValidationInfo();
     try {
       validator.validateElemValue("ID01", aId, stvi);
       Assert.fail("The operation should have resulted in an exception.");
     }
     catch (EXISchemaRuntimeException scre) {
       Assert.assertEquals(EXISchemaRuntimeException.NOT_ELEMENT,
                         scre.getCode());
     }

     // Element "A" has complex content.
     int eA = corpus.getElemOfNamespace(foons, "A");
     stvi = new SimpleTypeValidationInfo();
     try {
       validator.validateElemValue("abc", eA, stvi);
       Assert.fail("The operation should have resulted in an exception.");
     }
     catch (EXISchemaRuntimeException scre) {
       Assert.assertEquals(EXISchemaRuntimeException.ELEMENT_CONTENT_NOT_SIMPLE,
                         scre.getCode());
     }
   }

   /**
    * Element declaration of type "xsd:decimal" where the declaration
    * specifies the fixed value "1". 
    */
   public void testElemDecimalFixed_01() throws Exception {
     EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
         "/elementFixed.xsd", getClass());

     SimpleTypeValidator stv = new SimpleTypeValidator(corpus);

     int foons = corpus.getNamespaceOfSchema("urn:foo");

     int _decimal = corpus.getElemOfNamespace(foons, "decimal");
     Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(_decimal));
     
     stv.validateElemValue("1", _decimal, new SimpleTypeValidationInfo());
     stv.validateElemValue("1.0", _decimal, new SimpleTypeValidationInfo());
     
     try {
       stv.validateElemValue("1.1", _decimal, new SimpleTypeValidationInfo());
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ELEMENT_INVALID_PER_FIXED, sve.getCode());
       return;
     }
     Assert.fail("The operation should have resulted in an exception.");
   }
   
   /**
    * Element declaration of type "xsd:string" where the declaration
    * specifies the fixed value "1". 
    */
   public void testElemStringFixed_01() throws Exception {
     EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
         "/elementFixed.xsd", getClass());

     SimpleTypeValidator stv = new SimpleTypeValidator(corpus);

     int foons = corpus.getNamespaceOfSchema("urn:foo");

     int _string = corpus.getElemOfNamespace(foons, "string");
     Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(_string));
     
     stv.validateElemValue("1", _string, new SimpleTypeValidationInfo());
     
     try {
       stv.validateElemValue("1.0", _string, new SimpleTypeValidationInfo());
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ELEMENT_INVALID_PER_FIXED, sve.getCode());
       return;
     }
     Assert.fail("The operation should have resulted in an exception.");
   }

   /**
    * Element declaration of type "xsd:anySimpleType" where the declaration
    * specifies the fixed value "1". 
    */
   public void testElemAnySimpleTypeFixed_01() throws Exception {
     EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
         "/elementFixed.xsd", getClass());

     SimpleTypeValidator stv = new SimpleTypeValidator(corpus);

     int foons = corpus.getNamespaceOfSchema("urn:foo");

     int _anySimpleType = corpus.getElemOfNamespace(foons, "anySimpleType");
     Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(_anySimpleType));
     
     stv.validateElemValue("1", _anySimpleType, new SimpleTypeValidationInfo());
     
     try {
       stv.validateElemValue("1.0", _anySimpleType, new SimpleTypeValidationInfo());
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ELEMENT_INVALID_PER_FIXED, sve.getCode());
       return;
     }
     Assert.fail("The operation should have resulted in an exception.");
   }

   /**
    * Element declaration of type "xsd:anyURI" where the declaration
    * specifies the fixed value "1". 
    */
   public void testElemAnyURIFixed_01() throws Exception {
     EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
         "/elementFixed.xsd", getClass());

     SimpleTypeValidator stv = new SimpleTypeValidator(corpus);

     int foons = corpus.getNamespaceOfSchema("urn:foo");

     int _anyURI = corpus.getElemOfNamespace(foons, "anyURI");
     Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(_anyURI));
     
     stv.validateElemValue("1", _anyURI, new SimpleTypeValidationInfo());
     
     try {
       stv.validateElemValue("1.0", _anyURI, new SimpleTypeValidationInfo());
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ELEMENT_INVALID_PER_FIXED, sve.getCode());
       return;
     }
     Assert.fail("The operation should have resulted in an exception.");
   }

   /**
    * Element declaration of type "xsd:QName" where the declaration
    * specifies the fixed value "goo:xyz". 
    */
   public void testElemQNameFixed_01() throws Exception {
     EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
         "/elementFixed.xsd", getClass());

     SimpleTypeValidator stv = new SimpleTypeValidator(corpus);

     int foons = corpus.getNamespaceOfSchema("urn:foo");

     int _QName = corpus.getElemOfNamespace(foons, "QName");
     Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(_QName));
     
     PrefixUriBindings nspm = new PrefixUriBindings();
     stv.validateElemValue("p0:xyz", _QName, new SimpleTypeValidationInfo(), nspm.bind("p0", "urn:goo"));
     stv.validateElemValue("xyz", _QName, new SimpleTypeValidationInfo(), nspm.bindDefault("urn:goo"));
     
     boolean caught = false;
     try {
       stv.validateElemValue("1.0", _QName, new SimpleTypeValidationInfo());
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.INVALID_QNAME, sve.getCode());
       caught = true;
     }
     Assert.assertTrue(caught);
     caught = false;
     try {
       stv.validateElemValue("goo:abc", _QName, new SimpleTypeValidationInfo(), nspm.bind("goo", "urn:goo"));
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ELEMENT_INVALID_PER_FIXED, sve.getCode());
       caught = true;
     }
     Assert.assertTrue(caught);
   }
   
   /**
    * Element declaration of type "xsd:float" where the declaration
    * specifies the fixed value "1.23". 
    */
   public void testElemFloatFixed_01() throws Exception {
     EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
         "/elementFixed.xsd", getClass());

     SimpleTypeValidator stv = new SimpleTypeValidator(corpus);

     int foons = corpus.getNamespaceOfSchema("urn:foo");

     int _float = corpus.getElemOfNamespace(foons, "float");
     Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(_float));
     
     stv.validateElemValue("1.23", _float, new SimpleTypeValidationInfo());
     stv.validateElemValue("1.230", _float, new SimpleTypeValidationInfo());
     
     try {
       stv.validateElemValue("1.2", _float, new SimpleTypeValidationInfo());
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ELEMENT_INVALID_PER_FIXED, sve.getCode());
       return;
     }
     Assert.fail("The operation should have resulted in an exception.");
   }
   
   /**
    * Element declaration of type "xsd:double" where the declaration
    * specifies the fixed value "1.23". 
    */
   public void testElemDoubleFixed_01() throws Exception {
     EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
         "/elementFixed.xsd", getClass());

     SimpleTypeValidator stv = new SimpleTypeValidator(corpus);

     int foons = corpus.getNamespaceOfSchema("urn:foo");

     int _double = corpus.getElemOfNamespace(foons, "double");
     Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(_double));
     
     stv.validateElemValue("1.23", _double, new SimpleTypeValidationInfo());
     stv.validateElemValue("1.230", _double, new SimpleTypeValidationInfo());
     
     try {
       stv.validateElemValue("1.2", _double, new SimpleTypeValidationInfo());
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ELEMENT_INVALID_PER_FIXED, sve.getCode());
       return;
     }
     Assert.fail("The operation should have resulted in an exception.");
   }
   
   /**
    * Element declaration of type "xsd:dateTime" where the declaration
    * specifies the fixed value "2007-07-11T21:51:43Z". 
    */
   public void testElemDateTimeFixed_01() throws Exception {
     EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
         "/elementFixed.xsd", getClass());

     SimpleTypeValidator stv = new SimpleTypeValidator(corpus);

     int foons = corpus.getNamespaceOfSchema("urn:foo");

     int _dateTime = corpus.getElemOfNamespace(foons, "dateTime");
     Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(_dateTime));
     
     boolean caught = false;
     
     stv.validateElemValue("2007-07-11T21:51:43Z", _dateTime, new SimpleTypeValidationInfo());
     stv.validateElemValue("2007-07-11T23:51:43+02:00", _dateTime, new SimpleTypeValidationInfo());

     caught = false;
     try {
       stv.validateElemValue("2007-07-12T21:51:43Z", _dateTime, new SimpleTypeValidationInfo());
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ELEMENT_INVALID_PER_FIXED, sve.getCode());
       caught = true;
     }
     Assert.assertTrue(caught);
     caught = false;
     try {
       stv.validateElemValue("2007-07-11T21:51:43-05:00", _dateTime, new SimpleTypeValidationInfo());
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ELEMENT_INVALID_PER_FIXED, sve.getCode());
       caught = true;
     }
     Assert.assertTrue(caught);
   }
   
   /**
    * Element declaration of type "xsd:duration" where the declaration
    * specifies the fixed value "P3DT10H30M". 
    */
   public void testElemDurationFixed_01() throws Exception {
     EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
         "/elementFixed.xsd", getClass());

     SimpleTypeValidator stv = new SimpleTypeValidator(corpus);

     int foons = corpus.getNamespaceOfSchema("urn:foo");

     int _duration = corpus.getElemOfNamespace(foons, "duration");
     Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(_duration));
     
     stv.validateElemValue("P3DT10H30M", _duration, new SimpleTypeValidationInfo());
     stv.validateElemValue("P3DT9H90M", _duration, new SimpleTypeValidationInfo());
     
     try {
       stv.validateElemValue("P3DT9H30M", _duration, new SimpleTypeValidationInfo());
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ELEMENT_INVALID_PER_FIXED, sve.getCode());
       return;
     }
     Assert.fail("The operation should have resulted in an exception.");
   }
   
   /**
    * Element declaration of type "xsd:base64Binary" where the declaration
    * specifies the fixed value "QUJDREVGR0hJSg==". 
    */
   public void testElemBase64BinaryFixed_01() throws Exception {
     EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
         "/elementFixed.xsd", getClass());

     SimpleTypeValidator stv = new SimpleTypeValidator(corpus);

     int foons = corpus.getNamespaceOfSchema("urn:foo");

     int _base64Binary = corpus.getElemOfNamespace(foons, "base64Binary");
     Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(_base64Binary));
     
     stv.validateElemValue("QUJDREVGR0hJSg==", _base64Binary, new SimpleTypeValidationInfo());
     
     try {
       stv.validateElemValue("QUJDREVGR0hJ", _base64Binary, new SimpleTypeValidationInfo());
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ELEMENT_INVALID_PER_FIXED, sve.getCode());
       return;
     }
     Assert.fail("The operation should have resulted in an exception.");
   }
   
   /**
    * Element declaration of type "xsd:hexBinary" where the declaration
    * specifies the fixed value "4142434445464748494A". 
    */
   public void testElemHexBinaryFixed_01() throws Exception {
     EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
         "/elementFixed.xsd", getClass());

     SimpleTypeValidator stv = new SimpleTypeValidator(corpus);

     int foons = corpus.getNamespaceOfSchema("urn:foo");

     int _hexBinary = corpus.getElemOfNamespace(foons, "hexBinary");
     Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(_hexBinary));
     
     stv.validateElemValue("4142434445464748494A", _hexBinary, new SimpleTypeValidationInfo());
     
     try {
       stv.validateElemValue("4142434445464748494A4B", _hexBinary, new SimpleTypeValidationInfo());
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.ELEMENT_INVALID_PER_FIXED, sve.getCode());
       return;
     }
     Assert.fail("The operation should have resulted in an exception.");
   }
   
   /**
    * SimpleTypeValidator#validate method accepts complex types if they
    * have simple type content.
    */
   public void testSimpleContentComplexTypeValidation() throws Exception {
     EXISchema corpus =
         EXISchemaFactoryTestUtil.getEXISchema("/verySimple.xsd", getClass());
  
     int foons = corpus.getNamespaceOfSchema("urn:foo");

     SimpleTypeValidator validator = new SimpleTypeValidator(corpus);
     SimpleTypeValidationInfo stvi = new SimpleTypeValidationInfo();

     int eI = corpus.getElemOfNamespace(foons, "I");
     int ctype = corpus.getTypeOfElem(eI); // simple-content complex type.

     validator.validate("abcde", ctype, stvi);
     Assert.assertEquals("abcde", ((AtomicTypedValue)stvi.getTypedValue()).getValue());
     Assert.assertEquals(corpus, stvi.getTypedValue().getEXISchema());

     try {
       validator.validate("abcdefghijk", ctype);
     }
     catch (SchemaValidatorException sve) {
       Assert.assertEquals(SchemaValidatorException.MAX_LENGTH_INVALID,
                         sve.getCode());

       int eA = corpus.getElemOfNamespace(foons, "A");
       ctype = corpus.getTypeOfElem(eA); // element-only complex type
       try {
         validator.validate("abcde", ctype);
       }
       catch (SchemaValidatorException sve2) {
         Assert.assertEquals(SchemaValidatorException.COMPLEX_TYPE_NOT_OF_SIMPLE_CONTENT,
                           sve2.getCode());
         return;
       }
     }
     Assert.fail("The operation should have resulted in an exception.");
   }
   
   /**
    * Test validateAtomicValue return value 
    */
   public void testAtomicValueValidationReturnValue() throws Exception {
     
     EXISchema corpus =
         EXISchemaFactoryTestUtil.getEXISchema("/length.xsd", getClass());
  
     int foons = corpus.getNamespaceOfSchema("urn:foo");

     SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

     AtomicTypedValue atomicTypedValue;
     
     int hexBinary10 = corpus.getTypeOfNamespace(foons, "hexBinary10");

     byte[] octetValue = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
     
     atomicTypedValue = validator.validateAtomicValue(
         SimpleTypeValidator.encodeBinaryByHexBin(octetValue), octetValue, hexBinary10);
     Assert.assertEquals(hexBinary10, atomicTypedValue.getType());
     Assert.assertEquals(
         corpus.getSerialOfType(corpus.getPrimitiveTypeOfAtomicSimpleType(hexBinary10)),
         atomicTypedValue.getPrimTypeId());
     Assert.assertEquals(corpus, atomicTypedValue.getEXISchema());

     int string10 = corpus.getTypeOfNamespace(foons, "string10");
     
     String stringValue = "1234567890";
     atomicTypedValue = validator.validateAtomicValue(stringValue, stringValue, string10);
     Assert.assertEquals(string10, atomicTypedValue.getType());
     Assert.assertEquals(
         corpus.getSerialOfType(corpus.getPrimitiveTypeOfAtomicSimpleType(string10)),
         atomicTypedValue.getPrimTypeId());
     Assert.assertEquals(corpus, atomicTypedValue.getEXISchema());
     Assert.assertEquals(stringValue, atomicTypedValue.getValue());
   }

   /**
    * Test encoding UCS4 into UTF-16
    */
   public void testUCS4ToUTF16() throws Exception {

     final char[] utf16 = new char[2];

     Assert.assertEquals(-1, SimpleTypeValidator.toUTF16(-1, utf16));

     Assert.assertEquals(1, SimpleTypeValidator.toUTF16(0x00, utf16));
     Assert.assertEquals(0x00, utf16[0]);

     Assert.assertEquals(1, SimpleTypeValidator.toUTF16(0x7A, utf16));
     Assert.assertEquals(0x7A, utf16[0]);
     
     // Hiragana A
     Assert.assertEquals(1, SimpleTypeValidator.toUTF16(0x3042, utf16));
     Assert.assertEquals(0x3042, utf16[0]);
     
     // Kanji "ue" (i.e. up)
     Assert.assertEquals(1, SimpleTypeValidator.toUTF16(0x4E0A, utf16));
     Assert.assertEquals(0x4E0A, utf16[0]);
     
     // Kanji that consists of three sevens with one at the top,
     // two at the bottom, which was not present before vista.
     Assert.assertEquals(1, SimpleTypeValidator.toUTF16(0x3402, utf16));
     Assert.assertEquals(0x3402, utf16[0]);

     // U+2000B becomes 0xD840 followed by 0xDC0B in UTF-16
     // This character was not present before vista.
     Assert.assertEquals(2, SimpleTypeValidator.toUTF16(0x2000B, utf16));
     Assert.assertEquals(0xD840, utf16[0]);
     Assert.assertEquals(0xDC0B, utf16[1]);

     // Largest valid UTF-32 value 
     Assert.assertEquals(2, SimpleTypeValidator.toUTF16(0x10FFFF, utf16));
     Assert.assertEquals(0xDBFF, utf16[0]);
     Assert.assertEquals(0xDFFF, utf16[1]);

     // Off the limit
     Assert.assertEquals(-1, SimpleTypeValidator.toUTF16(0x110000, utf16));
   }
   
}
