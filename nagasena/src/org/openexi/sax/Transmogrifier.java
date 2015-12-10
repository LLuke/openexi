package org.openexi.sax;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.LocatorImpl;

import org.openexi.proc.EXIOptionsEncoder;
import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.SchemaId;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.grammars.EventTypeSchema;
import org.openexi.proc.grammars.Grammar;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.BitPackedScriber;
import org.openexi.proc.io.BitOutputStream;
import org.openexi.proc.io.BinaryDataSink;
import org.openexi.proc.io.PrefixUriBindings;
import org.openexi.proc.io.Scriber;
import org.openexi.proc.io.Scribble;
import org.openexi.proc.io.ScriberRuntimeException;
import org.openexi.proc.io.ScriberFactory;
import org.openexi.proc.io.ValueScriber;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.EmptySchema;
/**
 * The Transmogrifier converts an XML stream to an EXI stream.
 */
public final class Transmogrifier {

  private final XMLReader m_xmlReader;
  /** 
   * EXIEncoderSaxHandler handles SAX events coming from XMLReader. 
   **/
  private final SAXEventHandler m_saxHandler;
  
  private HeaderOptionsOutputType m_outputOptions;
  private final EXIOptions m_exiOptions;
  
  private boolean m_divertBuiltinGrammarToAnyType;
  
  private static final SchemaId SCHEMAID_NO_SCHEMA;
  private static final SchemaId SCHEMAID_EMPTY_SCHEMA;
  static {
    SCHEMAID_NO_SCHEMA = new SchemaId((String)null);
    SCHEMAID_EMPTY_SCHEMA = new SchemaId(""); 
  }

  /**
   * Create an instance of the Transmogrifier with a default SAX parser.
   * @throws TransmogrifierException
   */
  public Transmogrifier() throws TransmogrifierRuntimeException {
    this(false);
  }
  
  /**
   * Create an instance of the Transmogrifier, specifying the SAXParserFactory
   * from which to create the SAX parser.
   * @param saxParserFactory
   * @throws TransmogrifierException
   */
  public Transmogrifier(SAXParserFactory saxParserFactory) throws TransmogrifierRuntimeException {
    this(saxParserFactory, false);
  }
  
  Transmogrifier(boolean namespacePrefixesFeature) throws TransmogrifierRuntimeException {
    this(createSAXParserFactory(), namespacePrefixesFeature);
  }

  Transmogrifier(SAXParserFactory saxParserFactory, boolean namespacePrefixesFeature) throws TransmogrifierRuntimeException {
    if (!saxParserFactory.isNamespaceAware()) {
      throw new TransmogrifierRuntimeException(TransmogrifierRuntimeException.SAXPARSER_FACTORY_NOT_NAMESPACE_AWARE, 
          (String[])null);
    }
    // fixtures
    m_saxHandler = new SAXEventHandler();
    try {
      final SAXParser saxParser = saxParserFactory.newSAXParser();
      m_xmlReader = saxParser.getXMLReader();
      m_xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", namespacePrefixesFeature);
    }
    catch (Exception exc) {
      throw new TransmogrifierRuntimeException(TransmogrifierRuntimeException.XMLREADER_ACCESS_ERROR, 
          (String[])null);
    }
    m_xmlReader.setContentHandler(m_saxHandler);
    try {
      m_xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", m_saxHandler);
    }
    catch (SAXException se) {
      TransmogrifierRuntimeException te;
      te = new TransmogrifierRuntimeException(TransmogrifierRuntimeException.UNHANDLED_SAXPARSER_PROPERTY, 
          new String[] { "http://xml.org/sax/properties/lexical-handler" });
      te.setException(se);
      throw te;
    }
    /*
     * REVISIT: we *may* (or may not) eventually want to support internal DTD subset.
     * m_xmlReader.setProperty("http://xml.org/sax/properties/declaration-handler", saxHandler);
     */
    m_outputOptions = HeaderOptionsOutputType.none;
    m_exiOptions = new EXIOptions();
    m_divertBuiltinGrammarToAnyType = false;
  }

  /**
   * Change the way a Transmogrifier handles external general entities. When the value
   * of resolveExternalGeneralEntities is set to true, a Transmogrifier will try to 
   * resolve external general entities. Otherwise, external general entities will not
   * be resolved.
   * @param resolveExternalGeneralEntities  
   * @throws TransmogrifierException Thrown when the underlying XMLReader does not 
   * support the specified behavior.
   */
  public void setResolveExternalGeneralEntities(boolean resolveExternalGeneralEntities) throws TransmogrifierException {
    try {
      m_xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", resolveExternalGeneralEntities);
    }
    catch (SAXException se) {
      TransmogrifierException te;
      te = new TransmogrifierException(TransmogrifierException.UNHANDLED_SAXPARSER_FEATURE, 
          new String[] { "http://xml.org/sax/features/external-general-entities" });
      te.setException(se);
      throw te;
    }
  }
  /**
   * @y.exclude
   */
  public void setPrefixUriBindings(PrefixUriBindings prefixUriBindings) {
    m_saxHandler.setPrefixUriBindings(prefixUriBindings);
  }

  private void reset() {
    m_saxHandler.reset();
  }

  /**
   * Set an output stream to which encoded streams are written.
   * @param ostream output stream
   */
  public final void setOutputStream(OutputStream ostream) {
    m_saxHandler.setOutputStream(ostream);
  }

