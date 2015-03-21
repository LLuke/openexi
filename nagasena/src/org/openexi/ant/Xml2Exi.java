package org.openexi.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;

import org.xml.sax.InputSource;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.EXISchema;
import org.openexi.scomp.EXISchemaReader;

public class Xml2Exi extends Task {
  
  private String m_gramFile;
  private String m_xmlFile;
  private String m_exiFile;

  public Xml2Exi() {
  }

  public void setGram(String gramFile) {
    this.m_gramFile = gramFile;
  }

  public void setXml(String xmlFile) {
    this.m_xmlFile = xmlFile;
  }

  public void setExi(String exiFile) {
    this.m_exiFile = exiFile;
  }

  /**
   * Convert a XML into EXI.
   */
  public void execute() throws BuildException {
    URI baseUri = new File(getOwningTarget().getLocation().getFileName()).toURI();
    try {
      URI gramUri = Utils.resolveURI(m_gramFile, baseUri);
      URI xmlUri = Utils.resolveURI(m_xmlFile, baseUri);
      URI outputUri = Utils.resolveURI(m_exiFile, baseUri);
      
      InputStream inputStream = gramUri.toURL().openStream();
      EXISchemaReader schemaReader = new EXISchemaReader();
      EXISchema exiSchema = schemaReader.parse(inputStream);
      inputStream.close();

      FileOutputStream outputStream = new FileOutputStream(outputUri.toURL().getFile());
      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.setGrammarCache(new GrammarCache(exiSchema));
      transmogrifier.setOutputOptions(HeaderOptionsOutputType.all);
      transmogrifier.setOutputStream(outputStream);
      inputStream = xmlUri.toURL().openStream();
      transmogrifier.encode(new InputSource(inputStream));
      inputStream.close();
      outputStream.close();
    }
    catch (Exception e) {
      throw new BuildException(e);
    }
  }
  
}
