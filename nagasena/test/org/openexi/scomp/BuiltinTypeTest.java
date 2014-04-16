package org.openexi.scomp;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.proc.common.XmlUriConst;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.xml.sax.InputSource;

/**
 */
public class BuiltinTypeTest extends TestCase {

  public BuiltinTypeTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Test builtin simple types.
   */
  public void testBuiltinSimpleTypes() throws Exception {
    EXISchemaFactory schemaCompiler = new EXISchemaFactory();
    EXISchema corpus = schemaCompiler.compile((InputSource)null);

    int _anyType = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "anyType");
    Assert.assertTrue(_anyType != EXISchema.NIL_NODE);

    int _anySimpleType = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "anySimpleType");
    Assert.assertTrue(corpus.isSimpleType(_anySimpleType));
    Assert.assertEquals("anySimpleType", corpus.getNameOfType(_anySimpleType));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_anySimpleType));
    Assert.assertEquals(_anyType, corpus.getBaseTypeOfSimpleType(_anySimpleType));
    Assert.assertFalse(corpus.isIntegralSimpleType(_anySimpleType));
    Assert.assertEquals(EXISchema.UR_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_anySimpleType));
    Assert.assertEquals(EXISchemaConst.UNTYPED, corpus.ancestryIds[corpus.getSerialOfType((_anySimpleType))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_anySimpleType));
    Assert.assertEquals(EXISchemaConst.ANY_SIMPLE_TYPE, corpus.getSerialOfType(_anySimpleType));
    
    // atomic types

    int _string = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "string");
    Assert.assertTrue(corpus.isSimpleType(_string));
    Assert.assertEquals("string", corpus.getNameOfType(_string));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_string));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_string));
    Assert.assertFalse(corpus.isIntegralSimpleType(_string));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_string));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_string))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_string));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, corpus.getSerialOfType(_string));
    Assert.assertEquals(EXISchema.WHITESPACE_PRESERVE,
        corpus.getWhitespaceFacetValueOfStringSimpleType(_string));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfStringSimpleType(_string));

    int _boolean = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "boolean");
    Assert.assertTrue(corpus.isSimpleType(_boolean));
    Assert.assertEquals("boolean", corpus.getNameOfType(_boolean));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_boolean));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_boolean));
    Assert.assertFalse(corpus.isIntegralSimpleType(_boolean));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_boolean));
    Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_boolean))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_boolean));
    Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(_boolean));

    int _decimal = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "decimal");
    Assert.assertTrue(corpus.isSimpleType(_decimal));
    Assert.assertEquals("decimal", corpus.getNameOfType(_decimal));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_decimal));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_decimal));
    Assert.assertFalse(corpus.isIntegralSimpleType(_decimal));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_decimal));
    Assert.assertEquals(EXISchemaConst.DECIMAL_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_decimal))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_decimal));
    Assert.assertEquals(EXISchemaConst.DECIMAL_TYPE, corpus.getSerialOfType(_decimal));

    int _float = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "float");
    Assert.assertTrue(corpus.isSimpleType(_float));
    Assert.assertEquals("float", corpus.getNameOfType(_float));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_float));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_float));
    Assert.assertFalse(corpus.isIntegralSimpleType(_float));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_float));
    Assert.assertEquals(EXISchemaConst.FLOAT_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_float))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_float));
    Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, corpus.getSerialOfType(_float));

    int _double = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "double");
    Assert.assertTrue(corpus.isSimpleType(_double));
    Assert.assertEquals("double", corpus.getNameOfType(_double));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_double));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_double));
    Assert.assertFalse(corpus.isIntegralSimpleType(_double));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_double));
    Assert.assertEquals(EXISchemaConst.FLOAT_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_double))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_double));
    Assert.assertEquals(EXISchemaConst.DOUBLE_TYPE, corpus.getSerialOfType(_double));

    int _duration = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "duration");
    Assert.assertTrue(corpus.isSimpleType(_duration));
    Assert.assertEquals("duration", corpus.getNameOfType(_duration));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_duration));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_duration));
    Assert.assertFalse(corpus.isIntegralSimpleType(_duration));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_duration));
    Assert.assertEquals(EXISchemaConst.DURATION_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_duration))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_duration));
    Assert.assertEquals(EXISchemaConst.DURATION_TYPE, corpus.getSerialOfType(_duration));

    int _dateTime = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "dateTime");
    Assert.assertTrue(corpus.isSimpleType(_dateTime));
    Assert.assertEquals("dateTime", corpus.getNameOfType(_dateTime));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_dateTime));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_dateTime));
    Assert.assertFalse(corpus.isIntegralSimpleType(_dateTime));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_dateTime));
    Assert.assertEquals(EXISchemaConst.DATETIME_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_dateTime))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_dateTime));
    Assert.assertEquals(EXISchemaConst.DATETIME_TYPE, corpus.getSerialOfType(_dateTime));

    int _time = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "time");
    Assert.assertTrue(corpus.isSimpleType(_time));
    Assert.assertEquals("time", corpus.getNameOfType(_time));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_time));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_time));
    Assert.assertFalse(corpus.isIntegralSimpleType(_time));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_time));
    Assert.assertEquals(EXISchemaConst.TIME_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_time))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_time));
    Assert.assertEquals(EXISchemaConst.TIME_TYPE, corpus.getSerialOfType(_time));

    int _date = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "date");
    Assert.assertTrue(corpus.isSimpleType(_date));
    Assert.assertEquals("date", corpus.getNameOfType(_date));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_date));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_date));
    Assert.assertFalse(corpus.isIntegralSimpleType(_date));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_date));
    Assert.assertEquals(EXISchemaConst.DATE_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_date))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_date));
    Assert.assertEquals(EXISchemaConst.DATE_TYPE, corpus.getSerialOfType(_date));

    int _gYearMonth = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "gYearMonth");
    Assert.assertTrue(corpus.isSimpleType(_gYearMonth));
    Assert.assertEquals("gYearMonth", corpus.getNameOfType(_gYearMonth));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_gYearMonth));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_gYearMonth));
    Assert.assertFalse(corpus.isIntegralSimpleType(_gYearMonth));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_gYearMonth));
    Assert.assertEquals(EXISchemaConst.G_YEARMONTH_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_gYearMonth))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_gYearMonth));
    Assert.assertEquals(EXISchemaConst.G_YEARMONTH_TYPE, corpus.getSerialOfType(_gYearMonth));

    int _gYear = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "gYear");
    Assert.assertTrue(corpus.isSimpleType(_gYear));
    Assert.assertEquals("gYear", corpus.getNameOfType(_gYear));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_gYear));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_gYear));
    Assert.assertFalse(corpus.isIntegralSimpleType(_gYear));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_gYear));
    Assert.assertEquals(EXISchemaConst.G_YEAR_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_gYear))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_gYear));
    Assert.assertEquals(EXISchemaConst.G_YEAR_TYPE, corpus.getSerialOfType(_gYear));

    int _gMonthDay = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "gMonthDay");
    Assert.assertTrue(corpus.isSimpleType(_gMonthDay));
    Assert.assertEquals("gMonthDay", corpus.getNameOfType(_gMonthDay));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_gMonthDay));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_gMonthDay));
    Assert.assertFalse(corpus.isIntegralSimpleType(_gMonthDay));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_gMonthDay));
    Assert.assertEquals(EXISchemaConst.G_MONTHDAY_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_gMonthDay))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_gMonthDay));
    Assert.assertEquals(EXISchemaConst.G_MONTHDAY_TYPE, corpus.getSerialOfType(_gMonthDay));

    int _gDay = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "gDay");
    Assert.assertTrue(corpus.isSimpleType(_gDay));
    Assert.assertEquals("gDay", corpus.getNameOfType(_gDay));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_gDay));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_gDay));
    Assert.assertFalse(corpus.isIntegralSimpleType(_gDay));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_gDay));
    Assert.assertEquals(EXISchemaConst.G_DAY_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_gDay))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_gDay));
    Assert.assertEquals(EXISchemaConst.G_DAY_TYPE, corpus.getSerialOfType(_gDay));

    int _gMonth = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "gMonth");
    Assert.assertTrue(corpus.isSimpleType(_gMonth));
    Assert.assertEquals("gMonth", corpus.getNameOfType(_gMonth));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_gMonth));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_gMonth));
    Assert.assertFalse(corpus.isIntegralSimpleType(_gMonth));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_gMonth));
    Assert.assertEquals(EXISchemaConst.G_MONTH_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_gMonth))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_gMonth));
    Assert.assertEquals(EXISchemaConst.G_MONTH_TYPE, corpus.getSerialOfType(_gMonth));

    int _hexBinary = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "hexBinary");
    Assert.assertTrue(corpus.isSimpleType(_hexBinary));
    Assert.assertEquals("hexBinary", corpus.getNameOfType(_hexBinary));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_hexBinary));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_hexBinary));
    Assert.assertFalse(corpus.isIntegralSimpleType(_hexBinary));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_hexBinary));
    Assert.assertEquals(EXISchemaConst.HEXBINARY_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_hexBinary))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_hexBinary));
    Assert.assertEquals(EXISchemaConst.HEXBINARY_TYPE, corpus.getSerialOfType(_hexBinary));

    int _base64Binary = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "base64Binary");
    Assert.assertTrue(corpus.isSimpleType(_base64Binary));
    Assert.assertEquals("base64Binary", corpus.getNameOfType(_base64Binary));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_base64Binary));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_base64Binary));
    Assert.assertFalse(corpus.isIntegralSimpleType(_base64Binary));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_base64Binary));
    Assert.assertEquals(EXISchemaConst.BASE64BINARY_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_base64Binary))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_base64Binary));
    Assert.assertEquals(EXISchemaConst.BASE64BINARY_TYPE, corpus.getSerialOfType(_base64Binary));

    int _anyURI = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "anyURI");
    Assert.assertTrue(corpus.isSimpleType(_anyURI));
    Assert.assertEquals("anyURI", corpus.getNameOfType(_anyURI));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_anyURI));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_anyURI));
    Assert.assertFalse(corpus.isIntegralSimpleType(_anyURI));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_anyURI));
    Assert.assertEquals(EXISchemaConst.ANYURI_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_anyURI))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_anyURI));
    Assert.assertEquals(EXISchemaConst.ANYURI_TYPE, corpus.getSerialOfType(_anyURI));

    int _QName = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "QName");
    Assert.assertTrue(corpus.isSimpleType(_QName));
    Assert.assertEquals("QName", corpus.getNameOfType(_QName));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_QName));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_QName));
    Assert.assertFalse(corpus.isIntegralSimpleType(_QName));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_QName));
    Assert.assertEquals(EXISchemaConst.UNTYPED, // Ancestry ID does not matter for QName
        corpus.ancestryIds[corpus.getSerialOfType((_QName))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_QName));
    Assert.assertEquals(EXISchemaConst.QNAME_TYPE, corpus.getSerialOfType(_QName));

    int _NOTATION = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "NOTATION");
    Assert.assertTrue(corpus.isSimpleType(_NOTATION));
    Assert.assertEquals("NOTATION", corpus.getNameOfType(_NOTATION));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_NOTATION));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_NOTATION));
    Assert.assertFalse(corpus.isIntegralSimpleType(_NOTATION));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_NOTATION));
    Assert.assertEquals(EXISchemaConst.UNTYPED, // Ancestry ID does not matter for NOTATION
        corpus.ancestryIds[corpus.getSerialOfType((_NOTATION))]);
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_NOTATION));
    Assert.assertEquals(EXISchemaConst.NOTATION_TYPE, corpus.getSerialOfType(_NOTATION));

    int _normalizedString = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "normalizedString");
    Assert.assertTrue(corpus.isSimpleType(_normalizedString));
    Assert.assertEquals("normalizedString", corpus.getNameOfType(_normalizedString));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_normalizedString));
    Assert.assertEquals(_string, corpus.getBaseTypeOfSimpleType(_normalizedString));
    Assert.assertFalse(corpus.isIntegralSimpleType(_normalizedString));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_normalizedString));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_normalizedString))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_normalizedString));
    Assert.assertEquals(EXISchema.WHITESPACE_REPLACE,
                      corpus.getWhitespaceFacetValueOfStringSimpleType(_normalizedString));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfStringSimpleType(_normalizedString));

    int _token = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "token");
    Assert.assertTrue(corpus.isSimpleType(_token));
    Assert.assertEquals("token", corpus.getNameOfType(_token));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_token));
    Assert.assertEquals(_normalizedString, corpus.getBaseTypeOfSimpleType(_token));
    Assert.assertFalse(corpus.isIntegralSimpleType(_token));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_token));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_token))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_token));
    Assert.assertEquals(EXISchema.WHITESPACE_COLLAPSE,
                      corpus.getWhitespaceFacetValueOfStringSimpleType(_token));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfStringSimpleType(_token));

    int _language = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "language");
    Assert.assertTrue(corpus.isSimpleType(_language));
    Assert.assertEquals("language", corpus.getNameOfType(_language));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_language));
    Assert.assertEquals(_token, corpus.getBaseTypeOfSimpleType(_language));
    Assert.assertFalse(corpus.isIntegralSimpleType(_language));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_language));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_language))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_language));

    int _NMTOKEN = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "NMTOKEN");
    Assert.assertTrue(corpus.isSimpleType(_NMTOKEN));
    Assert.assertEquals("NMTOKEN", corpus.getNameOfType(_NMTOKEN));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_NMTOKEN));
    Assert.assertEquals(_token, corpus.getBaseTypeOfSimpleType(_NMTOKEN));
    Assert.assertFalse(corpus.isIntegralSimpleType(_NMTOKEN));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_NMTOKEN));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_NMTOKEN))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_NMTOKEN));
    // pattern "\\c+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfStringSimpleType(_NMTOKEN));

    int _Name = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "Name");
    Assert.assertTrue(corpus.isSimpleType(_Name));
    Assert.assertEquals("Name", corpus.getNameOfType(_Name));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_Name));
    Assert.assertEquals(_token, corpus.getBaseTypeOfSimpleType(_Name));
    Assert.assertFalse(corpus.isIntegralSimpleType(_Name));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_Name));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_Name))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_Name));
    // pattern "\\i\\c*"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfStringSimpleType(_Name));

    int _NCName = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "NCName");
    Assert.assertTrue(corpus.isSimpleType(_NCName));
    Assert.assertEquals("NCName", corpus.getNameOfType(_NCName));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_NCName));
    Assert.assertEquals(_Name, corpus.getBaseTypeOfSimpleType(_NCName));
    Assert.assertFalse(corpus.isIntegralSimpleType(_NCName));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_NCName));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_NCName))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_NCName));
    // pattern "[\\i-[:]][\\c-[:]]*"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfStringSimpleType(_NCName));

    int _ID = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "ID");
    Assert.assertTrue(corpus.isSimpleType(_ID));
    Assert.assertEquals("ID", corpus.getNameOfType(_ID));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_ID));
    Assert.assertEquals(_NCName, corpus.getBaseTypeOfSimpleType(_ID));
    Assert.assertFalse(corpus.isIntegralSimpleType(_ID));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_ID));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_ID))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_ID));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfStringSimpleType(_ID));

    int _IDREF = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "IDREF");
    Assert.assertTrue(corpus.isSimpleType(_IDREF));
    Assert.assertEquals("IDREF", corpus.getNameOfType(_IDREF));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_IDREF));
    Assert.assertEquals(_NCName, corpus.getBaseTypeOfSimpleType(_IDREF));
    Assert.assertFalse(corpus.isIntegralSimpleType(_IDREF));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_IDREF));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_IDREF))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_IDREF));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfStringSimpleType(_IDREF));

    int _ENTITY = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "ENTITY");
    Assert.assertTrue(corpus.isSimpleType(_ENTITY));
    Assert.assertEquals("ENTITY", corpus.getNameOfType(_ENTITY));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_ENTITY));
    Assert.assertEquals(_NCName, corpus.getBaseTypeOfSimpleType(_ENTITY));
    Assert.assertFalse(corpus.isIntegralSimpleType(_ENTITY));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_ENTITY));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_ENTITY))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_ENTITY));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfStringSimpleType(_ENTITY));

    int _integer = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "integer");
    Assert.assertTrue(corpus.isSimpleType(_integer));
    Assert.assertEquals("integer", corpus.getNameOfType(_integer));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_integer));
    Assert.assertEquals(_decimal, corpus.getBaseTypeOfSimpleType(_integer));
    Assert.assertTrue(corpus.isIntegralSimpleType(_integer));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_integer));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_integer))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_integer));

    int _nonPositiveInteger = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "nonPositiveInteger");
    Assert.assertTrue(corpus.isSimpleType(_nonPositiveInteger));
    Assert.assertEquals("nonPositiveInteger", corpus.getNameOfType(_nonPositiveInteger));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_nonPositiveInteger));
    Assert.assertEquals(_integer, corpus.getBaseTypeOfSimpleType(_nonPositiveInteger));
    Assert.assertTrue(corpus.isIntegralSimpleType(_nonPositiveInteger));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_nonPositiveInteger));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_nonPositiveInteger))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_nonPositiveInteger));

    int _negativeInteger = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "negativeInteger");
    Assert.assertTrue(corpus.isSimpleType(_negativeInteger));
    Assert.assertEquals("negativeInteger", corpus.getNameOfType(_negativeInteger));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_negativeInteger));
    Assert.assertEquals(_nonPositiveInteger, corpus.getBaseTypeOfSimpleType(_negativeInteger));
    Assert.assertTrue(corpus.isIntegralSimpleType(_negativeInteger));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_negativeInteger));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_negativeInteger))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_negativeInteger));

    int _long = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "long");
    Assert.assertTrue(corpus.isSimpleType(_long));
    Assert.assertEquals("long", corpus.getNameOfType(_long));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_long));
    Assert.assertEquals(_integer, corpus.getBaseTypeOfSimpleType(_long));
    Assert.assertTrue(corpus.isIntegralSimpleType(_long));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_long));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_long))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_long));

    int _int = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "int");
    Assert.assertTrue(corpus.isSimpleType(_int));
    Assert.assertEquals("int", corpus.getNameOfType(_int));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_int));
    Assert.assertEquals(_long, corpus.getBaseTypeOfSimpleType(_int));
    Assert.assertTrue(corpus.isIntegralSimpleType(_int));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_int));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_int))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_int));

    int _short = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "short");
    Assert.assertTrue(corpus.isSimpleType(_short));
    Assert.assertEquals("short", corpus.getNameOfType(_short));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_short));
    Assert.assertEquals(_int, corpus.getBaseTypeOfSimpleType(_short));
    Assert.assertTrue(corpus.isIntegralSimpleType(_short));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_short));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_short))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_short));

    int _byte = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "byte");
    Assert.assertTrue(corpus.isSimpleType(_byte));
    Assert.assertEquals("byte", corpus.getNameOfType(_byte));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_byte));
    Assert.assertEquals(_short, corpus.getBaseTypeOfSimpleType(_byte));
    Assert.assertTrue(corpus.isIntegralSimpleType(_byte));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_byte));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_byte))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_byte));

    int _nonNegativeInteger = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "nonNegativeInteger");
    Assert.assertTrue(corpus.isSimpleType(_nonNegativeInteger));
    Assert.assertEquals("nonNegativeInteger", corpus.getNameOfType(_nonNegativeInteger));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_nonNegativeInteger));
    Assert.assertEquals(_integer, corpus.getBaseTypeOfSimpleType(_nonNegativeInteger));
    Assert.assertTrue(corpus.isIntegralSimpleType(_nonNegativeInteger));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_nonNegativeInteger));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_nonNegativeInteger))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_nonNegativeInteger));

    int _unsignedLong = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "unsignedLong");
    Assert.assertTrue(corpus.isSimpleType(_unsignedLong));
    Assert.assertEquals("unsignedLong", corpus.getNameOfType(_unsignedLong));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_unsignedLong));
    Assert.assertEquals(_nonNegativeInteger, corpus.getBaseTypeOfSimpleType(_unsignedLong));
    Assert.assertTrue(corpus.isIntegralSimpleType(_unsignedLong));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_unsignedLong));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_unsignedLong))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_unsignedLong));

    int _unsignedInt = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "unsignedInt");
    Assert.assertTrue(corpus.isSimpleType(_unsignedInt));
    Assert.assertEquals("unsignedInt", corpus.getNameOfType(_unsignedInt));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_unsignedInt));
    Assert.assertEquals(_unsignedLong, corpus.getBaseTypeOfSimpleType(_unsignedInt));
    Assert.assertTrue(corpus.isIntegralSimpleType(_unsignedInt));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_unsignedInt));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_unsignedInt))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_unsignedInt));

    int _unsignedShort = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "unsignedShort");
    Assert.assertTrue(corpus.isSimpleType(_unsignedShort));
    Assert.assertEquals("unsignedShort", corpus.getNameOfType(_unsignedShort));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_unsignedShort));
    Assert.assertEquals(_unsignedInt, corpus.getBaseTypeOfSimpleType(_unsignedShort));
    Assert.assertTrue(corpus.isIntegralSimpleType(_unsignedShort));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_unsignedShort));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_unsignedShort))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_unsignedShort));

    int _unsignedByte = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "unsignedByte");
    Assert.assertTrue(corpus.isSimpleType(_unsignedByte));
    Assert.assertEquals("unsignedByte", corpus.getNameOfType(_unsignedByte));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_unsignedByte));
    Assert.assertEquals(_unsignedShort, corpus.getBaseTypeOfSimpleType(_unsignedByte));
    Assert.assertTrue(corpus.isIntegralSimpleType(_unsignedByte));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_unsignedByte));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_unsignedByte))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_unsignedByte));

    int _positiveInteger = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "positiveInteger");
    Assert.assertTrue(corpus.isSimpleType(_positiveInteger));
    Assert.assertEquals("positiveInteger", corpus.getNameOfType(_positiveInteger));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_positiveInteger));
    Assert.assertEquals(_nonNegativeInteger, corpus.getBaseTypeOfSimpleType(_positiveInteger));
    Assert.assertTrue(corpus.isIntegralSimpleType(_positiveInteger));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_positiveInteger));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE,
                      corpus.ancestryIds[corpus.getSerialOfType((_positiveInteger))]);
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_positiveInteger));

    // list types

    int _ENTITIES = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "ENTITIES");
    Assert.assertTrue(corpus.isSimpleType(_ENTITIES));
    Assert.assertEquals("ENTITIES", corpus.getNameOfType(_ENTITIES));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_ENTITIES));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_ENTITIES));
    Assert.assertFalse(corpus.isIntegralSimpleType(_ENTITIES));
    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(_ENTITIES));
    Assert.assertEquals(EXISchemaConst.UNTYPED, corpus.ancestryIds[corpus.getSerialOfType((_ENTITIES))]);
    Assert.assertEquals(_ENTITY, corpus.getItemTypeOfListSimpleType(_ENTITIES));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_ENTITIES));
    
    int _IDREFS = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "IDREFS");
    Assert.assertTrue(corpus.isSimpleType(_IDREFS));
    Assert.assertEquals("IDREFS", corpus.getNameOfType(_IDREFS));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_IDREFS));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_IDREFS));
    Assert.assertFalse(corpus.isIntegralSimpleType(_IDREFS));
    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(_IDREFS));
    Assert.assertEquals(_IDREF, corpus.getItemTypeOfListSimpleType(_IDREFS));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_IDREFS));
    
    int _NMTOKENS = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "NMTOKENS");
    Assert.assertTrue(corpus.isSimpleType(_NMTOKENS));
    Assert.assertEquals("NMTOKENS", corpus.getNameOfType(_NMTOKENS));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_NMTOKENS));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfSimpleType(_NMTOKENS));
    Assert.assertFalse(corpus.isIntegralSimpleType(_NMTOKENS));
    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_NMTOKENS));
    Assert.assertEquals(_NMTOKEN,
                      corpus.getItemTypeOfListSimpleType(_NMTOKENS));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_NMTOKENS));
  }

  /**
   * Test serial numbers of primitive types.
   */
  public void testPrimitiveTypeSerialNumbers() throws Exception {
    EXISchemaFactory schemaCompiler = new EXISchemaFactory();
    EXISchema corpus = schemaCompiler.compile((InputSource)null);
    int _anySimpleType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.ANY_SIMPLE_TYPE);
    Assert.assertTrue(_anySimpleType != EXISchema.NIL_NODE);
    Assert.assertEquals("anySimpleType", corpus.getNameOfType(_anySimpleType));
    Assert.assertEquals(EXISchemaConst.ANY_SIMPLE_TYPE, corpus.getSerialOfType(_anySimpleType));

    int _string = corpus.getBuiltinTypeOfSchema(EXISchemaConst.STRING_TYPE);
    Assert.assertTrue(_string != EXISchema.NIL_NODE);
    Assert.assertEquals("string", corpus.getNameOfType(_string));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, corpus.getSerialOfType(_string));

    int _boolean = corpus.getBuiltinTypeOfSchema(EXISchemaConst.BOOLEAN_TYPE);
    Assert.assertTrue(_boolean != EXISchema.NIL_NODE);
    Assert.assertEquals("boolean", corpus.getNameOfType(_boolean));
    Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(_boolean));

    int _decimal = corpus.getBuiltinTypeOfSchema(EXISchemaConst.DECIMAL_TYPE);
    Assert.assertTrue(_decimal != EXISchema.NIL_NODE);
    Assert.assertEquals("decimal", corpus.getNameOfType(_decimal));
    Assert.assertEquals(EXISchemaConst.DECIMAL_TYPE, corpus.getSerialOfType(_decimal));

    int _float = corpus.getBuiltinTypeOfSchema(EXISchemaConst.FLOAT_TYPE);
    Assert.assertTrue(_float != EXISchema.NIL_NODE);
    Assert.assertEquals("float", corpus.getNameOfType(_float));
    Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, corpus.getSerialOfType(_float));

    int _double = corpus.getBuiltinTypeOfSchema(EXISchemaConst.DOUBLE_TYPE);
    Assert.assertTrue(_double != EXISchema.NIL_NODE);
    Assert.assertEquals("double", corpus.getNameOfType(_double));
    Assert.assertEquals(EXISchemaConst.DOUBLE_TYPE, corpus.getSerialOfType(_double));

    int _duration = corpus.getBuiltinTypeOfSchema(EXISchemaConst.DURATION_TYPE);
    Assert.assertTrue(_duration != EXISchema.NIL_NODE);
    Assert.assertEquals("duration", corpus.getNameOfType(_duration));
    Assert.assertEquals(EXISchemaConst.DURATION_TYPE, corpus.getSerialOfType(_duration));

    int _dateTime = corpus.getBuiltinTypeOfSchema(EXISchemaConst.DATETIME_TYPE);
    Assert.assertTrue(_dateTime != EXISchema.NIL_NODE);
    Assert.assertEquals("dateTime", corpus.getNameOfType(_dateTime));
    Assert.assertEquals(EXISchemaConst.DATETIME_TYPE, corpus.getSerialOfType(_dateTime));

    int _time = corpus.getBuiltinTypeOfSchema(EXISchemaConst.TIME_TYPE);
    Assert.assertTrue(_time != EXISchema.NIL_NODE);
    Assert.assertEquals("time", corpus.getNameOfType(_time));
    Assert.assertEquals(EXISchemaConst.TIME_TYPE, corpus.getSerialOfType(_time));

    int _date = corpus.getBuiltinTypeOfSchema(EXISchemaConst.DATE_TYPE);
    Assert.assertTrue(_date != EXISchema.NIL_NODE);
    Assert.assertEquals("date", corpus.getNameOfType(_date));
    Assert.assertEquals(EXISchemaConst.DATE_TYPE, corpus.getSerialOfType(_date));

    int _gYearMonth = corpus.getBuiltinTypeOfSchema(EXISchemaConst.G_YEARMONTH_TYPE);
    Assert.assertTrue(_gYearMonth != EXISchema.NIL_NODE);
    Assert.assertEquals("gYearMonth", corpus.getNameOfType(_gYearMonth));
    Assert.assertEquals(EXISchemaConst.G_YEARMONTH_TYPE, corpus.getSerialOfType(_gYearMonth));

    int _gYear = corpus.getBuiltinTypeOfSchema(EXISchemaConst.G_YEAR_TYPE);
    Assert.assertTrue(_gYear != EXISchema.NIL_NODE);
    Assert.assertEquals("gYear", corpus.getNameOfType(_gYear));
    Assert.assertEquals(EXISchemaConst.G_YEAR_TYPE, corpus.getSerialOfType(_gYear));

    int _gMonthDay = corpus.getBuiltinTypeOfSchema(EXISchemaConst.G_MONTHDAY_TYPE);
    Assert.assertTrue(_gMonthDay != EXISchema.NIL_NODE);
    Assert.assertEquals("gMonthDay", corpus.getNameOfType(_gMonthDay));
    Assert.assertEquals(EXISchemaConst.G_MONTHDAY_TYPE, corpus.getSerialOfType(_gMonthDay));

    int _gDay = corpus.getBuiltinTypeOfSchema(EXISchemaConst.G_DAY_TYPE);
    Assert.assertTrue(_gDay != EXISchema.NIL_NODE);
    Assert.assertEquals("gDay", corpus.getNameOfType(_gDay));
    Assert.assertEquals(EXISchemaConst.G_DAY_TYPE, corpus.getSerialOfType(_gDay));

    int _gMonth = corpus.getBuiltinTypeOfSchema(EXISchemaConst.G_MONTH_TYPE);
    Assert.assertTrue(_gMonth != EXISchema.NIL_NODE);
    Assert.assertEquals("gMonth", corpus.getNameOfType(_gMonth));
    Assert.assertEquals(EXISchemaConst.G_MONTH_TYPE, corpus.getSerialOfType(_gMonth));

    int _hexBinary = corpus.getBuiltinTypeOfSchema(EXISchemaConst.HEXBINARY_TYPE);
    Assert.assertTrue(_hexBinary != EXISchema.NIL_NODE);
    Assert.assertEquals("hexBinary", corpus.getNameOfType(_hexBinary));
    Assert.assertEquals(EXISchemaConst.HEXBINARY_TYPE, corpus.getSerialOfType(_hexBinary));

    int _base64Binary = corpus.getBuiltinTypeOfSchema(EXISchemaConst.BASE64BINARY_TYPE);
    Assert.assertTrue(_base64Binary != EXISchema.NIL_NODE);
    Assert.assertEquals("base64Binary", corpus.getNameOfType(_base64Binary));
    Assert.assertEquals(EXISchemaConst.BASE64BINARY_TYPE, corpus.getSerialOfType(_base64Binary));

    int _anyURI = corpus.getBuiltinTypeOfSchema(EXISchemaConst.ANYURI_TYPE);
    Assert.assertTrue(_anyURI != EXISchema.NIL_NODE);
    Assert.assertEquals("anyURI", corpus.getNameOfType(_anyURI));
    Assert.assertEquals(EXISchemaConst.ANYURI_TYPE, corpus.getSerialOfType(_anyURI));

    int _QName = corpus.getBuiltinTypeOfSchema(EXISchemaConst.QNAME_TYPE);
    Assert.assertTrue(_QName != EXISchema.NIL_NODE);
    Assert.assertEquals("QName", corpus.getNameOfType(_QName));
    Assert.assertEquals(EXISchemaConst.QNAME_TYPE, corpus.getSerialOfType(_QName));

    int _NOTATION = corpus.getBuiltinTypeOfSchema(EXISchemaConst.NOTATION_TYPE);
    Assert.assertTrue(_NOTATION != EXISchema.NIL_NODE);
    Assert.assertEquals("NOTATION", corpus.getNameOfType(_NOTATION));
    Assert.assertEquals(EXISchemaConst.NOTATION_TYPE, corpus.getSerialOfType(_NOTATION));
  }

  /**
   * Test serial numbers of decimal-derived types.
   */
  public void testDecimalDerivedTypeSerialNumbers() throws Exception {
    EXISchemaFactory schemaCompiler = new EXISchemaFactory();
    EXISchema corpus = schemaCompiler.compile((InputSource)null);
    int _integerType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.INTEGER_TYPE);
    Assert.assertTrue(_integerType != EXISchema.NIL_NODE);
    Assert.assertEquals("integer", corpus.getNameOfType(_integerType));
    Assert.assertEquals(EXISchemaConst.INTEGER_TYPE, corpus.getSerialOfType(_integerType));

    int _nonNegativeIntegerType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.NON_NEGATIVE_INTEGER_TYPE);
    Assert.assertTrue(_nonNegativeIntegerType != EXISchema.NIL_NODE);
    Assert.assertEquals("nonNegativeInteger", corpus.getNameOfType(_nonNegativeIntegerType));
    Assert.assertEquals(EXISchemaConst.NON_NEGATIVE_INTEGER_TYPE, corpus.getSerialOfType(_nonNegativeIntegerType));

    int _unsignedLongType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.UNSIGNED_LONG_TYPE);
    Assert.assertTrue(_unsignedLongType != EXISchema.NIL_NODE);
    Assert.assertEquals("unsignedLong", corpus.getNameOfType(_unsignedLongType));
    Assert.assertEquals(EXISchemaConst.UNSIGNED_LONG_TYPE, corpus.getSerialOfType(_unsignedLongType));

    int _positiveIntegerType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.POSITIVE_INTEGER_TYPE);
    Assert.assertTrue(_positiveIntegerType != EXISchema.NIL_NODE);
    Assert.assertEquals("positiveInteger", corpus.getNameOfType(_positiveIntegerType));
    Assert.assertEquals(EXISchemaConst.POSITIVE_INTEGER_TYPE, corpus.getSerialOfType(_positiveIntegerType));

    int _nonPositiveIntegerType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.NON_POSITIVE_INTEGER_TYPE);
    Assert.assertTrue(_nonPositiveIntegerType != EXISchema.NIL_NODE);
    Assert.assertEquals("nonPositiveInteger", corpus.getNameOfType(_nonPositiveIntegerType));
    Assert.assertEquals(EXISchemaConst.NON_POSITIVE_INTEGER_TYPE, corpus.getSerialOfType(_nonPositiveIntegerType));

    int _negativeIntegerType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.NEGATIVE_INTEGER_TYPE);
    Assert.assertTrue(_negativeIntegerType != EXISchema.NIL_NODE);
    Assert.assertEquals("negativeInteger", corpus.getNameOfType(_negativeIntegerType));
    Assert.assertEquals(EXISchemaConst.NEGATIVE_INTEGER_TYPE, corpus.getSerialOfType(_negativeIntegerType));

    int _intType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.INT_TYPE);
    Assert.assertTrue(_intType != EXISchema.NIL_NODE);
    Assert.assertEquals("int", corpus.getNameOfType(_intType));
    Assert.assertEquals(EXISchemaConst.INT_TYPE, corpus.getSerialOfType(_intType));

    int _shortType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.SHORT_TYPE);
    Assert.assertTrue(_shortType != EXISchema.NIL_NODE);
    Assert.assertEquals("short", corpus.getNameOfType(_shortType));
    Assert.assertEquals(EXISchemaConst.SHORT_TYPE, corpus.getSerialOfType(_shortType));

    int _byteType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.BYTE_TYPE);
    Assert.assertTrue(_byteType != EXISchema.NIL_NODE);
    Assert.assertEquals("byte", corpus.getNameOfType(_byteType));
    Assert.assertEquals(EXISchemaConst.BYTE_TYPE, corpus.getSerialOfType(_byteType));

    int _unsignedShortType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.UNSIGNED_SHORT_TYPE);
    Assert.assertTrue(_unsignedShortType != EXISchema.NIL_NODE);
    Assert.assertEquals("unsignedShort", corpus.getNameOfType(_unsignedShortType));
    Assert.assertEquals(EXISchemaConst.UNSIGNED_SHORT_TYPE, corpus.getSerialOfType(_unsignedShortType));

    int _unsignedByteType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.UNSIGNED_BYTE_TYPE);
    Assert.assertTrue(_unsignedByteType != EXISchema.NIL_NODE);
    Assert.assertEquals("unsignedByte", corpus.getNameOfType(_unsignedByteType));
    Assert.assertEquals(EXISchemaConst.UNSIGNED_BYTE_TYPE, corpus.getSerialOfType(_unsignedByteType));

    int _longType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.LONG_TYPE);
    Assert.assertTrue(_longType != EXISchema.NIL_NODE);
    Assert.assertEquals("long", corpus.getNameOfType(_longType));
    Assert.assertEquals(EXISchemaConst.LONG_TYPE, corpus.getSerialOfType(_longType));

    int _unsignedIntType = corpus.getBuiltinTypeOfSchema(EXISchemaConst.UNSIGNED_INT_TYPE);
    Assert.assertTrue(_unsignedIntType != EXISchema.NIL_NODE);
    Assert.assertEquals("unsignedInt", corpus.getNameOfType(_unsignedIntType));
    Assert.assertEquals(EXISchemaConst.UNSIGNED_INT_TYPE, corpus.getSerialOfType(_unsignedIntType));
  }

  /**
   * Test "anyType"
   */
  public void testBuiltinComplexTypes() throws Exception {
    EXISchemaFactory schemaCompiler = new EXISchemaFactory();
    EXISchema corpus = schemaCompiler.compile((InputSource)null);

    int _anyType = corpus.getTypeOfSchema("http://www.w3.org/2001/XMLSchema", "anyType");
    Assert.assertTrue(_anyType != EXISchema.NIL_NODE);
    Assert.assertFalse(corpus.isSimpleType(_anyType));
    Assert.assertEquals("anyType", corpus.getNameOfType(_anyType));
    Assert.assertEquals(XmlUriConst.W3C_2001_XMLSCHEMA_URI_ID, corpus.getUriOfType(_anyType));
    Assert.assertEquals(EXISchemaConst.ANY_TYPE, corpus.getSerialOfType(_anyType));
  }
  
}
