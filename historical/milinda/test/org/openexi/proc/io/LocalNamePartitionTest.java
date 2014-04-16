package org.openexi.proc.io;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.proc.common.GrammarOptions;
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
    
    stringTable = new StringTable(null);
    
    localNamePartition = stringTable.getLocalNamePartition(XmlUriConst.W3C_XML_1998_URI);
    
    Assert.assertEquals(2, localNamePartition.width);

    Assert.assertEquals(4, localNamePartition.n_strings);
    Assert.assertEquals(0, localNamePartition.getCompactId("base"));
    Assert.assertEquals(1, localNamePartition.getCompactId("id"));
    Assert.assertEquals(2, localNamePartition.getCompactId("lang"));
    Assert.assertEquals(3, localNamePartition.getCompactId("space"));
    
    localNamePartition.addString("a");
    Assert.assertEquals(5, localNamePartition.n_strings);
    Assert.assertEquals(3, localNamePartition.width);
    localNamePartition.addString("b");
    Assert.assertEquals(6, localNamePartition.n_strings);
    Assert.assertEquals(3, localNamePartition.width);
    localNamePartition.addString("c");
    Assert.assertEquals(7, localNamePartition.n_strings);
    Assert.assertEquals(3, localNamePartition.width);
    localNamePartition.addString("d");
    Assert.assertEquals(8, localNamePartition.n_strings);
    Assert.assertEquals(3, localNamePartition.width);
    localNamePartition.addString("e");
    Assert.assertEquals(9, localNamePartition.n_strings);
    Assert.assertEquals(4, localNamePartition.width);
    
    localNamePartition.clear();
    
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
    
    stringTable = new StringTable(null);
    
    localNamePartition = stringTable.getLocalNamePartition(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI);
    
    Assert.assertEquals(1, localNamePartition.width);

    Assert.assertEquals(2, localNamePartition.n_strings);
    Assert.assertEquals(0, localNamePartition.getCompactId("nil"));
    Assert.assertEquals(1, localNamePartition.getCompactId("type"));
    
    localNamePartition.addString("a");
    Assert.assertEquals(3, localNamePartition.n_strings);
    Assert.assertEquals(2, localNamePartition.width);
    localNamePartition.addString("b");
    Assert.assertEquals(4, localNamePartition.n_strings);
    Assert.assertEquals(2, localNamePartition.width);
    localNamePartition.addString("c");
    Assert.assertEquals(5, localNamePartition.n_strings);
    Assert.assertEquals(3, localNamePartition.width);
    
    localNamePartition.clear();
    
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
    
    stringTable = new StringTable(null);
    
    localNamePartition = stringTable.getLocalNamePartition(XmlUriConst.W3C_2001_XMLSCHEMA_URI);

    Assert.assertEquals(0, localNamePartition.width);
    Assert.assertEquals(0, localNamePartition.n_strings);

    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    stringTable = new StringTable(grammarCache.getEXISchema());
    
    localNamePartition = stringTable.getLocalNamePartition(XmlUriConst.W3C_2001_XMLSCHEMA_URI);
    
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
      localNamePartition.addString(new String(new char[] { (char)c }));
    }
    Assert.assertEquals(64, localNamePartition.n_strings);
    Assert.assertEquals(6, localNamePartition.width);
    localNamePartition.addString("z");
    Assert.assertEquals(65, localNamePartition.n_strings);
    Assert.assertEquals(7, localNamePartition.width);
    
    localNamePartition.clear();

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
