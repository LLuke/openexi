package org.openexi.scomp;

import org.apache.xerces.xni.parser.XMLParseException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.schema.EXISchema;

/**
 */
public class EXISchemaFactorySubstGroupTest extends TestCase {

  public EXISchemaFactorySubstGroupTest(String name) {
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
   * Element substitution relationship that involves elements imported by
   * hub schema.
   */
  public void testSubstitutionHub() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/substHub.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    Assert.assertEquals(7, corpus.uris.length);
    Assert.assertEquals("urn:foo", corpus.uris[4]); 
    Assert.assertEquals("urn:goo", corpus.uris[5]); 
    Assert.assertEquals("urn:hoo", corpus.uris[6]); 
    
    int myroot = corpus.getGlobalElemOfSchema("urn:foo", "myroot");
    Assert.assertTrue(myroot != EXISchema.NIL_NODE);
    
    int part = corpus.getGlobalElemOfSchema("urn:goo", "part");
    Assert.assertTrue(part != EXISchema.NIL_NODE);

    int name = corpus.getGlobalElemOfSchema("urn:hoo", "name");
    Assert.assertTrue(name != EXISchema.NIL_NODE);
  }

  /**
   * As of 2003-09-11 version of xmlschema.jar, element substitution
   * relationship that involves elements where schemas are imported
   * directly is recognized correctly.
   */
  public void testSubstitutionDirect() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/substElems.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    Assert.assertEquals(6, corpus.uris.length);
    Assert.assertEquals("urn:goo", corpus.uris[4]); 
    Assert.assertEquals("urn:hoo", corpus.uris[5]); 
    
    int part = corpus.getGlobalElemOfSchema("urn:goo", "part");
    Assert.assertTrue(part != EXISchema.NIL_NODE);

