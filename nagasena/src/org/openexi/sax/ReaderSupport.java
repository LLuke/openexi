package org.openexi.sax;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.schema.Characters;
import org.openexi.schema.EXISchema;

public abstract class ReaderSupport implements Attributes {

  protected final EXIDecoder m_decoder;

  protected ContentHandler m_contentHandler;

  protected String[] m_namespaceDeclarationsLocus;
  protected int m_n_namespaceDeclarations;

  protected int m_attrLength;
  private String[] m_attrData;
  private Characters[] m_attrValue;
  
  protected static final String[] PREFIXES = { "xml", 
    "p0", "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9",
    "p10", "p11", "p12", "p13", "p14", "p15", "p16", "p17", "p18", "p19",
    "p20", "p21", "p22", "p23", "p24", "p25", "p26", "p27", "p28", "p29",
    "p30", "p31", "p32", "p33", "p34", "p35", "p36", "p37", "p38", "p39",
    "p40", "p41", "p42", "p43", "p44", "p45", "p46", "p47", "p48", "p49",
    "p50", "p51", "p52", "p53", "p54", "p55", "p56", "p57", "p58", "p59",
    "p60", "p61", "p62" };
  
  protected boolean m_preserveNS;
  protected boolean m_preserveLexicalValues;

  protected int m_n_prefixes;
  protected String[] m_prefixes;
  protected String[] m_prefixesColon;
  protected String[] m_uris;
  protected int[] m_n_qualifiedNames;
  protected String[][] m_qualifiedNames;

  private static final int ATTRIBUTE_URI_OFFSET = 0;
  private static final int ATTRIBUTE_LOCALNAME_OFFSET = 1;
  private static final int ATTRIBUTE_QNAME_OFFSET = 2;
  private static final int ATTRIBUTE_TYPE_OFFSET = 3;
  private static final int ATTRIBUTE_SZ = 4;

  protected final StringBuilder stringBuilder;

  protected ReaderSupport() {
    m_decoder = new EXIDecoder();
    m_namespaceDeclarationsLocus = new String[PREFIXES.length * 2];
    m_attrData = new String[32 * ATTRIBUTE_SZ];
    m_attrValue = new Characters[32];
    stringBuilder = new StringBuilder();
    populatePrefixes((EXISchema)null);
  }