  /** 
   * Set the bit alignment style for the encoded EXI stream.
   * @param alignmentType {@link org.openexi.proc.common.AlignmentType}. 
   * Default is <i>bit-packed</i>.
   * @throws EXIOptionsException
   */
  public final void setAlignmentType(AlignmentType alignmentType) throws EXIOptionsException {
    if (alignmentType == AlignmentType.compress && m_saxHandler.m_observeC14N) {
      throw new EXIOptionsException("Alignment type \"compression\" cannot be used with Canonical EXI.");
    }
    m_exiOptions.setAlignmentType(alignmentType);
    m_saxHandler.setAlignmentType(alignmentType);
  }
  /** 
   * Set to true if the XML input stream is an XML fragment (a non-compliant
   * XML document with multiple root elements).
   * @param isFragment true if the XML input stream is an XML fragment.
   */  
  public final void setFragment(boolean isFragment) {
    m_exiOptions.setFragment(isFragment);
  }
  /**
   * Set the size, in number of values, of the information that will be 
   * processed as a chunk of the entire XML stream. Reducing the block size 
   * can improve performance for devices with limited dynamic memory. 
   * Default is 1,000,000 items (not 1MB, but 1,000,000 complete Attribute 
   * and Element values). Block size is only used when the EXI stream is
   * encoded with EXI-compression.
   * @param blockSize number of values in each processing block. Default is 1,000,000.
   * @throws EXIOptionsException
   */  
  public final void setBlockSize(int blockSize) throws EXIOptionsException {
    m_exiOptions.setBlockSize(blockSize);
  }
  /**
   * Set the maximum length of a string that will be stored for reuse in the
   * String Table. By default, there is no maximum length. However, in data
   * sets that have long, unique strings of information, you can improve
   * performance by limiting the size to the length of strings that are more
   * likely to appear more than once.
   * @param valueMaxLength maximum length of entries in the String Table. 
   */

  public final void setValueMaxLength(int valueMaxLength) {
    m_exiOptions.setValueMaxLength(valueMaxLength);
    m_saxHandler.setValueMaxLength(valueMaxLength);
  }
  /**
   * Set the maximum number of values in the String Table. By default, there
   * is no limit. If the target device has limited dynamic memory, limiting 
   * the number of entries in the String Table can improve performance and
   * reduce the likelihood that you will exceed memory capacity.
   * @param valuePartitionCapacity maximum number of entries in the String Table
   */  
  public final void setValuePartitionCapacity(int valuePartitionCapacity) {
    m_exiOptions.setValuePartitionCapacity(valuePartitionCapacity);
  }
  
  public final void setDivertBuiltinGrammarToAnyType(boolean divertBuiltinGrammarToAnyType) {
    m_divertBuiltinGrammarToAnyType = divertBuiltinGrammarToAnyType;
  }
  
  /**
   * Set to <i>true</i> to preserve the original string values from the XML
   * stream. For example, a date string might be converted to a different
   * format when interpreted by the Transmogrifier. Preserving the lexical values
   * ensures that the identical strings are restored, and not just their 
   * logical values.
   * 
   * @param preserveLexicalValues <i>true</i> to keep original strings intact
   * @throws EXIOptionsException
   */  
  public final void setPreserveLexicalValues(boolean preserveLexicalValues) throws EXIOptionsException {
    if (m_exiOptions.getPreserveLexicalValues() != preserveLexicalValues) {
      if (m_outputOptions != HeaderOptionsOutputType.none && m_exiOptions.getDatatypeRepresentationMapBindingsCount() != 0 && preserveLexicalValues) {
        throw new EXIOptionsException("Preserve.lexicalValues option and datatypeRepresentationMap option cannot be specified together in EXI header options.");
      }
      m_exiOptions.setPreserveLexicalValues(preserveLexicalValues);
      m_saxHandler.setPreserveLexicalValues(preserveLexicalValues);
    }
  }
  
  /**
   * Set the GrammarCache used in transmogrifying XML data to EXI. 
   * @param grammarCache {@link org.openexi.proc.grammars.GrammarCache}
   * @throws EXIOptionsException
   */  
  public final void setGrammarCache(GrammarCache grammarCache) throws EXIOptionsException {
    setGrammarCache(grammarCache, (SchemaId)null);
  }
  
  /**
   * Set the GrammarCache to be used in encoding XML streams into EXI streams 
   * by the transmogrifier. 
   * The SchemaId contains the string that is written in the header when
   * <i>HeaderOptionsOutputType.all</i> is set.
   * @param grammarCache {@link org.openexi.proc.grammars.GrammarCache}
   * @param schemaId 
   * @throws EXIOptionsException
   */  
  public final void setGrammarCache(GrammarCache grammarCache, SchemaId schemaId) throws EXIOptionsException {
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
    m_saxHandler.setGrammarCache(grammarCache);
  }

  /**
   * Returns the GrammarCache that was previously set. 
   * @return a GrammarCache
   */
  public final GrammarCache getGrammarCache() {
    return m_saxHandler.getGrammarCache();
  }
  
  /**
   * Set a datatype representation map (DTRM). The DTRM allows you to remap
   * XMLSchema datatypes to EXI datatypes other than their default equivalents.
   * The map is created using a sequence of Qualified Name pairs that identify
   * a datatype definition in the XMLSchema namespace followed by the new 
   * corresponding datatype mapping in the EXI namespace.
   * <br /><br />
   * For example, the following lines map the boolean datatype from XMLSchema 
   * to the integer datatype in EXI.
   * <pre>
   *   QName q1 = new QName("xsd:boolean","http://www.w3.org/2001/XMLSchema");
   *   QName q2 = new QName("exi:integer","http://www.w3.org/2009/exi");
   *   QName[] dtrm = new QName[2];
   *   dtrm = {q1, q2}; // Each mapping requires 2 qualified names.
   *   transmogrifierInstance.setDatatypeRepresentationMap(dtrm, 1); // The array, and the number of pairs (1).
   * </pre>
   * 
   * @param dtrm a sequence of pairs of datatype QName and datatype representation QName
   * @param n_bindings the number of QName pairs
   */  
  public final void setDatatypeRepresentationMap(QName[] dtrm, int n_bindings) throws EXIOptionsException {
    if (!QName.isSame(m_exiOptions.getDatatypeRepresentationMap(), m_exiOptions.getDatatypeRepresentationMapBindingsCount(), dtrm, n_bindings)) {
      if (m_outputOptions != HeaderOptionsOutputType.none && m_exiOptions.getPreserveLexicalValues() && dtrm != null) {
        throw new EXIOptionsException("Preserve.lexicalValues option and datatypeRepresentationMap option cannot be specified together in EXI header options.");
      }
      m_exiOptions.setDatatypeRepresentationMap(dtrm, n_bindings);
      m_saxHandler.setDatatypeRepresentationMap(dtrm, n_bindings);
    }
  }
  /**
   * Set an external SAX entity resolver.
   * @param entityResolver {@link org.xml.sax.EntityResolver}
   */
  public final void setEntityResolver(EntityResolver entityResolver) {
    m_xmlReader.setEntityResolver(entityResolver);
  }

