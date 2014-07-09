package org.openexi.tryout;

import java.net.MalformedURLException;
import java.net.URL;
import java.io.*;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.apache.xerces.util.EncodingMap;
import org.openexi.util.MessageResolver;



class TextFrameDocument {

  private static final MessageResolver m_msgs =
      new MessageResolver(TextFrame.class);

  private final ITextFrame m_textFrame;
  private final XMLReader m_xmlReader;

  private Locator m_locator;
  private XMLSniffer.Result m_contentInfo = null;

  public TextFrameDocument(ITextFrame textFrame) {
    m_textFrame = textFrame;
    m_xmlReader = XMLHelper.createXMLReader();
  }

  public void setLocator(Locator locator)
      throws MalformedURLException, IOException {
    m_locator = locator;

    m_contentInfo = probeFile();
    loadFile(m_locator);
  }

  public Locator getLocator() {
    return m_locator;
  }

  private XMLSniffer.Result probeFile()
      throws MalformedURLException, IOException {
    XMLSniffer.Result res = null;
    URL url = new URL(m_locator.getSystemId());
    try {
      res = XMLSniffer.probeXml(url.openStream(), m_xmlReader);
    }
    catch (SAXException se) {
      m_textFrame.setStatusText(m_msgs.getMessage(
          TextFrameXMsg.TF_STATUS_NOT_A_VALID_XML,
          new String[] { m_locator.getSystemId() }));
      res = XMLSniffer.probeText(url.openStream());
    }
    return res;
  }

  private void loadFile(Locator locator)
      throws MalformedURLException, IOException {
    URL url = new URL(m_locator.getSystemId());

    String enc = EncodingMap.getIANA2JavaMapping(m_contentInfo.getIANAEncoding());

    StringBuffer buf = null;
    try {
      buf = doLoadFile(url, enc);
    }
    catch (CharConversionException ioe) {
      buf = doLoadFile(url); // fallback to system encoding
    }

    m_textFrame.setTextAreaText(buf.toString());
    if (locator.getLineNumber() > 0)
      m_textFrame.setSelectLine(locator.getLineNumber());
  }

  /**
   * Load a text file using system-default encoding.
   */
  private StringBuffer doLoadFile(URL url)
      throws IOException {
    return doLoadFile(url, null);
  }

  /**
   * Load a text file using the specified encoding.
   */
  private StringBuffer doLoadFile(URL url, String enc)
      throws IOException {
    StringBuffer buf;
    InputStream is = null;
    try {
      char[] cbuf = new char[8192];
      is = url.openStream();
      Reader reader = enc != null ? new InputStreamReader(is, enc) :
                                    new InputStreamReader(is);
      buf = new StringBuffer();
      int n;
      while ((n = reader.read(cbuf)) >= 0)
        buf.append(cbuf, 0, n);
    }
    catch (IOException ioe) {
      if (is != null) is.close();
      throw ioe;
    }
    return buf;
  }


}
