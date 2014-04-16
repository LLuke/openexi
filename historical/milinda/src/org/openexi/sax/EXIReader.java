package org.openexi.sax;

import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EXIEvent;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.EXIEventDTD;
import org.openexi.proc.events.EXIEventNS;
import org.openexi.proc.events.EXIEventSchemaNil;
import org.openexi.proc.events.EXIEventSchemaType;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.EXIDecoder;
import org.openexi.proc.EXISchemaResolver;

public final class EXIReader implements XMLReader, Attributes {

  private final EXIDecoder m_decoder;
  
  private ContentHandler m_contentHandler;
  
  private boolean m_hasLexicalHandler;
  private LexicalHandler m_lexicalHandler;

  private LocusItem[] m_locusStack;
  private int m_locusLastDepth;
  
  private String[] m_namespaceDeclarationsLocus;
  private int m_n_namespaceDeclarations;

  private int m_attrLength;
  private String[] m_attrData;

  private static final String[] PREFIXES = { "xml", 
    "p0", "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9",
    "p10", "p11", "p12", "p13", "p14", "p15", "p16", "p17", "p18", "p19",
    "p20", "p21", "p22", "p23", "p24", "p25", "p26", "p27", "p28", "p29",
    "p30", "p31", "p32", "p33", "p34", "p35", "p36", "p37", "p38", "p39",
    "p40", "p41", "p42", "p43", "p44", "p45", "p46", "p47", "p48", "p49",
    "p50", "p51", "p52", "p53", "p54", "p55", "p56", "p57", "p58", "p59",
    "p60", "p61", "p62" };

  public EXIReader() {
    m_decoder = new EXIDecoder();
    m_hasLexicalHandler = false;
    m_lexicalHandler = null;
    m_locusStack = new LocusItem[32];
    for (int i = 0; i < 32; i++) {
      m_locusStack[i] = new LocusItem();
    }
    m_locusLastDepth = -1;
    m_namespaceDeclarationsLocus = new String[PREFIXES.length * 2];
    m_attrData = new String[32 * 5];
  }
  
  private void reset() {
    m_locusLastDepth = -1;
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
  
  public final ContentHandler getContentHandler() {
    return m_contentHandler;
  }

  /**
   * Set a SAX DTDHandler to receive notation events and
   * unparsed entity declaration events.
   * @param dtdHandler SAX DTDHandler
   */
  public final void setDTDHandler(DTDHandler dtdHandler) {
    //m_dtdHandler = dtdHandler;
  }

  public final DTDHandler getDTDHandler() {
    //return m_dtdHandler;
    return null;
  }

  public void setEntityResolver(EntityResolver resolver) {
    // nothing to do.
  }
  
  public final EntityResolver getEntityResolver() {
    // nothing to do.
    return null;
  }
  
  public final void setErrorHandler(ErrorHandler errorHandler) {
    // nothing to do.
  }
  
  public final ErrorHandler getErrorHandler() {
    // nothing to do.
    return null;
  }
  
  public final void setProperty(String name, Object value) throws SAXNotRecognizedException {
    if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
      setLexicalHandler((LexicalHandler)value);
      return;
    }
    else if ("http://xml.org/sax/properties/declaration-handler".equals(name)) {
      // REVISIT: add support for declaration handler.
      return;
    }
    throw new SAXNotRecognizedException("Property '" + name + "' is not recognized.");
  }

