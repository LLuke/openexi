package org.openexi.fujitsu.sax;

import java.io.IOException;
import java.io.OutputStream;

import java.util.Iterator;
import java.util.TreeSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.LocatorImpl;

import org.openexi.fujitsu.proc.EXIOptionsEncoder;
import org.openexi.fujitsu.proc.HeaderOptionsOutputType;
import org.openexi.fujitsu.proc.common.AlignmentType;
import org.openexi.fujitsu.proc.common.EXIOptions;
import org.openexi.fujitsu.proc.common.EXIOptionsException;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.proc.common.SchemaId;
import org.openexi.fujitsu.proc.grammars.DocumentGrammarState;
import org.openexi.fujitsu.proc.grammars.EventTypeSchema;
import org.openexi.fujitsu.proc.grammars.EventTypeSchemaAttribute;
import org.openexi.fujitsu.proc.grammars.Grammar;
import org.openexi.fujitsu.proc.grammars.GrammarCache;
import org.openexi.fujitsu.proc.io.BitPackedScriber;
import org.openexi.fujitsu.proc.io.BitOutputStream;
import org.openexi.fujitsu.proc.io.PrefixUriBindings;
import org.openexi.fujitsu.proc.io.Scriber;
import org.openexi.fujitsu.proc.io.Scribble;
import org.openexi.fujitsu.proc.io.ScriberFactory;
import org.openexi.fujitsu.proc.io.StringTable;
import org.openexi.fujitsu.proc.io.ValueScriber;
import org.openexi.fujitsu.proc.util.URIConst;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EmptySchema;

public final class Transmogrifier {

  private final XMLReader m_xmlReader;
  /** 
   * EXIEncoderSaxHandler handles SAX events coming from XMLReader. 
   **/
  private final SAXEventHandler m_saxHandler;

  private HeaderOptionsOutputType m_outputOptions;
  private final EXIOptions m_exiOptions;
  
  private static final SchemaId SCHEMAID_NO_SCHEMA;
  private static final SchemaId SCHEMAID_EMPTY_SCHEMA;
  static {
    SCHEMAID_NO_SCHEMA = new SchemaId((String)null);
    SCHEMAID_EMPTY_SCHEMA = new SchemaId(""); 
  }
  
  public Transmogrifier() throws TransmogrifierException {
    this(createSAXParserFactory());
  }

  public Transmogrifier(SAXParserFactory saxParserFactory) throws TransmogrifierException {
    // fixtures
    m_saxHandler = new SAXEventHandler();
    try {
      final SAXParser saxParser = saxParserFactory.newSAXParser();
      m_xmlReader = saxParser.getXMLReader();
    }
    catch (Exception exc) {
      throw new TransmogrifierException(TransmogrifierException.XMLREADER_ACCESS_ERROR, 
          (String[])null, (LocatorOnSAXParseException)null);
    }
    m_xmlReader.setContentHandler(m_saxHandler);
    try {
      m_xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", m_saxHandler);
    }
    catch (SAXException se) {
      TransmogrifierException te;
      te = new TransmogrifierException(TransmogrifierException.UNHANDLED_SAXPARSER_PROPERTY, 
          new String[] { "http://xml.org/sax/properties/lexical-handler" }, (LocatorOnSAXParseException)null);
      te.setException(se);
      throw te;
    }
    /*
     * REVISIT: we *may* (or may not) eventually want to support internal DTD subset.
     * m_xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", saxHandler);
     */
    m_outputOptions = HeaderOptionsOutputType.none;
    m_exiOptions = new EXIOptions();
  }

  /**
   * Change the way a transmogrifier handles external general entities. When the value
   * of resolveExternalGeneralEntities is set to true, a transmogrifier will try to 
   * resolve external general entities. Otherwise, external general entities will not
   * be resolved.
   * @param resolveExternalGeneralEntities  
   * @throws TransmogrifierException Thrown when the underlying XMLReader does not 
   * support the specified behaviour.
   */
  public void setResolveExternalGeneralEntities(boolean resolveExternalGeneralEntities) throws TransmogrifierException {
    try {
      m_xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", resolveExternalGeneralEntities);
    }
    catch (SAXException se) {
      TransmogrifierException te;
      te = new TransmogrifierException(TransmogrifierException.UNHANDLED_SAXPARSER_FEATURE, 
          new String[] { "http://xml.org/sax/features/external-general-entities" }, (LocatorOnSAXParseException)null);
      te.setException(se);
      throw te;
    }
  }
  
  public void setPrefixUriBindings(PrefixUriBindings prefixUriBindings) {
    m_saxHandler.setPrefixUriBindings(prefixUriBindings);
  }

  private void reset() {
    m_saxHandler.reset();
  }

  /**
   * Set an output stream to which encoded streams are written out.
   * @param ostream output stream
   */
  public final void setOutputStream(OutputStream ostream) {
    m_saxHandler.setOutputStream(ostream);
  }

  public final void setAlignmentType(AlignmentType alignmentType) throws EXIOptionsException {
    m_exiOptions.setAlignmentType(alignmentType);
    m_saxHandler.setAlignmentType(alignmentType);
  }
  
  public final void setFragment(boolean isFragment) {
    m_exiOptions.setFragment(isFragment);
  }
  
  public final void setBlockSize(int blockSize) throws EXIOptionsException {
    m_exiOptions.setBlockSize(blockSize);
  }

  public final void setValueMaxLength(int valueMaxLength) {
    m_exiOptions.setValueMaxLength(valueMaxLength);
    m_saxHandler.setValueMaxLength(valueMaxLength);
  }
  
  public final void setValuePartitionCapacity(int valuePartitionCapacity) {
    m_exiOptions.setValuePartitionCapacity(valuePartitionCapacity);
  }
  
