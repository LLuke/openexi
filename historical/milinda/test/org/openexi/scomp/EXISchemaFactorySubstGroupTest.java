package org.openexi.scomp;

import org.apache.xerces.xni.parser.XMLParseException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;

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
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/substHub.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertTrue(foons != EXISchema.NIL_NODE);

    int goons = corpus.getNamespaceOfSchema("urn:goo");
    Assert.assertTrue(goons != EXISchema.NIL_NODE);

    int part = corpus.getElemOfNamespace(goons, "part");
    Assert.assertTrue(part != EXISchema.NIL_NODE);

    int hoons = corpus.getNamespaceOfSchema("urn:hoo");
    Assert.assertTrue(hoons != EXISchema.NIL_NODE);

    int name = corpus.getElemOfNamespace(hoons, "name");
    Assert.assertTrue(name != EXISchema.NIL_NODE);
    Assert.assertEquals(part, corpus.getSubstOfElem(name));
  }

  /**
   * As of 2003-09-11 version of xmlschema.jar, element substitution
   * relationship that involves elements where schemas are imported
   * directly is recognized correctly.
   */
  public void testSubstitutionDirect() throws Exception {
    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/substElems.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int goons = corpus.getNamespaceOfSchema("urn:goo");
    Assert.assertTrue(goons != EXISchema.NIL_NODE);

    int part = corpus.getElemOfNamespace(goons, "part");
    Assert.assertTrue(part != EXISchema.NIL_NODE);

    int hoons = corpus.getNamespaceOfSchema("urn:hoo");
    Assert.assertTrue(hoons != EXISchema.NIL_NODE);

    int name = corpus.getElemOfNamespace(hoons, "name");
    Assert.assertTrue(name != EXISchema.NIL_NODE);
    Assert.assertEquals(part, corpus.getSubstOfElem(name));
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
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    se = (XMLParseException)sce.getException();
    Assert.assertEquals(4, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("src-resolve.4.2:"));
    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];
    se = (XMLParseException)sce.getException();
    Assert.assertEquals(4, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("src-resolve:"));
    
    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertTrue(foons != EXISchema.NIL_NODE);

    int elemA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertTrue(elemA != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elemA));
    int typeA = corpus.getTypeOfElem(elemA);
    Assert.assertTrue(typeA != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.COMPLEX_TYPE_NODE, corpus.getNodeType(typeA));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getSubstOfElem(elemA));

    int elemB = corpus.getElemOfNamespace(foons, "B");
    Assert.assertTrue(elemB != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(elemB));
    int typeB = corpus.getTypeOfElem(elemB);
    Assert.assertTrue(typeB != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.SIMPLE_TYPE_NODE, corpus.getNodeType(typeB));
    Assert.assertEquals(EXISchemaConst.INT_TYPE, corpus.getSerialOfType(typeB));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getSubstOfElem(elemB));
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

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/substElems01.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    int goons, hoons;
    int partElem, nameElem;

    goons = corpus.getNamespaceOfSchema("urn:goo");
    partElem = corpus.getElemOfNamespace(goons, "part");

    hoons = corpus.getNamespaceOfSchema("urn:hoo");
    nameElem = corpus.getElemOfNamespace(hoons, "name");
    
    Assert.assertEquals(partElem, corpus.getSubstOfElem(nameElem));
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/substElems02.xsd", getClass(), m_compilerErrorHandler);

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
    
    goons = corpus.getNamespaceOfSchema("urn:goo");
    partElem = corpus.getElemOfNamespace(goons, "part");

    hoons = corpus.getNamespaceOfSchema("urn:hoo");
    nameElem = corpus.getElemOfNamespace(hoons, "name");
  }

  /**
   * It is not permitted to let an element designate another element
   * that has an attribute block="#all" as its substitution head.
   */
  public void testDisallowedSubstitutionNG01() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/substElemsNG01.xsd", getClass(), m_compilerErrorHandler);

    // xerces:
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
//    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
//    EXISchemaFactoryException[] saxErrors = m_compilerErrorHandler.getErrors(
//        EXISchemaFactoryException.SAXPARSE_ERROR);
//    Assert.assertEquals(1, saxErrors.length);
//
//    SAXException se = (SAXException)saxErrors[0].getException();
//    FjParseException fjpe = (FjParseException)se.getException();
//    Assert.assertEquals(
//        XmlExceptionCode.IDS_SCHEMA_PARSE_ERROR_ELEMDECL_SUBST_QNAME_ILLEGAL, fjpe.getCode());
//    Assert.assertTrue(fjpe.getSystemId().endsWith("substElemsNG01.xsd"));
//    Assert.assertEquals(8, fjpe.getLineNumber());
    
    int goons, hoons;
    int partElem, nameElem;

    goons = corpus.getNamespaceOfSchema("urn:goo");
    partElem = corpus.getElemOfNamespace(goons, "part");

    hoons = corpus.getNamespaceOfSchema("urn:hoo");
    nameElem = corpus.getElemOfNamespace(hoons, "name");
    
    Assert.assertEquals(partElem, corpus.getSubstOfElem(nameElem));
  }

  /**
   * It is not permitted to let an element designate another element
   * that has an attribute block="substitution" as its substitution head.
   */
  public void testDisallowedSubstitutionNG02() throws Exception {

    EXISchema corpus;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema(
        "/substElemsNG02.xsd", getClass(), m_compilerErrorHandler);

    // xerces:
    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());