  public final Object getProperty(String name) throws SAXNotRecognizedException {
    if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
      return this.m_lexicalHandler;
    }
    else if ("http://xml.org/sax/properties/declaration-handler".equals(name)) {
      // REVISIT: add support for declaration handler.
      return null;
    }
    throw new SAXNotRecognizedException("Property '" + name + "' is not recognized.");
  }

  public final void setFeature(String name, boolean value)
    throws SAXNotRecognizedException, SAXNotSupportedException {
    if ("http://xml.org/sax/features/namespaces".equals(name)) {
      if (!value) {
        throw new SAXNotSupportedException("");
      }
      return;
    }
    else if ("http://xml.org/sax/features/namespace-prefixes".equals(name)) {
      if (value) {
        throw new SAXNotSupportedException("");
      }
      return;
    }
    throw new SAXNotRecognizedException("Feature '" + name + "' is not recognized.");
  }
  
  public final boolean getFeature(String name) throws SAXNotRecognizedException {
    if ("http://xml.org/sax/features/namespaces".equals(name)) {
      return true;
    }
    else if ("http://xml.org/sax/features/namespace-prefixes".equals(name)) {
      return false;
    }
    throw new SAXNotRecognizedException("Feature '" + name + "' is not recognized.");
  }

  public void parse(InputSource input) throws IOException, SAXException {
    final InputStream inputStream;
    if ((inputStream = input.getByteStream()) != null) {
      parse(inputStream);
    }
    else
      throw new SAXNotSupportedException("");
  }

  public void parse(String systemId) throws SAXException {
    throw new SAXNotSupportedException("");
  }

  ///////////////////////////////////////////////////////////////////////////
  // Methods to configure EXIDecoder
  ///////////////////////////////////////////////////////////////////////////

  public final void setAlignmentType(AlignmentType alignmentType) throws EXIOptionsException {
    m_decoder.setAlignmentType(alignmentType);
  }

  public final void setFragment(boolean isFragment) {
    m_decoder.setFragment(isFragment);
  }

  public final void setPreserveLexicalValues(boolean preserveLexicalValues) throws EXIOptionsException {
    m_decoder.setPreserveLexicalValues(preserveLexicalValues);
  }

  public final void setEXISchema(GrammarCache grammarCache) throws EXIOptionsException {
    m_decoder.setEXISchema(grammarCache);
  }

  public final void setEXISchemaResolver(EXISchemaResolver schemaResolver) {
    m_decoder.setEXISchemaResolver(schemaResolver);
  }
  
  /**
   * Set a datatype representation map.
   * @param dtrm a sequence of pairs of datatype qname and datatype representation qname
   * @param n_bindings the number of qname pairs
   */
  public final void setDatatypeRepresentationMap(QName[] dtrm, int n_bindings) throws EXIOptionsException {
    m_decoder.setDatatypeRepresentationMap(dtrm, n_bindings);
  }

  /**
   * Set the block size used for EXI compression
   */
  public final void setBlockSize(int blockSize) throws EXIOptionsException {
    m_decoder.setBlockSize(blockSize);
  }

  public final void setValueMaxLength(int valueMaxLength) {
    m_decoder.setValueMaxLength(valueMaxLength);
  }

  public final void setValuePartitionCapacity(int valuePartitionCapacity) {
    m_decoder.setValuePartitionCapacity(valuePartitionCapacity);
  }

  ///////////////////////////////////////////////////////////////////////////
  // 
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Set a SAX lexical handler to receive SAX lexical events.
   * @param lexicalHandler SAX lexical handler
   */
  public void setLexicalHandler(LexicalHandler lexicalHandler) {
    m_hasLexicalHandler = (m_lexicalHandler = lexicalHandler) != null;
  }
  
  private void parse(InputStream inputStream) throws IOException, SAXException {

    reset();
    
    m_decoder.setInputStream(inputStream);
    
    final Scanner scanner;
    try {
      scanner = m_decoder.processHeader();
    }
    catch (EXIOptionsException eoe) {
      throw new SAXException(eoe.getMessage(), eoe);
    }

    final boolean preserveNS = scanner.getPreserveNS();
    final boolean preserveLexicalValues = scanner.getPreserveLexicalValues();
    
    boolean noRead = false;
    EXIEvent exiEvent = null;
    while (noRead || (exiEvent = scanner.nextEvent()) != null) {
      noRead = false;
      String uri, localName, prefix, qualifiedName;
      final LocusItem locusItem;
      switch (exiEvent.getEventVariety()) {
        case EXIEvent.EVENT_SD:
          m_contentHandler.startDocument();
          break;
        case EXIEvent.EVENT_ED:
          m_contentHandler.endDocument();
          break;
        case EXIEvent.EVENT_CH:
          final CharacterSequence characterSequence;
          characterSequence = exiEvent.getCharacters();
          m_contentHandler.characters(characterSequence.getCharacters(), characterSequence.getStartIndex(), characterSequence.length());
          break;
        case EXIEvent.EVENT_SE: {
          String attrUri, attrName, attrQualifiedName, attrPrefix;
          m_attrLength = 0;
          uri = exiEvent.getURI();
          localName = exiEvent.getName();
          qualifiedName = null;
          locusItem = pushLocusItem(uri, localName);
          if (preserveNS) {
            prefix = exiEvent.getPrefix();
            while ((exiEvent = scanner.nextEvent()) != null && exiEvent.getEventVariety() == EXIEvent.EVENT_NS) {
              final String nsUri =  exiEvent.getURI();
              final String nsPrefix = exiEvent.getPrefix();
              if (((EXIEventNS)exiEvent).getLocalElementNs()) {
                prefix = nsPrefix;
              }
              m_contentHandler.startPrefixMapping(nsPrefix, nsUri);
              pushNamespaceDeclaration(nsPrefix, nsUri);
            }
            qualifiedName = prefix.length() != 0 ? prefix + ":" + localName : localName;
            locusItem.elementQualifiedName = qualifiedName;
          }
          else {
            int i;
            if (uri.length() != 0) {
              for (i = m_n_namespaceDeclarations - 1; i > -1; i--)  {
                if (uri.equals(m_namespaceDeclarationsLocus[i << 1 | 1]))
                  break;
              }
              if (i > -1)
                prefix = m_namespaceDeclarationsLocus[i << 1];
              else {
                prefix = m_n_namespaceDeclarations < PREFIXES.length ? PREFIXES[m_n_namespaceDeclarations] :
                  "p" + m_n_namespaceDeclarations;
                m_contentHandler.startPrefixMapping(prefix, uri);
                pushNamespaceDeclaration(prefix, uri);
              }
              qualifiedName = prefix.length() != 0 ? prefix + ":" + localName : localName;
              locusItem.elementQualifiedName = qualifiedName;
            }
            else {
              for (i = m_n_namespaceDeclarations - 1; i > -1; i--)  {
                // look for a namespace declaration for prefix ""
                if (m_namespaceDeclarationsLocus[i << 1].length() == 0) {
                  if (m_namespaceDeclarationsLocus[i << 1 | 1].length() != 0) { // i.e. it was xmlns="..."
                    m_contentHandler.startPrefixMapping("", ""); // reclaim the prefix "" for the uri ""
                    pushNamespaceDeclaration("", "");
                  }
                  break;
                }
              }
              qualifiedName = localName;
              locusItem.elementQualifiedName = qualifiedName;
            }
            exiEvent = scanner.nextEvent();
          }
          if (exiEvent != null && exiEvent.getEventVariety() == EXIEvent.EVENT_TP) {
            if (preserveNS) {
              attrPrefix = exiEvent.getPrefix();
              assert attrPrefix.length() != 0;
              attrQualifiedName = attrPrefix + ":type";
            }
            else {
              int i;
              for (i = m_n_namespaceDeclarations - 1; i > -1; i--)  {
                if (XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI.equals(m_namespaceDeclarationsLocus[i << 1 | 1]))
                    break;
              }
              if (i == -1) {
                attrPrefix = m_n_namespaceDeclarations < PREFIXES.length ? PREFIXES[m_n_namespaceDeclarations] :
                  "p" + m_n_namespaceDeclarations;
                m_contentHandler.startPrefixMapping(attrPrefix, XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI);
                pushNamespaceDeclaration(attrPrefix, XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI);
              }
              attrQualifiedName = null;
            }
            final EXIEventSchemaType eventSchemaType = (EXIEventSchemaType)exiEvent;
            final String typeQualifiedName;
            if (preserveLexicalValues) {
              typeQualifiedName = eventSchemaType.getCharacters().makeString();
            }
            else {
              final String typeName = eventSchemaType.getTypeName();
              final String typePrefix;
              if (preserveNS) {
                typePrefix = eventSchemaType.getTypePrefix();
                typeQualifiedName = typePrefix.length() != 0 ? typePrefix + ":" + typeName : typeName;
              }
              else {
                final String typeUri = eventSchemaType.getTypeURI();
                if (typeUri.length() != 0) {
                  int i;
                  for (i = m_n_namespaceDeclarations - 1; i > -1; i--)  {
                    if (typeUri.equals(m_namespaceDeclarationsLocus[i << 1 | 1])) {
                      break;
                    }
                  }                  
                  if (i != -1) {
                    typePrefix = m_namespaceDeclarationsLocus[i << 1];
                  }
                  else {
                    typePrefix = m_n_namespaceDeclarations < PREFIXES.length ? PREFIXES[m_n_namespaceDeclarations] :
                      "p" + m_n_namespaceDeclarations;
                    m_contentHandler.startPrefixMapping(typePrefix, typeUri);
                    pushNamespaceDeclaration(typePrefix, typeUri);
                  }
                  typeQualifiedName = typePrefix + ":" + typeName;
                }
                else 
                  typeQualifiedName = typeName;
              }
            }
            addAttribute(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "type", attrQualifiedName, "", typeQualifiedName);
            exiEvent = scanner.nextEvent();
          }
          if (exiEvent != null && exiEvent.getEventVariety() == EXIEvent.EVENT_NL) {
            if (preserveNS) {
              attrPrefix = exiEvent.getPrefix();
              assert attrPrefix.length() != 0;
              attrQualifiedName = attrPrefix + ":nil";
            }
            else {
              int i;
              for (i = m_n_namespaceDeclarations - 1; i > -1; i--)  {
                if (XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI.equals(m_namespaceDeclarationsLocus[i << 1 | 1]))
                    break;
              }
              if (i == -1) {
                attrPrefix = m_n_namespaceDeclarations < PREFIXES.length ? PREFIXES[m_n_namespaceDeclarations] :
                  "p" + m_n_namespaceDeclarations;
                m_contentHandler.startPrefixMapping(attrPrefix, XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI);
                pushNamespaceDeclaration(attrPrefix, XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI);
              }
              attrQualifiedName = null;
            }
            addAttribute(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "nil", attrQualifiedName, "", preserveLexicalValues ? 
                exiEvent.getCharacters().makeString() : Boolean.toString(((EXIEventSchemaNil)exiEvent).isNilled())); 
            exiEvent = scanner.nextEvent();
          }
          if (exiEvent != null && exiEvent.getEventVariety() == EXIEvent.EVENT_AT) {
            do { // process attributes
              attrUri = exiEvent.getURI();
              attrName =  exiEvent.getName();
              if (preserveNS) {
                attrPrefix = exiEvent.getPrefix();
                attrQualifiedName = attrPrefix.length() != 0 ? attrPrefix + ":" + attrName : attrName;
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
                    attrPrefix = m_n_namespaceDeclarations < PREFIXES.length ? PREFIXES[m_n_namespaceDeclarations] :
                      "p" + m_n_namespaceDeclarations;
                    m_contentHandler.startPrefixMapping(attrPrefix, attrUri);
                    pushNamespaceDeclaration(attrPrefix, attrUri);
                  }
                  attrQualifiedName = attrPrefix.length() != 0 ? attrPrefix + ":" + attrName : attrName;
                }
                else
                  attrQualifiedName = attrName;
              }
              addAttribute(attrUri, attrName, attrQualifiedName, "", exiEvent.getCharacters().makeString());
            } 
            while ((exiEvent = scanner.nextEvent()) != null && exiEvent.getEventVariety() == EXIEvent.EVENT_AT);
          }
          m_contentHandler.startElement(uri, localName, qualifiedName, this);
          if (exiEvent != null)
            noRead = true;
          break;
        }
        case EXIEvent.EVENT_EE: {
          locusItem = m_locusStack[m_locusLastDepth--];
          m_contentHandler.endElement(locusItem.elementURI, locusItem.elementLocalName, locusItem.elementQualifiedName);
          final int n_prefixes = locusItem.n_namespaceDeclarations;
          for (int i = 0; i < n_prefixes; i++) {
            m_contentHandler.endPrefixMapping(m_namespaceDeclarationsLocus[--m_n_namespaceDeclarations << 1]);
          }
          break;
        }
        case EXIEvent.EVENT_CM:
          if (m_hasLexicalHandler) {
            characterSequence = exiEvent.getCharacters();
            m_lexicalHandler.comment(characterSequence.getCharacters(), characterSequence.getStartIndex(), characterSequence.length());
          }
          break;
        case EXIEvent.EVENT_PI:
          m_contentHandler.processingInstruction(exiEvent.getName(), exiEvent.getCharacters().makeString());
          break;
        case EXIEvent.EVENT_DTD: 
          if (m_hasLexicalHandler) {
            final EXIEventDTD eventDTD =(EXIEventDTD)exiEvent;
            m_lexicalHandler.startDTD(exiEvent.getName(), eventDTD.getPublicId(), eventDTD.getSystemId());
            m_lexicalHandler.endDTD();
          }
          break;
        case EXIEvent.EVENT_ER:
          m_contentHandler.skippedEntity(exiEvent.getName());
          break;
        case EXIEvent.EVENT_NL:
        case EXIEvent.EVENT_TP:
        case EXIEvent.EVENT_AT:
          assert false;
          break;
        default:
          break;
      }
    }
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Attribute Implementation
  ///////////////////////////////////////////////////////////////////////////
  
  public final int getLength () {
    return m_attrLength;
  }

  public final String getURI(int index) {
    if (index >= 0 && index < m_attrLength) {
      return m_attrData[5 * index];
    } else {
      return null;
    }
  }

  public final String getLocalName(int index) {
    if (index >= 0 && index < m_attrLength) {
      return m_attrData[5 * index + 1];
    } else {
      return null;
    }
  }

  public final String getQName(int index) {
    if (index >= 0 && index < m_attrLength) {
      return m_attrData[5 * index + 2];
    } else {
      return null;
    }
  }

  public final String getType(int index) {
    if (index >= 0 && index < m_attrLength) {
      return m_attrData[5 * index + 3];
    } else {
      return null;
    }
  }

  public final String getValue(int index) {
    if (index >= 0 && index < m_attrLength) {
      return m_attrData[5 * index + 4];
    } else {
      return null;
    }
  }

  public final int getIndex(String uri, String localName) {
    int max = m_attrLength * 5;
    for (int i = 0; i < max; i += 5) {
        if (m_attrData[i].equals(uri) && m_attrData[i+1].equals(localName)) {
            return i / 5;
        }
    } 
    return -1;
  }

  public final int getIndex(String qName) {
    int max = m_attrLength * 5;
    for (int i = 0; i < max; i += 5) {
        if (m_attrData[i+2].equals(qName)) {
            return i / 5;
        }
    } 
    return -1;
  }

  public final String getType(String uri, String localName) {
    int max = m_attrLength * 5;
    for (int i = 0; i < max; i += 5) {
        if (m_attrData[i].equals(uri) && m_attrData[i+1].equals(localName)) {
            return m_attrData[i+3];
        }
    } 
    return null;
  }

  public final String getType(String qName) {
    int max = m_attrLength * 5;
    for (int i = 0; i < max; i += 5) {
        if (m_attrData[i+2].equals(qName)) {
            return m_attrData[i+3];
        }
    }
    return null;
  }

  public final String getValue(String uri, String localName) {
    final int max = 5 * m_attrLength;
    for (int i = 0; i < max; i += 5) {
      if (m_attrData[i].equals(uri) && m_attrData[i + 1].equals(localName)) {
        return m_attrData[i+4];
      }
    }
    return null;
  }

  public final String getValue(String qName) {
    final int max = 5 * m_attrLength;
    for (int i = 0; i < max; i += 5) {
      if (m_attrData[i + 2].equals(qName)) {
        return m_attrData[i + 4];
      }
    }
    return null;
  }

  private final void addAttribute(String uri, String localName, String qName, String type, String value) {
    assert value != null;
    final int attrLength = m_attrLength + 1;
    if (m_attrData.length < 5 * attrLength) {
      final String[] attrData = new String[2 * m_attrData.length];
      System.arraycopy(m_attrData, 0, attrData, 0, 5 * m_attrLength);
      m_attrData = attrData;
    }
    m_attrData[m_attrLength * 5] = uri;
    m_attrData[m_attrLength * 5 + 1] = localName;
    m_attrData[m_attrLength * 5 + 2] = qName;
    m_attrData[m_attrLength * 5 + 3] = type;
    m_attrData[m_attrLength * 5 + 4] = value;
    m_attrLength = attrLength;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Convenience Functions
  ///////////////////////////////////////////////////////////////////////////
  
  private LocusItem pushLocusItem(String uri, String localName) {
    if (++m_locusLastDepth == m_locusStack.length) {
      final int locusStackCapacity = m_locusLastDepth + 8;
      final LocusItem[] locusStack = new LocusItem[locusStackCapacity];
      System.arraycopy(m_locusStack, 0, locusStack, 0, m_locusLastDepth);
      for (int i = m_locusLastDepth; i < locusStackCapacity; i++) {
        locusStack[i] = new LocusItem();
      }
      m_locusStack = locusStack;
    }
    final LocusItem locusItem;
    locusItem = m_locusStack[m_locusLastDepth];
    locusItem.elementURI = uri;
    locusItem.elementLocalName = localName;
    locusItem.elementQualifiedName = null;
    locusItem.n_namespaceDeclarations = 0;
    return locusItem;
  }
  
  private void pushNamespaceDeclaration(String prefix, String uri) {
    if (2 * m_n_namespaceDeclarations == m_namespaceDeclarationsLocus.length) {
      final String[] namespaceDeclarationsLocus = new String[m_namespaceDeclarationsLocus.length + 16]; 
      System.arraycopy(m_namespaceDeclarationsLocus, 0, namespaceDeclarationsLocus, 0, m_namespaceDeclarationsLocus.length);
      m_namespaceDeclarationsLocus = namespaceDeclarationsLocus;
    }
    final int pos = m_n_namespaceDeclarations++ << 1;
    m_namespaceDeclarationsLocus[pos] = prefix;
    m_namespaceDeclarationsLocus[pos | 1] = uri;
    if (m_locusLastDepth != -1)
      ++m_locusStack[m_locusLastDepth].n_namespaceDeclarations;
  }

  private static final class LocusItem {
    String elementURI;
    String elementLocalName;
    String elementQualifiedName;
    int n_namespaceDeclarations;
  }

}