  /**
   * Tells the encoder whether to or not to start the stream by
   * adding an EXI cookie.
   * @param outputCookie <i>true</i> to include the EXI cookie
   */
  public final void setOutputCookie(boolean outputCookie) {
    m_saxHandler.setOutputCookie(outputCookie);
  }
  /**
   * Set the header output options. Choices are set using the 
   * {@link org.openexi.proc.HeaderOptionsOutputType} enumeration.
   * Options are <i>all, lessSchemaID</i> (that is, all values
   * except for the SchemaId), or <i>none.</i>
   * @param outputOptions {@link org.openexi.proc.HeaderOptionsOutputType}
   * @throws EXIOptionsException
   */
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
  
  /**
   * Set to true to preserve whitespace (for example, spaces, tabs, and
   * line breaks) in the encoded EXI stream. By default, non-essential whitespace
   * is removed from the encoded stream.
   * @param preserveWhitespaces <i>true</i> to retain whitespace in the encoded EXI stream
   */
  public final void setPreserveWhitespaces(boolean preserveWhitespaces) {
    m_saxHandler.setPreserveWhitespaces(preserveWhitespaces);
  }
  
  /**
   * Set to true to have the transmogrifier observe EXI Canonicalization
   * encoding rules.
   * @param observeC14N <i>true</i> to enable EXI Canonicalization encoding rules
   */
  public final void setObserveC14N(boolean observeC14N) throws EXIOptionsException {
    if (m_saxHandler.m_observeC14N && m_exiOptions.getAlignmentType() == AlignmentType.compress) {
      throw new EXIOptionsException("Canonical EXI cannot be used when alignment type is \"compression\"");
    }
    m_saxHandler.setObserveC14N(observeC14N);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Methods for controlling Deflater parameters
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Set ZLIB compression level.
   * @param level the new compression level (0-9)
   * @see java.util.zip.Deflator#setLevel(int level)
   * Not for public use.
   * @y.exclude
   */
  public void setDeflateLevel(int level) {
    m_saxHandler.setDeflateLevel(level);
  }

  /**
   * Set ZLIB compression strategy.
   * @param strategy the new compression strategy
   * @see java.util.zip.Deflator#setStrategy(int strategy)
   * Not for public use.
   * @y.exclude
   */
  public void setDeflateStrategy(int strategy) {
    m_saxHandler.setDeflateStrategy(strategy);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Encode methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Parses XML input source and converts it to an EXI stream.
   * @param is XML input source
   */
  public void encode(InputSource is) throws TransmogrifierException, IOException {
    reset();
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
      else {
        LocatorImpl locator = null;
        if (se instanceof SAXParseException) {
          SAXParseException spe = (SAXParseException)se;
          locator = new LocatorImpl();
          locator.setSystemId(spe.getSystemId());
          locator.setLineNumber(spe.getLineNumber());
          locator.setColumnNumber(spe.getColumnNumber());
        }
        //se.printStackTrace();
        throw new TransmogrifierException(TransmogrifierException.SAX_ERROR,
            new String[] { se.getMessage() }, locator);
      }
    }
  }
  
  /**
   * Returns the SAXTransmogrifier, which implements both the ContentHandler
   * and LexicalHandler. SAX programmers can connect the SAXTransmogrifier to
   * their favorite XML Parser to convert SAX events into an EXI stream.
   */
  public SAXTransmogrifier getSAXTransmogrifier() {
    reset();
    return m_saxHandler;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// SAX-based Encoder
  ///////////////////////////////////////////////////////////////////////////

  private final class SAXEventHandler implements SAXTransmogrifier {

    private static final String W3C_2000_XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    private XMLLocusItemEx[] m_locusStack;
    private int m_locusLastDepth;
    private boolean m_inDTD;
    
    private boolean[] m_xmlSpaceStack;
    private int m_xmlSpaceLastDepth;

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
    
    private ComparableAttribute[] m_comparableAttributes;
    private int m_n_comparableAttributes;
    
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
    private boolean m_observeC14N;
    
    SAXEventHandler() {
      m_schema = null;
      m_grammarCache = new GrammarCache((EXISchema)null);
      m_locusStack = new XMLLocusItemEx[32];
      for (int i = 0; i < 32; i++) {
        m_locusStack[i] = new XMLLocusItemEx();
      }
      m_locusLastDepth = -1;
      m_xmlSpaceStack = new boolean[32];
      m_xmlSpaceLastDepth = -1;
      m_prefixUriBindingsDefault = new PrefixUriBindings();
      m_prefixUriBindings = null;
      m_charBuf = new char[128];
      m_decls = new NamespaceDeclarations();
      qname = new QName();
      m_scriber = ScriberFactory.createScriber(AlignmentType.bitPacked);
      setStringTable(Scriber.createStringTable(m_grammarCache));
      m_outputCookie = false;
      m_optionsEncoder = new EXIOptionsEncoder();
      m_scribble = new Scribble();
      m_zlibLevel = java.util.zip.Deflater.DEFAULT_COMPRESSION;
      m_zlibStrategy = java.util.zip.Deflater.DEFAULT_STRATEGY;
      m_preserveWhitespaces = false;
      m_observeC14N = false;
      m_comparableAttributes = new ComparableAttribute[32];
      for (int i = 0; i < m_comparableAttributes.length; i++) {
        m_comparableAttributes[i] = new ComparableAttribute();
      }
      m_n_comparableAttributes = 0;
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
        setStringTable(Scriber.createStringTable(m_grammarCache));
        m_scriber.setValueMaxLength(m_exiOptions.getValueMaxLength());
        m_scriber.setPreserveLexicalValues(m_exiOptions.getPreserveLexicalValues());
      }
    }
    
    private void setGrammarCache(GrammarCache grammarCache) {
      if (m_grammarCache != grammarCache) {
        m_grammarCache = grammarCache;
        final EXISchema schema;
        if ((schema = m_grammarCache.getEXISchema()) != m_schema) {
          m_schema = schema;
          m_scriber.setSchema(m_schema, m_exiOptions.getDatatypeRepresentationMap(), m_exiOptions.getDatatypeRepresentationMapBindingsCount());
          setStringTable(Scriber.createStringTable(m_grammarCache));
        }
      }
    }
    
    public final GrammarCache getGrammarCache() {
      return m_grammarCache;
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

    public final void setObserveC14N(boolean observeC14N) {
      m_observeC14N = observeC14N;
    }
    
    //////////// SAX event handlers

    public final void setDocumentLocator(final Locator locator) {
      m_locator = locator;
    }

    public final void startDocument() throws SAXException {
      m_locusLastDepth = -1;
      m_xmlSpaceLastDepth = 0;
      m_xmlSpaceStack[m_xmlSpaceLastDepth] = false;
      m_decls.clear();
      m_charPos = 0;
      try {
        Scriber.writeHeaderPreamble(m_outputStream, m_outputCookie, m_outputOptions != HeaderOptionsOutputType.none);
        BitOutputStream bitOutputStream = null;
        if (m_outputOptions != HeaderOptionsOutputType.none) {
          final BitOutputStream outputStream;
          try {
            outputStream = m_optionsEncoder.encode(m_exiOptions, m_outputOptions == HeaderOptionsOutputType.all, m_observeC14N, m_outputStream);
          }
          catch (EXIOptionsException eoe) {
            throw new SAXException(new TransmogrifierException(TransmogrifierException.EXI_OPTIONS_ENCODER_EXCEPTION,
                new String[] { eoe.getMessage() }, new LocatorImpl(m_locator)));
          }
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
        m_scriber.stringTable.setValuePartitionCapacity(m_exiOptions.getValuePartitionCapacity());
        m_scriber.setBlockSize(m_exiOptions.getBlockSize());
        m_scriber.setDeflateParams(m_zlibLevel, m_zlibStrategy);
        
        m_stringValueScriber = m_scriber.getValueScriberByID(Scriber.CODEC_STRING);
        
        m_grammarCache.retrieveRootGrammar(m_exiOptions.isFragment(), m_scriber.eventTypesWorkSpace).init(m_scriber.currentState);
        m_prefixUriBindings = m_prefixUriBindingsDefault;
        
        EventTypeList eventTypes = m_scriber.getNextEventTypes();
        final EventType eventType;
        if ((eventType = eventTypes.getSD()) != null)
          m_scriber.startDocument();
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
      final PrefixUriBindings bindings;
      if (prefix.length() != 0) {
        if (uri.length() != 0) {
          bindings = m_prefixUriBindings.bind(prefix, uri);
          m_decls.addDecl(prefix, uri, bindings == m_prefixUriBindings);
        }
        else {
          bindings = m_prefixUriBindings.unbind(prefix);
          m_decls.addDecl(prefix, "", bindings == m_prefixUriBindings);
        }
      }
      else {
        if (uri.length() != 0) {
          bindings = m_prefixUriBindings.bindDefault(uri);
          m_decls.addDecl("", uri, bindings == m_prefixUriBindings);
        }
        else {
          bindings = m_prefixUriBindings.unbindDefault();
          m_decls.addDecl("", "", bindings == m_prefixUriBindings);
        }
      }
      m_prefixUriBindings = bindings;
    }

    public final void endPrefixMapping(final String prefix)
      throws SAXException {
    }
    
    public final void startElement(final String uri, final String localName, final String qualifiedName, Attributes attrs)
      throws SAXException {
      if (m_charPos > 0)
        do_characters(false);
      m_contentState = STAG;

      m_n_comparableAttributes = 0;

      final String elementPrefix;
      final String[] nsdecls;
      final int n_nsdecls;
      if (hasNS) {
        nsdecls = m_decls.getDecls(m_observeC14N);
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
      if (++m_xmlSpaceLastDepth == m_xmlSpaceStack.length) {
        final int xmlSpaceStackCapacity = m_xmlSpaceLastDepth + 8;
        final boolean[] xmlSpaceStack = new boolean[xmlSpaceStackCapacity];
        System.arraycopy(m_xmlSpaceStack, 0, xmlSpaceStack, 0, m_xmlSpaceLastDepth);
        m_xmlSpaceStack = xmlSpaceStack;
      }
      // inherit parent's xml:space value
      m_xmlSpaceStack[m_xmlSpaceLastDepth] = m_xmlSpaceStack[m_xmlSpaceLastDepth - 1]; 
      locusItem.prefixUriBindings = m_prefixUriBindings;
      try {
        EventTypeList eventTypes = m_scriber.getNextEventTypes();
        int i, i_len;
        EventType eventType = null;
        String eventTypeUri;
        byte itemType = -1; 
        boolean isWildcardElem = false;
        loop:
        for (i = 0, i_len = eventTypes.getLength(); i < i_len; i++) {
          eventType = eventTypes.item(i);
          switch (itemType = eventType.itemType) {
            case EventType.ITEM_SE:
              if (localName.equals(eventType.name)) {
                eventTypeUri = eventType.uri;
                if (!uri.equals(eventTypeUri))
                  continue;
                break loop;
              }
              break;
            case EventType.ITEM_SCHEMA_WC_ANY:
              isWildcardElem = true;
              break loop;
            case EventType.ITEM_SCHEMA_WC_NS:
              isWildcardElem = true;
              eventTypeUri = eventType.uri;
              if (!uri.equals(eventTypeUri))
                continue;
              break loop;
            case EventType.ITEM_SE_WC:
              isWildcardElem = true;
              break loop;
            default:
              break;
          }
        }
        final int epos = i;
        if (epos < i_len) {
          m_scriber.writeEventType(eventType);
          m_scriber.writeQName(qname.setValue(uri, localName, elementPrefix), eventType);
          locusItem.elementURI = qname.uriId;
          locusItem.elementLocalName = qname.localNameId;
          if (isWildcardElem)
            m_scriber.startWildcardElement(epos, qname.uriId, qname.localNameId);
          else
            m_scriber.startElement(eventType);
          final byte grammarType = m_scriber.currentState.targetGrammar.grammarType;
          assert grammarType == Grammar.BUILTIN_GRAMMAR_ELEMENT || grammarType == Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT || 
              grammarType == Grammar.SCHEMA_GRAMMAR_ELEMENT_AND_TYPE;
          if (hasNS) {
            if (n_nsdecls != 0) {
              eventTypes = m_scriber.getNextEventTypes();
              for (i = 0, i_len = eventTypes.getLength(); i < i_len; i++) {
                eventType = eventTypes.item(i);
                if ((itemType = eventType.itemType) == EventType.ITEM_NS) {
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

          boolean isSchemaInformedGrammar = m_scriber.currentState.targetGrammar.isSchemaInformed();
          final boolean useATStarForXsiType = m_divertBuiltinGrammarToAnyType && grammarType == Grammar.BUILTIN_GRAMMAR_ELEMENT;
          
          int positionOfNil  = -1; // position of legitimate xsi:nil
          int positionOfType = -1; // position of legitimate xsi:type
          if ((i_len = attrs.getLength()) != 0) {
            for (i = 0; i < i_len; i++) {
              final String instanceUri = attrs.getURI(i);
              final String instanceQName = attrs.getQName(i);
              if (W3C_2000_XMLNS_URI.equals(instanceUri) ||
                  instanceQName.startsWith("xmlns") && (instanceQName.length() == 5 || instanceQName.charAt(5) == ':')) { // i.e. "xmlns" or "xmlns:*"
                continue;
              }
              else if (XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI.equals(instanceUri)) {
                final String instanceName = attrs.getLocalName(i);
                if ("type".equals(instanceName)) {
                  positionOfType = i;
                  continue;
                }
                else if ("nil".equals(instanceName)) {
                  positionOfNil = i;
                  continue;
                }
              }
              else if (XmlUriConst.W3C_XML_1998_URI.equals(instanceUri)) {
                if ("space".equals(attrs.getLocalName(i))) {
                  final String attrValue = attrs.getValue(i);
                  if ("preserve".equals(attrValue)) {
                    m_xmlSpaceStack[m_xmlSpaceLastDepth] = true;
                  }
                  else if ("default".equals(attrValue)) {
                    m_xmlSpaceStack[m_xmlSpaceLastDepth] = false;
                  }
                }
              }
              final ComparableAttribute comparableAttribute;
              final String _prefix = hasNS ? getPrefixOfQualifiedName(instanceQName) : null;
              comparableAttribute = acquireComparableAttribute();
              comparableAttribute.init(instanceUri, attrs.getLocalName(i), _prefix, i);
            }
          }
          final EXISchema corpus = m_grammarCache.getEXISchema();
          if (useATStarForXsiType || positionOfType != -1) {
            EventType eventTypeForType;            
            if ((eventTypeForType = matchXsiType(m_scriber.getNextEventTypes(), useATStarForXsiType)) == null) { // xsi:type had no matching event type
              assert isSchemaInformedGrammar;
              TransmogrifierException te;
              te = new TransmogrifierException(TransmogrifierException.UNEXPECTED_ATTR, 
                  new String[] { "type", XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, attrs.getValue(positionOfType) }, new LocatorImpl(m_locator));
              throw new SAXException(te);
            }
            m_scriber.writeEventType(eventTypeForType);
            final String prefix = hasNS ? (positionOfType != -1 ? getPrefixOfQualifiedName(attrs.getQName(positionOfType)) : "") : null;
            m_scriber.writeQName(qname.setValue(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "type", prefix), eventTypeForType);
            if (positionOfType != -1) {
              final String xsiType = attrs.getValue(positionOfType);
              setXsiTypeValue(qname, xsiType, m_prefixUriBindings);
            }
            else {
              qname.qName = "anyType";
              qname.namespaceName = XmlUriConst.W3C_2001_XMLSCHEMA_URI;
              qname.localName = "anyType";
              qname.prefix = "";
            }
            m_scriber.writeXsiTypeValue(qname);
            if (!isSchemaInformedGrammar && eventTypeForType.itemType == EventType.ITEM_AT_WC_ANY_UNTYPED) {
              m_scriber.wildcardAttribute(eventTypeForType.getIndex(), XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID, 
                  EXISchemaConst.XSI_LOCALNAME_TYPE_ID);
            }
            if (corpus != null) {
              final int tp;
              if ((tp = corpus.getTypeOfSchema(qname.namespaceName, qname.localName)) != EXISchema.NIL_NODE) {
                m_scriber.xsitp(tp);
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
              eventTypes = m_scriber.getNextEventTypes();
              boolean nilAvailable = false;
              for (i = 0, i_len = eventTypes.getLength(); i < i_len; i++) {
                eventType = eventTypes.item(i);
                itemType = eventType.itemType;  
                if (itemType == EventType.ITEM_SCHEMA_NIL) {
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
                else if (itemType == EventType.ITEM_AT_WC_ANY_UNTYPED) {
                  assert nilAvailable;
                  break;
                }
              }
              if (i == i_len) {
                TransmogrifierException te;
                te = new TransmogrifierException(TransmogrifierException.UNEXPECTED_ATTR, 
                    new String[] { "nil", XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, nilval  }, new LocatorImpl(m_locator));
                throw new SAXException(te);
              }
            }
            if (nilled != null) {
              m_scriber.writeEventType(eventType);
              m_scriber.writeQName(qname.setValue(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "nil", 
                  hasNS ? getPrefixOfQualifiedName(attrs.getQName(positionOfNil)) : null), eventType);
              m_scriber.writeXsiNilValue(nilled, nilval);
              if (nilled) {
                m_scriber.nillify(eventType.getIndex());
              }
            }
            else {
              if (isSchemaInformedGrammar) {
                assert itemType == EventType.ITEM_AT_WC_ANY_UNTYPED; 
                // process invalid xsi:nil value using AT(*) [untyped value]
                m_scriber.writeEventType(eventType);
                m_scriber.writeQName(qname.setValue(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "nil", 
                    hasNS ? getPrefixOfQualifiedName(attrs.getQName(positionOfNil)) : null), eventType);
                m_stringValueScriber.scribe(nilval, m_scribble, EXISchemaConst.XSI_LOCALNAME_NIL_ID,  
                    XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID, EXISchema.NIL_NODE, m_scriber);
              }
              else {
                // treat it as a vanilla attribute
                final ComparableAttribute comparableAttribute;
                final String _prefix = hasNS ? getPrefixOfQualifiedName(attrs.getQName(positionOfNil)) : null;
                comparableAttribute = acquireComparableAttribute();
                comparableAttribute.init(attrs.getURI(positionOfNil), attrs.getLocalName(positionOfNil), _prefix, positionOfNil);
              }
            }
          }
          final int n_attrs = m_n_comparableAttributes;
          if (n_attrs != 0) {
            if (n_attrs != 1) { 
              // Sort attributes
              int h = n_attrs * 10 / 13;
              while (true) {
                int swaps = 0;
                for (i = 0; i + h < n_attrs; ++i) {
                  final int p = i;
                  final int q = i + h;
                  if (m_comparableAttributes[p].compareTo(m_comparableAttributes[q]) > 0) {
                    // do the swap
                    final ComparableAttribute _at_p = m_comparableAttributes[p];
                    m_comparableAttributes[p] = m_comparableAttributes[q];
                    m_comparableAttributes[q] = _at_p;
                    ++swaps;
                  }
                }
                if (h == 1) {
                  if (swaps == 0) {
                    break;
                  }
                } else {
                  h = h * 10 / 13;
                }
              }
            }
            for (i = 0; i < n_attrs; i++) {
              eventTypes = m_scriber.getNextEventTypes();
              final ComparableAttribute attr = m_comparableAttributes[i]; 
              final String instanceUri  = attr.uri;
              final String instanceName = attr.name;
              int tp = EXISchema.NIL_NODE;
              ValueScriber valueScriber = null;
              if (isSchemaInformedGrammar) {
                eventType = eventTypes.getSchemaAttribute(instanceUri, instanceName);
                if (eventType != null) {
                  final EventTypeSchema eventTypeSchemaAttribute;
                  eventTypeSchemaAttribute = (EventTypeSchema)eventType;
                  if ((tp = eventTypeSchemaAttribute.nd) != EXISchema.NIL_NODE) {
                    valueScriber = m_scriber.getValueScriber(tp);
                    if (!valueScriber.process(attrs.getValue(attr.index), tp, m_schema, m_scribble, m_scriber)) {
                      valueScriber = m_stringValueScriber;
                      eventType = eventTypes.getSchemaAttributeInvalid(instanceUri, instanceName);
                      tp = EXISchema.NIL_NODE;
                    }
                  }
                  else {
                    valueScriber = m_stringValueScriber;
                  }
                  if (eventType != null) {
                    m_scriber.writeEventType(eventType);
                    final String prefix = attr.prefix;
                    if (hasNS)
                      verifyPrefix(instanceUri, prefix);
                    m_scriber.writeQName(qname.setValue(instanceUri, instanceName, prefix), eventType);
                    valueScriber.scribe(attrs.getValue(attr.index), m_scribble, qname.localNameId, qname.uriId, tp, m_scriber);
                    m_scriber.attribute(eventType);
                    continue;
                  }
                }
                if (eventType == null) {
                  if ((eventType = eventTypes.getSchemaAttributeWildcardNS(instanceUri)) == null) {
                    eventType = eventTypes.getSchemaAttributeWildcardAny();
                  }
                  if (eventType != null) {
                    final int _attr;
                    if ((_attr = corpus.getGlobalAttrOfSchema(instanceUri, instanceName)) != EXISchema.NIL_NODE) {
                      tp = corpus.getTypeOfAttr(_attr);
                      assert tp != EXISchema.NIL_NODE;
                      valueScriber = m_scriber.getValueScriber(tp);
                      if (!valueScriber.process(attrs.getValue(attr.index), tp, m_schema, m_scribble, m_scriber)) {
                        tp = EXISchema.NIL_NODE; // revert variable tp back to NIL_NODE
                        eventType = null;
                      }
                    }
                    else
                      valueScriber = m_stringValueScriber;
                  }
                  if (eventType == null && (eventType = eventTypes.getAttributeWildcardAnyUntyped()) != null)
                    valueScriber = m_stringValueScriber;
                  if (eventType != null) {
                    m_scriber.writeEventType(eventType);
                    final String prefix = attr.prefix;
                    if (hasNS)
                      verifyPrefix(instanceUri, prefix);
                    m_scriber.writeQName(qname.setValue(instanceUri, instanceName, prefix), eventType);
                    valueScriber.scribe(attrs.getValue(attr.index), m_scribble, qname.localNameId, qname.uriId, tp, m_scriber);
                    if (itemType == EventType.ITEM_AT_WC_ANY_UNTYPED)
                      m_scriber.wildcardAttribute(eventType.getIndex(), qname.uriId, qname.localNameId);                          
                    continue;
                  }
                }
              }
              else { // built-in grammar
                if ((eventType = eventTypes.getLearnedAttribute(instanceUri, instanceName)) != null ||
                    (eventType = eventTypes.getAttributeWildcardAnyUntyped()) != null) {
                  valueScriber = m_stringValueScriber;
                }
                if (eventType != null) {
                  m_scriber.writeEventType(eventType);
                  final String prefix = attr.prefix;
                  if (hasNS)
                    verifyPrefix(instanceUri, prefix);
                  m_scriber.writeQName(qname.setValue(instanceUri, instanceName, prefix), eventType);
                  valueScriber.scribe(attrs.getValue(attr.index), m_scribble, qname.localNameId, qname.uriId, tp, m_scriber);
                  if (eventType.itemType == EventType.ITEM_AT_WC_ANY_UNTYPED)
                    m_scriber.wildcardAttribute(eventType.getIndex(), qname.uriId, qname.localNameId);
                  continue;
                }
              }
              TransmogrifierException te;
              te = new TransmogrifierException(TransmogrifierException.UNEXPECTED_ATTR, 
                  new String[] { instanceName, instanceUri, attrs.getValue(attr.index) }, new LocatorImpl(m_locator));
              throw new SAXException(te);
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

    public final BinaryDataSink startBinaryData(long totalLength) throws SAXException {
      m_charPos = 0;
      try {
        final EventTypeList eventTypes;
        eventTypes = m_scriber.getNextEventTypes();
        EventType eventType;
        if ((eventType = eventTypes.getSchemaCharacters()) != null) {
          final int tp = m_scriber.currentState.contentDatatype;
          assert tp != EXISchema.NIL_NODE;
          final ValueScriber valueScriber = m_scriber.getValueScriber(tp);
          if (valueScriber instanceof BinaryDataSink) {
            final BinaryDataSink binaryDataSink = (BinaryDataSink)valueScriber;
            m_scriber.writeEventType(eventType);
            m_scriber.characters(eventType);
            try {
              binaryDataSink.startBinaryData(totalLength, m_scribble, m_scriber);
            }
            catch (ScriberRuntimeException se) {
              TransmogrifierException te;
              te = new TransmogrifierException(TransmogrifierException.SCRIBER_ERROR, 
                  new String[] { se.getMessage() }, new LocatorImpl(m_locator));
              te.setException(se);
              throw new SAXException(te);
            }
            return binaryDataSink;
          }
        }
        TransmogrifierException te;
        te = new TransmogrifierException(TransmogrifierException.UNEXPECTED_BINARY_VALUE, 
            (String[])null, new LocatorImpl(m_locator));
        throw new SAXException(te);
      }
      catch (IOException ioe) {
        throw new SAXException(ioe);
      }
    }
    
    public final void binaryData(byte[] octets, int offset, int length, BinaryDataSink binaryDataSink) throws SAXException {
      try {
        binaryDataSink.binaryData(octets, offset, length, m_scribble, m_scriber);
      }
      catch (IOException ioe) {
        throw new SAXException(ioe);
      }
    }
    
    public final void endBinaryData(BinaryDataSink binaryDataSink) throws SAXException {
      final XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
      try {
        binaryDataSink.endBinaryData(m_scribble, locusItem.elementLocalName, locusItem.elementURI, m_scriber);
      }
      catch (ScriberRuntimeException se) {
        TransmogrifierException te;
        te = new TransmogrifierException(TransmogrifierException.SCRIBER_ERROR, 
            new String[] { se.getMessage() }, new LocatorImpl(m_locator));
        te.setException(se);
        throw new SAXException(te);
      }
      catch (IOException ioe) {
        throw new SAXException(ioe);
      }
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
        eventTypes = m_scriber.getNextEventTypes();
        int tp = EXISchema.NIL_NODE;
        EventType eventType;
        if ((eventType = eventTypes.getSchemaCharacters()) != null) {
          tp = m_scriber.currentState.contentDatatype;
          assert tp != EXISchema.NIL_NODE;
          ValueScriber valueScriber = m_scriber.getValueScriber(tp);
          // REVISIT: avoid converting to string.
          final String stringValue = new String(m_charBuf, 0, m_charPos);
          if (valueScriber.process(stringValue, tp, m_schema, m_scribble, m_scriber)) {
            m_scriber.writeEventType(eventType);
            m_scriber.characters(eventType);
            final XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
            valueScriber.scribe(stringValue, m_scribble, locusItem.elementLocalName,  
                locusItem.elementURI, tp, m_scriber);
            m_charPos = 0;
            return;
          }
        }
        final boolean xmlSpacePreserve = m_xmlSpaceStack[m_xmlSpaceLastDepth];
        if ((eventType = eventTypes.getCharacters()) != null) {
          boolean preserveWhitespaces = m_preserveWhitespaces || xmlSpacePreserve;
          if (!preserveWhitespaces) {
            if (tp != EXISchema.NIL_NODE)
              preserveWhitespaces = true;
            else {
              if (m_scriber.currentState.targetGrammar.isSchemaInformed())
                preserveWhitespaces = eventType.itemType == EventType.ITEM_SCHEMA_CH_MIXED; 
              else
                preserveWhitespaces = isContent;
            }
          }
          if (preserveWhitespaces) {
            m_scriber.writeEventType(eventType);
            m_scriber.undeclaredCharacters(eventType.getIndex());
            final XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
            m_stringValueScriber.scribe(new String(m_charBuf, 0, m_charPos), m_scribble,
                locusItem.elementLocalName, locusItem.elementURI, EXISchema.NIL_NODE, m_scriber);
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
                  m_scriber.undeclaredCharacters(eventType.getIndex());
                  final XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
                  m_stringValueScriber.scribe(new String(m_charBuf, 0, m_charPos), m_scribble,
                      locusItem.elementLocalName, locusItem.elementURI, EXISchema.NIL_NODE, m_scriber);
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
        if (xmlSpacePreserve && m_preserveWhitespaces) {
          TransmogrifierException te;
          te = new TransmogrifierException(TransmogrifierException.CANNOT_PRESERVE_WHITESPACES, 
              (String[])null, new LocatorImpl(m_locator));
          throw new SAXException(te);
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
        eventTypes = m_scriber.getNextEventTypes();

        EventType eventType = null;
        if ((eventType = eventTypes.getEE()) != null) {
          m_scriber.writeEventType(eventType);
          m_scriber.endElement();
        }
        else {
          assert m_scriber.currentState.targetGrammar.isSchemaInformed();
          if ((eventType = eventTypes.getSchemaCharacters()) != null) {
            final int tp = m_scriber.currentState.contentDatatype;
            assert tp != EXISchema.NIL_NODE;
            ValueScriber valueScriber = m_scriber.getValueScriber(tp);
            if (valueScriber.process("", tp, m_schema, m_scribble, m_scriber)) {
              m_scriber.writeEventType(eventType);
              m_scriber.characters(eventType);
              final XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
              valueScriber.scribe("", m_scribble, locusItem.elementLocalName, locusItem.elementURI, tp, m_scriber);
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
        final EventTypeList eventTypes = m_scriber.getNextEventTypes();
        final EventType eventType;
        if ((eventType = eventTypes.getED()) != null) {
          m_scriber.endDocument();
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
        final EventTypeList eventTypes = m_scriber.getNextEventTypes();
        final int len = eventTypes.getLength();
        for (int i = 0; i < len; i++) {
          final EventType eventType = eventTypes.item(i);
          if (eventType.itemType == EventType.ITEM_PI) {
            m_scriber.miscContent(eventType.getIndex());
            try {
              m_scriber.writeEventType(eventType);
              m_scriber.writeName(target);
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
        final EventTypeList eventTypes = m_scriber.getNextEventTypes();
        final int len = eventTypes.getLength();
        for (int i = 0; i < len; i++) {
          final EventType eventType = eventTypes.item(i);
          if (eventType.itemType == EventType.ITEM_ER) {
            m_scriber.miscContent(eventType.getIndex());
            try {
              m_scriber.writeEventType(eventType);
              m_scriber.writeName(name);
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
        final EventTypeList eventTypes = m_scriber.getNextEventTypes();
        final int len = eventTypes.getLength();
        for (int i = 0; i < len; i++) {
          EventType eventType = eventTypes.item(i);
          if (eventType.itemType == EventType.ITEM_CM) {
            m_scriber.miscContent(eventType.getIndex());
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
      EventTypeList eventTypes = m_scriber.getNextEventTypes();
      int i, len;
      EventType eventType = null;
      for (i = 0, len = eventTypes.getLength(); i < len; i++) {
        eventType = eventTypes.item(i);
        if (eventType.itemType == EventType.ITEM_DTD) {
            break;
        }
      }
      if (i < len) {
        try {
          m_scriber.writeEventType(eventType);
          m_scriber.writeName(name);
          m_scriber.writePublic(publicId != null ? publicId : "");
          m_scriber.writeSystem(systemId != null ? systemId : "");
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

    private void setStringTable(StringTable stringTable) {
      m_scriber.setStringTable(stringTable);
    }
    
    private ComparableAttribute acquireComparableAttribute() {
      if (m_n_comparableAttributes == m_comparableAttributes.length) {
        final ComparableAttribute[] comparableAttributes;
        final int len = m_n_comparableAttributes + 32; // new array size
        comparableAttributes = new ComparableAttribute[len];
        System.arraycopy(m_comparableAttributes, 0, comparableAttributes, 0, m_n_comparableAttributes);
        for (int i = m_n_comparableAttributes; i < len; i++) {
          comparableAttributes[i] = new ComparableAttribute();
        }
        m_comparableAttributes = comparableAttributes;
      }
      return m_comparableAttributes[m_n_comparableAttributes++];
    }
    
    private void verifyPrefix(String uri, String prefix) throws SAXException, IOException {
      if (prefix.length() != 0) {
        final TransmogrifierException te;
        final String _uri = m_prefixUriBindings.getUri(prefix);
        if (_uri == null) {
          te = new TransmogrifierException(TransmogrifierException.PREFIX_NOT_BOUND, 
              new String[] { prefix }, new LocatorImpl(m_locator));
          throw new SAXException(te);
        }
        else if (!uri.equals(_uri)) {
          te = new TransmogrifierException(TransmogrifierException.PREFIX_BOUND_TO_ANOTHER_NAMESPACE, 
              new String[] { prefix, _uri }, new LocatorImpl(m_locator));
          throw new SAXException(te);
        }
      }
    }
    
    private EventType matchXsiType(EventTypeList eventTypes, boolean useWildcardAT) {
      final Grammar targetGrammar = m_scriber.currentState.targetGrammar;
      final boolean isSchemaInformedGrammar = targetGrammar.isSchemaInformed();
      for (int j = 0, j_len = eventTypes.getLength(); j < j_len; j++) {
        final EventType eventType = eventTypes.item(j);
        switch (eventType.itemType) {
          case EventType.ITEM_SCHEMA_TYPE:
            assert targetGrammar.grammarType != Grammar.BUILTIN_GRAMMAR_ELEMENT;
            return eventType;
          case EventType.ITEM_AT_WC_ANY_UNTYPED:
            if (!isSchemaInformedGrammar)
              return eventType;
            break;
          case EventType.ITEM_AT:
            assert !isSchemaInformedGrammar;
            if (!useWildcardAT && "type".equals(eventType.name) && 
                XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI.equals(eventType.uri)) {
              return eventType;
            }
            break;
        }
      }
      return null;
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
    byte priority; // 0 ... 2 ( 0 -> xsi:type, 1 -> xsi:nil, 2 -> other) 
    String uri;
    String name;
    String prefix;
    int index;
    public void init(String uri, String name, String prefix, int index) {
      this.uri = uri;
      this.name = name;
      this.index = index;
      this.prefix = prefix;
      byte priority = 2;
      if (uri.equals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI)) {
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

    public final String[] getDecls(boolean doSort) {
      if (doSort && n_decls > 1) {
        int h = n_decls * 10 / 13;
        while (true) {
          int swaps = 0;
          for (int i = 0; i + h < n_decls; ++i) {
            final int p = i << 1;
            final int q = (i + h) << 1;
            if (decls[p].compareTo(decls[q]) > 0) {
              // do the swap
              final String prefix = decls[p];
              final String uri = decls[p + 1];
              decls[p] = decls[q];
              decls[p + 1] = decls[q + 1];
              decls[q] = prefix;
              decls[q + 1] = uri;
              ++swaps;
            }
          }
          if (h == 1) {
            if (swaps == 0) {
              break;
            }
          } else {
            h = h * 10 / 13;
          }
        }
      }
      return decls;
    }

    public final int getDeclsCount() {
      return n_decls;
    }
    
    private void addDecl(String prefix, String uri, boolean checkDuplicate) {
      final int n_strings = n_decls << 1;
      if (checkDuplicate) {
        for (int i = 0; i < n_strings; i += 2) {
          if (decls[i].equals(prefix)) {
            decls[i + 1] = uri;
            return;
          }
        }
      }
      final String[] pairs;
      if (n_strings != decls.length)
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
  
  private static SAXParserFactory createSAXParserFactory() {
    final SAXParserFactory saxParserFactory;
    saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setNamespaceAware(true);
    return saxParserFactory;
  }
  
  private static class XMLLocusItemEx {
    int elementURI;
    int elementLocalName;
    PrefixUriBindings prefixUriBindings;
  }
  
}
