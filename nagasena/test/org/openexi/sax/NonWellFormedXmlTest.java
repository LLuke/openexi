package org.openexi.sax;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import junit.framework.Assert;

import org.xml.sax.InputSource;
import org.xml.sax.Locator;

import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.schema.TestBase;

public class NonWellFormedXmlTest extends TestBase {

  public NonWellFormedXmlTest(String name) {
    super(name);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  ///////////////////////////////////////////////////////////////////////////
  // Test cases
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Not a well-formed XML 
   */
  public void testNoEndTag() throws Exception {
    
    final String xmlString;
    xmlString = "<?xml version='1.0' ?>" +
      "<None xmlns='urn:foo'>\n" + 
      "  <A>\n" + 
      "</None>";
    
    Transmogrifier encoder = new Transmogrifier();

    encoder.setGrammarCache(new GrammarCache(GrammarOptions.DEFAULT_OPTIONS));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    encoder.setOutputStream(baos);
    
    try {
      encoder.encode(new InputSource(new StringReader(xmlString)));
    }
    catch (TransmogrifierException te) {
      Locator locator = te.getLocator();
      Assert.assertEquals(3, locator.getLineNumber());
      return;
    }
    Assert.fail();
  }
  
}
