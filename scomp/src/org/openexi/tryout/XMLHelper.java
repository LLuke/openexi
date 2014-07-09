package org.openexi.tryout;

import java.io.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.LocatorImpl;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

class XMLHelper {

  static final String SAX_FEATURE_NAMESPACES =
      "http://xml.org/sax/features/namespaces";
  static final String SAX_FEATURE_NAMESPACE_PREFIXES =
      "http://xml.org/sax/features/namespace-prefixes";
  static final String SAX_FEATURE_VALIDATION =
      "http://xml.org/sax/features/validation";

  /**
   * Returns one-based element path of an element. It returns a path of
   * length zero if the node was a Document. There is an assumption that
   * the node is part of the document tree.
   */
  public static final int[] getElementPath(Node node) {
    int[] steps = null;
    if (node != null) {
      if (node.getNodeType() == Node.ATTRIBUTE_NODE)
        node = ((Attr)node).getOwnerElement();
      int n_steps;
      Node current = node;
      for (n_steps = 0; current.getNodeType() != Node.DOCUMENT_NODE;
           n_steps++, current = current.getParentNode());
      steps = new int[n_steps];
      int i;
      for (i = n_steps - 1; i >= 0; i--, node = node.getParentNode()) {
        int n = 1;
        Node prev = node;
        do {
          if ((prev = prev.getPreviousSibling()) != null &&
              prev.getNodeType() == Node.ELEMENT_NODE)
            n++;
        } while (prev != null);
        steps[i] = n;
      }
    }
    return steps;
  }

  /**
   * Returns line and column number of an element at the specified path.
   */
  public static final Locator getLineColumn(String systemId, int[] path,
                                            XMLReader xmlReader)
    throws IOException, SAXException, LineColumnNotFound {

    Locator locator = null;
    if (systemId != null) {
      InputSource is = new InputSource(systemId);
      locator = getLineColumn(is, path, xmlReader);
    }
    return locator;
  }

  /**
   * Returns line and column number of an element at the specified path.
   */
  public static final Locator getLineColumn(InputSource is, int[] path,
                                            XMLReader xmlReader)
      throws IOException, SAXException, LineColumnNotFound {
    if (path != null && path.length > 0) {
      try {
        ContentHandler hdlr = new LineColumnPathHandler(path);

        xmlReader.setContentHandler(hdlr);
        try {
          xmlReader.parse(is);
        }
        catch (LineColumnSAXException e) {
          if (e.isFound())
            return e.getLocator();
          else
            throw new LineColumnNotFound(e.getMessage(), e);
        }
      }
      finally {
        InputStream istream = is.getByteStream();
        Reader reader = is.getCharacterStream();
        if (istream != null)
          istream.close();
        if (reader != null)
          reader.close();
      }
    }
    else // int[0] represents a Document
      return createLocator(is.getSystemId(), 1, 1);

    // Supposed not to reach here.
    return null;
  }

  public static final Locator getLineColumn(String systemId, Attributes attrs,
                                            XMLReader xmlReader)
      throws IOException, SAXException, LineColumnNotFound {
    return getLineColumn(systemId, null, null, attrs, xmlReader);
  }

  public static final Locator getLineColumn(String systemId,
                                            String namespaceName,
                                            String localName,
                                            Attributes attrs,
                                            XMLReader xmlReader)
      throws IOException, SAXException, LineColumnNotFound {
    Locator locator = null;
    if (systemId != null) {
      InputSource is = new InputSource(systemId);
      locator = getLineColumn(is, namespaceName, localName, attrs, xmlReader);
    }
    return locator;
  }

  public static final Locator getLineColumn(InputSource is,
                                            String namespaceName,
                                            String localName,
                                            Attributes attrs,
                                            XMLReader xmlReader)
      throws IOException, SAXException, LineColumnNotFound {
    if (attrs != null) {
      try {
        ContentHandler hdlr =
            new LineColumnAttrsHandler(namespaceName, localName, attrs);

        xmlReader.setContentHandler(hdlr);
        try {
          xmlReader.parse(is);
        }
        catch (LineColumnSAXException e) {
          if (e.isFound())
            return e.getLocator();
          else
            throw new LineColumnNotFound(e.getMessage(), e);
        }
      }
      finally {
        InputStream istream = is.getByteStream();
        Reader reader = is.getCharacterStream();
        if (istream != null)
          istream.close();
        if (reader != null)
          reader.close();
      }
    }
    return null;
  }

  /**
   * Create a SAX Locator given systemId, line and column number.
   */
  public static final Locator createLocator(String systemId,
                                             int line, int col) {
    LocatorImpl locator = new LocatorImpl();
    locator.setSystemId    (systemId);
    locator.setLineNumber  (line);
    locator.setColumnNumber(col);
    return locator;
  }