    int name = corpus.getGlobalElemOfSchema("urn:hoo", "name");
    Assert.assertTrue(name != EXISchema.NIL_NODE);
  }

  /**
   * An element declaration with no type association thus fallback to
   * anyType. Its substitution group does not resolve, which somehow
   * makes its type null. There is a workaround in SchemaCompiler to
   * use anyType whenever it finds the type being null.
   */
  public void testAnyTypeElemSubstUnresolvable() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/anyTypeElemSubstUnresolvable.xsd", getClass(), m_compilerErrorHandler);

    // xerces (as of 2.11.0) somehow reports two errors for this.
    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());
    EXISchemaFactoryException sce;
    XMLParseException se;
    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    se = (XMLParseException)sce.getException();
    Assert.assertEquals(4, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("src-resolve.4.2:"));
    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];
    se = (XMLParseException)sce.getException();
    Assert.assertEquals(4, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("src-resolve:"));
    
    Assert.assertEquals("urn:foo", corpus.uris[4]);
    
    int elemA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertTrue(elemA != EXISchema.NIL_NODE);
    Assert.assertEquals(4, corpus.getUriOfElem(elemA));
    Assert.assertEquals("A", corpus.getNameOfElem(elemA));

    int elemB = corpus.getGlobalElemOfSchema("urn:foo", "B");
    Assert.assertTrue(elemB != EXISchema.NIL_NODE);
    Assert.assertEquals(4, corpus.getUriOfElem(elemB));
    Assert.assertEquals("B", corpus.getNameOfElem(elemB));
  }

  /**
   * It is perfectly fine to place substitutionGroup and block
   * attributes on the same element.
   * 
   * For example,
   * substitutionGroup="goo:part" block="#all"
   * or
   * substitutionGroup="goo:part" block="substitution"
   * 
   * In fact, they do not interact each other when they co-occur
   * on the same element.
   */
  public void testDisallowedSubstitutionCoOccur() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema(
        "/substElems01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryTestUtil.getEXISchema(
        "/substElems02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
  }

  /**
   * It is not permitted to let an element designate another element
   * that has an attribute block="#all" as its substitution head.
   */
  public void testDisallowedSubstitutionNG01() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema(
        "/substElemsNG01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
  }

  /**
   * It is not permitted to let an element designate another element
   * that has an attribute block="substitution" as its substitution head.
   */
  public void testDisallowedSubstitutionNG02() throws Exception {

    EXISchemaFactoryTestUtil.getEXISchema(
        "/substElemsNG02.xsd", getClass(), m_compilerErrorHandler);

    // xerces:
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
  }
  
  /**
   * C is the head of a group with members D and E.
   * (B|(D|E))* is a valid restriction of the base (B|C)*.
   */
  public void testResrictionBySubstitutionOK_01() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/restrictionBySubstOK01.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int _base = corpus.getTypeOfSchema("urn:foo", "base");
    Assert.assertTrue(_base != EXISchema.NIL_NODE);

    int _derived = corpus.getTypeOfSchema("urn:foo", "derived");
    Assert.assertTrue(_derived != EXISchema.NIL_NODE);
    //Assert.assertEquals(_base, corpus.getBaseTypeOfSimpleType(_derived));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertTrue(eA != EXISchema.NIL_NODE);

    int eB = corpus.getGlobalElemOfSchema("urn:foo", "B");
    Assert.assertTrue(eB != EXISchema.NIL_NODE);

    int eC = corpus.getGlobalElemOfSchema("urn:foo", "C");
    Assert.assertTrue(eC != EXISchema.NIL_NODE);

    int eD = corpus.getGlobalElemOfSchema("urn:foo", "D");
    Assert.assertTrue(eD != EXISchema.NIL_NODE);

    int eE = corpus.getGlobalElemOfSchema("urn:foo", "E");
    Assert.assertTrue(eE != EXISchema.NIL_NODE);

    int eF = corpus.getGlobalElemOfSchema("urn:foo", "F");
    Assert.assertTrue(eF != EXISchema.NIL_NODE);
  }
  
  /**
   * C is the head of a group with members D.
   * (B|(D|E))* is not a valid restriction of the base (B|C)*.
   */
  public void testResrictionBySubstitutionNG_01() throws Exception {

    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema(
          "/restrictionBySubstNG01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(13, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("rcase-RecurseLax.2:"));

    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];
    se = (XMLParseException)sce.getException();
    Assert.assertEquals(13, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));

    int _base = corpus.getTypeOfSchema("urn:foo", "base");
    Assert.assertTrue(_base != EXISchema.NIL_NODE);

    int _derived = corpus.getTypeOfSchema("urn:foo", "derived");
    Assert.assertTrue(_derived != EXISchema.NIL_NODE);
    //Assert.assertEquals(_base, corpus.getBaseTypeOfSimpleType(_derived));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertTrue(eA != EXISchema.NIL_NODE);

    int eB = corpus.getGlobalElemOfSchema("urn:foo", "B");
    Assert.assertTrue(eB != EXISchema.NIL_NODE);

    int eC = corpus.getGlobalElemOfSchema("urn:foo", "C");
    Assert.assertTrue(eC != EXISchema.NIL_NODE);

    int eD = corpus.getGlobalElemOfSchema("urn:foo", "D");
    Assert.assertTrue(eD != EXISchema.NIL_NODE);

    int eE = corpus.getGlobalElemOfSchema("urn:foo", "E");
    Assert.assertTrue(eE != EXISchema.NIL_NODE);

    int eF = corpus.getGlobalElemOfSchema("urn:foo", "F");
    Assert.assertTrue(eF != EXISchema.NIL_NODE);
  }

  /**
   * C is the head of a group with members D and E.
   * ((D|E)|B)* is a valid restriction of the base (C|B)*.
   */
  public void testResrictionBySubstitutionOK_02() throws Exception {
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/restrictionBySubstOK02.xsd", getClass(), m_compilerErrorHandler);
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int _base = corpus.getTypeOfSchema("urn:foo", "base");
    Assert.assertTrue(_base != EXISchema.NIL_NODE);

    int _derived = corpus.getTypeOfSchema("urn:foo", "derived");
    Assert.assertTrue(_derived != EXISchema.NIL_NODE);
    //Assert.assertEquals(_base, corpus.getBaseTypeOfSimpleType(_derived));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertTrue(eA != EXISchema.NIL_NODE);

    int eB = corpus.getGlobalElemOfSchema("urn:foo", "B");
    Assert.assertTrue(eB != EXISchema.NIL_NODE);

    int eC = corpus.getGlobalElemOfSchema("urn:foo", "C");
    Assert.assertTrue(eC != EXISchema.NIL_NODE);

    int eD = corpus.getGlobalElemOfSchema("urn:foo", "D");
    Assert.assertTrue(eD != EXISchema.NIL_NODE);

    int eE = corpus.getGlobalElemOfSchema("urn:foo", "E");
    Assert.assertTrue(eE != EXISchema.NIL_NODE);

    int eF = corpus.getGlobalElemOfSchema("urn:foo", "F");
    Assert.assertTrue(eF != EXISchema.NIL_NODE);
  }
  
  /**
   * C is the head of a group with members D.
   * ((D|E)|B)* is not a valid restriction of the base (C|B)*.
   */
  public void testResrictionBySubstitutionNG_02() throws Exception {

    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema(
          "/restrictionBySubstNG02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(2, m_compilerErrorHandler.getTotalCount());

    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(13, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("rcase-RecurseLax.2:"));

    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[1];
    se = (XMLParseException)sce.getException();
    Assert.assertEquals(13, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));

    int _base = corpus.getTypeOfSchema("urn:foo", "base");
    Assert.assertTrue(_base != EXISchema.NIL_NODE);

    int _derived = corpus.getTypeOfSchema("urn:foo", "derived");
    Assert.assertTrue(_derived != EXISchema.NIL_NODE);
    //Assert.assertEquals(_base, corpus.getBaseTypeOfSimpleType(_derived));
    
    int eA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertTrue(eA != EXISchema.NIL_NODE);

    int eB = corpus.getGlobalElemOfSchema("urn:foo", "B");
    Assert.assertTrue(eB != EXISchema.NIL_NODE);

    int eC = corpus.getGlobalElemOfSchema("urn:foo", "C");
    Assert.assertTrue(eC != EXISchema.NIL_NODE);

    int eD = corpus.getGlobalElemOfSchema("urn:foo", "D");
    Assert.assertTrue(eD != EXISchema.NIL_NODE);

    int eE = corpus.getGlobalElemOfSchema("urn:foo", "E");
    Assert.assertTrue(eE != EXISchema.NIL_NODE);

    int eF = corpus.getGlobalElemOfSchema("urn:foo", "F");
    Assert.assertTrue(eF != EXISchema.NIL_NODE);
  }

  /**
   * SchemaCompiler does not put an element in its own substitution group
   * because it is self-evident.
   * 
   * Given an element definition:
   * <xsd:element name="A" type="xsd:string" substitutionGroup="foo:A" />
   * 
   * The element A will end up having no substitutable elements.
   */
  public void testSelfSubstitution_01() throws Exception {

    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/substElemsSelf01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(6, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("e-props-correct.6:"));

    Assert.assertEquals("urn:foo", corpus.uris[4]);

    int fooA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertTrue(fooA != EXISchema.NIL_NODE);
    Assert.assertEquals(4, corpus.getUriOfElem(fooA));
    Assert.assertEquals("A", corpus.getNameOfElem(fooA));
    
    int fooZ = corpus.getGlobalElemOfSchema("urn:foo", "Z");
    Assert.assertTrue(fooZ != EXISchema.NIL_NODE);
    Assert.assertEquals(4, corpus.getUriOfElem(fooZ));
    Assert.assertEquals("Z", corpus.getNameOfElem(fooZ));
  }
  
  /**
   * SchemaCompiler does not put an element in its own substitution group
   * because it is self-evident.
   * 
   * Given element definitions:
   * <xsd:element name="A" type="xsd:string" substitutionGroup="foo:B" />
   * <xsd:element name="B" type="xsd:string" substitutionGroup="foo:A" />
   * 
   * The element A and B will end up having B and A in their respective
   * substitutable element set.
   */
  public void testMutualSubstitution_01() throws Exception {
      
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/substElemsMutual01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
    
    EXISchemaFactoryException sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.XMLSCHEMA_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("e-props-correct.6:"));

    Assert.assertEquals("urn:foo", corpus.uris[4]);

    int fooA = corpus.getGlobalElemOfSchema("urn:foo", "A");
    Assert.assertTrue(fooA != EXISchema.NIL_NODE);
    Assert.assertEquals(4, corpus.getUriOfElem(fooA));
    Assert.assertEquals("A", corpus.getNameOfElem(fooA));

    int fooB = corpus.getGlobalElemOfSchema("urn:foo", "B");
    Assert.assertTrue(fooB != EXISchema.NIL_NODE);
    Assert.assertEquals(4, corpus.getUriOfElem(fooB));
    Assert.assertEquals("B", corpus.getNameOfElem(fooB));
    
    int fooZ = corpus.getGlobalElemOfSchema("urn:foo", "Z");
    Assert.assertTrue(fooZ != EXISchema.NIL_NODE);
    Assert.assertEquals(4, corpus.getUriOfElem(fooZ));
    Assert.assertEquals("Z", corpus.getNameOfElem(fooZ));
  }
  
}
