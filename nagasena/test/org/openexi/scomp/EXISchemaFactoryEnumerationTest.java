package org.openexi.scomp;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.xerces.xni.parser.XMLParseException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.schema.Base64;
import org.openexi.schema.EXISchema;
import org.openexi.schema.HexBin;

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
    int e, string1Derived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    string1Derived = corpus.getTypeOfSchema("urn:foo", "string1Derived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfAtomicSimpleType(string1Derived));
    e = corpus.getEnumerationFacetOfAtomicSimpleType(string1Derived, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e));
    Assert.assertEquals("X", corpus.getStringValueOfVariant(e));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG01.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce.getCode());
    
    string1Derived = corpus.getTypeOfSchema("urn:foo", "string1Derived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(string1Derived));
  }
  
  /**
   * Invalid enumerated value per minLength facet constraint. 
   */
  public void testEnumMinLength_01() throws Exception {
    EXISchema corpus;
    int e, stringMin2Derived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK03.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    stringMin2Derived = corpus.getTypeOfSchema("urn:foo", "stringMin2Derived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfAtomicSimpleType(stringMin2Derived));
    e = corpus.getEnumerationFacetOfAtomicSimpleType(stringMin2Derived, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e));
    Assert.assertEquals("XY", corpus.getStringValueOfVariant(e));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG03.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));

    stringMin2Derived = corpus.getTypeOfSchema("urn:foo", "stringMin2Derived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(stringMin2Derived));
  }
  
  /**
   * Invalid enumerated value per maxLength facet constraint. 
   */
  public void testEnumMaxLength_01() throws Exception {
    EXISchema corpus;
    int e, stringMax1Derived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK05.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    stringMax1Derived = corpus.getTypeOfSchema("urn:foo", "stringMax1Derived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfAtomicSimpleType(stringMax1Derived));
    e = corpus.getEnumerationFacetOfAtomicSimpleType(stringMax1Derived, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e));
    Assert.assertEquals("X", corpus.getStringValueOfVariant(e));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG05.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    
    stringMax1Derived = corpus.getTypeOfSchema("urn:foo", "stringMax1Derived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(stringMax1Derived));
  }
  
  /**
   * Invalid xsd:string enumerated value per length facet constraint. 
   */
  public void testStringEnumLength_01() throws Exception {
    EXISchema corpus;
    int e, stringDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK07.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    stringDerived = corpus.getTypeOfSchema("urn:foo", "stringDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfAtomicSimpleType(stringDerived));
    e = corpus.getEnumerationFacetOfAtomicSimpleType(stringDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e));
    Assert.assertEquals("Nagoya", corpus.getStringValueOfVariant(e));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG07.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce.getCode());
    
    stringDerived = corpus.getTypeOfSchema("urn:foo", "stringDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(stringDerived));
  }

  /**
   * Invalid xsd:anyURI enumerated value per length facet constraint. 
   */
  public void testAnyURIEnumLength_01() throws Exception {
    EXISchema corpus;
    int e, anyURIDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK08.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    anyURIDerived = corpus.getTypeOfSchema("urn:foo", "anyURIDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfAtomicSimpleType(anyURIDerived));
    e = corpus.getEnumerationFacetOfAtomicSimpleType(anyURIDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_STRING, corpus.getTypeOfVariant(e));
    Assert.assertEquals("urn:foo", corpus.getStringValueOfVariant(e));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG08.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce.getCode());
    
    anyURIDerived = corpus.getTypeOfSchema("urn:foo", "anyURIDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(anyURIDerived));
  }

  /**
   * Invalid xsd:QName enumerated value per length facet constraint. 
   */
  public void testQNameEnumLength_01() throws Exception {
    EXISchema corpus;
    int qNameDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK09.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    qNameDerived = corpus.getTypeOfSchema("urn:foo", "qNameDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(qNameDerived));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG09.xsd", getClass(), m_compilerErrorHandler);

    // xerces does not seem to apply length facet to QName.
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    qNameDerived = corpus.getTypeOfSchema("urn:foo", "qNameDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(qNameDerived));
  }

  /**
   * Invalid xsd:NOTATION enumerated value per length facet constraint. 
   */
  public void testNOTATIONEnumLength_01() throws Exception {
    EXISchema corpus;
    int notationDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK10.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    notationDerived = corpus.getTypeOfSchema("urn:foo", "notationDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(notationDerived));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG10.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    notationDerived = corpus.getTypeOfSchema("urn:foo", "notationDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(notationDerived));
  }

  /**
   * Invalid xsd:decimal enumerated value per maxExclusive facet constraint. 
   */
  public void testDecimalEnumLength_01() throws Exception {
    EXISchema corpus;
    int e, decimalDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK11.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    decimalDerived = corpus.getTypeOfSchema("urn:foo", "decimalDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfAtomicSimpleType(decimalDerived));
    e = corpus.getEnumerationFacetOfAtomicSimpleType(decimalDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_DECIMAL, corpus.getTypeOfVariant(e));
    Assert.assertEquals("99", corpus.getVariantCharacters(e).makeString());

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG11.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    
    decimalDerived = corpus.getTypeOfSchema("urn:foo", "decimalDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(decimalDerived));
  }

  /**
   * Invalid xsd:float enumerated value per maxExclusive facet constraint. 
   */
  public void testFloatEnumLength_01() throws Exception {
    EXISchema corpus;
    int e, floatDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK12.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    floatDerived = corpus.getTypeOfSchema("urn:foo", "floatDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfAtomicSimpleType(floatDerived));
    e = corpus.getEnumerationFacetOfAtomicSimpleType(floatDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e));
    Assert.assertEquals("1E2", corpus.getVariantCharacters(e).makeString());

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG12.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    
    floatDerived = corpus.getTypeOfSchema("urn:foo", "floatDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(floatDerived));
  }
  
  /**
   * Invalid xsd:double enumerated value per maxExclusive facet constraint. 
   */
  public void testDoubleEnumLength_01() throws Exception {
    EXISchema corpus;
    int e, doubleDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK13.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    doubleDerived = corpus.getTypeOfSchema("urn:foo", "doubleDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfAtomicSimpleType(doubleDerived));
    e = corpus.getEnumerationFacetOfAtomicSimpleType(doubleDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_FLOAT, corpus.getTypeOfVariant(e));
    Assert.assertEquals("1E2", corpus.getVariantCharacters(e).makeString());

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG13.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    
    doubleDerived = corpus.getTypeOfSchema("urn:foo", "doubleDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(doubleDerived));
  }
  
  /**
   * Invalid xsd:dateTime enumerated value per maxExclusive facet constraint. 
   */
  public void testDateTimeEnumLength_01() throws Exception {
    EXISchema corpus;
    int e, dateTimeDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK14.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    dateTimeDerived = corpus.getTypeOfSchema("urn:foo", "dateTimeDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfAtomicSimpleType(dateTimeDerived));
    e = corpus.getEnumerationFacetOfAtomicSimpleType(dateTimeDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_DATETIME, corpus.getTypeOfVariant(e));
    Assert.assertEquals("2003-03-19T13:20:00-04:59", corpus.getDateTimeValueOfVariant(e).toString());

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG14.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    
    dateTimeDerived = corpus.getTypeOfSchema("urn:foo", "dateTimeDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(dateTimeDerived));
  }
  
  /**
   * Invalid xsd:duration enumerated value per maxExclusive facet constraint. 
   */
  public void testDurationEnumLength_01() throws Exception {
    EXISchema corpus;
    int e, durationDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK15.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    durationDerived = corpus.getTypeOfSchema("urn:foo", "durationDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfAtomicSimpleType(durationDerived));
    e = corpus.getEnumerationFacetOfAtomicSimpleType(durationDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_DURATION, corpus.getTypeOfVariant(e));
    Assert.assertEquals(m_datatypeFactory.newDuration("P1Y2M3DT10H29M"), corpus.getDurationValueOfVariant(e));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG15.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    
    durationDerived = corpus.getTypeOfSchema("urn:foo", "durationDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(durationDerived));
  }
  
  /**
   * Invalid xsd:base64Binary enumerated value per length facet constraint. 
   */
  public void testBase64BinaryEnumLength_01() throws Exception {
    EXISchema corpus;
    int e, base64BinaryDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK16.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    base64BinaryDerived = corpus.getTypeOfSchema("urn:foo", "base64BinaryDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfAtomicSimpleType(base64BinaryDerived));
    e = corpus.getEnumerationFacetOfAtomicSimpleType(base64BinaryDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_BASE64, corpus.getTypeOfVariant(e));
    final byte[] octets = corpus.getBinaryValueOfVariant(e);
    final char[] base64BinaryChars = new char[16];
    Assert.assertEquals(8, Base64.encode(octets, 0, octets.length, base64BinaryChars, 0));
    Assert.assertEquals("YWFhYWE=", new String(base64BinaryChars, 0, 8));

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG16.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    
    base64BinaryDerived = corpus.getTypeOfSchema("urn:foo", "base64BinaryDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(base64BinaryDerived));
  }
  
  /**
   * Invalid xsd:hexBinary enumerated value per length facet constraint. 
   */
  public void testHexBinaryEnumLength_01() throws Exception {
    EXISchema corpus;
    int e, hexBinaryDerived;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK17.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    hexBinaryDerived = corpus.getTypeOfSchema("urn:foo", "hexBinaryDerived");
    Assert.assertEquals(1, corpus.getEnumerationFacetCountOfAtomicSimpleType(hexBinaryDerived));
    e = corpus.getEnumerationFacetOfAtomicSimpleType(hexBinaryDerived, 0);
    Assert.assertEquals(EXISchema.VARIANT_HEXBIN, corpus.getTypeOfVariant(e));
    final byte[] octets = corpus.getBinaryValueOfVariant(e);
    final StringBuffer hexBin = new StringBuffer();
    HexBin.encode(octets, octets.length, hexBin);
    Assert.assertEquals("6161616161", hexBin.toString());

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG17.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(14, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));
    
    hexBinaryDerived = corpus.getTypeOfSchema("urn:foo", "hexBinaryDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(hexBinaryDerived));
  }
  
  /**
   * Invalid enumerated value with regards to union of xsd:int and xsd:NMTOKEN. 
   */
  public void testUnionEnumLength_01() throws Exception {
    
    EXISchemaFactoryTestUtil.getEXISchema("/enumerationOK18.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    m_compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
    
    EXISchemaFactoryTestUtil.getEXISchema("/enumerationNG18.xsd", getClass(), m_compilerErrorHandler);
    
    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
            EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(12, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("enumeration-valid-restriction:"));

    Assert.assertEquals(EXISchemaFactoryException.XMLSCHEMA_ERROR, sce.getCode());
  }

  /**
   * Obsolete xsd:gMonth enumerated values are simply ignored. 
   */
  public void testGMonthEnumObsolete_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/enumeratedGMonthObsolete.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int gMonthDerived = corpus.getTypeOfSchema("urn:foo", "gMonthDerived");
    Assert.assertEquals(0, corpus.getEnumerationFacetCountOfAtomicSimpleType(gMonthDerived));
  }

}