  /**
   * Create a SAX Locator given a SAXParseException.
   */
  public static final Locator createLocator(SAXParseException spe) {
    LocatorImpl locator = new LocatorImpl();
    locator.setSystemId    (spe.getSystemId());
    locator.setLineNumber  (spe.getLineNumber());
    locator.setColumnNumber(spe.getColumnNumber());
    return locator;
  }

  /**
   * Create a JAXP DocumentBuilder instance.
   * @param ns true if namespace-aware, otherwise false
   * @param validating true if validating, otherwise false
   * @return DocumentBuilder
   */
  public static final DocumentBuilder createDocBuilder(boolean ns,
                                                       boolean validating) {
    DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
    dfactory.setNamespaceAware(ns);
    dfactory.setValidating(validating);
    try {
      return dfactory.newDocumentBuilder();
    }
    catch (ParserConfigurationException pce) {
      pce.printStackTrace();
      throw new RuntimeException(pce.getMessage());
    }
  }

  /**
   * Create an non-validating, namespace-aware XMLReader that does not
   * report prefix attributes while parsing.
   */
  public static final XMLReader createXMLReader() {
    try {
      return createXMLReader(true, false, false);
    }
    catch (SAXException se) {
      se.printStackTrace();
      throw new RuntimeException(se.getMessage());
    }
  }

  /**
   * Create an XMLReader as specified.
   */
  public static final XMLReader createXMLReader(boolean ns, boolean prefix,
                                                boolean validating)
      throws SAXException {
    SAXParserFactory sfactory = SAXParserFactory.newInstance();
    sfactory.setNamespaceAware(ns);
    
    XMLReader xmlReader;
    try {
      xmlReader = sfactory.newSAXParser().getXMLReader();
    }
    catch (ParserConfigurationException pce) {
      throw new SAXException(pce);
    }
    xmlReader.setFeature(SAX_FEATURE_NAMESPACE_PREFIXES, prefix);
    xmlReader.setFeature(SAX_FEATURE_VALIDATION, validating);
    return xmlReader;
  }

  /**
   * Create a default, vanilla Transformer.
   */
  public static final Transformer createDefaultTransformer() {
    Transformer transformer = null;
    TransformerFactory tfactory = TransformerFactory.newInstance();
    try {
      transformer = tfactory.newTransformer();
    }
    catch (TransformerConfigurationException tce) {
      tce.printStackTrace();
      throw new RuntimeException(tce.getMessage());
    }
    return transformer;
  }

  /**
   * Create a new DOM Document.
   */
  public static final Document createDocument() {
    Document doc = null;
    DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
      doc = docBuilder.newDocument();
    }
    catch (ParserConfigurationException pce) {
      pce.printStackTrace();
      throw new RuntimeException(pce.getMessage());
    }
    return doc;
  }

