package org.openexi.scomp;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import junit.framework.Assert;

import org.openexi.schema.EXISchema;
import org.xml.sax.InputSource;

public final class CompileSchemas {

  private static final String[] SCHEMA_INSTANCES = { 
    "HeaderOptions.xsd", 
    "EmptySchema.txt", 
    "schema-for-json.xsd",
    "Grammar.xsd" };
  
  private static final String[] COMPILED_SCHEMAS = { 
    "HeaderOptions.xsc",
    "EmptySchema.xsc", 
    "schema-for-json.xsc",
    "Grammar.xsc" };

  public static void main(String args[]) throws IOException {
    
    for (int i = 0; i < SCHEMA_INSTANCES.length; i++) {
      final EXISchema corpus;
      final String schemaInstance = SCHEMA_INSTANCES[i];
      try {
        EXISchemaFactoryErrorMonitor compilerErrorHandler = new EXISchemaFactoryErrorMonitor();
        
        EXISchemaFactory schemaFactory = new EXISchemaFactory();
        schemaFactory.setCompilerErrorHandler(compilerErrorHandler);
  
        final InputSource inputSource;
        if (schemaInstance.endsWith(".xsd")) {
          URL url = CompileSchemas.class.getResource(schemaInstance);
          inputSource = new InputSource(url.openStream());
          inputSource.setSystemId(url.toString());
        }
        else
          inputSource = null;
        
        corpus = schemaFactory.compile(inputSource);
        Assert.assertEquals(0, compilerErrorHandler.getTotalCount());
        Assert.assertNotNull(corpus);
      }
      catch (Exception exc) {
        //exc.printStackTrace();
        Assert.fail("Failed to compile " + schemaInstance);
        return;
      }
      
      URL schemaURI = CompileSchemas.class.getResource(schemaInstance);
      
      FileOutputStream fos = null;
      DataOutputStream dos = null;

      URL url = new URL(schemaURI, COMPILED_SCHEMAS[i]);

      try {
        fos = new FileOutputStream(url.getFile());
        dos = new DataOutputStream(fos);
        corpus.writeOut(dos);
        dos.flush();
        fos.flush();
      }
      finally {
        if (dos != null) dos.close();
        if (fos != null) fos.close();
      }
    }
  }

}
