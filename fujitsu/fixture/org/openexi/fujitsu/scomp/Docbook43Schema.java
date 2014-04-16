package org.openexi.fujitsu.scomp;


import java.net.URL;

import org.xml.sax.InputSource;

import junit.framework.Assert;

import org.openexi.fujitsu.schema.EXISchema;

/**
 */
public class Docbook43Schema {
  
  private static final EXISchema m_corpus;
  
  static {
    EXISchema corpus = null;
    try {
      EXISchemaFactoryErrorMonitor compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
  
      EXISchemaFactory schemaCompiler = new EXISchemaFactory();
      schemaCompiler.setCompilerErrorHandler(compilerErrorHandler);
  
      InputSource inputSource;
      URL url = Docbook43Schema.class.getResource("/docbook-xsd-4.3/docbook.xsd");
      inputSource = new InputSource(url.openStream());
      inputSource.setSystemId(url.toString());
      
      corpus = schemaCompiler.compile(inputSource);
      Assert.assertEquals(0, compilerErrorHandler.getTotalCount());
      Assert.assertNotNull(corpus);
    }
    catch (Exception exc) {
      Assert.fail("Failed to compile docbook 4.3 schemas");
    }
    finally {
      m_corpus = corpus;
    }
  }
  
  private Docbook43Schema() {
  }

  /**
   * Returns the schema that represents the Docbook Schema.
   */
  public static EXISchema getEXISchema() {
    return m_corpus;
  }

}