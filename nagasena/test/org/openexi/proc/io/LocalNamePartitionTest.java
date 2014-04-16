package org.openexi.proc.io;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.IGrammar;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.schema.EXISchema;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;

public class LocalNamePartitionTest extends TestCase {
  
  public LocalNamePartitionTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m_compilerErrors = new EXISchemaFactoryErrorMonitor();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    m_compilerErrors.clear();
  }

  private EXISchemaFactoryErrorMonitor m_compilerErrors;

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   */
  public void testXmlNames() throws Exception {
    
    StringTable stringTable;
    StringTable.LocalNamePartition localNamePartition;
    
    stringTable = Scriber.createStringTable(null); 
    
    int uriId = stringTable.getCompactIdOfURI(XmlUriConst.W3C_XML_1998_URI);
    localNamePartition = stringTable.getLocalNamePartition(uriId);
    
    Assert.assertEquals(2, localNamePartition.width);

    Assert.assertEquals(4, localNamePartition.n_strings);
    Assert.assertEquals(0, localNamePartition.getCompactId("base"));
    Assert.assertEquals(1, localNamePartition.getCompactId("id"));
    Assert.assertEquals(2, localNamePartition.getCompactId("lang"));
    Assert.assertEquals(3, localNamePartition.getCompactId("space"));
    
    localNamePartition.addName("a", (IGrammar)null);
    Assert.assertEquals(5, localNamePartition.n_strings);
    Assert.assertEquals(3, localNamePartition.width);
    localNamePartition.addName("b", (IGrammar)null);
    Assert.assertEquals(6, localNamePartition.n_strings);
    Assert.assertEquals(3, localNamePartition.width);
    localNamePartition.addName("c", (IGrammar)null);
    Assert.assertEquals(7, localNamePartition.n_strings);
    Assert.assertEquals(3, localNamePartition.width);
    localNamePartition.addName("d", (IGrammar)null);
    Assert.assertEquals(8, localNamePartition.n_strings);
    Assert.assertEquals(3, localNamePartition.width);
    localNamePartition.addName("e", (IGrammar)null);
    Assert.assertEquals(9, localNamePartition.n_strings);
    Assert.assertEquals(4, localNamePartition.width);
    
    localNamePartition.reset();
    
    Assert.assertEquals(2, localNamePartition.width);

    Assert.assertEquals(4, localNamePartition.n_strings);
    Assert.assertEquals(0, localNamePartition.getCompactId("base"));
    Assert.assertEquals(1, localNamePartition.getCompactId("id"));
    Assert.assertEquals(2, localNamePartition.getCompactId("lang"));
    Assert.assertEquals(3, localNamePartition.getCompactId("space"));
  }
  
  /**
   */
  public void testXsiNames() throws Exception {
    
    StringTable stringTable;
    StringTable.LocalNamePartition localNamePartition;
    
    stringTable = Scriber.createStringTable(null); 
    
    int uriId = stringTable.getCompactIdOfURI(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI);
    localNamePartition = stringTable.getLocalNamePartition(uriId);
    
    Assert.assertEquals(1, localNamePartition.width);

    Assert.assertEquals(2, localNamePartition.n_strings);
    Assert.assertEquals(0, localNamePartition.getCompactId("nil"));
    Assert.assertEquals(1, localNamePartition.getCompactId("type"));
    
    localNamePartition.addName("a", (IGrammar)null);
    Assert.assertEquals(3, localNamePartition.n_strings);
    Assert.assertEquals(2, localNamePartition.width);
    localNamePartition.addName("b", (IGrammar)null);
    Assert.assertEquals(4, localNamePartition.n_strings);
    Assert.assertEquals(2, localNamePartition.width);
    localNamePartition.addName("c", (IGrammar)null);
    Assert.assertEquals(5, localNamePartition.n_strings);
    Assert.assertEquals(3, localNamePartition.width);
    
    localNamePartition.reset();
    
    Assert.assertEquals(1, localNamePartition.width);

    Assert.assertEquals(2, localNamePartition.n_strings);
    Assert.assertEquals(0, localNamePartition.getCompactId("nil"));
    Assert.assertEquals(1, localNamePartition.getCompactId("type"));
  }
  
  /**
   */
  public void testXsdNames() throws Exception {
    
    StringTable stringTable;
    StringTable.LocalNamePartition localNamePartition;
    
    stringTable = Scriber.createStringTable(null);
    
    int uriId;
    
    uriId = stringTable.getCompactIdOfURI(XmlUriConst.W3C_2001_XMLSCHEMA_URI);
    Assert.assertEquals(-1, uriId);
    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    stringTable = Scriber.createStringTable(grammarCache); 
    
    uriId = stringTable.getCompactIdOfURI(XmlUriConst.W3C_2001_XMLSCHEMA_URI);
    localNamePartition = stringTable.getLocalNamePartition(uriId);
    
    Assert.assertEquals(6, localNamePartition.width);

    Assert.assertEquals(46, localNamePartition.n_strings);
    Assert.assertEquals(0, localNamePartition.getCompactId("ENTITIES"));
    Assert.assertEquals(10, localNamePartition.getCompactId("QName"));
    Assert.assertEquals(20, localNamePartition.getCompactId("double"));
    Assert.assertEquals(30, localNamePartition.getCompactId("integer"));
    Assert.assertEquals(40, localNamePartition.getCompactId("time"));
    Assert.assertEquals(45, localNamePartition.getCompactId("unsignedShort"));
    
    for (int i = 46; i < 64; i++) {
      int c = 'a' + (64 - i);
      localNamePartition.addName(new String(new char[] { (char)c }), (IGrammar)null);
    }
    Assert.assertEquals(64, localNamePartition.n_strings);
    Assert.assertEquals(6, localNamePartition.width);
    localNamePartition.addName("z", (IGrammar)null);
    Assert.assertEquals(65, localNamePartition.n_strings);
    Assert.assertEquals(7, localNamePartition.width);
    
    localNamePartition.reset();

    Assert.assertEquals(6, localNamePartition.width);

    Assert.assertEquals(46, localNamePartition.n_strings);
    Assert.assertEquals(0, localNamePartition.getCompactId("ENTITIES"));
    Assert.assertEquals(10, localNamePartition.getCompactId("QName"));
    Assert.assertEquals(20, localNamePartition.getCompactId("double"));
    Assert.assertEquals(30, localNamePartition.getCompactId("integer"));
    Assert.assertEquals(40, localNamePartition.getCompactId("time"));
    Assert.assertEquals(45, localNamePartition.getCompactId("unsignedShort"));
  }


}
