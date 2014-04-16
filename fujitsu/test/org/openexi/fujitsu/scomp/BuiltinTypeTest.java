package org.openexi.fujitsu.scomp;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaConst;

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
    EXISchema corpus = schemaCompiler.compile();

    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

    int _anyType = corpus.getTypeOfNamespace(xsdns, "anyType");

    int _anySimpleType = corpus.getTypeOfNamespace(xsdns, "anySimpleType");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_anySimpleType));
    Assert.assertEquals("anySimpleType", corpus.getNameOfType(_anySimpleType));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_anySimpleType));
    Assert.assertEquals(_anyType, corpus.getBaseTypeOfType(_anySimpleType));
    Assert.assertTrue(corpus.isUrType(_anySimpleType));
    Assert.assertFalse(corpus.isIntegralSimpleType(_anySimpleType));
    Assert.assertEquals(EXISchema.UR_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_anySimpleType));
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_anySimpleType));
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_anySimpleType));
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getItemTypeOfListSimpleType(_anySimpleType));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_anySimpleType));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_anySimpleType));
    Assert.assertEquals(EXISchemaConst.ANY_SIMPLE_TYPE, corpus.getSerialOfType(_anySimpleType));
    Assert.assertTrue(corpus.isFixtureType(_anySimpleType));
    Assert.assertTrue(corpus.hasSubType(_anySimpleType));
    Assert.assertEquals(EXISchema.WHITESPACE_ABSENT,
        corpus.getWhitespaceFacetValueOfSimpleType(_anySimpleType));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_anySimpleType));
    
    // atomic types

    int _string = corpus.getTypeOfNamespace(xsdns, "string");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_string));
    Assert.assertEquals("string", corpus.getNameOfType(_string));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_string));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_string));
    Assert.assertFalse(corpus.isUrType(_string));
    Assert.assertFalse(corpus.isIntegralSimpleType(_string));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_string));
    Assert.assertEquals(_string,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_string));
    Assert.assertEquals(_string,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_string));
    Assert.assertEquals(EXISchema.NIL_NODE,
                      corpus.getItemTypeOfListSimpleType(_string));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_string));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_string));
    Assert.assertEquals(EXISchemaConst.STRING_TYPE, corpus.getSerialOfType(_string));
    Assert.assertTrue(corpus.isFixtureType(_string));
    Assert.assertTrue(corpus.hasSubType(_string));
    Assert.assertEquals(EXISchema.WHITESPACE_PRESERVE,
        corpus.getWhitespaceFacetValueOfSimpleType(_string));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_string));

    int _boolean = corpus.getTypeOfNamespace(xsdns, "boolean");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_boolean));
    Assert.assertEquals("boolean", corpus.getNameOfType(_boolean));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_boolean));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_boolean));
    Assert.assertFalse(corpus.isUrType(_boolean));
    Assert.assertFalse(corpus.isIntegralSimpleType(_boolean));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_boolean));
    Assert.assertEquals(_boolean,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_boolean));
    Assert.assertEquals(_boolean,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_boolean));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_boolean));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_boolean));
    Assert.assertEquals(EXISchemaConst.BOOLEAN_TYPE, corpus.getSerialOfType(_boolean));
    Assert.assertTrue(corpus.isFixtureType(_boolean));
    Assert.assertFalse(corpus.hasSubType(_boolean));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_boolean));

    int _decimal = corpus.getTypeOfNamespace(xsdns, "decimal");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_decimal));
    Assert.assertEquals("decimal", corpus.getNameOfType(_decimal));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_decimal));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_decimal));
    Assert.assertFalse(corpus.isUrType(_decimal));
    Assert.assertFalse(corpus.isIntegralSimpleType(_decimal));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_decimal));
    Assert.assertEquals(_decimal,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_decimal));
    Assert.assertEquals(_decimal,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_decimal));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_decimal));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_decimal));
    Assert.assertEquals(EXISchemaConst.DECIMAL_TYPE, corpus.getSerialOfType(_decimal));
    Assert.assertTrue(corpus.isFixtureType(_decimal));
    Assert.assertTrue(corpus.hasSubType(_decimal));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_decimal));

    int _float = corpus.getTypeOfNamespace(xsdns, "float");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_float));
    Assert.assertEquals("float", corpus.getNameOfType(_float));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_float));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_float));
    Assert.assertFalse(corpus.isUrType(_float));
    Assert.assertFalse(corpus.isIntegralSimpleType(_float));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_float));
    Assert.assertEquals(_float,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_float));
    Assert.assertEquals(_float,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_float));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_float));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_float));
    Assert.assertEquals(EXISchemaConst.FLOAT_TYPE, corpus.getSerialOfType(_float));
    Assert.assertTrue(corpus.isFixtureType(_float));
    Assert.assertFalse(corpus.hasSubType(_float));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_float));

    int _double = corpus.getTypeOfNamespace(xsdns, "double");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_double));
    Assert.assertEquals("double", corpus.getNameOfType(_double));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_double));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_double));
    Assert.assertFalse(corpus.isUrType(_double));
    Assert.assertFalse(corpus.isIntegralSimpleType(_double));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_double));
    Assert.assertEquals(_double,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_double));
    Assert.assertEquals(_double,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_double));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_double));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_double));
    Assert.assertEquals(EXISchemaConst.DOUBLE_TYPE, corpus.getSerialOfType(_double));
    Assert.assertTrue(corpus.isFixtureType(_double));
    Assert.assertFalse(corpus.hasSubType(_double));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_double));

    int _duration = corpus.getTypeOfNamespace(xsdns, "duration");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_duration));
    Assert.assertEquals("duration", corpus.getNameOfType(_duration));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_duration));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_duration));
    Assert.assertFalse(corpus.isUrType(_duration));
    Assert.assertFalse(corpus.isIntegralSimpleType(_duration));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_duration));
    Assert.assertEquals(_duration,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_duration));
    Assert.assertEquals(_duration,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_duration));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_duration));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_duration));
    Assert.assertEquals(EXISchemaConst.DURATION_TYPE, corpus.getSerialOfType(_duration));
    Assert.assertTrue(corpus.isFixtureType(_duration));
    Assert.assertFalse(corpus.hasSubType(_duration));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_duration));

    int _dateTime = corpus.getTypeOfNamespace(xsdns, "dateTime");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_dateTime));
    Assert.assertEquals("dateTime", corpus.getNameOfType(_dateTime));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_dateTime));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_dateTime));
    Assert.assertFalse(corpus.isUrType(_dateTime));
    Assert.assertFalse(corpus.isIntegralSimpleType(_dateTime));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_dateTime));
    Assert.assertEquals(_dateTime,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_dateTime));
    Assert.assertEquals(_dateTime,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_dateTime));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_dateTime));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_dateTime));
    Assert.assertEquals(EXISchemaConst.DATETIME_TYPE, corpus.getSerialOfType(_dateTime));
    Assert.assertTrue(corpus.isFixtureType(_dateTime));
    Assert.assertFalse(corpus.hasSubType(_dateTime));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_dateTime));

    int _time = corpus.getTypeOfNamespace(xsdns, "time");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_time));
    Assert.assertEquals("time", corpus.getNameOfType(_time));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_time));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_time));
    Assert.assertFalse(corpus.isUrType(_time));
    Assert.assertFalse(corpus.isIntegralSimpleType(_time));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_time));
    Assert.assertEquals(_time,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_time));
    Assert.assertEquals(_time,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_time));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_time));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_time));
    Assert.assertEquals(EXISchemaConst.TIME_TYPE, corpus.getSerialOfType(_time));
    Assert.assertTrue(corpus.isFixtureType(_time));
    Assert.assertFalse(corpus.hasSubType(_time));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_time));

    int _date = corpus.getTypeOfNamespace(xsdns, "date");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_date));
    Assert.assertEquals("date", corpus.getNameOfType(_date));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_date));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_date));
    Assert.assertFalse(corpus.isUrType(_date));
    Assert.assertFalse(corpus.isIntegralSimpleType(_date));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_date));
    Assert.assertEquals(_date,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_date));
    Assert.assertEquals(_date,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_date));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_date));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_date));
    Assert.assertEquals(EXISchemaConst.DATE_TYPE, corpus.getSerialOfType(_date));
    Assert.assertTrue(corpus.isFixtureType(_date));
    Assert.assertFalse(corpus.hasSubType(_date));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_date));

    int _gYearMonth = corpus.getTypeOfNamespace(xsdns, "gYearMonth");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_gYearMonth));
    Assert.assertEquals("gYearMonth", corpus.getNameOfType(_gYearMonth));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_gYearMonth));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_gYearMonth));
    Assert.assertFalse(corpus.isUrType(_gYearMonth));
    Assert.assertFalse(corpus.isIntegralSimpleType(_gYearMonth));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_gYearMonth));
    Assert.assertEquals(_gYearMonth,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_gYearMonth));
    Assert.assertEquals(_gYearMonth,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_gYearMonth));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_gYearMonth));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_gYearMonth));
    Assert.assertEquals(EXISchemaConst.G_YEARMONTH_TYPE, corpus.getSerialOfType(_gYearMonth));
    Assert.assertTrue(corpus.isFixtureType(_gYearMonth));
    Assert.assertFalse(corpus.hasSubType(_gYearMonth));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_gYearMonth));

    int _gYear = corpus.getTypeOfNamespace(xsdns, "gYear");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_gYear));
    Assert.assertEquals("gYear", corpus.getNameOfType(_gYear));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_gYear));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_gYear));
    Assert.assertFalse(corpus.isUrType(_gYear));
    Assert.assertFalse(corpus.isIntegralSimpleType(_gYear));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_gYear));
    Assert.assertEquals(_gYear,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_gYear));
    Assert.assertEquals(_gYear,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_gYear));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_gYear));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_gYear));
    Assert.assertEquals(EXISchemaConst.G_YEAR_TYPE, corpus.getSerialOfType(_gYear));
    Assert.assertTrue(corpus.isFixtureType(_gYear));
    Assert.assertFalse(corpus.hasSubType(_gYear));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_gYear));

    int _gMonthDay = corpus.getTypeOfNamespace(xsdns, "gMonthDay");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_gMonthDay));
    Assert.assertEquals("gMonthDay", corpus.getNameOfType(_gMonthDay));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_gMonthDay));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_gMonthDay));
    Assert.assertFalse(corpus.isUrType(_gMonthDay));
    Assert.assertFalse(corpus.isIntegralSimpleType(_gMonthDay));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_gMonthDay));
    Assert.assertEquals(_gMonthDay,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_gMonthDay));
    Assert.assertEquals(_gMonthDay,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_gMonthDay));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_gMonthDay));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_gMonthDay));
    Assert.assertEquals(EXISchemaConst.G_MONTHDAY_TYPE, corpus.getSerialOfType(_gMonthDay));
    Assert.assertTrue(corpus.isFixtureType(_gMonthDay));
    Assert.assertFalse(corpus.hasSubType(_gMonthDay));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_gMonthDay));

    int _gDay = corpus.getTypeOfNamespace(xsdns, "gDay");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_gDay));
    Assert.assertEquals("gDay", corpus.getNameOfType(_gDay));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_gDay));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_gDay));
    Assert.assertFalse(corpus.isUrType(_gDay));
    Assert.assertFalse(corpus.isIntegralSimpleType(_gDay));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_gDay));
    Assert.assertEquals(_gDay,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_gDay));
    Assert.assertEquals(_gDay,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_gDay));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_gDay));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_gDay));
    Assert.assertEquals(EXISchemaConst.G_DAY_TYPE, corpus.getSerialOfType(_gDay));
    Assert.assertTrue(corpus.isFixtureType(_gDay));
    Assert.assertFalse(corpus.hasSubType(_gDay));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_gDay));

    int _gMonth = corpus.getTypeOfNamespace(xsdns, "gMonth");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_gMonth));
    Assert.assertEquals("gMonth", corpus.getNameOfType(_gMonth));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_gMonth));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_gMonth));
    Assert.assertFalse(corpus.isUrType(_gMonth));
    Assert.assertFalse(corpus.isIntegralSimpleType(_gMonth));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_gMonth));
    Assert.assertEquals(_gMonth,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_gMonth));
    Assert.assertEquals(_gMonth,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_gMonth));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_gMonth));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_gMonth));
    Assert.assertEquals(EXISchemaConst.G_MONTH_TYPE, corpus.getSerialOfType(_gMonth));
    Assert.assertTrue(corpus.isFixtureType(_gMonth));
    Assert.assertFalse(corpus.hasSubType(_gMonth));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_gMonth));

    int _hexBinary = corpus.getTypeOfNamespace(xsdns, "hexBinary");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_hexBinary));
    Assert.assertEquals("hexBinary", corpus.getNameOfType(_hexBinary));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_hexBinary));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_hexBinary));
    Assert.assertFalse(corpus.isUrType(_hexBinary));
    Assert.assertFalse(corpus.isIntegralSimpleType(_hexBinary));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_hexBinary));
    Assert.assertEquals(_hexBinary,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_hexBinary));
    Assert.assertEquals(_hexBinary,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_hexBinary));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_hexBinary));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_hexBinary));
    Assert.assertEquals(EXISchemaConst.HEXBINARY_TYPE, corpus.getSerialOfType(_hexBinary));
    Assert.assertTrue(corpus.isFixtureType(_hexBinary));
    Assert.assertFalse(corpus.hasSubType(_hexBinary));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_hexBinary));

    int _base64Binary = corpus.getTypeOfNamespace(xsdns, "base64Binary");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_base64Binary));
    Assert.assertEquals("base64Binary", corpus.getNameOfType(_base64Binary));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_base64Binary));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_base64Binary));
    Assert.assertFalse(corpus.isUrType(_base64Binary));
    Assert.assertFalse(corpus.isIntegralSimpleType(_base64Binary));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_base64Binary));
    Assert.assertEquals(_base64Binary,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_base64Binary));
    Assert.assertEquals(_base64Binary,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_base64Binary));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_base64Binary));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_base64Binary));
    Assert.assertEquals(EXISchemaConst.BASE64BINARY_TYPE, corpus.getSerialOfType(_base64Binary));
    Assert.assertTrue(corpus.isFixtureType(_base64Binary));
    Assert.assertFalse(corpus.hasSubType(_base64Binary));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_base64Binary));

    int _anyURI = corpus.getTypeOfNamespace(xsdns, "anyURI");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_anyURI));
    Assert.assertEquals("anyURI", corpus.getNameOfType(_anyURI));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_anyURI));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_anyURI));
    Assert.assertFalse(corpus.isUrType(_anyURI));
    Assert.assertFalse(corpus.isIntegralSimpleType(_anyURI));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_anyURI));
    Assert.assertEquals(_anyURI,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_anyURI));
    Assert.assertEquals(_anyURI,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_anyURI));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_anyURI));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_anyURI));
    Assert.assertEquals(EXISchemaConst.ANYURI_TYPE, corpus.getSerialOfType(_anyURI));
    Assert.assertTrue(corpus.isFixtureType(_anyURI));
    Assert.assertFalse(corpus.hasSubType(_anyURI));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_anyURI));

    int _QName = corpus.getTypeOfNamespace(xsdns, "QName");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_QName));
    Assert.assertEquals("QName", corpus.getNameOfType(_QName));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_QName));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_QName));
    Assert.assertFalse(corpus.isUrType(_QName));
    Assert.assertFalse(corpus.isIntegralSimpleType(_QName));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_QName));
    Assert.assertEquals(_QName,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_QName));
    Assert.assertEquals(_QName,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_QName));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_QName));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_QName));
    Assert.assertEquals(EXISchemaConst.QNAME_TYPE, corpus.getSerialOfType(_QName));
    Assert.assertTrue(corpus.isFixtureType(_QName));
    Assert.assertFalse(corpus.hasSubType(_QName));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_QName));

    int _NOTATION = corpus.getTypeOfNamespace(xsdns, "NOTATION");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_NOTATION));
    Assert.assertEquals("NOTATION", corpus.getNameOfType(_NOTATION));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_NOTATION));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_NOTATION));
    Assert.assertFalse(corpus.isUrType(_NOTATION));
    Assert.assertFalse(corpus.isIntegralSimpleType(_NOTATION));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_NOTATION));
    Assert.assertEquals(_NOTATION,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_NOTATION));
    Assert.assertEquals(_NOTATION,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_NOTATION));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_NOTATION));
    Assert.assertTrue(corpus.isPrimitiveSimpleType(_NOTATION));
    Assert.assertEquals(EXISchemaConst.NOTATION_TYPE, corpus.getSerialOfType(_NOTATION));
    Assert.assertTrue(corpus.isFixtureType(_NOTATION));
    Assert.assertFalse(corpus.hasSubType(_NOTATION));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_NOTATION));

    int _normalizedString = corpus.getTypeOfNamespace(xsdns, "normalizedString");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_normalizedString));
    Assert.assertEquals("normalizedString", corpus.getNameOfType(_normalizedString));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_normalizedString));
    Assert.assertEquals(_string, corpus.getBaseTypeOfType(_normalizedString));
    Assert.assertFalse(corpus.isUrType(_normalizedString));
    Assert.assertFalse(corpus.isIntegralSimpleType(_normalizedString));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_normalizedString));
    Assert.assertEquals(_string,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_normalizedString));
    Assert.assertEquals(_normalizedString,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_normalizedString));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_normalizedString));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_normalizedString));
    Assert.assertEquals(EXISchema.WHITESPACE_REPLACE,
                      corpus.getWhitespaceFacetValueOfSimpleType(_normalizedString));
    Assert.assertTrue(corpus.isFixtureType(_normalizedString));
    Assert.assertTrue(corpus.hasSubType(_normalizedString));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_normalizedString));

    int _token = corpus.getTypeOfNamespace(xsdns, "token");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_token));
    Assert.assertEquals("token", corpus.getNameOfType(_token));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_token));
    Assert.assertEquals(_normalizedString, corpus.getBaseTypeOfType(_token));
    Assert.assertFalse(corpus.isUrType(_token));
    Assert.assertFalse(corpus.isIntegralSimpleType(_token));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_token));
    Assert.assertEquals(_string,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_token));
    Assert.assertEquals(_token,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_token));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_token));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_token));
    Assert.assertEquals(EXISchema.WHITESPACE_COLLAPSE,
                      corpus.getWhitespaceFacetValueOfSimpleType(_token));
    Assert.assertTrue(corpus.isFixtureType(_token));
    Assert.assertTrue(corpus.hasSubType(_token));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_token));

    int _language = corpus.getTypeOfNamespace(xsdns, "language");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_language));
    Assert.assertEquals("language", corpus.getNameOfType(_language));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_language));
    Assert.assertEquals(_token, corpus.getBaseTypeOfType(_language));
    Assert.assertFalse(corpus.isUrType(_language));
    Assert.assertFalse(corpus.isIntegralSimpleType(_language));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_language));
    Assert.assertEquals(_string,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_language));
    Assert.assertEquals(_language,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_language));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_language));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_language));
    Assert.assertTrue(corpus.isFixtureType(_language));
    Assert.assertFalse(corpus.hasSubType(_language));

    int _NMTOKEN = corpus.getTypeOfNamespace(xsdns, "NMTOKEN");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_NMTOKEN));
    Assert.assertEquals("NMTOKEN", corpus.getNameOfType(_NMTOKEN));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_NMTOKEN));
    Assert.assertEquals(_token, corpus.getBaseTypeOfType(_NMTOKEN));
    Assert.assertFalse(corpus.isUrType(_NMTOKEN));
    Assert.assertFalse(corpus.isIntegralSimpleType(_NMTOKEN));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_NMTOKEN));
    Assert.assertEquals(_string,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_NMTOKEN));
    Assert.assertEquals(_NMTOKEN,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_NMTOKEN));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_NMTOKEN));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_NMTOKEN));
    Assert.assertTrue(corpus.isFixtureType(_NMTOKEN));
    Assert.assertFalse(corpus.hasSubType(_NMTOKEN));
    // pattern "\\c+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_NMTOKEN));

    int _Name = corpus.getTypeOfNamespace(xsdns, "Name");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_Name));
    Assert.assertEquals("Name", corpus.getNameOfType(_Name));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_Name));
    Assert.assertEquals(_token, corpus.getBaseTypeOfType(_Name));
    Assert.assertFalse(corpus.isUrType(_Name));
    Assert.assertFalse(corpus.isIntegralSimpleType(_Name));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_Name));
    Assert.assertEquals(_string,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_Name));
    Assert.assertEquals(_Name,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_Name));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_Name));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_Name));
    Assert.assertTrue(corpus.isFixtureType(_Name));
    Assert.assertTrue(corpus.hasSubType(_Name));
    // pattern "\\i\\c*"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_Name));

    int _NCName = corpus.getTypeOfNamespace(xsdns, "NCName");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_NCName));
    Assert.assertEquals("NCName", corpus.getNameOfType(_NCName));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_NCName));
    Assert.assertEquals(_Name, corpus.getBaseTypeOfType(_NCName));
    Assert.assertFalse(corpus.isUrType(_NCName));
    Assert.assertFalse(corpus.isIntegralSimpleType(_NCName));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_NCName));
    Assert.assertEquals(_string,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_NCName));
    Assert.assertEquals(_NCName,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_NCName));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_NCName));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_NCName));
    Assert.assertTrue(corpus.isFixtureType(_NCName));
    Assert.assertTrue(corpus.hasSubType(_NCName));
    // pattern "[\\i-[:]][\\c-[:]]*"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_NCName));

    int _ID = corpus.getTypeOfNamespace(xsdns, "ID");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_ID));
    Assert.assertEquals("ID", corpus.getNameOfType(_ID));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_ID));
    Assert.assertEquals(_NCName, corpus.getBaseTypeOfType(_ID));
    Assert.assertFalse(corpus.isUrType(_ID));
    Assert.assertFalse(corpus.isIntegralSimpleType(_ID));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_ID));
    Assert.assertEquals(_string,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_ID));
    Assert.assertEquals(_ID,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_ID));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_ID));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_ID));
    Assert.assertTrue(corpus.isFixtureType(_ID));
    Assert.assertFalse(corpus.hasSubType(_ID));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_ID));

    int _IDREF = corpus.getTypeOfNamespace(xsdns, "IDREF");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_IDREF));
    Assert.assertEquals("IDREF", corpus.getNameOfType(_IDREF));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_IDREF));
    Assert.assertEquals(_NCName, corpus.getBaseTypeOfType(_IDREF));
    Assert.assertFalse(corpus.isUrType(_IDREF));
    Assert.assertFalse(corpus.isIntegralSimpleType(_IDREF));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_IDREF));
    Assert.assertEquals(_string,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_IDREF));
    Assert.assertEquals(_IDREF,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_IDREF));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_IDREF));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_IDREF));
    Assert.assertTrue(corpus.isFixtureType(_IDREF));
    Assert.assertFalse(corpus.hasSubType(_IDREF));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_IDREF));

    int _ENTITY = corpus.getTypeOfNamespace(xsdns, "ENTITY");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_ENTITY));
    Assert.assertEquals("ENTITY", corpus.getNameOfType(_ENTITY));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_ENTITY));
    Assert.assertEquals(_NCName, corpus.getBaseTypeOfType(_ENTITY));
    Assert.assertFalse(corpus.isUrType(_ENTITY));
    Assert.assertFalse(corpus.isIntegralSimpleType(_ENTITY));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_ENTITY));
    Assert.assertEquals(_string,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_ENTITY));
    Assert.assertEquals(_ENTITY,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_ENTITY));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_ENTITY));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_ENTITY));
    Assert.assertTrue(corpus.isFixtureType(_ENTITY));
    Assert.assertFalse(corpus.hasSubType(_ENTITY));
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_ENTITY));

    int _integer = corpus.getTypeOfNamespace(xsdns, "integer");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_integer));
    Assert.assertEquals("integer", corpus.getNameOfType(_integer));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_integer));
    Assert.assertEquals(_decimal, corpus.getBaseTypeOfType(_integer));
    Assert.assertFalse(corpus.isUrType(_integer));
    Assert.assertTrue(corpus.isIntegralSimpleType(_integer));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_integer));
    Assert.assertEquals(_decimal,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_integer));
    Assert.assertEquals(_integer,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_integer));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_integer));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_integer));
    Assert.assertTrue(corpus.isFixtureType(_integer));
    Assert.assertTrue(corpus.hasSubType(_integer));
    // pattern "[\\-+]?[0-9]+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_integer));

    int _nonPositiveInteger = corpus.getTypeOfNamespace(xsdns, "nonPositiveInteger");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_nonPositiveInteger));
    Assert.assertEquals("nonPositiveInteger", corpus.getNameOfType(_nonPositiveInteger));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_nonPositiveInteger));
    Assert.assertEquals(_integer, corpus.getBaseTypeOfType(_nonPositiveInteger));
    Assert.assertFalse(corpus.isUrType(_nonPositiveInteger));
    Assert.assertTrue(corpus.isIntegralSimpleType(_nonPositiveInteger));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_nonPositiveInteger));
    Assert.assertEquals(_decimal,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_nonPositiveInteger));
    Assert.assertEquals(_nonPositiveInteger,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_nonPositiveInteger));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_nonPositiveInteger));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_nonPositiveInteger));
    Assert.assertTrue(corpus.isFixtureType(_nonPositiveInteger));
    Assert.assertTrue(corpus.hasSubType(_nonPositiveInteger));
    // pattern "[\\-+]?[0-9]+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_nonPositiveInteger));

    int _negativeInteger = corpus.getTypeOfNamespace(xsdns, "negativeInteger");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_negativeInteger));
    Assert.assertEquals("negativeInteger", corpus.getNameOfType(_negativeInteger));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_negativeInteger));
    Assert.assertEquals(_nonPositiveInteger, corpus.getBaseTypeOfType(_negativeInteger));
    Assert.assertFalse(corpus.isUrType(_negativeInteger));
    Assert.assertTrue(corpus.isIntegralSimpleType(_negativeInteger));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_negativeInteger));
    Assert.assertEquals(_decimal,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_negativeInteger));
    Assert.assertEquals(_negativeInteger,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_negativeInteger));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_negativeInteger));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_negativeInteger));
    Assert.assertTrue(corpus.isFixtureType(_negativeInteger));
    Assert.assertFalse(corpus.hasSubType(_negativeInteger));
    // pattern "[\\-+]?[0-9]+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_negativeInteger));

    int _long = corpus.getTypeOfNamespace(xsdns, "long");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_long));
    Assert.assertEquals("long", corpus.getNameOfType(_long));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_long));
    Assert.assertEquals(_integer, corpus.getBaseTypeOfType(_long));
    Assert.assertFalse(corpus.isUrType(_long));
    Assert.assertTrue(corpus.isIntegralSimpleType(_long));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_long));
    Assert.assertEquals(_decimal,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_long));
    Assert.assertEquals(_long,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_long));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_long));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_long));
    Assert.assertTrue(corpus.isFixtureType(_long));
    Assert.assertTrue(corpus.hasSubType(_long));
    // pattern "[\\-+]?[0-9]+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_long));

    int _int = corpus.getTypeOfNamespace(xsdns, "int");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_int));
    Assert.assertEquals("int", corpus.getNameOfType(_int));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_int));
    Assert.assertEquals(_long, corpus.getBaseTypeOfType(_int));
    Assert.assertFalse(corpus.isUrType(_int));
    Assert.assertTrue(corpus.isIntegralSimpleType(_int));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_int));
    Assert.assertEquals(_decimal,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_int));
    Assert.assertEquals(_int,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_int));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_int));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_int));
    Assert.assertTrue(corpus.isFixtureType(_int));
    Assert.assertTrue(corpus.hasSubType(_int));
    // pattern "[\\-+]?[0-9]+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_int));

    int _short = corpus.getTypeOfNamespace(xsdns, "short");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_short));
    Assert.assertEquals("short", corpus.getNameOfType(_short));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_short));
    Assert.assertEquals(_int, corpus.getBaseTypeOfType(_short));
    Assert.assertFalse(corpus.isUrType(_short));
    Assert.assertTrue(corpus.isIntegralSimpleType(_short));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_short));
    Assert.assertEquals(_decimal,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_short));
    Assert.assertEquals(_short,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_short));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_short));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_short));
    Assert.assertTrue(corpus.isFixtureType(_short));
    Assert.assertTrue(corpus.hasSubType(_short));
    // pattern "[\\-+]?[0-9]+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_short));

    int _byte = corpus.getTypeOfNamespace(xsdns, "byte");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_byte));
    Assert.assertEquals("byte", corpus.getNameOfType(_byte));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_byte));
    Assert.assertEquals(_short, corpus.getBaseTypeOfType(_byte));
    Assert.assertFalse(corpus.isUrType(_byte));
    Assert.assertTrue(corpus.isIntegralSimpleType(_byte));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_byte));
    Assert.assertEquals(_decimal,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_byte));
    Assert.assertEquals(_byte,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_byte));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_byte));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_byte));
    Assert.assertTrue(corpus.isFixtureType(_byte));
    Assert.assertFalse(corpus.hasSubType(_byte));
    // pattern "[\\-+]?[0-9]+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_byte));

    int _nonNegativeInteger = corpus.getTypeOfNamespace(xsdns, "nonNegativeInteger");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_nonNegativeInteger));
    Assert.assertEquals("nonNegativeInteger", corpus.getNameOfType(_nonNegativeInteger));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_nonNegativeInteger));
    Assert.assertEquals(_integer, corpus.getBaseTypeOfType(_nonNegativeInteger));
    Assert.assertFalse(corpus.isUrType(_nonNegativeInteger));
    Assert.assertTrue(corpus.isIntegralSimpleType(_nonNegativeInteger));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_nonNegativeInteger));
    Assert.assertEquals(_decimal,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_nonNegativeInteger));
    Assert.assertEquals(_nonNegativeInteger,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_nonNegativeInteger));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_nonNegativeInteger));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_nonNegativeInteger));
    Assert.assertTrue(corpus.isFixtureType(_nonNegativeInteger));
    Assert.assertTrue(corpus.hasSubType(_nonNegativeInteger));
    // pattern "[\\-+]?[0-9]+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_nonNegativeInteger));

    int _unsignedLong = corpus.getTypeOfNamespace(xsdns, "unsignedLong");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_unsignedLong));
    Assert.assertEquals("unsignedLong", corpus.getNameOfType(_unsignedLong));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_unsignedLong));
    Assert.assertEquals(_nonNegativeInteger, corpus.getBaseTypeOfType(_unsignedLong));
    Assert.assertFalse(corpus.isUrType(_unsignedLong));
    Assert.assertTrue(corpus.isIntegralSimpleType(_unsignedLong));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_unsignedLong));
    Assert.assertEquals(_decimal,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_unsignedLong));
    Assert.assertEquals(_unsignedLong,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_unsignedLong));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_unsignedLong));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_unsignedLong));
    Assert.assertTrue(corpus.isFixtureType(_unsignedLong));
    Assert.assertTrue(corpus.hasSubType(_unsignedLong));
    // pattern "[\\-+]?[0-9]+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_unsignedLong));

    int _unsignedInt = corpus.getTypeOfNamespace(xsdns, "unsignedInt");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_unsignedInt));
    Assert.assertEquals("unsignedInt", corpus.getNameOfType(_unsignedInt));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_unsignedInt));
    Assert.assertEquals(_unsignedLong, corpus.getBaseTypeOfType(_unsignedInt));
    Assert.assertFalse(corpus.isUrType(_unsignedInt));
    Assert.assertTrue(corpus.isIntegralSimpleType(_unsignedInt));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_unsignedInt));
    Assert.assertEquals(_decimal,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_unsignedInt));
    Assert.assertEquals(_unsignedInt,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_unsignedInt));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_unsignedInt));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_unsignedInt));
    Assert.assertTrue(corpus.isFixtureType(_unsignedInt));
    Assert.assertTrue(corpus.hasSubType(_unsignedInt));
    // pattern "[\\-+]?[0-9]+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_unsignedInt));

    int _unsignedShort = corpus.getTypeOfNamespace(xsdns, "unsignedShort");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_unsignedShort));
    Assert.assertEquals("unsignedShort", corpus.getNameOfType(_unsignedShort));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_unsignedShort));
    Assert.assertEquals(_unsignedInt, corpus.getBaseTypeOfType(_unsignedShort));
    Assert.assertFalse(corpus.isUrType(_unsignedShort));
    Assert.assertTrue(corpus.isIntegralSimpleType(_unsignedShort));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_unsignedShort));
    Assert.assertEquals(_decimal,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_unsignedShort));
    Assert.assertEquals(_unsignedShort,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_unsignedShort));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_unsignedShort));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_unsignedShort));
    Assert.assertTrue(corpus.isFixtureType(_unsignedShort));
    Assert.assertTrue(corpus.hasSubType(_unsignedShort));
    // pattern "[\\-+]?[0-9]+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_unsignedShort));

    int _unsignedByte = corpus.getTypeOfNamespace(xsdns, "unsignedByte");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_unsignedByte));
    Assert.assertEquals("unsignedByte", corpus.getNameOfType(_unsignedByte));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_unsignedByte));
    Assert.assertEquals(_unsignedShort, corpus.getBaseTypeOfType(_unsignedByte));
    Assert.assertFalse(corpus.isUrType(_unsignedByte));
    Assert.assertTrue(corpus.isIntegralSimpleType(_unsignedByte));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_unsignedByte));
    Assert.assertEquals(_decimal,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_unsignedByte));
    Assert.assertEquals(_unsignedByte,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_unsignedByte));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_unsignedByte));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_unsignedByte));
    Assert.assertTrue(corpus.isFixtureType(_unsignedByte));
    Assert.assertFalse(corpus.hasSubType(_unsignedByte));
    // pattern "[\\-+]?[0-9]+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_unsignedByte));

    int _positiveInteger = corpus.getTypeOfNamespace(xsdns, "positiveInteger");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_positiveInteger));
    Assert.assertEquals("positiveInteger", corpus.getNameOfType(_positiveInteger));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_positiveInteger));
    Assert.assertEquals(_nonNegativeInteger, corpus.getBaseTypeOfType(_positiveInteger));
    Assert.assertFalse(corpus.isUrType(_positiveInteger));
    Assert.assertTrue(corpus.isIntegralSimpleType(_positiveInteger));
    Assert.assertEquals(EXISchema.ATOMIC_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_positiveInteger));
    Assert.assertEquals(_decimal,
                      corpus.getPrimitiveTypeOfAtomicSimpleType(_positiveInteger));
    Assert.assertEquals(_positiveInteger,
                      corpus.getBuiltinTypeOfAtomicSimpleType(_positiveInteger));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_positiveInteger));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_positiveInteger));
    Assert.assertTrue(corpus.isFixtureType(_positiveInteger));
    Assert.assertFalse(corpus.hasSubType(_positiveInteger));
    // pattern "[\\-+]?[0-9]+"
    Assert.assertEquals(0, corpus.getRestrictedCharacterCountOfSimpleType(_positiveInteger));

    // list types

    int _ENTITIES = corpus.getTypeOfNamespace(xsdns, "ENTITIES");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_ENTITIES));
    Assert.assertEquals("ENTITIES", corpus.getNameOfType(_ENTITIES));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", corpus.getTargetNamespaceNameOfType(_ENTITIES));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_ENTITIES));
    Assert.assertFalse(corpus.isUrType(_ENTITIES));
    Assert.assertFalse(corpus.isIntegralSimpleType(_ENTITIES));
    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(_ENTITIES));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getPrimitiveTypeOfAtomicSimpleType(_ENTITIES));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getBuiltinTypeOfAtomicSimpleType(_ENTITIES));
    Assert.assertEquals(_ENTITY, corpus.getItemTypeOfListSimpleType(_ENTITIES));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_ENTITIES));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_ENTITIES));
    Assert.assertTrue(corpus.isFixtureType(_ENTITIES));
    Assert.assertFalse(corpus.hasSubType(_ENTITIES));
    Assert.assertEquals(1, corpus.getMinLengthFacetValueOfSimpleType(_ENTITIES));
    
    int _IDREFS = corpus.getTypeOfNamespace(xsdns, "IDREFS");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_IDREFS));
    Assert.assertEquals("IDREFS", corpus.getNameOfType(_IDREFS));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema", corpus.getTargetNamespaceNameOfType(_IDREFS));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_IDREFS));
    Assert.assertFalse(corpus.isUrType(_IDREFS));
    Assert.assertFalse(corpus.isIntegralSimpleType(_IDREFS));
    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE, corpus.getVarietyOfSimpleType(_IDREFS));
    Assert.assertEquals(_IDREF, corpus.getItemTypeOfListSimpleType(_IDREFS));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_IDREFS));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_IDREFS));
    Assert.assertTrue(corpus.isFixtureType(_IDREFS));
    Assert.assertFalse(corpus.hasSubType(_IDREFS));
    Assert.assertEquals(1, corpus.getMinLengthFacetValueOfSimpleType(_IDREFS));
    
    int _NMTOKENS = corpus.getTypeOfNamespace(xsdns, "NMTOKENS");
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(_NMTOKENS));
    Assert.assertEquals("NMTOKENS", corpus.getNameOfType(_NMTOKENS));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_NMTOKENS));
    Assert.assertEquals(_anySimpleType, corpus.getBaseTypeOfType(_NMTOKENS));
    Assert.assertFalse(corpus.isUrType(_NMTOKENS));
    Assert.assertFalse(corpus.isIntegralSimpleType(_NMTOKENS));
    Assert.assertEquals(EXISchema.LIST_SIMPLE_TYPE,
                      corpus.getVarietyOfSimpleType(_NMTOKENS));
    Assert.assertEquals(_NMTOKEN,
                      corpus.getItemTypeOfListSimpleType(_NMTOKENS));
    Assert.assertTrue(corpus.isBuiltinSimpleType(_NMTOKENS));
    Assert.assertFalse(corpus.isPrimitiveSimpleType(_NMTOKENS));
    Assert.assertTrue(corpus.isFixtureType(_NMTOKENS));
    Assert.assertFalse(corpus.hasSubType(_NMTOKENS));
    Assert.assertEquals(1, corpus.getMinLengthFacetValueOfSimpleType(_NMTOKENS));
  }

  /**
   * Test serial numbers of primitive types.
   */
  public void testPrimitiveTypeSerialNumbers() throws Exception {
    EXISchemaFactory schemaCompiler = new EXISchemaFactory();
    EXISchema corpus = schemaCompiler.compile();
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
    EXISchema corpus = schemaCompiler.compile();
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
    EXISchema corpus = schemaCompiler.compile();    int xsdns = corpus.getNamespaceOfSchema("http://www.w3.org/2001/XMLSchema");

    int _anyType = corpus.getTypeOfNamespace(xsdns, "anyType");
    Assert.assertTrue(_anyType != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(_anyType));
    Assert.assertEquals("anyType", corpus.getNameOfType(_anyType));
    Assert.assertEquals("http://www.w3.org/2001/XMLSchema",
                      corpus.getTargetNamespaceNameOfType(_anyType));
    Assert.assertEquals(EXISchemaConst.ANY_TYPE, corpus.getSerialOfType(_anyType));
    Assert.assertEquals(_anyType, corpus.getBaseTypeOfType(_anyType));
    Assert.assertTrue(corpus.isUrType(_anyType));
    Assert.assertEquals(EXISchema.CONTENT_MIXED,
                      corpus.getContentClassOfComplexType(_anyType));
    Assert.assertEquals(0, corpus.getAttrUseCountOfComplexType(_anyType));
    Assert.assertTrue(corpus.isFixtureType(_anyType));
    Assert.assertTrue(corpus.hasSubType(_anyType));
    int attrwc = corpus.getAttrWildcardOfComplexType(_anyType);
    Assert.assertTrue(attrwc != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(attrwc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(attrwc));
    int particle = corpus.getContentTypeOfComplexType(_anyType);
    Assert.assertTrue(particle != EXISchema.NIL_NODE);
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(particle));
    Assert.assertEquals(1, corpus.getMaxOccursOfParticle(particle));
    int sequence = corpus.getTermOfParticle(particle);
    Assert.assertTrue(sequence != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.GROUP_NODE, corpus.getNodeType(sequence));
    Assert.assertEquals(1, corpus.getParticleCountOfGroup(sequence));
    int innerParticle = corpus.getParticleOfGroup(sequence, 0);
    Assert.assertTrue(innerParticle != EXISchema.NIL_NODE);
    Assert.assertEquals(0, corpus.getMinOccursOfParticle(innerParticle));
    Assert.assertEquals(EXISchema.UNBOUNDED_OCCURS, corpus.getMaxOccursOfParticle(innerParticle));
    int wc = corpus.getTermOfParticle(innerParticle);
    Assert.assertTrue(wc != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.WILDCARD_NODE, corpus.getNodeType(wc));
    Assert.assertEquals(EXISchema.WC_TYPE_ANY, corpus.getConstraintTypeOfWildcard(wc));
    Assert.assertEquals(EXISchema.WC_PROCESS_LAX, corpus.getProcessContentsOfWildcard(wc));
  }
  
}