//    Assert.assertEquals(1, m_compilerErrorHandler.getTotalCount());
//    EXISchemaFactoryException[] saxErrors = m_compilerErrorHandler.getErrors(
//        EXISchemaFactoryException.SAXPARSE_ERROR);
//    Assert.assertEquals(1, saxErrors.length);
//
//    SAXException se = (SAXException)saxErrors[0].getException();
//    FjParseException fjpe = (FjParseException)se.getException();
//    Assert.assertEquals(
//        XmlExceptionCode.IDS_SCHEMA_PARSE_ERROR_ELEMDECL_SUBST_QNAME_ILLEGAL, fjpe.getCode());
//    Assert.assertTrue(fjpe.getSystemId().endsWith("substElemsNG02.xsd"));
//    Assert.assertEquals(8, fjpe.getLineNumber());
    
    int goons, hoons;
    int partElem, nameElem;

    goons = corpus.getNamespaceOfSchema("urn:goo");
    partElem = corpus.getElemOfNamespace(goons, "part");

    hoons = corpus.getNamespaceOfSchema("urn:hoo");
    nameElem = corpus.getElemOfNamespace(hoons, "name");
    
    Assert.assertEquals(partElem, corpus.getSubstOfElem(nameElem));
  }
  
  /**
   * C is the head of a group with members D and E.
   * (B|(D|E))* is a valid restriction of the base (B|C)*.
   */
  public void testResrictionBySubstitutionOK_01() throws Exception {

    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/restrictionBySubstOK01.xsd", getClass());

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertTrue(foons != EXISchema.NIL_NODE);

    int _base = corpus.getTypeOfNamespace(foons, "base");
    Assert.assertTrue(_base != EXISchema.NIL_NODE);

    int _derived = corpus.getTypeOfNamespace(foons, "derived");
    Assert.assertTrue(_derived != EXISchema.NIL_NODE);
    //Assert.assertEquals(_base, corpus.getBaseTypeOfSimpleType(_derived));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertTrue(eA != EXISchema.NIL_NODE);
    Assert.assertEquals(_derived, corpus.getTypeOfElem(eA));

    int eB = corpus.getElemOfNamespace(foons, "B");
    Assert.assertTrue(eB != EXISchema.NIL_NODE);

    int eC = corpus.getElemOfNamespace(foons, "C");
    Assert.assertTrue(eC != EXISchema.NIL_NODE);

    int eD = corpus.getElemOfNamespace(foons, "D");
    Assert.assertTrue(eD != EXISchema.NIL_NODE);
    Assert.assertEquals(eC, corpus.getSubstOfElem(eD));

    int eE = corpus.getElemOfNamespace(foons, "E");
    Assert.assertTrue(eE != EXISchema.NIL_NODE);
    Assert.assertEquals(eC, corpus.getSubstOfElem(eE));

    int eF = corpus.getElemOfNamespace(foons, "F");
    Assert.assertTrue(eF != EXISchema.NIL_NODE);
    Assert.assertEquals(eC, corpus.getSubstOfElem(eF));

    int choice1 = corpus.getGroupOfElem(eA);
    Assert.assertTrue(choice1 != EXISchema.NIL_NODE);
    
    int contentType = corpus.getContentTypeOfComplexType(_derived);
    Assert.assertEquals(choice1, corpus.getTermOfParticle(contentType));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(contentType));
    Assert.assertEquals(EXISchema.UNBOUNDED_OCCURS, corpus.getMaxOccursOfParticle(contentType));
    
    int particle1, particle2, particle3, particle4, choice2;
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(choice1));
    particle1 = corpus.getParticleOfGroup(choice1, 0);
    Assert.assertEquals(eB, corpus.getTermOfParticle(particle1));
    particle2 = corpus.getParticleOfGroup(choice1, 1);
    choice2 = corpus.getTermOfParticle(particle2);
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(choice2));
    particle3 = corpus.getParticleOfGroup(choice2, 0);
    Assert.assertEquals(eD, corpus.getTermOfParticle(particle3));
    particle4 = corpus.getParticleOfGroup(choice2, 1);
    Assert.assertEquals(eE, corpus.getTermOfParticle(particle4));
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
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(13, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("rcase-RecurseLax.2:"));

    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];
    se = (XMLParseException)sce.getException();
    Assert.assertEquals(13, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertTrue(foons != EXISchema.NIL_NODE);

    int _base = corpus.getTypeOfNamespace(foons, "base");
    Assert.assertTrue(_base != EXISchema.NIL_NODE);

    int _derived = corpus.getTypeOfNamespace(foons, "derived");
    Assert.assertTrue(_derived != EXISchema.NIL_NODE);
    //Assert.assertEquals(_base, corpus.getBaseTypeOfSimpleType(_derived));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertTrue(eA != EXISchema.NIL_NODE);
    Assert.assertEquals(_derived, corpus.getTypeOfElem(eA));

    int eB = corpus.getElemOfNamespace(foons, "B");
    Assert.assertTrue(eB != EXISchema.NIL_NODE);

    int eC = corpus.getElemOfNamespace(foons, "C");
    Assert.assertTrue(eC != EXISchema.NIL_NODE);

    int eD = corpus.getElemOfNamespace(foons, "D");
    Assert.assertTrue(eD != EXISchema.NIL_NODE);
    Assert.assertEquals(eC, corpus.getSubstOfElem(eD));

    int eE = corpus.getElemOfNamespace(foons, "E");
    Assert.assertTrue(eE != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getSubstOfElem(eE));

    int eF = corpus.getElemOfNamespace(foons, "F");
    Assert.assertTrue(eF != EXISchema.NIL_NODE);
    Assert.assertEquals(eC, corpus.getSubstOfElem(eF));

    int choice1 = corpus.getGroupOfElem(eA);
    Assert.assertTrue(choice1 != EXISchema.NIL_NODE);
    
    int contentType = corpus.getContentTypeOfComplexType(_derived);
    Assert.assertEquals(choice1, corpus.getTermOfParticle(contentType));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(contentType));
    Assert.assertEquals(EXISchema.UNBOUNDED_OCCURS, corpus.getMaxOccursOfParticle(contentType));
    
    int particle1, particle2, particle3, particle4, choice2;
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(choice1));
    particle1 = corpus.getParticleOfGroup(choice1, 0);
    Assert.assertEquals(eB, corpus.getTermOfParticle(particle1));
    particle2 = corpus.getParticleOfGroup(choice1, 1);
    choice2 = corpus.getTermOfParticle(particle2);
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(choice2));
    particle3 = corpus.getParticleOfGroup(choice2, 0);
    Assert.assertEquals(eD, corpus.getTermOfParticle(particle3));
    particle4 = corpus.getParticleOfGroup(choice2, 1);
    Assert.assertEquals(eE, corpus.getTermOfParticle(particle4));
  }

  /**
   * C is the head of a group with members D and E.
   * ((D|E)|B)* is a valid restriction of the base (C|B)*.
   */
  public void testResrictionBySubstitutionOK_02() throws Exception {

    EXISchema corpus =
      EXISchemaFactoryTestUtil.getEXISchema("/restrictionBySubstOK02.xsd", getClass());

    Assert.assertEquals(0, m_compilerErrorHandler.getTotalCount());

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertTrue(foons != EXISchema.NIL_NODE);

    int _base = corpus.getTypeOfNamespace(foons, "base");
    Assert.assertTrue(_base != EXISchema.NIL_NODE);

    int _derived = corpus.getTypeOfNamespace(foons, "derived");
    Assert.assertTrue(_derived != EXISchema.NIL_NODE);
    //Assert.assertEquals(_base, corpus.getBaseTypeOfSimpleType(_derived));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertTrue(eA != EXISchema.NIL_NODE);
    Assert.assertEquals(_derived, corpus.getTypeOfElem(eA));

    int eB = corpus.getElemOfNamespace(foons, "B");
    Assert.assertTrue(eB != EXISchema.NIL_NODE);

    int eC = corpus.getElemOfNamespace(foons, "C");
    Assert.assertTrue(eC != EXISchema.NIL_NODE);

    int eD = corpus.getElemOfNamespace(foons, "D");
    Assert.assertTrue(eD != EXISchema.NIL_NODE);
    Assert.assertEquals(eC, corpus.getSubstOfElem(eD));

    int eE = corpus.getElemOfNamespace(foons, "E");
    Assert.assertTrue(eE != EXISchema.NIL_NODE);
    Assert.assertEquals(eC, corpus.getSubstOfElem(eE));

    int eF = corpus.getElemOfNamespace(foons, "F");
    Assert.assertTrue(eF != EXISchema.NIL_NODE);
    Assert.assertEquals(eC, corpus.getSubstOfElem(eF));

    int choice1 = corpus.getGroupOfElem(eA);
    Assert.assertTrue(choice1 != EXISchema.NIL_NODE);
    
    int contentType = corpus.getContentTypeOfComplexType(_derived);
    Assert.assertEquals(choice1, corpus.getTermOfParticle(contentType));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(contentType));
    Assert.assertEquals(EXISchema.UNBOUNDED_OCCURS, corpus.getMaxOccursOfParticle(contentType));
    
    int particle1, particle2, particle3, particle4, choice2;
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(choice1));
    particle1 = corpus.getParticleOfGroup(choice1, 0);
    choice2 = corpus.getTermOfParticle(particle1);
    particle2 = corpus.getParticleOfGroup(choice2, 0);
    Assert.assertEquals(eD, corpus.getTermOfParticle(particle2));
    particle3 = corpus.getParticleOfGroup(choice2, 1);
    Assert.assertEquals(eE, corpus.getTermOfParticle(particle3));
    particle4 = corpus.getParticleOfGroup(choice1, 1);
    Assert.assertEquals(eB, corpus.getTermOfParticle(particle4));
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
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(13, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("rcase-RecurseLax.2:"));

    sce = m_compilerErrorHandler.getErrors(
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[1];
    se = (XMLParseException)sce.getException();
    Assert.assertEquals(13, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("derivation-ok-restriction.5.4.2:"));

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertTrue(foons != EXISchema.NIL_NODE);

    int _base = corpus.getTypeOfNamespace(foons, "base");
    Assert.assertTrue(_base != EXISchema.NIL_NODE);

    int _derived = corpus.getTypeOfNamespace(foons, "derived");
    Assert.assertTrue(_derived != EXISchema.NIL_NODE);
    //Assert.assertEquals(_base, corpus.getBaseTypeOfSimpleType(_derived));
    
    int eA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertTrue(eA != EXISchema.NIL_NODE);
    Assert.assertEquals(_derived, corpus.getTypeOfElem(eA));

    int eB = corpus.getElemOfNamespace(foons, "B");
    Assert.assertTrue(eB != EXISchema.NIL_NODE);

    int eC = corpus.getElemOfNamespace(foons, "C");
    Assert.assertTrue(eC != EXISchema.NIL_NODE);

    int eD = corpus.getElemOfNamespace(foons, "D");
    Assert.assertTrue(eD != EXISchema.NIL_NODE);
    Assert.assertEquals(eC, corpus.getSubstOfElem(eD));

    int eE = corpus.getElemOfNamespace(foons, "E");
    Assert.assertTrue(eE != EXISchema.NIL_NODE);
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getSubstOfElem(eE));

    int eF = corpus.getElemOfNamespace(foons, "F");
    Assert.assertTrue(eF != EXISchema.NIL_NODE);
    Assert.assertEquals(eC, corpus.getSubstOfElem(eF));

    int choice1 = corpus.getGroupOfElem(eA);
    Assert.assertTrue(choice1 != EXISchema.NIL_NODE);
    
    int contentType = corpus.getContentTypeOfComplexType(_derived);
    Assert.assertEquals(choice1, corpus.getTermOfParticle(contentType));
    Assert.assertEquals(1, corpus.getMinOccursOfParticle(contentType));
    Assert.assertEquals(EXISchema.UNBOUNDED_OCCURS, corpus.getMaxOccursOfParticle(contentType));

    int particle1, particle2, particle3, particle4, choice2;
    Assert.assertEquals(2, corpus.getParticleCountOfGroup(choice1));
    particle1 = corpus.getParticleOfGroup(choice1, 0);
    choice2 = corpus.getTermOfParticle(particle1);
    particle2 = corpus.getParticleOfGroup(choice2, 0);
    Assert.assertEquals(eD, corpus.getTermOfParticle(particle2));
    particle3 = corpus.getParticleOfGroup(choice2, 1);
    Assert.assertEquals(eE, corpus.getTermOfParticle(particle3));
    particle4 = corpus.getParticleOfGroup(choice1, 1);
    Assert.assertEquals(eB, corpus.getTermOfParticle(particle4));
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
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(6, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("e-props-correct.6:"));

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int fooA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(fooA));
    
    Assert.assertEquals(0, corpus.getSubstitutableCountOfElem(fooA));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getSubstOfElem(fooA));
    
    Assert.assertTrue(corpus.isSubstitutableElemByAnother(fooA, fooA));
    
    int fooZ = corpus.getElemOfNamespace(foons, "Z");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(fooZ));
    
    Assert.assertFalse(corpus.isSubstitutableElemByAnother(fooA, fooZ));
    Assert.assertFalse(corpus.isSubstitutableElemByAnother(fooZ, fooA));
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
        EXISchemaFactoryException.SCHEMAPARSE_ERROR)[0];
    XMLParseException se = (XMLParseException)sce.getException();
    Assert.assertEquals(8, se.getLineNumber());
    Assert.assertTrue(se.getMessage().startsWith("e-props-correct.6:"));

    int foons = corpus.getNamespaceOfSchema("urn:foo");
    Assert.assertEquals(EXISchema.NAMESPACE_NODE, corpus.getNodeType(foons));
    
    int fooA = corpus.getElemOfNamespace(foons, "A");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(fooA));

    int fooB = corpus.getElemOfNamespace(foons, "B");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(fooB));
    
    Assert.assertEquals(0, corpus.getSubstitutableCountOfElem(fooA));
    Assert.assertEquals(EXISchema.NIL_NODE, corpus.getSubstOfElem(fooB));
    Assert.assertFalse(corpus.isSubstitutableElemByAnother(fooA, fooB));

    Assert.assertEquals(1, corpus.getSubstitutableCountOfElem(fooB));
    Assert.assertEquals(fooA, corpus.getSubstitutableOfElem(fooB, 0));
    Assert.assertEquals(fooB, corpus.getSubstOfElem(fooA));
    Assert.assertTrue(corpus.isSubstitutableElemByAnother(fooB, fooA));
    
    int fooZ = corpus.getElemOfNamespace(foons, "Z");
    Assert.assertEquals(EXISchema.ELEMENT_NODE, corpus.getNodeType(fooZ));
    
    Assert.assertFalse(corpus.isSubstitutableElemByAnother(fooA, fooZ));
    Assert.assertFalse(corpus.isSubstitutableElemByAnother(fooZ, fooA));

    Assert.assertFalse(corpus.isSubstitutableElemByAnother(fooB, fooZ));
    Assert.assertFalse(corpus.isSubstitutableElemByAnother(fooZ, fooB));
  }
  
}
