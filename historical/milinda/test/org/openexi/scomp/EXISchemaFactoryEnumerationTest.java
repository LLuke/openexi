package org.openexi.scomp;

import java.math.BigDecimal;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.xerces.xni.parser.XMLParseException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.schema.EXISchema;
import org.openexi.schema.SimpleTypeValidator;

public class EXISchemaFactoryEnumerationTest extends TestCase {

  public EXISchemaFactoryEnumerationTest(String name) {
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

  @Override
  protected void setUp() throws Exception {
      super.setUp();
      m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
  }

  private final DatatypeFactory m_datatypeFactory;
  private EXISchemaFactoryErrorMonitor m_compilerErrorHandler;

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Invalid enumerated value per length facet constraint. 
   */
  public void testEnumLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, string1Derived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    string1Derived = corpus.getTypeOfNamespace(foons, "string1Derived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(string1Derived));
    e = corpus.getEnumerationFacetOfSimpleType(string1Derived, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e));
    Assert.assertEquals("X", corpus.getStringValueOfVariant(e));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce.getCode());
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    string1Derived = corpus.getTypeOfNamespace(foons, "string1Derived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfSimpleType(string1Derived));
  }
  
  /**
   * Invalid enumerated value per minLength facet constraint. 
   */
  public void testEnumMinLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, stringMin2Derived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK03.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    foons = corpus.getNamespaceOfSchema("urn:foo");

