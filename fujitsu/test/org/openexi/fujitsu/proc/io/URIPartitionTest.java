package org.openexi.fujitsu.proc.io;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.proc.grammars.GrammarCache;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.fujitsu.scomp.EXISchemaFactoryTestUtil;

public class URIPartitionTest extends TestCase {

  public URIPartitionTest(String name) {
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
  public void testInitialState() throws Exception {
    
    StringTable stringTable;
    StringTable.URIPartition uriPartition;
    
    stringTable = new StringTable(null);
    
    uriPartition = stringTable.getURIPartition();
    
    Assert.assertEquals(2, uriPartition.width);
    Assert.assertEquals(2, uriPartition.forwardedWidth);

    Assert.assertEquals(3, uriPartition.n_strings);
    Assert.assertEquals(0, uriPartition.getCompactId(""));
    Assert.assertEquals(1, uriPartition.getCompactId("http://www.w3.org/XML/1998/namespace"));
    Assert.assertEquals(2, uriPartition.getCompactId("http://www.w3.org/2001/XMLSchema-instance"));
    Assert.assertEquals(-1, uriPartition.getCompactId("http://www.w3.org/2001/XMLSchema"));

    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    stringTable = new StringTable(grammarCache.getEXISchema());
    
    uriPartition = stringTable.getURIPartition();
    
    Assert.assertEquals(2, uriPartition.width);
    Assert.assertEquals(3, uriPartition.forwardedWidth);

    Assert.assertEquals(4, uriPartition.n_strings);
    Assert.assertEquals(0, uriPartition.getCompactId(""));
    Assert.assertEquals(1, uriPartition.getCompactId("http://www.w3.org/XML/1998/namespace"));
    Assert.assertEquals(2, uriPartition.getCompactId("http://www.w3.org/2001/XMLSchema-instance"));
    Assert.assertEquals(3, uriPartition.getCompactId("http://www.w3.org/2001/XMLSchema"));
  }

  /**
   */
  public void testInternString() throws Exception {
    
    StringTable stringTable;
    StringTable.URIPartition uriPartition;
    
    stringTable = new StringTable(null);
    
    uriPartition = stringTable.getURIPartition();
    
    Assert.assertEquals(2, uriPartition.width);
    Assert.assertEquals(2, uriPartition.forwardedWidth);
    Assert.assertEquals(3, uriPartition.n_strings);
    
    Assert.assertEquals(3, uriPartition.internString("03"));
    Assert.assertEquals(2, uriPartition.width);
    Assert.assertEquals(3, uriPartition.forwardedWidth);
    Assert.assertEquals(4, uriPartition.n_strings);
    Assert.assertEquals(4, uriPartition.internString("04"));
    Assert.assertEquals(3, uriPartition.width);
    Assert.assertEquals(3, uriPartition.forwardedWidth);
    Assert.assertEquals(5, uriPartition.n_strings);
    Assert.assertEquals(5, uriPartition.internString("05"));
    Assert.assertEquals(3, uriPartition.width);
    Assert.assertEquals(3, uriPartition.forwardedWidth);
    Assert.assertEquals(6, uriPartition.n_strings);
    Assert.assertEquals(6, uriPartition.internString("06"));
    Assert.assertEquals(3, uriPartition.width);
    Assert.assertEquals(3, uriPartition.forwardedWidth);
    Assert.assertEquals(7, uriPartition.n_strings);
    Assert.assertEquals(3, uriPartition.internString("03"));
    Assert.assertEquals(7, uriPartition.internString("07"));
    Assert.assertEquals(3, uriPartition.width);
    Assert.assertEquals(4, uriPartition.forwardedWidth);
    Assert.assertEquals(8, uriPartition.n_strings);
    Assert.assertEquals(8, uriPartition.internString("08"));
    Assert.assertEquals(4, uriPartition.width);
    Assert.assertEquals(4, uriPartition.forwardedWidth);
    Assert.assertEquals(9, uriPartition.n_strings);

    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    stringTable = new StringTable(grammarCache.getEXISchema());
    
    uriPartition = stringTable.getURIPartition();
    
    Assert.assertEquals(2, uriPartition.width);
    Assert.assertEquals(3, uriPartition.forwardedWidth);
    Assert.assertEquals(4, uriPartition.n_strings);
    
    Assert.assertEquals(4, uriPartition.internString("04"));
    Assert.assertEquals(3, uriPartition.width);
    Assert.assertEquals(3, uriPartition.forwardedWidth);
    Assert.assertEquals(5, uriPartition.n_strings);
    Assert.assertEquals(5, uriPartition.internString("05"));
    Assert.assertEquals(3, uriPartition.width);
    Assert.assertEquals(3, uriPartition.forwardedWidth);
    Assert.assertEquals(6, uriPartition.n_strings);
    Assert.assertEquals(6, uriPartition.internString("06"));
    Assert.assertEquals(3, uriPartition.width);
    Assert.assertEquals(3, uriPartition.forwardedWidth);
    Assert.assertEquals(7, uriPartition.n_strings);
    Assert.assertEquals(4, uriPartition.internString("04"));
    Assert.assertEquals(7, uriPartition.internString("07"));
    Assert.assertEquals(3, uriPartition.width);
    Assert.assertEquals(4, uriPartition.forwardedWidth);
    Assert.assertEquals(8, uriPartition.n_strings);
    Assert.assertEquals(8, uriPartition.internString("08"));
    Assert.assertEquals(4, uriPartition.width);
    Assert.assertEquals(4, uriPartition.forwardedWidth);
    Assert.assertEquals(9, uriPartition.n_strings);
  }

}
