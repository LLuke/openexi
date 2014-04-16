package org.openexi.sax;

import javax.xml.parsers.SAXParserFactory;

import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.schema.EmptySchema;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TransmogrifierTest extends TestCase {
  
  public TransmogrifierTest(String name) {
    super(name);
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * SAXParser factory for use with Transmogrifier needs to be aware of namespaces.
   */
  public void testSAXParserFactoryMisConfigured_01() throws Exception {

    SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

    saxParserFactory.setNamespaceAware(true);
    new Transmogrifier(saxParserFactory);
    
    saxParserFactory.setNamespaceAware(false);
    try {
      new Transmogrifier(saxParserFactory);
    }
    catch (TransmogrifierRuntimeException te) {
      Assert.assertEquals(TransmogrifierRuntimeException.SAXPARSER_FACTORY_NOT_NAMESPACE_AWARE, te.getCode());
      return;
    }
    Assert.fail();
  }
  
  /**
   * Tests accessors to the GrammarCache.
   */
  public void testGrammarCacheAccessor_01() throws Exception {

    GrammarCache grammarCache = new GrammarCache(EmptySchema.getEXISchema(), GrammarOptions.STRICT_OPTIONS);
    
    Transmogrifier encoder = new Transmogrifier();
    
    encoder.setGrammarCache(grammarCache);
    Assert.assertSame(grammarCache, encoder.getGrammarCache());
  }
  
}
