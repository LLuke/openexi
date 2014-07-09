package org.openexi.tryout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import javax.xml.parsers.SAXParserFactory;

import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.SchemaId;
import org.openexi.sax.Transmogrifier;
import org.openexi.sax.TransmogrifierException;
import org.openexi.schema.EXISchema;
import org.openexi.scomp.GrammarCache4Grammar;
import org.xml.sax.InputSource;

class XmlPageDocument extends PageDocument {

  private final ITryoutMainFrame m_mainFrame;
  
  private URI m_fileURI;
  private byte[] m_exiStream;
  
  private final SAXParserFactory m_saxParserFactory;
  
  public XmlPageDocument(IPage xmlPage, ITryoutMainFrame mainFrame) {
    m_mainFrame = mainFrame;
    m_fileURI = null;
    m_exiStream = null;
    m_saxParserFactory = SAXParserFactory.newInstance();
    m_saxParserFactory.setNamespaceAware(true);
  }
  
  public URI getFileURI() {
    return m_fileURI;
  }
  
  public byte[] getBytes() throws IOException, EXIOptionsException, TransmogrifierException {
    EXISchema schema = m_mainFrame.getEXISchema();
    assert schema != null;
    
    GrammarQuestionPageDocument.GrammarFormat grammarFormat = m_mainFrame.getGrammarFormat();    
    ByteArrayOutputStream outputStream;
    outputStream = new ByteArrayOutputStream();
    schema.writeXml(outputStream, false);
    byte[] grammarXml = outputStream.toByteArray();
    outputStream.close();
    if (grammarFormat == GrammarQuestionPageDocument.GrammarFormat.xml) {
      return grammarXml;
    }
    outputStream = new ByteArrayOutputStream();
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(GrammarCache4Grammar.getGrammarCache(), new SchemaId("nagasena:grammar"));
    transmogrifier.setOutputOptions(HeaderOptionsOutputType.all);
    transmogrifier.setOutputStream(outputStream);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(grammarXml);
    transmogrifier.encode(new InputSource(inputStream));
    inputStream.close();
    byte[] grammarBytes = outputStream.toByteArray();
    outputStream.close();
    return grammarBytes;
  }

  public void updateValidatorMainFrame() {
  }
  
  public void setEXIStream(byte[] exiStream) {
    m_exiStream = exiStream;
    m_mainFrame.setSaveAsEnabled(m_exiStream != null);
  }
  
  /////////////////////////////////////////////////////////////////////////
  // PageDocument implementation
  /////////////////////////////////////////////////////////////////////////

  @Override
  public void moveToNext() {
  }

  @Override
  public void updateMainFrame() {
    m_mainFrame.setSaveAsEnabled(m_exiStream != null);
  }

  @Override
  public void updatePage() {
  }

}
