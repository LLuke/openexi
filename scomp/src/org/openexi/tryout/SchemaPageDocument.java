package org.openexi.tryout;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.swing.SwingUtilities;

import org.openexi.schema.EXISchema;
import org.openexi.schema.EmptySchema;
import org.openexi.util.MessageResolver;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

class SchemaPageDocument extends PageDocument {

  private static final String XMLSCHEMA_URI = "http://www.w3.org/2001/XMLSchema";

  private final ITryoutMainFrame m_mainFrame;
  private final ISchemaPage m_schemaPage;
  private final MessageResolver     m_messages;
  
  private XMLFileType m_fileType;
  private URI m_fileURI;
  
  private EXISchema m_schema;

  public SchemaPageDocument(ISchemaPage schemaPage, ITryoutMainFrame mainFrame) {
    m_schemaPage = schemaPage;
    m_mainFrame = mainFrame;
    m_messages = m_mainFrame.getMessageResolver();
    m_fileType = XMLFileType.none;
    m_fileURI = null;
    m_schema = EmptySchema.getEXISchema();
  }
  
  public URI getFileURI() {
    return m_fileURI;
  }
  
  public EXISchema getSchema() {
    return m_schema;
  }

  /////////////////////////////////////////////////////////////////////////
  // PageDocument implementation
  /////////////////////////////////////////////////////////////////////////

  @Override
  public void moveToNext() {
    assert m_schema != null;
    m_schemaPage.getMainFrame().moveToNextPage(ITryoutMainFrame.PAGE_NAME_GRAMMAR_QUESTION);
  }

  @Override
  public void updateMainFrame() {
    updateRightArrowEnabled();
    m_mainFrame.setSaveAsEnabled(false);
  }

  @Override
  public void updatePage() {
  }

  private void validateFile() {
    SwingUtilities.invokeLater(
        new EXISchemaFactoryThread(m_schemaPage,
          new IValidationContext() {
            public String getSystemId() {
              return getFileURI().toString();
            }
            public void setDone(EXISchema schema, boolean hasFatalError, int n_errors) {
              if (!hasFatalError) {
                assert schema != null;
                setEXISchema(schema);
                if (n_errors != 0) {
                  m_schemaPage.showMessage("It is recommended schema error(s) be fixed before you move to the next step.");
                }
                else {
                  m_schemaPage.showMessage("Move to the next page to save the grammar.");
                }
              }
            }
          }
      ));
  }
  
  public void discardEXISchema() {
    setEXISchema(EmptySchema.getEXISchema());
    setFileType(XMLFileType.none);
    setUri(null);
    m_schemaPage.resetPage();
  }
  
  private void setEXISchema(EXISchema schema) {
    assert schema != null;
    m_schema = schema;
    if (m_schemaPage == m_mainFrame.getPage())
      updateRightArrowEnabled();
  }
  
  private void setUri(URI uri) {
    m_fileURI = uri;
    m_schemaPage.setFileName(m_fileURI != null ? m_fileURI.toString() : "");
  }
  
  private void updateRightArrowEnabled() {
    m_mainFrame.setRightArrowEnabled(m_schema != EmptySchema.getEXISchema());
  }
  
  public void probeFile(URI uri, XMLReader xmlReader) {
    setUri(uri);
    URL url = null;
    XMLFileType fileType = XMLFileType.none;
    XMLSniffer.Result xmlInfo = null;
    try {
      url = uri.toURL();
      xmlInfo = XMLSniffer.probeXml(url.openStream(), xmlReader);
    }
    catch (IOException ioe) {
      fileType = XMLFileType.other;
      m_schemaPage.setStatusText(
          m_messages.getMessage(SchemaCompilerXMsg.MF_STATUS_IO_ERROR_FILE_READ,
                                new String[] { new File(uri).getAbsolutePath() }));
    }
    catch (SAXException se) {
      fileType = XMLFileType.other;
      m_schemaPage.setStatusText(
          m_messages.getMessage(SchemaCompilerXMsg.MF_STATUS_NOT_A_VALID_XML,
                                new String[] { new File(uri).getAbsolutePath()} ));
    }
    
    if (xmlInfo != null)
      fileType = XMLSCHEMA_URI.equals(xmlInfo.getNamespaceName()) ? XMLFileType.schema : XMLFileType.xml;
    
    assert fileType != XMLFileType.none;

    setFileType(fileType);
  }
  
  private void setFileType(XMLFileType fileType) {
    switch (m_fileType = fileType) {
      case schema:
        validateFile();
        m_schemaPage.updateXMLFileTypeLabel(m_fileType);
        break;
      case xml:
        m_schemaPage.updateXMLFileTypeLabel(m_fileType);
        m_schemaPage.showMessage("XML but Not an XML Schema.");
        break;
      case other:
        m_schemaPage.updateXMLFileTypeLabel(m_fileType);
        m_schemaPage.showMessage("Not an XML Schema.");
        break;
      case none:
        m_schemaPage.updateXMLFileTypeLabel(m_fileType);
        break;
    }
  }

  public void updateRightButton() {
    if (m_fileType == XMLFileType.schema)
      m_mainFrame.setRightArrowEnabled(true);
    else
      m_mainFrame.setRightArrowEnabled(false);
  }
  
  
}
