package org.openexi.proc.io;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.schema.EXISchema;
import org.openexi.scomp.EXISchemaFactoryErrorMonitor;
import org.openexi.scomp.EXISchemaFactoryTestUtil;

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
    
    stringTable = Scriber.createStringTable(null);
    
    Assert.assertEquals(2, stringTable.uriWidth);
    Assert.assertEquals(2, stringTable.uriForwardedWidth);

    Assert.assertEquals(3, stringTable.n_uris);
    Assert.assertEquals(0, stringTable.getCompactIdOfURI(""));
    Assert.assertEquals(1, stringTable.getCompactIdOfURI("http://www.w3.org/XML/1998/namespace"));
    Assert.assertEquals(2, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema-instance"));
    Assert.assertEquals(-1, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema"));

    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    stringTable = Scriber.createStringTable(grammarCache); 
    
    Assert.assertEquals(2, stringTable.uriWidth);
    Assert.assertEquals(3, stringTable.uriForwardedWidth);

    Assert.assertEquals(4, stringTable.n_uris);
    Assert.assertEquals(0, stringTable.getCompactIdOfURI(""));
    Assert.assertEquals(1, stringTable.getCompactIdOfURI("http://www.w3.org/XML/1998/namespace"));
    Assert.assertEquals(2, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema-instance"));
    Assert.assertEquals(3, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema"));
  }

  /**
   */
  public void testInit() throws Exception {
    StringTable stringTable;
    
    EXISchema corpus;
    GrammarCache grammarCache;
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema(getClass(), m_compilerErrors);
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    stringTable = Scriber.createStringTable(grammarCache); 
    
    Assert.assertEquals(2, stringTable.uriWidth);
    Assert.assertEquals(3, stringTable.uriForwardedWidth);

    Assert.assertEquals(4, stringTable.n_uris);
    Assert.assertEquals(0, stringTable.getCompactIdOfURI(""));
    Assert.assertEquals(1, stringTable.getCompactIdOfURI("http://www.w3.org/XML/1998/namespace"));
    Assert.assertEquals(2, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema-instance"));
    Assert.assertEquals(3, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema"));
    
    stringTable.reset();
    
    Assert.assertEquals(2, stringTable.uriWidth);
    Assert.assertEquals(3, stringTable.uriForwardedWidth);

    Assert.assertEquals(4, stringTable.n_uris);
    Assert.assertEquals(0, stringTable.getCompactIdOfURI(""));
    Assert.assertEquals(1, stringTable.getCompactIdOfURI("http://www.w3.org/XML/1998/namespace"));
    Assert.assertEquals(2, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema-instance"));
    Assert.assertEquals(3, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema"));
    
    corpus = EXISchemaFactoryTestUtil.getEXISchema("/exi/LocationSightings/LocationSightings.xsd", getClass(), m_compilerErrors);
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    stringTable = Scriber.createStringTable(grammarCache); 

    Assert.assertEquals(3, stringTable.uriWidth);
    Assert.assertEquals(3, stringTable.uriForwardedWidth);

    Assert.assertEquals(5, stringTable.n_uris);
    Assert.assertEquals(0, stringTable.getCompactIdOfURI(""));
    Assert.assertEquals(1, stringTable.getCompactIdOfURI("http://www.w3.org/XML/1998/namespace"));
    Assert.assertEquals(2, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema-instance"));
    Assert.assertEquals(3, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema"));
    Assert.assertEquals(4, stringTable.getCompactIdOfURI("http://berjon.com/ns/dahut-sighting"));

    stringTable.reset();

    Assert.assertEquals(3, stringTable.uriWidth);
    Assert.assertEquals(3, stringTable.uriForwardedWidth);

    Assert.assertEquals(5, stringTable.n_uris);
    Assert.assertEquals(0, stringTable.getCompactIdOfURI(""));
    Assert.assertEquals(1, stringTable.getCompactIdOfURI("http://www.w3.org/XML/1998/namespace"));
    Assert.assertEquals(2, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema-instance"));
    Assert.assertEquals(3, stringTable.getCompactIdOfURI("http://www.w3.org/2001/XMLSchema"));
    Assert.assertEquals(4, stringTable.getCompactIdOfURI("http://berjon.com/ns/dahut-sighting"));
  }
  
  /**
   */
  public void testInternString() throws Exception {
    StringTable stringTable;
    
    stringTable = Scriber.createStringTable(null); 
    
    Assert.assertEquals(2, stringTable.uriWidth);
    Assert.assertEquals(2, stringTable.uriForwardedWidth);
    Assert.assertEquals(3, stringTable.n_uris);
    
    Assert.assertEquals(3, stringTable.internURI("03"));
    Assert.assertEquals(2, stringTable.uriWidth);
    Assert.assertEquals(3, stringTable.uriForwardedWidth);
    Assert.assertEquals(4, stringTable.n_uris);
    Assert.assertEquals(4, stringTable.internURI("04"));
    Assert.assertEquals(3, stringTable.uriWidth);
    Assert.assertEquals(3, stringTable.uriForwardedWidth);
    Assert.assertEquals(5, stringTable.n_uris);
    Assert.assertEquals(5, stringTable.internURI("05"));
    Assert.assertEquals(3, stringTable.uriWidth);
    Assert.assertEquals(3, stringTable.uriForwardedWidth);
    Assert.assertEquals(6, stringTable.n_uris);
    Assert.assertEquals(6, stringTable.internURI("06"));
    Assert.assertEquals(3, stringTable.uriWidth);
    Assert.assertEquals(3, stringTable.uriForwardedWidth);
    Assert.assertEquals(7, stringTable.n_uris);
    Assert.assertEquals(3, stringTable.internURI("03"));
    Assert.assertEquals(7, stringTable.internURI("07"));
    Assert.assertEquals(3, stringTable.uriWidth);
    Assert.assertEquals(4, stringTable.uriForwardedWidth);
    Assert.assertEquals(8, stringTable.n_uris);
    Assert.assertEquals(8, stringTable.internURI("08"));
    Assert.assertEquals(4, stringTable.uriWidth);
    Assert.assertEquals(4, stringTable.uriForwardedWidth);
    Assert.assertEquals(9, stringTable.n_uris);

    
    EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema(
        getClass(), m_compilerErrors);
    
    Assert.assertEquals(0, m_compilerErrors.getTotalCount());

    GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);
    
    stringTable = Scriber.createStringTable(grammarCache); 
    
    Assert.assertEquals(2, stringTable.uriWidth);
    Assert.assertEquals(3, stringTable.uriForwardedWidth);
    Assert.assertEquals(4, stringTable.n_uris);
    
    Assert.assertEquals(4, stringTable.internURI("04"));
    Assert.assertEquals(3, stringTable.uriWidth);
    Assert.assertEquals(3, stringTable.uriForwardedWidth);
    Assert.assertEquals(5, stringTable.n_uris);
    Assert.assertEquals(5, stringTable.internURI("05"));
    Assert.assertEquals(3, stringTable.uriWidth);
    Assert.assertEquals(3, stringTable.uriForwardedWidth);
    Assert.assertEquals(6, stringTable.n_uris);
    Assert.assertEquals(6, stringTable.internURI("06"));
    Assert.assertEquals(3, stringTable.uriWidth);
    Assert.assertEquals(3, stringTable.uriForwardedWidth);
    Assert.assertEquals(7, stringTable.n_uris);
    Assert.assertEquals(4, stringTable.internURI("04"));
    Assert.assertEquals(7, stringTable.internURI("07"));
    Assert.assertEquals(3, stringTable.uriWidth);
    Assert.assertEquals(4, stringTable.uriForwardedWidth);
    Assert.assertEquals(8, stringTable.n_uris);
    Assert.assertEquals(8, stringTable.internURI("08"));
    Assert.assertEquals(4, stringTable.uriWidth);
    Assert.assertEquals(4, stringTable.uriForwardedWidth);
    Assert.assertEquals(9, stringTable.n_uris);
  }

}