  public final void setPreserveLexicalValues(boolean preserveLexicalValues) throws EXIOptionsException {
    if (m_exiOptions.getPreserveLexicalValues() != preserveLexicalValues) {
      if (m_outputOptions != HeaderOptionsOutputType.none && m_exiOptions.getDatatypeRepresentationMapBindingsCount() != 0 && preserveLexicalValues) {
        throw new EXIOptionsException("Preserve.lexicalValues option and datatypeRepresentationMap option cannot be specified together in EXI header options.");
      }
      m_exiOptions.setPreserveLexicalValues(preserveLexicalValues);
      m_saxHandler.setPreserveLexicalValues(preserveLexicalValues);
    }
  }
  
  public final void setEXISchema(GrammarCache grammarCache) throws EXIOptionsException {
    setEXISchema(grammarCache, null);
  }
  
  public final void setEXISchema(GrammarCache grammarCache, SchemaId schemaId) throws EXIOptionsException {
    final EXISchema schema = grammarCache.getEXISchema();
    if (schemaId == null) {
      if (schema == null) {
        schemaId = SCHEMAID_NO_SCHEMA;
      }
      else if (schema == EmptySchema.getEXISchema()) {
        schemaId = SCHEMAID_EMPTY_SCHEMA;
      }
    }
    m_exiOptions.setSchemaId(schemaId);
    m_exiOptions.setGrammarOptions(grammarCache.grammarOptions);
    m_saxHandler.setEXISchema(grammarCache);
  }
  
  public final void setDatatypeRepresentationMap(QName[] dtrm, int n_bindings) throws EXIOptionsException {
    if (!QName.isSame(m_exiOptions.getDatatypeRepresentationMap(), m_exiOptions.getDatatypeRepresentationMapBindingsCount(), dtrm, n_bindings)) {
      if (m_outputOptions != HeaderOptionsOutputType.none && m_exiOptions.getPreserveLexicalValues() && dtrm != null) {
        throw new EXIOptionsException("Preserve.lexicalValues option and datatypeRepresentationMap option cannot be specified together in EXI header options.");
      }
      m_exiOptions.setDatatypeRepresentationMap(dtrm, n_bindings);
      m_saxHandler.setDatatypeRepresentationMap(dtrm, n_bindings);
    }
  }

  public final void setEntityResolver(EntityResolver entityResolver) {
    m_xmlReader.setEntityResolver(entityResolver);
  }

  /**
   * Tells the encoder whether to or not to start the stream by
   * adding an EXI cookie.
   * @param outputCookie
   */
  public final void setOutputCookie(boolean outputCookie) {
    m_saxHandler.setOutputCookie(outputCookie);
  }
  
  public final void setOutputOptions(HeaderOptionsOutputType outputOptions) throws EXIOptionsException {
    if (m_outputOptions != outputOptions) {
      if (outputOptions != HeaderOptionsOutputType.none) {
        if (m_exiOptions.getPreserveLexicalValues() && m_exiOptions.getDatatypeRepresentationMap() != null) {
          throw new EXIOptionsException("Preserve.lexicalValues option and datatypeRepresentationMap option cannot be specified together in EXI header options.");
        }
      }
      m_outputOptions = outputOptions;
    }
  }

