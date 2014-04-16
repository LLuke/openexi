package org.openexi.fujitsu.scomp;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.PrefixUriBindings;
import org.openexi.fujitsu.schema.SchemaValidatorException;
import org.openexi.fujitsu.schema.SimpleTypeValidator;

public class EnumerationValidatorTest extends TestCase {

  public EnumerationValidatorTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
  }

  private EXISchemaFactoryErrorMonitor m_compilerErrorHandler;
  
  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Test enumerated string
   */
  public void testStringEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int stringDerived = corpus.getTypeOfNamespace(foons, "stringDerived");

    validator.validate("Tokyo", stringDerived);

    try {
      caught = false;
      validator.validate("", stringDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      validator.validate("Kobe", stringDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated string through indirect enumeration specification
   */
  public void testStringEnums2() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int stringDerived2 = corpus.getTypeOfNamespace(foons, "stringDerived2");

    validator.validate("Tokyo", stringDerived2);

    try {
      caught = false;
      validator.validate("Kobe", stringDerived2);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated ID
   */
  public void testIdEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int idDerived = corpus.getTypeOfNamespace(foons, "idDerived");

    validator.validate("Tokyo", idDerived);

    try {
      caught = false;
      validator.validate("", idDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_NAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      validator.validate("Kobe", idDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated anyURI
   */
  public void testAnyURIEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int anyURIDerived = corpus.getTypeOfNamespace(foons, "anyURIDerived");

    validator.validate("urn:foo", anyURIDerived);
    validator.validate("urn:goo", anyURIDerived);
    validator.validate("urn:hoo", anyURIDerived);

    try {
      caught = false;
      validator.validate("urn:ioo", anyURIDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated QName with prefix
   */
  public void testQNameEnums_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught;

    int qNameDerived = corpus.getTypeOfNamespace(foons, "qNameDerived");

    final PrefixUriBindings emptyMap = new PrefixUriBindings();
    
    PrefixUriBindings npm = emptyMap;
    npm = npm.bind("foo", "urn:foo");
    npm = npm.bind("goo", "urn:goo");
    npm = npm.bind("hoo", "urn:hoo");

    // valid qname in instance, valid qname in schema
    validator.validate("foo:A", qNameDerived, npm);
    validator.validate("goo:A", qNameDerived, npm);
    
    caught = false;
    try {
      // valid qname in instance, no such qname in schema due to lack
      // of namespace declaration. "hoo:A" is in the enumeration without
      // proper namespace declaration. 
      validator.validate("hoo:A", qNameDerived, npm);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    caught = false;
    try {
      validator.validate("ioo:A", qNameDerived, npm);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_QNAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    // intentionally binding "ioo" to namespace name "urn:foo"
    validator.validate("ioo:A", qNameDerived, npm.bind("ioo", "urn:foo"));

    caught = false;
    try {
      validator.validate("ioo:A", qNameDerived, npm.bind("ioo", "urn:ioo"));
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated QName without prefix
   */
  public void testQNameEnums_02() throws Exception {
    EXISchema corpus =
        EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd",
                                               getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int qNameDerived = corpus.getTypeOfNamespace(foons, "qNameDerived2");

    PrefixUriBindings npm = new PrefixUriBindings();

    // valid qname in instance, valid qname in schema
    validator.validate("A", qNameDerived, npm);
    validator.validate("B", qNameDerived, npm);
    validator.validate("C", qNameDerived, npm);
    
    try {
      // "D" is not an enumeration.
      caught = false;
      validator.validate("D", qNameDerived, npm);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      // "A" in namespace "urn:foo" is not an enumeration.
      caught = false;
      validator.validate("A", qNameDerived, npm.bindDefault("urn:foo"));
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      // "A" in namespace "urn:foo" is not an enumeration.
      caught = false;
      validator.validate("foo:A", qNameDerived, npm.bind("foo", "urn:foo"));
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
    
    try {
      // "foo:A" where "foo" is not bound to a namespace is an invalid QName.
      caught = false;
      validator.validate("foo:A", qNameDerived, npm);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_QNAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    validator.validate("A", qNameDerived);
  }
  
  /**
   * Test enumerated NOTATION
   */
  public void testNotationEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int notationDerived = corpus.getTypeOfNamespace(foons, "notationDerived");

    final PrefixUriBindings emptyMap = new PrefixUriBindings();
    PrefixUriBindings npm;
    npm = (PrefixUriBindings)emptyMap.bind("foo", "urn:foo");
    npm = (PrefixUriBindings)npm.bind("goo", "urn:goo");
    npm = (PrefixUriBindings)npm.bind("hoo", "urn:hoo");

    // valid qname in instance, valid qname in schema
    validator.validate("foo:cat", notationDerived, npm);
    validator.validate("foo:dog", notationDerived, npm);

    try {
      // valid qname in instance, no such qname in schema due to lack
      // of namespace declaration. "hoo:A" is in the enumeration without
      // proper namespace declaration. 
      caught = false;
      validator.validate("hoo:cat", notationDerived, npm);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      validator.validate("ioo:cat", notationDerived, npm);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_QNAME, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    // intentionally binding "ioo" to namespace name "urn:foo"
    validator.validate("ioo:cat", notationDerived, npm.bind("ioo", "urn:foo"));

    try {
      caught = false;
      validator.validate("ioo:cat", notationDerived, npm.bind("ioo", "urn:ioo"));
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated base64Binary
   */
  public void testBase64BinaryEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());
 
    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int base64BinaryDerived = corpus.getTypeOfNamespace(foons, "base64BinaryDerived");

    validator.validate("YWFhYWE=", base64BinaryDerived); // aaaaa
    validator.validate("Y2NjY2M=", base64BinaryDerived); // ccccc
    validator.validate("ZWVlZWU=", base64BinaryDerived); // eeeee

    try {
      caught = false;
      validator.validate("YmJiYmI=", base64BinaryDerived); // bbbbb
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated hexBinary
   */
  public void testHexBinaryEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int hexBinaryDerived = corpus.getTypeOfNamespace(foons, "hexBinaryDerived");

    validator.validate("6161616161", hexBinaryDerived); // aaaaa
    validator.validate("6363636363", hexBinaryDerived); // ccccc
    validator.validate("6565656565", hexBinaryDerived); // eeeee

    try {
      caught = false;
      validator.validate("6262626262", hexBinaryDerived); // bbbbb
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated decimal
   */
  public void testDecimalEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int decimalDerived = corpus.getTypeOfNamespace(foons, "decimalDerived");

    validator.validate("101.2345678", decimalDerived);

    try {
      caught = false;
      validator.validate("101.23456789", decimalDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated long
   */
  public void testLongEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int longDerived = corpus.getTypeOfNamespace(foons, "longDerived");

    validator.validate("113", longDerived);

    try {
      caught = false;
      validator.validate("110", longDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated int
   */
  public void testIntEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int intDerived = corpus.getTypeOfNamespace(foons, "intDerived");

    validator.validate("110", intDerived);

    try {
      caught = false;
      validator.validate("100", intDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated float
   */
  public void testFloatEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int floatDerived = corpus.getTypeOfNamespace(foons, "floatDerived");

    validator.validate("103.01", floatDerived);
    validator.validate("105.01", floatDerived);
    validator.validate("107.01", floatDerived);

    try {
      caught = false;
      validator.validate("104.01", floatDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated double
   */
  public void testDoubleEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int doubleDerived = corpus.getTypeOfNamespace(foons, "doubleDerived");

    validator.validate("-1E4", doubleDerived);
    validator.validate("12", doubleDerived);
    validator.validate("INF", doubleDerived);

    try {
      caught = false;
      validator.validate("12.789e-2", doubleDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated dateTime
   */
  public void testDateTimeEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int dateTimeDerived = corpus.getTypeOfNamespace(foons, "dateTimeDerived");

    validator.validate("2003-03-20T13:20:00-05:00", dateTimeDerived);
    validator.validate("2003-03-20T12:20:00-06:00", dateTimeDerived);

    try {
      caught = false;
      validator.validate("2003-03-10T13:20:00-05:00", dateTimeDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated date
   */
  public void testDateEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int dateDerived = corpus.getTypeOfNamespace(foons, "dateDerived");

    validator.validate("2003-03-19-05:00", dateDerived);
    validator.validate("2003-03-21-05:00", dateDerived);
    validator.validate("2003-03-23-05:00", dateDerived);

    try {
      caught = false;
      validator.validate("2003-03-20-05:00", dateDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated time
   */
  public void testTimeEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int timeDerived = corpus.getTypeOfNamespace(foons, "timeDerived");

    validator.validate("13:20:00-05:00", timeDerived);
    validator.validate("13:22:00-05:00", timeDerived);
    validator.validate("13:24:00-05:00", timeDerived);
    validator.validate("12:22:00-06:00", timeDerived);
    
    try {
      caught = false;
      validator.validate("13:21:00-05:00", timeDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated gYearMonth
   */
  public void testGYearMonthEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int gYearMonthDerived = corpus.getTypeOfNamespace(foons, "gYearMonthDerived");

    validator.validate("2003-04-05:00", gYearMonthDerived);
    validator.validate("2003-06-05:00", gYearMonthDerived);
    validator.validate("2003-08-05:00", gYearMonthDerived);

    try {
      caught = false;
      validator.validate("2003-05-05:00", gYearMonthDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated gMonthDay
   */
  public void testGMonthDayEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int gMonthDayDerived = corpus.getTypeOfNamespace(foons, "gMonthDayDerived");

    validator.validate("--09-16+09:00", gMonthDayDerived);
    validator.validate("--09-18+09:00", gMonthDayDerived);
    validator.validate("--09-20+09:00", gMonthDayDerived);

    try {
      caught = false;
      validator.validate("--09-17+09:00", gMonthDayDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated gYear
   */
  public void testGYearEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int gYearDerived = corpus.getTypeOfNamespace(foons, "gYearDerived");

    validator.validate("1969+09:00", gYearDerived);
    validator.validate("1971+09:00", gYearDerived);
    validator.validate("1973+09:00", gYearDerived);

    try {
      caught = false;
      validator.validate("1970+09:00", gYearDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated gMonth
   */
  public void testGMonthEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int gMonthDerived = corpus.getTypeOfNamespace(foons, "gMonthDerived");

    validator.validate("--07--+09:00", gMonthDerived);
    validator.validate("--09--+09:00", gMonthDerived);
    validator.validate("--11--+09:00", gMonthDerived);

    try {
      caught = false;
      validator.validate("--08--+09:00", gMonthDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated gDay
   */
  public void testGDayEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int gDayDerived = corpus.getTypeOfNamespace(foons, "gDayDerived");

    validator.validate("---16+09:00", gDayDerived);
    validator.validate("---18+09:00", gDayDerived);
    validator.validate("---20+09:00", gDayDerived);

    try {
      caught = false;
      validator.validate("---17+09:00", gDayDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated duration
   */
  public void testDurationEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int dateTimeDerived = corpus.getTypeOfNamespace(foons, "durationDerived");

    validator.validate("P1Y2M4DT10H30M", dateTimeDerived);

    try {
      caught = false;
      validator.validate("P1Y2M6DT10H30M", dateTimeDerived);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated union (int and NMTOKEN)
   */
  public void testUnionedIntNmtokenEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int unionedEnum = corpus.getTypeOfNamespace(foons, "unionedEnum");

    validator.validate("100", unionedEnum);
    validator.validate("Tokyo", unionedEnum);
    validator.validate("101", unionedEnum);

    try {
      caught = false;
      validator.validate("102", unionedEnum);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      validator.validate("Osaka", unionedEnum);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated union (float and dateTime)
   */
  public void testUnionedFloatDateTimeEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int unionOfFloatDateTimeEnum = corpus.getTypeOfNamespace(foons, "unionOfFloatDateTimeEnum");

    validator.validate("2003-03-19T13:20:00-05:00", unionOfFloatDateTimeEnum);
    validator.validate("103.010", unionOfFloatDateTimeEnum);
    validator.validate("2003-03-21T13:20:00-05:00", unionOfFloatDateTimeEnum);
    validator.validate("2003-03-21T12:20:00-06:00", unionOfFloatDateTimeEnum);
    
    try {
      caught = false;
      validator.validate("102.01", unionOfFloatDateTimeEnum);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      validator.validate("2003-03-20T13:20:00-05:00", unionOfFloatDateTimeEnum);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated union (duration and double)
   */
  public void testUnionedDoubleDurationEnums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int unionOfDoubleDurationEnum = corpus.getTypeOfNamespace(foons, "unionOfDoubleDurationEnum");

    validator.validate("P1Y2M3DT10H30M", unionOfDoubleDurationEnum);
    validator.validate("103.010", unionOfDoubleDurationEnum);
    validator.validate("P1Y2M5DT10H30M", unionOfDoubleDurationEnum);

    try {
      caught = false;
      validator.validate("102.01", unionOfDoubleDurationEnum);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      validator.validate("P1Y2M4DT10H30M", unionOfDoubleDurationEnum);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

  /**
   * Test enumerated union (base64Binary and float)
   */
  public void testUnionedFloatBase64Enums() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumeration.xsd", getClass());

    int foons = corpus.getNamespaceOfSchema("urn:foo");

    SimpleTypeValidator validator = new SimpleTypeValidator(corpus);

    boolean caught = false;

    int unionOfFloatBase64Enum = corpus.getTypeOfNamespace(foons, "unionOfFloatBase64Enum");

    validator.validate("YWFhYWE=", unionOfFloatBase64Enum); // aaaaa
    validator.validate("103.010", unionOfFloatBase64Enum);
    validator.validate("Y2NjY2M=", unionOfFloatBase64Enum); // ccccc

    try {
      caught = false;
      validator.validate("102.01", unionOfFloatBase64Enum);
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }

    try {
      caught = false;
      validator.validate("YmJiYmI=", unionOfFloatBase64Enum); // bbbbb
    }
    catch (SchemaValidatorException sve) {
      caught = true;
      Assert.assertEquals(SchemaValidatorException.INVALID_ENUMERATION, sve.getCode());
    }
    finally {
      if (!caught)
        Assert.fail("The operation should have resulted in an exception.");
    }
  }

}