  protected void reset() {
    m_n_namespaceDeclarations = 0;
    pushNamespaceDeclaration(PREFIXES[0], XmlUriConst.W3C_XML_1998_URI);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // XMLReader APIs
  ///////////////////////////////////////////////////////////////////////////
  
  /**
   * Set a SAX content handler to receive SAX events.
   * @param contentHandler SAX content handler
   */
  public final void setContentHandler(ContentHandler contentHandler) {
    m_contentHandler = contentHandler;
  }
  
  /**
   * Get the SAX content handler currently in use.
   * @return ContentHandler SAX content handler.
   */
  public final ContentHandler getContentHandler() {
    return m_contentHandler;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Methods to configure ReaderSupport
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Set the GrammarCache used in parsing EXI streams. 
   * @param grammarCache {@link org.openexi.proc.grammars.GrammarCache}
   * @throws EXIOptionsException
   */
  public void setGrammarCache(GrammarCache grammarCache) throws EXIOptionsException {
    populatePrefixes(grammarCache.getEXISchema());
    m_decoder.setGrammarCache(grammarCache);
  }

  ///////////////////////////////////////////////////////////////////////////
  // 
  ///////////////////////////////////////////////////////////////////////////
  
  /**
   * Parses the header and returns an scanner.
   * @param inputStream
   * @return Scanner
   */
  protected final Scanner processHeader(InputStream inputStream) throws IOException, SAXException {
    m_decoder.setInputStream(inputStream);
    
    final Scanner scanner;
    try {
      scanner = m_decoder.processHeader();
    }
    catch (EXIOptionsException eoe) {
      throw new SAXException(eoe.getMessage(), eoe);
    }
    m_preserveNS = scanner.getPreserveNS();
    m_preserveLexicalValues = scanner.getPreserveLexicalValues();
    return scanner;
  }

  protected final boolean doAttribute(EventDescription exiEvent, Scanner scanner) throws IOException, SAXException {
    boolean namespaceDeclAdded = false;
    String attrQualifiedName, attrPrefix;
    final String attrUri = exiEvent.getURI();
    final String attrName =  exiEvent.getName();
    if (m_preserveNS) {
      attrPrefix = exiEvent.getPrefix();
      if (attrPrefix.length() != 0) {
        stringBuilder.setLength(0);
        attrQualifiedName = stringBuilder.append(attrPrefix).append(':').append(attrName).toString();
      }
      else
        attrQualifiedName = attrName;
    }
    else {
      final int uriId = exiEvent.getURIId();
      if (uriId < m_n_prefixes) {
        final int nameId = exiEvent.getNameId();
        if (nameId < m_n_qualifiedNames[uriId])
          attrQualifiedName = m_qualifiedNames[uriId][nameId];
        else {
          stringBuilder.setLength(0);
          attrQualifiedName = stringBuilder.append(m_prefixesColon[uriId]).append(attrName).toString();
        }
      }
      else { 
        if (attrUri.length() != 0) {
          attrPrefix = "";
          int i;
          for (i = m_n_namespaceDeclarations - 1; i > -1; i--)  {
            if (attrUri.equals(m_namespaceDeclarationsLocus[i << 1 | 1])) {
              attrPrefix = m_namespaceDeclarationsLocus[i << 1];
              break;
            }
          }
          if (i == -1) {
            if (m_n_namespaceDeclarations < PREFIXES.length)
              attrPrefix = PREFIXES[m_n_namespaceDeclarations];
            else {
              stringBuilder.setLength(0);
              attrPrefix = stringBuilder.append('p').append(m_n_namespaceDeclarations).toString();
            }
            if (m_contentHandler != null)
              m_contentHandler.startPrefixMapping(attrPrefix, attrUri);
            pushNamespaceDeclaration(attrPrefix, attrUri);
            namespaceDeclAdded = true;
          }
          if (attrPrefix.length() != 0 ) {
            stringBuilder.setLength(0);
            attrQualifiedName = stringBuilder.append(attrPrefix).append(':').append(attrName).toString();
          }
          else
            attrQualifiedName = attrName;
        }
        else
          attrQualifiedName = attrName;
      }
    }
    addAttribute(attrUri, attrName, attrQualifiedName, "", exiEvent.getCharacters());
    return namespaceDeclAdded;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Attribute Implementation
  ///////////////////////////////////////////////////////////////////////////
  
  /** @y.exclude */
  public final int getLength () {
    return m_attrLength;
  }
  
  /** @y.exclude */
  public final String getURI(int index) {
    if (-1 < index && index < m_attrLength)
      return m_attrData[ATTRIBUTE_SZ * index + ATTRIBUTE_URI_OFFSET];
    else
      return null;
  }
  
  /** @y.exclude */
  public final String getLocalName(int index) {
    if (-1 < index && index < m_attrLength)
      return m_attrData[ATTRIBUTE_SZ * index + ATTRIBUTE_LOCALNAME_OFFSET];
    else
      return null;
  }
  
  /** @y.exclude */
  public final String getQName(int index) {
    if (-1 < index && index < m_attrLength)
      return m_attrData[ATTRIBUTE_SZ * index + ATTRIBUTE_QNAME_OFFSET];
    else
      return null;
  }
  
  /** @y.exclude */
  public final String getType(int index) {
    if (-1 < index && index < m_attrLength)
      return m_attrData[ATTRIBUTE_SZ * index + ATTRIBUTE_TYPE_OFFSET];
    else
      return null;
  }
  
  /** @y.exclude */
  public final String getValue(int index) {
    if (-1 < index && index < m_attrLength)
      return m_attrValue[index].makeString();
    else
      return null;
  }
  
  /** @y.exclude */
  public final int getIndex(String uri, String localName) {
    final int max = m_attrLength * ATTRIBUTE_SZ;
    for (int i = 0; i < max; i += ATTRIBUTE_SZ) {
      if (m_attrData[i + ATTRIBUTE_URI_OFFSET].equals(uri) && m_attrData[i + ATTRIBUTE_LOCALNAME_OFFSET].equals(localName)) {
        return i / ATTRIBUTE_SZ;
      }
    } 
    return -1;
  }
  
  /** @y.exclude */
  public final int getIndex(String qName) {
    final int max = m_attrLength * ATTRIBUTE_SZ;
    for (int i = 0; i < max; i += ATTRIBUTE_SZ) {
      if (m_attrData[i + ATTRIBUTE_QNAME_OFFSET].equals(qName)) {
        return i / ATTRIBUTE_SZ;
      }
    } 
    return -1;
  }
  
  /** @y.exclude */
  public final String getType(String uri, String localName) {
    final int max = m_attrLength * ATTRIBUTE_SZ;
    for (int i = 0; i < max; i += ATTRIBUTE_SZ) {
      if (m_attrData[i + ATTRIBUTE_URI_OFFSET].equals(uri) && m_attrData[i + ATTRIBUTE_LOCALNAME_OFFSET].equals(localName)) {
        return m_attrData[i + ATTRIBUTE_TYPE_OFFSET];
      }
    } 
    return null;
  }
  
  /** @y.exclude */
  public final String getType(String qName) {
    final int max = m_attrLength * ATTRIBUTE_SZ;
    for (int i = 0; i < max; i += ATTRIBUTE_SZ) {
      if (m_attrData[i + ATTRIBUTE_QNAME_OFFSET].equals(qName)) {
        return m_attrData[i + ATTRIBUTE_TYPE_OFFSET];
      }
    }
    return null;
  }
  
  /** @y.exclude */
  public final String getValue(String uri, String localName) {
    final int max = ATTRIBUTE_SZ * m_attrLength;
    for (int i = 0; i < max; i += ATTRIBUTE_SZ) {
      if (m_attrData[i + ATTRIBUTE_URI_OFFSET].equals(uri) && m_attrData[i + ATTRIBUTE_LOCALNAME_OFFSET].equals(localName)) {
        return m_attrValue[i / ATTRIBUTE_SZ].makeString();
      }
    }
    return null;
  }
  
  /** @y.exclude */
  public final String getValue(String qName) {
    final int max = ATTRIBUTE_SZ * m_attrLength;
    for (int i = 0; i < max; i += ATTRIBUTE_SZ) {
      if (m_attrData[i + ATTRIBUTE_QNAME_OFFSET].equals(qName)) {
        return m_attrValue[i / ATTRIBUTE_SZ].makeString();
      }
    }
    return null;
  }

  protected final void addAttribute(String uri, String localName, String qName, String type, Characters value) {
    assert value != null;
    final int attrLength = m_attrLength + 1;
    if (m_attrData.length < ATTRIBUTE_SZ * attrLength) {
      final String[] attrData = new String[m_attrData.length + 32 * ATTRIBUTE_SZ];
      System.arraycopy(m_attrData, 0, attrData, 0, ATTRIBUTE_SZ * m_attrLength);
      m_attrData = attrData;
      final Characters[] attrValue = new Characters[m_attrValue.length + 32];
      System.arraycopy(m_attrValue, 0, attrValue, 0, m_attrLength);
      m_attrValue = attrValue;
    }
    m_attrData[m_attrLength * ATTRIBUTE_SZ + ATTRIBUTE_URI_OFFSET] = uri;
    m_attrData[m_attrLength * ATTRIBUTE_SZ + ATTRIBUTE_LOCALNAME_OFFSET] = localName;
    m_attrData[m_attrLength * ATTRIBUTE_SZ + ATTRIBUTE_QNAME_OFFSET] = qName;
    m_attrData[m_attrLength * ATTRIBUTE_SZ + ATTRIBUTE_TYPE_OFFSET] = type;
    m_attrValue[m_attrLength] = value;
    m_attrLength = attrLength;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Convenience Functions
  ///////////////////////////////////////////////////////////////////////////
  
  protected final void pushNamespaceDeclaration(String prefix, String uri) {
    if (2 * m_n_namespaceDeclarations == m_namespaceDeclarationsLocus.length) {
      final String[] namespaceDeclarationsLocus = new String[m_namespaceDeclarationsLocus.length + 16]; 
      System.arraycopy(m_namespaceDeclarationsLocus, 0, namespaceDeclarationsLocus, 0, m_namespaceDeclarationsLocus.length);
      m_namespaceDeclarationsLocus = namespaceDeclarationsLocus;
    }
    final int pos = m_n_namespaceDeclarations++ << 1;
    m_namespaceDeclarationsLocus[pos] = prefix;
    m_namespaceDeclarationsLocus[pos | 1] = uri;
  }

  private void populatePrefixes(EXISchema schema) {
    String[][] localNames;
    if (schema != null) {
        m_uris = schema.uris;
        localNames = schema.localNames;
    }
    else {
        m_uris = new String[] { "", XmlUriConst.W3C_XML_1998_URI, XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI };
        localNames = new String[3][];
        localNames[0] = null;
        localNames[1] = new String[] { "base", "id", "lang", "space" };
        localNames[2] = new String[] { "nil", "type" };
    }
    m_n_prefixes = m_uris.length;
    m_prefixes = new String[m_n_prefixes];
    m_prefixesColon = new String[m_n_prefixes];
    m_prefixes[0] = "";
    m_prefixesColon[0] = "";
    m_prefixes[1] = "xml";
    m_prefixesColon[1] = "xml:";
    m_prefixes[2] = "xsi";
    m_prefixesColon[2] = "xsi:";
    if (m_n_prefixes > 3) {
      m_prefixes[3] = "xsd";
      m_prefixesColon[3] = "xsd:";
      for (int i = 4; i < m_n_prefixes; i++) {
        stringBuilder.setLength(0);
        m_prefixes[i] = stringBuilder.append('s').append(i - 4).toString();
        stringBuilder.setLength(0);
        m_prefixesColon[i] = stringBuilder.append(m_prefixes[i]).append(':').toString();
      }
    }

    m_n_qualifiedNames = new int[m_uris.length];
    m_n_qualifiedNames[0] = 0;
    m_qualifiedNames = new String[m_uris.length][];
    for (int i = 1; i < m_uris.length; i++) {
      final String[] names = localNames[i];
      m_n_qualifiedNames[i] = names.length;
      m_qualifiedNames[i] = new String[names.length];
        for (int j = 0; j < names.length; j++) {
          stringBuilder.setLength(0);
          m_qualifiedNames[i][j] = stringBuilder.append(m_prefixesColon[i]).append(names[j]).toString();
        }
    }
  }
  
}
