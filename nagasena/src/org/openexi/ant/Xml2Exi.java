package org.openexi.ant;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;

import org.xml.sax.InputSource;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.SchemaId;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.EXISchema;
import org.openexi.scomp.EXISchemaReader;

public class Xml2Exi extends Task {
  
  private String m_gramFile;
  private String m_xmlFile;
  private String m_exiFile;
  private AlignmentType m_alignment = AlignmentType.bitPacked;
  private boolean m_isStrict;
  private SchemaId m_schemaId;

  public Xml2Exi() {
	  m_isStrict = false;
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

  public void setAlignment(String alignment) {
    this.m_alignment = AlignmentType.valueOf(alignment);
  }
  
  public void setStrict(String isStrict) {
    this.m_isStrict = Boolean.valueOf(isStrict);
  }

  public void setSchemaId(String schemaId) {
    this.m_schemaId = new SchemaId(schemaId);
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
      
      InputStream inputStream;
      
      inputStream = gramUri.toURL().openStream();
      EXISchemaReader schemaReader = new EXISchemaReader();
      EXISchema exiSchema = schemaReader.parse(inputStream);
      inputStream.close();

      FileOutputStream outputStream = new FileOutputStream(outputUri.toURL().getFile());
      BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.setGrammarCache(new GrammarCache(exiSchema,
    		  m_isStrict ? GrammarOptions.STRICT_OPTIONS : GrammarOptions.DEFAULT_OPTIONS), m_schemaId);
      transmogrifier.setAlignmentType(m_alignment);
      transmogrifier.setOutputOptions(m_schemaId == null ?
    		  HeaderOptionsOutputType.lessSchemaId : HeaderOptionsOutputType.all);
      transmogrifier.setOutputStream(bufferedOutputStream);
      inputStream = xmlUri.toURL().openStream();
      BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
      transmogrifier.encode(new InputSource(bufferedInputStream));
      bufferedInputStream.close();
      inputStream.close();
      bufferedOutputStream.flush();
      bufferedOutputStream.close();
      outputStream.close();
    }
    catch (Exception e) {
      throw new BuildException(e);
    }
  }
  
}
