package org.openexi.ant;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;

import org.xml.sax.InputSource;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.SchemaId;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.EXISchema;
import org.openexi.scomp.EXISchemaFactory;
import org.openexi.scomp.EXISchemaFactoryErrorHandler;
import org.openexi.scomp.EXISchemaFactoryException;
import org.openexi.scomp.GrammarCache4Grammar;

public class Xsd2Gram extends Task {

  private final EXISchemaFactory m_schemaFactory;
  
  private String m_xsdFile;
  private String m_gramFile;

  public Xsd2Gram() {
    m_schemaFactory = new EXISchemaFactory();
  }
  
  public void setXsd(String xsdFile) {
    this.m_xsdFile = xsdFile;
  }

  public void setGram(String gramFile) {
    this.m_gramFile = gramFile;
  }

  /**
   * Convert a XSD into EXI Grammar.
   */
  public void execute() throws BuildException {
    URI baseUri = new File(getOwningTarget().getLocation().getFileName()).toURI();
    try {
      URI xsdUri = Utils.resolveURI(m_xsdFile, baseUri);
      URI outputUri = Utils.resolveURI(m_gramFile, baseUri);
      
      InputSource inputSource = new InputSource(xsdUri.toString());
      InputStream inputStream = xsdUri.toURL().openStream();
      inputSource.setByteStream(inputStream);
  
      m_schemaFactory.setCompilerErrorHandler(new EXISchemaFactoryErrorHandler() {
        public void warning(EXISchemaFactoryException exc) throws EXISchemaFactoryException {
          System.err.println("Warning: " + exc.getMessage());
        }
        public void error(EXISchemaFactoryException exc) throws EXISchemaFactoryException {
          System.err.println("Error: " + exc.getMessage());
        }
        public void fatalError(EXISchemaFactoryException exc) throws EXISchemaFactoryException {
          throw exc;
        }
      });
      EXISchema exiSchema;
      try {
        exiSchema = m_schemaFactory.compile(inputSource);
      }
      catch (EXISchemaFactoryException sce) {
        throw new BuildException(sce);
      }
      finally {
        inputStream.close();
      }
  
      ByteArrayOutputStream memoryStream = new ByteArrayOutputStream();
      exiSchema.writeXml(memoryStream, false);
      final byte[] grammarXml = memoryStream.toByteArray();
      memoryStream.close();
  
      FileOutputStream outputStream = new FileOutputStream(outputUri.toURL().getFile());
      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.setGrammarCache(GrammarCache4Grammar.getGrammarCache(), new SchemaId("nagasena:grammar"));
      transmogrifier.setOutputOptions(HeaderOptionsOutputType.all);
      transmogrifier.setOutputStream(outputStream);
      inputStream = new ByteArrayInputStream(grammarXml);
      transmogrifier.encode(new InputSource(inputStream));
      inputStream.close();
      outputStream.close();
    }
    catch (Exception e) {
      throw new BuildException(e);
    }
  }

}