//  /**
//   * Obtain a text child of the specified node. If there is not currently
//   * a text child, one is created and attached to the node.
//   */
//  public static Text obtainChildTextNode(Node node) {
//    NodeList nodeList = node.getChildNodes();
//    int i, len;
//    for (i = 0, len = nodeList.getLength(); i < len; i++) {
//      Node child = nodeList.item(i);
//      short nodeType = child.getNodeType();
//      if (nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE)
//        return (Text)child;
//    }
//    Document doc = node.getOwnerDocument();
//    Text text = doc.createTextNode("");
//    node.appendChild(text);
//    return text;
//  }

  /**
   * Create a validating/non-validating DocumentBuilderFactory in terms of
   * XML Schema.
   * @param schemaValidation whether to validate or not to validate
   * @return a configured DocumentBuilderFactory
   */
  public static DocumentBuilderFactory createDocumentBuilderFactory(boolean schemaValidation) {
    DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
    dfactory.setNamespaceAware(true);
    dfactory.setValidating(schemaValidation);
    if (schemaValidation) {
      dfactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                            "http://www.w3.org/2001/XMLSchema");
    }
    return dfactory;
  }

  /**
   * Retrieve text value of an element.
   * @param elem element node
   * @return text value of an element
   */
  public static String getElementValue(Element elem) {
    String val = null;
    if (elem != null) {
      val = "";
      for (Node kid = elem.getFirstChild(); kid != null; kid = kid.getNextSibling()) {
        switch (kid.getNodeType()) {
          case Node.TEXT_NODE:
          case Node.CDATA_SECTION_NODE:
            val += kid.getNodeValue();
            break;
          case Node.ELEMENT_NODE:
            val += getElementValue((Element)kid);
            break;
          default:
            break;
        }
      }
    }
    return val;
  }

  /**
   * Converts a SAXParseException into a locator.
   * @param spe SAXParseException
   * @return Locator
   */
  public static Locator toLocator(SAXParseException spe) {
    LocatorImpl locator = new LocatorImpl();
    locator.setSystemId(spe.getSystemId());
    locator.setPublicId(spe.getPublicId());
    locator.setLineNumber(spe.getLineNumber());
    locator.setColumnNumber(spe.getColumnNumber());
    return locator;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Local utility classes
  ///////////////////////////////////////////////////////////////////////////

  /**
   * org.xml.sax.ContentHandler implementation that locates converts
   * absolute path (i.e. int[]) into line and column numbers.
   */
  private static final class LineColumnPathHandler extends DefaultHandler {

    private final int[]  m_path; // absolute path
    private final int[]  m_count; // element counter

    int     m_pos; // sax-depth cursor
    int     m_current; // counter cursor

    private boolean m_found;

    Locator m_locator;

    LineColumnPathHandler(int[] path) {
      m_path = path;
      m_count = new int[path.length];
    }

    public void setDocumentLocator(Locator locator) {
      m_locator = locator;
    }

    public void startDocument() throws SAXException {
      m_pos     = -1;
      m_current = 0;  // counter cursor initializes to 0
      m_found = false;
    }

    public void endDocument() throws SAXException {
      if (!m_found)
        throw new LineColumnSAXException(false, null);
    }

    public void startElement(java.lang.String namespaceURI,
                             java.lang.String localName,
                             java.lang.String qualName,
                             Attributes atts) throws SAXException {
      ++m_pos;
      if (m_pos < m_path.length) {
        if (++m_count[m_pos] > m_path[m_pos]) // invalid path
          throw new LineColumnSAXException(false, null);

        if (m_current == m_pos && m_count[m_current] == m_path[m_current]) {
          if (++m_current == m_path.length) { // found
            m_found = true;
            throw new LineColumnSAXException(true, new LocatorImpl(m_locator));
          }
        }
      }
    }

    public void endElement(java.lang.String namespaceURI,
                           java.lang.String localName,
                           java.lang.String qualName) throws SAXException {
      --m_pos;
    }
  }

  /**
   * org.xml.sax.ContentHandler implementation that locates an element
   * by its specified set of attributes and returns its line and column
   * numbers.
   */
  private static final class LineColumnAttrsHandler extends DefaultHandler {

    private final String m_namespaceName;
    private final String m_localName;
    private final Attributes m_attrs; // attributes
    private boolean m_found;

    Locator m_locator;

    LineColumnAttrsHandler(String namespaceName, String localName, Attributes attrs) {
      m_namespaceName = namespaceName;
      m_localName = localName;
      m_attrs = attrs;
    }

    public void setDocumentLocator(Locator locator) {
      m_locator = locator;
    }

    public void startDocument() throws SAXException {
      m_found = false;
    }

    public void endDocument() throws SAXException {
      if (!m_found)
        throw new LineColumnSAXException(false, null);
    }

    public void startElement(java.lang.String namespaceURI,
                             java.lang.String localName,
                             java.lang.String qualName,
                             Attributes attrs) throws SAXException {
      boolean matched = false;
      if (m_localName != null) {
        if (namespaceURI != null && namespaceURI.length() > 0) {
          matched = namespaceURI.equals(m_namespaceName) &&
                    localName.equals(m_localName);
        }
        else
          matched = m_namespaceName == null && qualName.equals(m_localName);
      }
      else // we care only about attributes
        matched = true;

      if (matched) {
        int i, len;
        if (!m_found) {
          for (i = 0, len = m_attrs.getLength(); i < len; i++) {
            int n;
            if ( (n = attrs.getIndex(m_attrs.getURI(i),
                                     m_attrs.getLocalName(i))) >= 0) {
              if (!attrs.getValue(n).equals(m_attrs.getValue(i)))
                break;
            }
            else
              break;
          }
          if (i == len) {
            m_found = true;
            throw new LineColumnSAXException(true, new LocatorImpl(m_locator));
          }
        }
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  // Exceptions
  ///////////////////////////////////////////////////////////////////////////

  /**
   * LineColumnNotFound indicates that line and column numbers could not
   * be located though an attempt has been made.
   */
  public static final class LineColumnNotFound
      extends Exception implements NestedException {

    private static final long serialVersionUID = -5778448746425160603L;
    
    private Exception m_exception;

    private LineColumnNotFound(String msg, Exception e) {
      super(msg);
      m_exception = e;
    }

    public Exception getException() {
      return m_exception;
    }
  }

  /**
   * org.xml.sax.SAXException implementation that carries a Locator
   * and a flag that indicates if the position has been located.
   */
  private static final class LineColumnSAXException extends SAXException {
    private static final long serialVersionUID = -394671064726027340L;
    private final boolean m_found;
    private final Locator m_locator;
    LineColumnSAXException(boolean found, Locator locator) {
      super("");
      m_found   = found;
      m_locator = locator;
    }
    Locator getLocator() {
      return m_locator;
    }
    boolean isFound() {
      return m_found;
    }
  }


}