  public final void setPreserveWhitespaces(boolean preserveWhitespaces) {
    m_saxHandler.setPreserveWhitespaces(preserveWhitespaces);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Methods for controlling Deflater parameters
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Set ZLIB compression level.
   * @param level the new compression level (0-9)
   * @see java.util.zip.Deflator#setLevel(int level)
   */
  public void setDeflateLevel(int level) {
    m_saxHandler.setDeflateLevel(level);
  }

  /**
   * Set ZLIB compression strategy.
   * @param strategy the new compression strategy
   * @see java.util.zip.Deflator#setStrategy(int strategy)
   */
  public void setDeflateStrategy(int strategy) {
    m_saxHandler.setDeflateStrategy(strategy);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Encode methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Parses XML input and convert to EXI stream.
   * @param is XML input
   */
  public void encode(InputSource is) throws TransmogrifierException, IOException {
    try {
      m_xmlReader.parse(is);
    }
    catch (SAXException se) {
      final Exception e;
      if ((e = se.getException()) != null) {
        if (e instanceof TransmogrifierException)
          throw (TransmogrifierException)e;
        else if (e instanceof IOException)
          throw (IOException)e;
      }
      //se.printStackTrace();
      throw new TransmogrifierException(TransmogrifierException.SAX_ERROR,
          new String[] { se.getMessage() }, (LocatorOnSAXParseException)null);
    }
  }
  
  public SAXTransmogrifier getSAXTransmogrifier() {
    reset();
    return m_saxHandler;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// SAX-based Encoder
  ///////////////////////////////////////////////////////////////////////////

  private final class SAXEventHandler implements SAXTransmogrifier {

    private final DocumentGrammarState m_documentState;
    
    private XMLLocusItemEx[] m_locusStack;
    private int m_locusLastDepth;
    private boolean m_inDTD;

    private static final byte STAG = 0; // start tag
    private static final byte CONT = 1; // non-whitespace content
    private static final byte ETAG = 2; // end tag
    private byte m_contentState;

    private GrammarCache m_grammarCache;
    private EXISchema m_schema;
    private boolean hasNS; // hasNS is subservient to m_grammarCache (derivative)

    private PrefixUriBindings m_prefixUriBindingsDefault;
    private PrefixUriBindings m_prefixUriBindings; // current bindings
    
    private char[] m_charBuf;
    private int m_charPos;
    
    private final TreeSet<ComparableAttribute> sortedAttributes;
    
    private final NamespaceDeclarations m_decls;
    private final QName qname;
    
    // scriber is to be created based on alignment type as necessary
    private Scriber m_scriber;  
    private ValueScriber m_stringValueScriber;
    
    private OutputStream m_outputStream;

    private Locator m_locator;
    
    private boolean m_outputCookie;
    
    private final EXIOptionsEncoder m_optionsEncoder;
    
    private final Scribble m_scribble;
    
    private int m_zlibLevel;
    private int m_zlibStrategy;
    
    private boolean m_preserveWhitespaces;
    
    SAXEventHandler() {
      m_grammarCache = null;
      m_schema = null;
      m_documentState = new DocumentGrammarState();
      m_locusStack = new XMLLocusItemEx[32];
      for (int i = 0; i < 32; i++) {
        m_locusStack[i] = new XMLLocusItemEx();
      }
      m_locusLastDepth = -1;
      m_prefixUriBindingsDefault = new PrefixUriBindings();
      m_prefixUriBindings = null;
      m_charBuf = new char[128];
      sortedAttributes = new TreeSet<ComparableAttribute>();
      m_decls = new NamespaceDeclarations();
      qname = new QName();
      m_scriber = ScriberFactory.createScriber(AlignmentType.bitPacked);
      m_scriber.setStringTable(new StringTable(m_schema));
      m_outputCookie = false;
      m_optionsEncoder = new EXIOptionsEncoder();
      m_scribble = new Scribble();
      m_zlibLevel = java.util.zip.Deflater.DEFAULT_COMPRESSION;
      m_zlibStrategy = java.util.zip.Deflater.DEFAULT_STRATEGY;
      m_preserveWhitespaces = false;
    }
    
    private void reset() {
      m_prefixUriBindings = null;
      m_locator = null;
      m_inDTD = false;
      m_contentState = ETAG;
    }
    
    public final void setAlignmentType(AlignmentType alignmentType) {
      if (m_scriber.getAlignmentType() != alignmentType) {
        m_scriber = ScriberFactory.createScriber(alignmentType);
        m_scriber.setSchema(m_schema, m_exiOptions.getDatatypeRepresentationMap(), m_exiOptions.getDatatypeRepresentationMapBindingsCount());
        m_scriber.setStringTable(new StringTable(m_schema));
        m_scriber.setValueMaxLength(m_exiOptions.getValueMaxLength());
        m_scriber.setPreserveLexicalValues(m_exiOptions.getPreserveLexicalValues());
      }
    }
    
    private void setEXISchema(GrammarCache grammarCache) {
      if (m_grammarCache != grammarCache) {
        m_grammarCache = grammarCache;
        final EXISchema schema;
        if ((schema = m_grammarCache.getEXISchema()) != m_schema) {
          m_schema = schema;
          m_scriber.setSchema(m_schema, m_exiOptions.getDatatypeRepresentationMap(), m_exiOptions.getDatatypeRepresentationMapBindingsCount());
          m_scriber.setStringTable(new StringTable(m_schema));
        }
      }
    }

    public final void setValueMaxLength(int valueMaxLength) {
      m_scriber.setValueMaxLength(valueMaxLength);
    }

    public final void setPreserveLexicalValues(boolean preserveLexicalValues) {
      m_scriber.setPreserveLexicalValues(preserveLexicalValues);
    }

    public final void setDatatypeRepresentationMap(QName[] datatypeRepresentationMap, int n_bindings) throws EXIOptionsException {
      m_scriber.setSchema(m_schema, datatypeRepresentationMap, n_bindings);
    }
    
    /**
     * Set an output stream to which encoded streams are written out.
     * @param ostream output stream
     */
    private void setOutputStream(OutputStream ostream) {
      m_outputStream = ostream;
    }

    void setPrefixUriBindings(PrefixUriBindings prefixUriBindings) {
      m_prefixUriBindingsDefault = prefixUriBindings != null ?  prefixUriBindings : new PrefixUriBindings();
    }

    private void setOutputCookie(boolean outputCookie) {
      m_outputCookie = outputCookie;
    }
    
    public void setDeflateLevel(int level) {
      m_zlibLevel = level;
    }

    public void setDeflateStrategy(int strategy) {
      m_zlibStrategy = strategy;
    }

    public final void setPreserveWhitespaces(boolean preserveWhitespaces) {
      m_preserveWhitespaces = preserveWhitespaces;
    }

    //////////// SAX event handlers

    public final void setDocumentLocator(final Locator locator) {
      m_locator = locator;
    }

    public final void startDocument() throws SAXException {
      m_locusLastDepth = -1;
      m_decls.clear();
      m_charPos = 0;
      try {
        Scriber.writeHeaderPreamble(m_outputStream, m_outputCookie, m_outputOptions != HeaderOptionsOutputType.none);
        BitOutputStream bitOutputStream = null;
        if (m_outputOptions != HeaderOptionsOutputType.none) {
          BitOutputStream outputStream;
          outputStream = m_optionsEncoder.encode(m_exiOptions, m_outputOptions == HeaderOptionsOutputType.all, m_outputStream);
          if (m_exiOptions.getAlignmentType() == AlignmentType.bitPacked) {
            bitOutputStream = outputStream;
          }
        }
        m_scriber.reset();
        if (bitOutputStream != null)
          ((BitPackedScriber)m_scriber).setBitOutputStream(bitOutputStream);
        else
          m_scriber.setOutputStream(m_outputStream);
        hasNS = GrammarOptions.hasNS(m_grammarCache.grammarOptions);
        m_scriber.setPreserveNS(hasNS);
        m_scriber.getStringTable().setValuePartitionCapacity(m_exiOptions.getValuePartitionCapacity());
        m_scriber.setBlockSize(m_exiOptions.getBlockSize());
        m_scriber.setDeflateParams(m_zlibLevel, m_zlibStrategy);
        
        m_stringValueScriber = m_scriber.getValueScriberByID(Scriber.CODEC_STRING);
        
        m_grammarCache.retrieveDocumentGrammar(m_exiOptions.isFragment(), m_documentState.eventTypesWorkSpace).init(m_documentState);
        m_prefixUriBindings = m_prefixUriBindingsDefault;
        
        EventTypeList eventTypes = m_documentState.getNextEventTypes();
        final EventType eventType;
        if ((eventType = eventTypes.getSD()) != null)
          m_documentState.startDocument();
        else {
          throw new SAXException(new TransmogrifierException(TransmogrifierException.UNEXPECTED_SD,
              (String[])null, new LocatorImpl(m_locator)));
        }
        m_scriber.writeEventType(eventType);
      }
      catch (IOException ioe) {
        throw new SAXException(ioe);
      }
    }

    public final void startPrefixMapping(final String prefix, final String uri)
      throws SAXException {
      if (prefix.length() != 0) {
        if (uri.length() != 0) {
          m_prefixUriBindings = m_prefixUriBindings.bind(prefix, uri);
          m_decls.addDecl(prefix, uri);
        }
        else {
          m_prefixUriBindings = m_prefixUriBindings.unbind(prefix);
          m_decls.addDecl(prefix, "");
        }
      }
      else {
        if (uri.length() != 0) {
          m_prefixUriBindings = m_prefixUriBindings.bindDefault(uri);
          m_decls.addDecl("", uri);
        }
        else {
          m_prefixUriBindings = m_prefixUriBindings.unbindDefault();
          m_decls.addDecl("", "");
        }
      }
    }

    public final void endPrefixMapping(final String prefix)
      throws SAXException {
    }
    
    public final void startElement(final String uri, final String localName, final String qualifiedName, Attributes attrs)
        throws SAXException {
      if (m_charPos > 0)
        do_characters(false);
      m_contentState = STAG;

      final String elementPrefix;
      final String[] nsdecls;
      final int n_nsdecls;
      if (hasNS) {
        nsdecls = m_decls.getDecls();
        if ((n_nsdecls = m_decls.getDeclsCount()) != 0) {
          m_decls.clear();
        }
        final int _pos;
        if ((_pos = qualifiedName.indexOf(':')) != -1)
          elementPrefix = qualifiedName.substring(0, _pos);
        else
          elementPrefix = "";
      }
      else {
        nsdecls = (String[])null;
        n_nsdecls = 0;
        elementPrefix = null;
      }
      if (++m_locusLastDepth == m_locusStack.length) {
        final int locusStackCapacity = m_locusLastDepth + 8;
        final XMLLocusItemEx[] locusStack = new XMLLocusItemEx[locusStackCapacity];
        System.arraycopy(m_locusStack, 0, locusStack, 0, m_locusLastDepth);
        for (int i = m_locusLastDepth; i < locusStackCapacity; i++) {
          locusStack[i] = new XMLLocusItemEx();
        }
        m_locusStack = locusStack;
      }
      final XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
      locusItem.prefixUriBindings = m_prefixUriBindings;
      locusItem.elementURI = uri;
      locusItem.elementLocalName = localName;
      try {
        EventTypeList eventTypes = m_documentState.getNextEventTypes();
        int i, j, i_len, j_len;
        EventType eventType = null;
        String eventTypeUri;
        byte itemType = -1; 
        boolean isUndeclaredElem = false;
        loop:
        for (i = 0, i_len = eventTypes.getLength(); i < i_len; i++) {
          eventType = eventTypes.item(i);
          switch (itemType = eventType.itemType) {
            case EventCode.ITEM_SCHEMA_SE:
            case EventCode.ITEM_SE:
              if (localName.equals(eventType.getName())) {
                eventTypeUri = eventType.getURI();
                if (!uri.equals(eventTypeUri))
                  continue;
                break loop;
              }
              break;
            case EventCode.ITEM_SCHEMA_WC_ANY:
              break loop;
            case EventCode.ITEM_SCHEMA_WC_NS:
              eventTypeUri = eventType.getURI();
              if (!uri.equals(eventTypeUri))
                continue;
              break loop;
            case EventCode.ITEM_SE_WC:
              isUndeclaredElem = true;
              break loop;
            default:
              break;
          }
        }
        final int epos = i;
        if (epos < i_len) {
          m_scriber.writeEventType(eventType);
          if (!isUndeclaredElem)
            m_documentState.startElement(epos, uri, localName);
          else
            m_documentState.startUndeclaredElement(uri, localName);
          final byte grammarType = m_documentState.currentState.targetGrammar.getGrammarType();
          assert grammarType == Grammar.SCHEMA_GRAMMAR_ELEMENT || grammarType == Grammar.BUILTIN_GRAMMAR_ELEMENT || 
            grammarType == Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT;
          m_scriber.writeQName(qname.setValue(uri, localName, elementPrefix), itemType);
          if (hasNS) {
            if (n_nsdecls != 0) {
              eventTypes = m_documentState.getNextEventTypes();
              for (i = 0, i_len = eventTypes.getLength(); i < i_len; i++) {
                eventType = eventTypes.item(i);
                if ((itemType = eventType.itemType) == EventCode.ITEM_NS) {
                  int pos;
                  for (i = 0, pos = 0; i < n_nsdecls; i++, pos += 2) {
                    m_scriber.writeEventType(eventType);
                    final String _prefix = nsdecls[pos];
                    m_scriber.writeNS(nsdecls[pos + 1], _prefix, elementPrefix.equals(_prefix));
                  }
                  break;
                }
              }
            }
          }
          else {
            assert elementPrefix == null;
          }

          boolean isSchemaInformedGrammar = m_documentState.currentState.targetGrammar.isSchemaInformed();
          
          int positionOfNil  = -1; // position of legitimate xsi:nil
          int positionOfType = -1; // position of legitimate xsi:type
          EventType eventTypeForType = null;
          int n_attrs;
          if ((n_attrs = attrs.getLength()) != 0) {
            sortedAttributes.clear();
            for (i = 0, i_len = n_attrs; i < i_len; i++) {
              if (URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI.equals(attrs.getURI(i))) {
                final String instanceName = attrs.getLocalName(i);
                if ("type".equals(instanceName)) {
                  eventTypes = m_documentState.getNextEventTypes();
                  iterateEventTypes:
                  for (j = 0, j_len = eventTypes.getLength(); j < j_len; j++) {
                    eventType = eventTypes.item(j);
                    switch (itemType = eventType.itemType) {
                      case EventCode.ITEM_SCHEMA_TYPE:
                        break iterateEventTypes;
                      case EventCode.ITEM_AT_WC_ANY_UNTYPED:
                        if (!isSchemaInformedGrammar)
                          break iterateEventTypes;
                        break;
                      case EventCode.ITEM_AT:
                        assert !isSchemaInformedGrammar;
                        if ("type".equals(eventType.getName()) && 
                            URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI.equals(eventType.getURI())) {
                          break iterateEventTypes;
                        }
                        break;
                    }
                  }
                  if (j != j_len) { // xsi:type had a matching event type
                    positionOfType = i;
                    eventTypeForType = eventType;
                    --n_attrs;
                    continue;
                  }
                  assert isSchemaInformedGrammar;
                  TransmogrifierException te;
                  te = new TransmogrifierException(TransmogrifierException.UNEXPECTED_ATTR, 
                      new String[] { "type", URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI  }, new LocatorImpl(m_locator));
                  throw new SAXException(te);
                }
                else if ("nil".equals(instanceName)) {
                  positionOfNil = i;
                  continue;
                }
              }
              final ComparableAttribute comparableAttribute;
              final String _prefix = hasNS ? getPrefixOfQualifiedName(attrs.getQName(i)) : null;
              comparableAttribute = new ComparableAttribute(attrs.getURI(i), attrs.getLocalName(i), _prefix, i);
              sortedAttributes.add(comparableAttribute);
            }
          }
          
          final EXISchema corpus = m_grammarCache.getEXISchema();
          if (positionOfType != -1) {
            m_scriber.writeEventType(eventTypeForType);
            m_scriber.writeQName(qname.setValue(URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "type", 
                hasNS ? getPrefixOfQualifiedName(attrs.getQName(positionOfType)) : null), itemType);
            final String xsiType = attrs.getValue(positionOfType);
            m_scriber.writeXsiTypeValue(setXsiTypeValue(qname, xsiType, m_prefixUriBindings));
            if (!isSchemaInformedGrammar && (itemType = eventTypeForType.itemType) == EventCode.ITEM_AT_WC_ANY_UNTYPED) {
              m_documentState.undeclaredAttribute(URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "type");
            }
            final String typeNamespaceName = qname.namespaceName;
            final int ns;
            if (corpus != null && (ns = corpus.getNamespaceOfSchema(typeNamespaceName)) != EXISchema.NIL_NODE) {
              int tp;
              if ((tp = corpus.getTypeOfNamespace(ns, qname.localName)) != EXISchema.NIL_NODE) {
                m_documentState.xsitp(tp);
                isSchemaInformedGrammar = true;
              }
            }
          }
          if (positionOfNil != -1) {
            final String nilval = attrs.getValue(positionOfNil);
            Boolean nilled = null;
            if (isSchemaInformedGrammar) {
              int length = nilval.length();
              int limit = length - 1;
              skipTrailingWhiteSpaces:
              for (; limit > 0; limit--) {
                switch (nilval.charAt(limit)) {
                  case '\t':
                  case '\n':
                  case '\r':
                  case ' ':
                    break;
                  default:
                    break skipTrailingWhiteSpaces;
                }
              }
              ++limit;
              int pos;
              skipWhiteSpaces:
              for (pos = 0; pos < length; pos++) {
                switch (nilval.charAt(pos)) {
                  case '\t':
                  case '\n':
                  case '\r':
                  case ' ':
                    break;
                  default:
                    break skipWhiteSpaces;
                }
              }
              if (pos != 0 || limit != length) {
                length = limit - pos;
              }
              eventTypes = m_documentState.getNextEventTypes();
              boolean nilAvailable = false;
              for (i = 0, i_len = eventTypes.getLength(); i < i_len; i++) {
                eventType = eventTypes.item(i);
                itemType = eventType.itemType;  
                if (itemType == EventCode.ITEM_SCHEMA_NIL) {
                  nilAvailable = true;
                  if (length == 4) {
                    if (nilval.charAt(pos) == 't' && nilval.charAt(pos + 1) == 'r' && nilval.charAt(pos + 2) == 'u' && 
                        nilval.charAt(pos + 3) == 'e') {
                      nilled = Boolean.TRUE;
                      break;
                    }
                  }
                  else if (length == 5) {
                    if (nilval.charAt(pos) == 'f' && nilval.charAt(pos + 1) == 'a' && nilval.charAt(pos + 2) == 'l' && 
                        nilval.charAt(pos + 3) == 's' && nilval.charAt(pos + 4) == 'e') {
                      nilled = Boolean.FALSE;
                      break;
                    }
                  }
                  else if (length == 1) {
                    final char c = nilval.charAt(pos);
                    if (c == '1') {
                      nilled = Boolean.TRUE;
                      break;
                    }
                    else if (c == '0') {
                      nilled = Boolean.FALSE;
                      break;
                    }
                  }
                }
                else if (itemType == EventCode.ITEM_AT_WC_ANY_UNTYPED) {
                  assert nilAvailable;
                  break;
                }
              }
              if (i == i_len) {
                TransmogrifierException te;
                te = new TransmogrifierException(TransmogrifierException.UNEXPECTED_ATTR, 
                    new String[] { "nil", URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI  }, new LocatorImpl(m_locator));
                throw new SAXException(te);
              }
            }
            if (nilled != null) {
              m_scriber.writeEventType(eventType);
              m_scriber.writeQName(qname.setValue(URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "nil", 
                  hasNS ? getPrefixOfQualifiedName(attrs.getQName(positionOfNil)) : null), itemType);
              m_scriber.writeXsiNilValue(nilled, nilval);
              if (nilled) {
                m_documentState.nillify();
              }
              --n_attrs;
            }
            else {
              if (isSchemaInformedGrammar) {
                assert itemType == EventCode.ITEM_AT_WC_ANY_UNTYPED; 
                // process invalid xsi:nil value using AT(*) [untyped value]
                m_scriber.writeEventType(eventType);
                m_scriber.writeQName(qname.setValue(URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "nil", 
                    hasNS ? getPrefixOfQualifiedName(attrs.getQName(positionOfNil)) : null), itemType);
                m_stringValueScriber.scribe(nilval, m_scribble, "nil", URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, EXISchema.NIL_NODE);
                --n_attrs;
              }
              else {
                // treat it as a vanilla attribute
                final ComparableAttribute comparableAttribute;
                final String _prefix = hasNS ? getPrefixOfQualifiedName(attrs.getQName(positionOfNil)) : null;
                comparableAttribute = new ComparableAttribute(attrs.getURI(positionOfNil), attrs.getLocalName(positionOfNil), _prefix, positionOfNil);
                sortedAttributes.add(comparableAttribute);
              }
            }
          }
          if (n_attrs != 0) {
            final Iterator<ComparableAttribute> iter; 
            for (i = 0, iter = sortedAttributes.iterator(); i < n_attrs; i++) {
              eventTypes = m_documentState.getNextEventTypes();
              final ComparableAttribute attr = iter.next();
              final String instanceUri  = attr.uri;
              final String instanceName = attr.name;
              int tp = EXISchema.NIL_NODE;
              ValueScriber valueScriber = null;
              if (isSchemaInformedGrammar) {
                eventType = eventTypes.getSchemaAttribute(instanceUri, instanceName);
                if (eventType != null) {
                  final EventTypeSchemaAttribute eventTypeSchemaAttribute;
                  eventTypeSchemaAttribute = (EventTypeSchemaAttribute)eventType;
                  final int index;
                  if (eventTypeSchemaAttribute.useSpecificType()) {
                    tp = corpus.getTypeOfAttr(eventTypeSchemaAttribute.getSchemaSubstance());
                    assert tp != EXISchema.NIL_NODE;
                    valueScriber = m_scriber.getValueScriber(tp);
                    if (valueScriber.process(attrs.getValue(attr.index), tp, m_schema, m_scribble)) {
                      index = eventType.getIndex();
                    }
                    else {
                      valueScriber = m_stringValueScriber;
                      eventType = eventTypes.getSchemaAttributeInvalid(instanceUri, instanceName);
                      index = ((EventTypeSchema)eventType).serial;
                      tp = EXISchema.NIL_NODE;
                    }
                  }
                  else {
                    valueScriber = m_stringValueScriber;
                    index = eventType.getIndex();
                  }
                  m_documentState.attribute(index, instanceUri, instanceName);
                }
                else {
                  if ((eventType = eventTypes.getSchemaAttributeWildcardNS(instanceUri)) == null) {
                    eventType = eventTypes.getSchemaAttributeWildcardAny();
                  }
                  if (eventType != null) {
                    final int ns;
                    if ((ns = corpus.getNamespaceOfSchema(instanceUri)) != EXISchema.NIL_NODE) {
                      final int _attr;
                      if ((_attr = corpus.getAttrOfNamespace(ns, instanceName)) != EXISchema.NIL_NODE) {
                        tp = corpus.getTypeOfAttr(_attr);
                        assert tp != EXISchema.NIL_NODE;
                        valueScriber = m_scriber.getValueScriber(tp);
                        if (valueScriber.process(attrs.getValue(attr.index), tp, m_schema, m_scribble)) {
                          m_documentState.undeclaredAttribute(instanceUri, instanceName);                          
                        }
                        else {
                          tp = EXISchema.NIL_NODE; // revert variable tp back to NIL_NODE
                          eventType = null;
                        }
                      }
                      else
                        valueScriber = m_stringValueScriber;
                    }
                    else
                      valueScriber = m_stringValueScriber;
                  }
                  if (eventType == null && (eventType = eventTypes.getAttributeWildcardAnyUntyped()) != null)
                    valueScriber = m_stringValueScriber;
                  assert eventType == null || eventType != null && valueScriber != null;
                }
              }
              else { // built-in grammar
                if ((eventType = eventTypes.getAttribute(instanceUri, instanceName)) != null ||
                    (eventType = eventTypes.getAttributeWildcardAnyUntyped()) != null) {
                  valueScriber = m_stringValueScriber;
                }
              }
              if (eventType != null) {
                m_scriber.writeEventType(eventType);
                qname.setValue(instanceUri, instanceName, attr.prefix);
                m_scriber.writeQName(qname, eventType.itemType);
                valueScriber.scribe(attrs.getValue(attr.index), m_scribble, instanceName, instanceUri, tp);
                if (eventType.itemType == EventCode.ITEM_AT_WC_ANY_UNTYPED)
                  m_documentState.undeclaredAttribute(instanceUri, instanceName);                          
              }
              else {
                TransmogrifierException te;
                te = new TransmogrifierException(TransmogrifierException.UNEXPECTED_ATTR, 
                    new String[] { instanceName, instanceUri }, new LocatorImpl(m_locator));
                throw new SAXException(te);
              }
            }
          }
        }
        else {
          TransmogrifierException te;
          te = new TransmogrifierException(TransmogrifierException.UNEXPECTED_ELEM, new String[] { localName, uri  }, new LocatorImpl(m_locator));
          throw new SAXException(te);
        }
      }
      catch (IOException ioe) {
        throw new SAXException(ioe.getMessage(), ioe);
      }
    }

    public final void ignorableWhitespace(final char[] ch, final int start, final int len)
      throws SAXException {
      appendCharacters(ch, start, len);
    }
    
    public final void characters(final char[] ch, final int start, final int len)
      throws SAXException {
      appendCharacters(ch, start, len);
    }

    /**
     * Process characters. 
     * @param isContent true when the characters need to be treated as a content.
     * @throws SAXException
     */
    private void do_characters(boolean isContent) throws SAXException {
      isContent = isContent || m_contentState == CONT;
      try {
        final EventTypeList eventTypes;
        eventTypes = m_documentState.getNextEventTypes();
        int tp = EXISchema.NIL_NODE;
        EventType eventType;
        if ((eventType = eventTypes.getSchemaCharacters()) != null) {
          tp = ((EventTypeSchema)eventType).getSchemaSubstance();
          assert tp != EXISchema.NIL_NODE;
          ValueScriber valueScriber = m_scriber.getValueScriber(tp);
          // REVISIT: avoid converting to string.
          final String stringValue = new String(m_charBuf, 0, m_charPos);
          if (valueScriber.process(stringValue, tp, m_schema, m_scribble)) {
            m_scriber.writeEventType(eventType);
            m_documentState.characters();
            final XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
            valueScriber.scribe(stringValue, m_scribble, locusItem.elementLocalName, locusItem.elementURI, tp);
            m_charPos = 0;
            return;
          }
        }
        if ((eventType = eventTypes.getCharacters()) != null) {
          boolean preserveWhitespaces = m_preserveWhitespaces;
          if (!preserveWhitespaces) {
            if (tp != EXISchema.NIL_NODE)
              preserveWhitespaces = true;
            else {
              if (m_documentState.currentState.targetGrammar.isSchemaInformed())
                preserveWhitespaces = eventType.itemType == EventCode.ITEM_SCHEMA_CH_MIXED; 
              else
                preserveWhitespaces = isContent;
            }
          }
          if (preserveWhitespaces) {
            m_scriber.writeEventType(eventType);
            m_documentState.undeclaredCharacters();
            final XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
            m_stringValueScriber.scribe(new String(m_charBuf, 0, m_charPos), m_scribble,
                locusItem.elementLocalName, locusItem.elementURI, EXISchema.NIL_NODE);
          }
          else {
            final int len = m_charPos;
            iLoop:
            for (int i = 0; i < len; i++) {
              switch (m_charBuf[i]) {
                case '\t':
                case '\n':
                case '\r':
                case ' ':
                  continue;
                default:
                  m_scriber.writeEventType(eventType);
                  m_documentState.undeclaredCharacters();
                  final XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
                  m_stringValueScriber.scribe(new String(m_charBuf, 0, m_charPos), m_scribble,
                      locusItem.elementLocalName, locusItem.elementURI, EXISchema.NIL_NODE);
                  break iLoop;
              }
            }
          }
          m_charPos = 0;
          return;
        }
        final int len = m_charPos;
        for (int i = 0; i < len; i++) {
          switch (m_charBuf[i]) {
            case '\t':
            case '\n':
            case '\r':
            case ' ':
              continue;
            default:
              TransmogrifierException te;
              te = new TransmogrifierException(TransmogrifierException.UNEXPECTED_CHARS, 
                  new String[] { new String(m_charBuf, 0, m_charPos) }, new LocatorImpl(m_locator));
              throw new SAXException(te);
          }
        }
      }
      catch (IOException ioe) {
        throw new SAXException(ioe);
      }
      m_charPos = 0;
    }
    
    public final void endElement(final String uri, final String localName, final String qname)
      throws SAXException {
      if (m_charPos > 0)
        do_characters(m_contentState == STAG);
      m_contentState = ETAG;
      try {
        final EventTypeList eventTypes;
        eventTypes = m_documentState.getNextEventTypes();

        EventType eventType = null;
        if ((eventType = eventTypes.getEE()) != null) {
          m_scriber.writeEventType(eventType);
          m_documentState.endElement(uri, localName);
        }
        else {
          assert m_documentState.currentState.targetGrammar.isSchemaInformed();
          if ((eventType = eventTypes.getSchemaCharacters()) != null) {
            final int tp = ((EventTypeSchema)eventType).getSchemaSubstance();
            assert tp != EXISchema.NIL_NODE;
            ValueScriber valueScriber = m_scriber.getValueScriber(tp);
            if (valueScriber.process("", tp, m_schema, m_scribble)) {
              m_scriber.writeEventType(eventType);
              m_documentState.characters();
              final XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
              valueScriber.scribe("", m_scribble, locusItem.elementLocalName, locusItem.elementURI, tp);
              endElement(uri, localName, qname);
              return; // Good luck!
            }
          }
          throw new SAXException(new TransmogrifierException(TransmogrifierException.UNEXPECTED_END_ELEM,
              new String[] { localName, uri }, new LocatorImpl(m_locator)));
        }
      }
      catch (IOException ioe) {
        throw new SAXException(ioe);
      }
      
      m_prefixUriBindings = m_locusLastDepth-- != 0 ? m_locusStack[m_locusLastDepth].prefixUriBindings : m_prefixUriBindingsDefault;  
    }

    public final void endDocument() throws SAXException {
      if (m_charPos > 0)
        do_characters(false);
      try {
        final EventTypeList eventTypes = m_documentState.getNextEventTypes();
        final EventType eventType;
        if ((eventType = eventTypes.getED()) != null) {
          m_documentState.endDocument();
        }
        else {
          throw new SAXException(new TransmogrifierException(TransmogrifierException.UNEXPECTED_ED,
              (String[])null, new LocatorImpl(m_locator)));
        }
        m_scriber.writeEventType(eventType);
        m_scriber.finish();
      }
      catch (IOException ioe) {
        throw new SAXException(ioe);
      }
    }
    
    public final void processingInstruction(final String target, final String data)
      throws SAXException {
      if (m_exiOptions.getPreservePIs()) {
        if (m_charPos > 0)
          do_characters(true);
        final EventTypeList eventTypes = m_documentState.getNextEventTypes();
        final int len = eventTypes.getLength();
        for (int i = 0; i < len; i++) {
          final EventType eventType = eventTypes.item(i);
          if (eventType.itemType == EventCode.ITEM_PI) {
              m_documentState.miscContent();
              try {
                m_scriber.writeEventType(eventType);
                m_scriber.writeText(target);
                m_scriber.writeText(data);
              }
              catch (IOException ioe) {
                throw new SAXException(ioe);
              }
              m_contentState = CONT;
              break;
          }
        }
      }
    }

    public final void skippedEntity(final String name)
      throws SAXException {
      if (m_exiOptions.getPreserveDTD()) {
        if (m_charPos > 0)
          do_characters(true);
        final EventTypeList eventTypes = m_documentState.getNextEventTypes();
        final int len = eventTypes.getLength();
        for (int i = 0; i < len; i++) {
          final EventType eventType = eventTypes.item(i);
          if (eventType.itemType == EventCode.ITEM_ER) {
            m_documentState.miscContent();
            try {
              m_scriber.writeEventType(eventType);
              m_scriber.writeText(name);
            }
            catch (IOException ioe) {
              throw new SAXException(ioe);
            }
            m_contentState = CONT;
            break;
          }
        }
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // LexicalHandler APIs
    ///////////////////////////////////////////////////////////////////////////

    public final void comment(char[] ch, int start, int length) 
      throws SAXException {
      if (!m_inDTD && m_exiOptions.getPreserveComments()) {
        if (m_charPos > 0)
          do_characters(true);
        final EventTypeList eventTypes = m_documentState.getNextEventTypes();
        final int len = eventTypes.getLength();
        for (int i = 0; i < len; i++) {
          EventType eventType = eventTypes.item(i);
          if (eventType.itemType == EventCode.ITEM_CM) {
            m_documentState.miscContent();
            try {
              m_scriber.writeEventType(eventType);
              m_scriber.writeText(new String(ch, start, length));
            }
            catch (IOException ioe) {
              throw new SAXException(ioe);
            }
            m_contentState = CONT;
            break;
          }
        }
      }
    }

    public void startDTD(String name, String publicId, String systemId) 
      throws SAXException {
      EventTypeList eventTypes = m_documentState.getNextEventTypes();
      int i, len;
      EventType eventType = null;
      for (i = 0, len = eventTypes.getLength(); i < len; i++) {
        eventType = eventTypes.item(i);
        if (eventType.itemType == EventCode.ITEM_DTD) {
            break;
        }
      }
      if (i < len) {
        try {
          m_scriber.writeEventType(eventType);
          m_scriber.writeText(name);
          m_scriber.writeText(publicId != null ? publicId : "");
          m_scriber.writeText(systemId != null ? systemId : "");
          m_scriber.writeText("");
        }
        catch (IOException ioe) {
          throw new SAXException(ioe);
        }
      }
      m_inDTD = true;
    }
    
    public void endDTD() {
      m_inDTD = false;
    }

    public void startCDATA() {
    }
    
    public void endCDATA() {
    }
    
    public void startEntity(String name) {
    }

    public void endEntity(String name) {
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Private convenience functions
    ///////////////////////////////////////////////////////////////////////////
    
    private void appendCharacters(final char[] ch, final int start, final int len) {
      while (m_charPos + len > m_charBuf.length) {
        final char[] charBuf = new char[2 * m_charBuf.length];
        System.arraycopy(m_charBuf, 0, charBuf, 0, m_charBuf.length);
        m_charBuf = charBuf;
      }
      System.arraycopy(ch, start, m_charBuf, m_charPos, len);
      m_charPos += len;
    }
  }

  private final static String getPrefixOfQualifiedName(String qualifiedName) { 
    final int pos;
    if ((pos = qualifiedName.indexOf(':')) != -1)
      return qualifiedName.substring(0, pos);
    else
      return "";
  }

  private static QName setXsiTypeValue(QName qname, String qualifiedName, PrefixUriBindings namespacePrefixMap) {
    qname.qName = qualifiedName;
    final int i = qualifiedName.indexOf(':');
    if (i != -1) { // with prefix
      qname.prefix = qualifiedName.substring(0, i);
      qname.namespaceName = namespacePrefixMap.getUri(qname.prefix);
      if (qname.namespaceName != null) {
        qname.localName = qualifiedName.substring(i + 1);
      }
      else { //  prefix did not resolve into an uri  
        qname.namespaceName = "";
        qname.localName = qualifiedName;
        qname.prefix = "";
      }
    }
    else { // no prefix
      qname.localName = qualifiedName;
      qname.namespaceName = namespacePrefixMap.getDefaultUri();
      qname.prefix = "";
    }
    return qname;
  }

  private static class ComparableAttribute implements Comparable<ComparableAttribute> {
    final byte priority; // 0 ... 2 ( 0 -> xsi:type, 1 -> xsi:nil, 2 -> other) 
    final String uri;
    final String name;
    final String prefix;
    final int index;
    ComparableAttribute(String uri, String name, String prefix, int index) {
      this.uri = uri;
      this.name = name;
      this.index = index;
      this.prefix = prefix;
      byte priority = 2;
      if (uri.equals(URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI)) {
        assert !"type".equals(name); 
        if ("nil".equals(name))
          priority = 1;
      }
      this.priority = priority;
    }
    public int compareTo(ComparableAttribute other) {
      int res;
      if ((res = priority - other.priority) != 0)
        return res;
      if ((res = name.compareTo(other.name)) != 0)
        return res;
      else {
        return uri.compareTo(other.uri);
      }
    }
  }
  
  private final class NamespaceDeclarations {
    private String[] decls; // new pairs of prefixes and namespaces
    private int n_decls;
    
    public NamespaceDeclarations() {
      decls = new String[16];
      n_decls = 0;
    }

    public final String[] getDecls() {
      return decls;
    }
     
    public final int getDeclsCount() {
      return n_decls;
    }
    
    public void addDecl(String prefix, String uri) {
      final String[] pairs;
      final int n_strings;
      if ((n_strings = n_decls << 1) != decls.length)
        pairs = decls;
      else {
        pairs = new String[2 * n_strings];
        System.arraycopy(decls, 0, pairs, 0, n_strings);
      }
      pairs[n_strings] = prefix;
      pairs[n_strings + 1] = uri;
      
      decls = pairs;
      ++n_decls;
    }
    
    public final void clear() {
      n_decls = 0;
    }
  }
  
  private static SAXParserFactory createSAXParserFactory() throws TransmogrifierException {
    final SAXParserFactory saxParserFactory;
    saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setNamespaceAware(true);
    return saxParserFactory;
  }
  
  private static class XMLLocusItemEx {
    String elementURI;
    String elementLocalName;
    PrefixUriBindings prefixUriBindings;
  }
  
}