    stringMin2Derived = corpus.getTypeOfNamespace(foons, "stringMin2Derived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(stringMin2Derived));
    e = corpus.getEnumerationFacetOfSimpleType(stringMin2Derived, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e));
    Assert.assertEquals("XY", corpus.getStringValueOfVariant(e));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG03.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));

    foons = corpus.getNamespaceOfSchema("urn:foo");

    stringMin2Derived = corpus.getTypeOfNamespace(foons, "stringMin2Derived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfSimpleType(stringMin2Derived));
  }
  
  /**
   * Invalid enumerated value per maxLength facet constraint. 
   */
  public void testEnumMaxLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, stringMax1Derived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK05.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    foons = corpus.getNamespaceOfSchema("urn:foo");

    stringMax1Derived = corpus.getTypeOfNamespace(foons, "stringMax1Derived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(stringMax1Derived));
    e = corpus.getEnumerationFacetOfSimpleType(stringMax1Derived, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e));
    Assert.assertEquals("X", corpus.getStringValueOfVariant(e));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG05.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    stringMax1Derived = corpus.getTypeOfNamespace(foons, "stringMax1Derived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfSimpleType(stringMax1Derived));
  }
  
  /**
   * Invalid xsd:string enumerated value per length facet constraint. 
   */
  public void testStringEnumLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, stringDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK07.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    foons = corpus.getNamespaceOfSchema("urn:foo");

    stringDerived = corpus.getTypeOfNamespace(foons, "stringDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(stringDerived));
    e = corpus.getEnumerationFacetOfSimpleType(stringDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e));
    Assert.assertEquals("Nagoya", corpus.getStringValueOfVariant(e));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG07.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce.getCode());
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    stringDerived = corpus.getTypeOfNamespace(foons, "stringDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfSimpleType(stringDerived));
  }

  /**
   * Invalid xsd:anyURI enumerated value per length facet constraint. 
   */
  public void testAnyURIEnumLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, anyURIDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK08.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    foons = corpus.getNamespaceOfSchema("urn:foo");

    anyURIDerived = corpus.getTypeOfNamespace(foons, "anyURIDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(anyURIDerived));
    e = corpus.getEnumerationFacetOfSimpleType(anyURIDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e));
    Assert.assertEquals("urn:foo", corpus.getStringValueOfVariant(e));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG08.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce.getCode());
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    anyURIDerived = corpus.getTypeOfNamespace(foons, "anyURIDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfSimpleType(anyURIDerived));
  }

  /**
   * Invalid xsd:QName enumerated value per length facet constraint. 
   */
  public void testQNameEnumLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, qNameDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK09.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    foons = corpus.getNamespaceOfSchema("urn:foo");

    qNameDerived = corpus.getTypeOfNamespace(foons, "qNameDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(qNameDerived));
    e = corpus.getEnumerationFacetOfSimpleType(qNameDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_QNAME, corpus.getTypeOfVariant(e));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfQName(corpus.getQNameValueOfVariant(e)));
    Assert.assertEquals("teapot", corpus.getNameOfQName(corpus.getQNameValueOfVariant(e)));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG09.xsd", getClass(), m_compilerErrorHandler);

    // xerces does not seem to apply length facet to QName.
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    qNameDerived = corpus.getTypeOfNamespace(foons, "qNameDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(qNameDerived));
    e = corpus.getEnumerationFacetOfSimpleType(qNameDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_QNAME, corpus.getTypeOfVariant(e));
    int qname = corpus.getQNameValueOfVariant(e);
    Assert.assertEquals("teapot", corpus.getNameOfQName(qname));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfQName(qname));
  }

  /**
   * Invalid xsd:NOTATION enumerated value per length facet constraint. 
   */
  public void testNOTATIONEnumLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, notationDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK10.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    foons = corpus.getNamespaceOfSchema("urn:foo");

    notationDerived = corpus.getTypeOfNamespace(foons, "notationDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(notationDerived));
    e = corpus.getEnumerationFacetOfSimpleType(notationDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_QNAME, corpus.getTypeOfVariant(e));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfQName(corpus.getQNameValueOfVariant(e)));
    Assert.assertEquals("teapot", corpus.getNameOfQName(corpus.getQNameValueOfVariant(e)));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG10.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    notationDerived = corpus.getTypeOfNamespace(foons, "notationDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(notationDerived));
    e = corpus.getEnumerationFacetOfSimpleType(notationDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_QNAME, corpus.getTypeOfVariant(e));
    int qname = corpus.getQNameValueOfVariant(e);
    Assert.assertEquals("teapot", corpus.getNameOfQName(qname));
    Assert.assertEquals("urn:foo", corpus.getNamespaceNameOfQName(qname));
  }

  /**
   * Invalid xsd:decimal enumerated value per maxExclusive facet constraint. 
   */
  public void testDecimalEnumLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, decimalDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK11.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    foons = corpus.getNamespaceOfSchema("urn:foo");

    decimalDerived = corpus.getTypeOfNamespace(foons, "decimalDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(decimalDerived));
    e = corpus.getEnumerationFacetOfSimpleType(decimalDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(e));
    Assert.assertEquals(0, BigDecimal.valueOf(99).compareTo(corpus.getDecimalValueOfVariant(e)));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG11.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    decimalDerived = corpus.getTypeOfNamespace(foons, "decimalDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfSimpleType(decimalDerived));
  }

  /**
   * Invalid xsd:float enumerated value per maxExclusive facet constraint. 
   */
  public void testFloatEnumLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, floatDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK12.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    foons = corpus.getNamespaceOfSchema("urn:foo");

    floatDerived = corpus.getTypeOfNamespace(foons, "floatDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(floatDerived));
    e = corpus.getEnumerationFacetOfSimpleType(floatDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e));
    Assert.assertEquals((float)100.00, corpus.getFloatValueOfVariant(e), 0.001);

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG12.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    floatDerived = corpus.getTypeOfNamespace(foons, "floatDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfSimpleType(floatDerived));
  }
  
  /**
   * Invalid xsd:double enumerated value per maxExclusive facet constraint. 
   */
  public void testDoubleEnumLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, doubleDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK13.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    foons = corpus.getNamespaceOfSchema("urn:foo");

    doubleDerived = corpus.getTypeOfNamespace(foons, "doubleDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(doubleDerived));
    e = corpus.getEnumerationFacetOfSimpleType(doubleDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_DOUBLE, corpus.getTypeOfVariant(e));
    Assert.assertEquals(0, Double.compare((double)100.00, corpus.getDoubleValueOfVariant(e)));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG13.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    doubleDerived = corpus.getTypeOfNamespace(foons, "doubleDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfSimpleType(doubleDerived));
  }
  
  /**
   * Invalid xsd:dateTime enumerated value per maxExclusive facet constraint. 
   */
  public void testDateTimeEnumLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, dateTimeDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK14.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    foons = corpus.getNamespaceOfSchema("urn:foo");

    dateTimeDerived = corpus.getTypeOfNamespace(foons, "dateTimeDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(dateTimeDerived));
    e = corpus.getEnumerationFacetOfSimpleType(dateTimeDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(e));
    Assert.assertEquals(m_datatypeFactory.newXMLGregorianCalendar("2003-03-19T13:20:00-04:59"),
            corpus.getDateTimeValueOfVariant(e).getXMLGregorianCalendar());

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG14.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    dateTimeDerived = corpus.getTypeOfNamespace(foons, "dateTimeDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfSimpleType(dateTimeDerived));
  }
  
  /**
   * Invalid xsd:duration enumerated value per maxExclusive facet constraint. 
   */
  public void testDurationEnumLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, durationDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK15.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    foons = corpus.getNamespaceOfSchema("urn:foo");

    durationDerived = corpus.getTypeOfNamespace(foons, "durationDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(durationDerived));
    e = corpus.getEnumerationFacetOfSimpleType(durationDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_DURATION, corpus.getTypeOfVariant(e));
    Assert.assertEquals(m_datatypeFactory.newDuration("P1Y2M3DT10H29M"), corpus.getDurationValueOfVariant(e));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG15.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    durationDerived = corpus.getTypeOfNamespace(foons, "durationDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfSimpleType(durationDerived));
  }
  
  /**
   * Invalid xsd:base64Binary enumerated value per length facet constraint. 
   */
  public void testBase64BinaryEnumLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, base64BinaryDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK16.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    foons = corpus.getNamespaceOfSchema("urn:foo");

    base64BinaryDerived = corpus.getTypeOfNamespace(foons, "base64BinaryDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(base64BinaryDerived));
    e = corpus.getEnumerationFacetOfSimpleType(base64BinaryDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_BINARY, corpus.getTypeOfVariant(e));
    Assert.assertEquals("YWFhYWE=",
        SimpleTypeValidator.encodeBinaryByBase64(corpus.getBinaryValueOfVariant(e)));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG16.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    base64BinaryDerived = corpus.getTypeOfNamespace(foons, "base64BinaryDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfSimpleType(base64BinaryDerived));
  }
  
  /**
   * Invalid xsd:hexBinary enumerated value per length facet constraint. 
   */
  public void testHexBinaryEnumLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, hexBinaryDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK17.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    foons = corpus.getNamespaceOfSchema("urn:foo");

    hexBinaryDerived = corpus.getTypeOfNamespace(foons, "hexBinaryDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfSimpleType(hexBinaryDerived));
    e = corpus.getEnumerationFacetOfSimpleType(hexBinaryDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_BINARY, corpus.getTypeOfVariant(e));
    Assert.assertEquals("6161616161",
        SimpleTypeValidator.encodeBinaryByHexBin(corpus.getBinaryValueOfVariant(e)));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG17.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    hexBinaryDerived = corpus.getTypeOfNamespace(foons, "hexBinaryDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfSimpleType(hexBinaryDerived));
  }
  
  /**
   * Invalid enumerated value with regards to union of xsd:int and xsd:NMTOKEN. 
   */
  public void testUnionEnumLength_01() throws Exception {
    EXISchema corpus;
    int foons, e, unionDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK18.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    foons = corpus.getNamespaceOfSchema("urn:foo");

    unionDerived = corpus.getTypeOfNamespace(foons, "unionDerived");
    Assert.assertEquals(2, corpus.getEnumerationFacetCountOfSimpleType(unionDerived));
    e = corpus.getEnumerationFacetOfSimpleType(unionDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_INT, corpus.getTypeOfVariant(e));
    Assert.assertEquals(100, corpus.getIntValueOfVariant(e));
    e = corpus.getEnumerationFacetOfSimpleType(unionDerived, 1);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e));
    Assert.assertEquals("xyz", corpus.getStringValueOfVariant(e));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG18.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(12, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));

    Assert.assertEquals(EXISchemaFactoryException.SCHEMAPARSE_ERROR, sce.getCode());
    

    foons = corpus.getNamespaceOfSchema("urn:foo");

    unionDerived = corpus.getTypeOfNamespace(foons, "unionDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfSimpleType(unionDerived));
  }
    
}
