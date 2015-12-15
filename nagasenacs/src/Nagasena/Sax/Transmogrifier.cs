using System;
using System.IO;
using System.Diagnostics;
using System.Collections.Generic;
using System.Xml;

using Org.System.Xml.Sax;
using LocatorImpl = Org.System.Xml.Sax.Helpers.LocatorImpl;

using DeflateStrategy = ICSharpCode.SharpZipLib.Zip.Compression.DeflateStrategy;

using EXIOptionsEncoder = Nagasena.Proc.EXIOptionsEncoder;
using HeaderOptionsOutputType = Nagasena.Proc.HeaderOptionsOutputType;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EXIOptions = Nagasena.Proc.Common.EXIOptions;
using EXIOptionsException = Nagasena.Proc.Common.EXIOptionsException;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using QName = Nagasena.Proc.Common.QName;
using SchemaId = Nagasena.Proc.Common.SchemaId;
using StringTable = Nagasena.Proc.Common.StringTable;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using EventTypeSchema = Nagasena.Proc.Grammars.EventTypeSchema;
using Grammar = Nagasena.Proc.Grammars.Grammar;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using BitPackedScriber = Nagasena.Proc.IO.BitPackedScriber;
using BitOutputStream = Nagasena.Proc.IO.BitOutputStream;
using BinaryDataSink = Nagasena.Proc.IO.BinaryDataSink;
using PrefixUriBindings = Nagasena.Proc.IO.PrefixUriBindings;
using Scriber = Nagasena.Proc.IO.Scriber;
using Scribble = Nagasena.Proc.IO.Scribble;
using ScriberRuntimeException = Nagasena.Proc.IO.ScriberRuntimeException;
using ScriberFactory = Nagasena.Proc.IO.ScriberFactory;
using ValueScriber = Nagasena.Proc.IO.ValueScriber;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using EmptySchema = Nagasena.Schema.EmptySchema;

namespace Nagasena.Sax {

  /// <summary>
  /// The Transmogrifier converts an XML stream to an EXI stream.
  /// </summary>
  public sealed class Transmogrifier {

    private readonly SaxAdapter m_saxAdapter;
    /// <summary>
    /// EXIEncoderSaxHandler handles SAX events that comes from SaxAdapter. 
    /// </summary>
    private readonly SAXEventHandler m_saxHandler;

    private HeaderOptionsOutputType m_outputOptions;
    private readonly EXIOptions m_exiOptions;

    private bool m_divertBuiltinGrammarToAnyType;

    private static readonly SchemaId SCHEMAID_NO_SCHEMA;
    private static readonly SchemaId SCHEMAID_EMPTY_SCHEMA;
    static Transmogrifier() {
      SCHEMAID_NO_SCHEMA = new SchemaId((string)null);
      SCHEMAID_EMPTY_SCHEMA = new SchemaId("");
    }

    /// <summary>
    /// Create an instance of the Transmogrifier
    /// </summary>
    /// <param name="saxParserFactory"> </param>
    /// <exception cref="TransmogrifierException"> </exception>
    public Transmogrifier()
      : this(false) {
    }

    internal Transmogrifier(bool namespacePrefixesFeature) {
      // fixtures
      m_saxHandler = new SAXEventHandler(this);
      m_saxAdapter = new SaxAdapter();
      m_saxAdapter.ContentHandler = m_saxHandler;
      try {
        m_saxAdapter.LexicalHandler = m_saxHandler;
      }
      catch (SaxException se) {
        TransmogrifierRuntimeException te;
        te = new TransmogrifierRuntimeException(TransmogrifierRuntimeException.UNHANDLED_SAXPARSER_PROPERTY, 
          new string[] { "http://xml.org/sax/properties/lexical-handler" });
        te.Exception = se;
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

    /// <summary>
    /// @y.exclude
    /// </summary>
    public PrefixUriBindings PrefixUriBindings {
      set {
        m_saxHandler.PrefixUriBindings = value;
      }
    }

    private void reset() {
      m_saxHandler.reset();
    }

    /// <summary>
    /// Set an output stream to which encoded streams are written. </summary>
    /// <param name="ostream"> output stream </param>
    public Stream OutputStream {
      set {
        m_saxHandler.OutputStream = value;
      }
    }

    /// <summary>
    /// Set the bit alignment style for the encoded EXI stream. </summary>
    /// <param name="alignmentType"> <seealso cref="Nagasena.Proc.Common.AlignmentType"/>. 
    /// Default is <i>bit-packed</i>. </param>
    /// <exception cref="EXIOptionsException"> </exception>
    public AlignmentType AlignmentType {
      set {
        m_exiOptions.AlignmentType = value;
        m_saxHandler.AlignmentType = value;
      }
    }

    /// <summary>
    /// Set to true if the XML input stream is an XML fragment (a non-compliant
    /// XML document with multiple root elements). </summary>
    /// <param name="isFragment"> true if the XML input stream is an XML fragment. </param>
    public bool Fragment {
      set {
        m_exiOptions.Fragment = value;
      }
    }

    /// <summary>
    /// Set the size, in number of values, of the information that will be 
    /// processed as a chunk of the entire XML stream. Reducing the block size 
    /// can improve performance for devices with limited dynamic memory. 
    /// Default is 1,000,000 items (not 1MB, but 1,000,000 complete Attribute 
    /// and Element values). Block size is only used when the EXI stream is
    /// encoded with EXI-compression. </summary>
    /// <param name="blockSize"> number of values in each processing block. Default is 1,000,000. </param>
    /// <exception cref="EXIOptionsException"> </exception>
    public int BlockSize {
      set {
        m_exiOptions.BlockSize = value;
      }
    }

    /// <summary>
    /// Set the maximum length of a string that will be stored for reuse in the
    /// String Table. By default, there is no maximum length. However, in data
    /// sets that have long, unique strings of information, you can improve
    /// performance by limiting the size to the length of strings that are more
    /// likely to appear more than once. </summary>
    /// <param name="valueMaxLength"> maximum length of entries in the String Table.  </param>

    public int ValueMaxLength {
      set {
        m_exiOptions.ValueMaxLength = value;
        m_saxHandler.ValueMaxLength = value;
      }
    }

    /// <summary>
    /// Set the maximum number of values in the String Table. By default, there
    /// is no limit. If the target device has limited dynamic memory, limiting 
    /// the number of entries in the String Table can improve performance and
    /// reduce the likelihood that you will exceed memory capacity. </summary>
    /// <param name="valuePartitionCapacity"> maximum number of entries in the String Table </param>
    public int ValuePartitionCapacity {
      set {
        m_exiOptions.ValuePartitionCapacity = value;
      }
    }

    public bool DivertBuiltinGrammarToAnyType {
      set {
        m_divertBuiltinGrammarToAnyType = value;
      }
    }

    /// <summary>
    /// Set to <i>true</i> to preserve the original string values from the XML
    /// stream. For example, a date string might be converted to a different
    /// format when interpreted by the Transmogrifier. Preserving the lexical values
    /// ensures that the identical strings are restored, and not just their 
    /// logical values.
    /// </summary>
    /// <param name="preserveLexicalValues"> <i>true</i> to keep original strings intact </param>
    /// <exception cref="EXIOptionsException"> </exception>
    public bool PreserveLexicalValues {
      set {
        if (m_exiOptions.PreserveLexicalValues != value) {
          if (m_outputOptions != HeaderOptionsOutputType.none && m_exiOptions.DatatypeRepresentationMapBindingsCount != 0 && value) {
            throw new EXIOptionsException("Preserve.lexicalValues option and datatypeRepresentationMap option cannot be specified together in EXI header options.");
          }
          m_exiOptions.PreserveLexicalValues = value;
          m_saxHandler.PreserveLexicalValues = value;
        }
      }
    }

    /// <summary>
    /// Set the GrammarCache used in transmogrifying XML data to EXI. </summary>
    /// <param name="grammarCache"> <seealso cref="Nagasena.Proc.grammars.GrammarCache"/> </param>
    /// <exception cref="EXIOptionsException"> </exception>
    public GrammarCache GrammarCache {
      set {
        setGrammarCache(value, (SchemaId)null);
      }
      get {
        return m_saxHandler.GrammarCache;
      }
    }

    /// <summary>
    /// Set the GrammarCache to be used in encoding XML streams into EXI streams 
    /// by the transmogrifier. 
    /// The SchemaId contains the string that is written in the header when
    /// <i>HeaderOptionsOutputType.all</i> is set. </summary>
    /// <param name="grammarCache"> <seealso cref="Nagasena.Proc.grammars.GrammarCache"/> </param>
    /// <param name="schemaId"> </param>
    /// <exception cref="EXIOptionsException"> </exception>
    public void setGrammarCache(GrammarCache grammarCache, SchemaId schemaId) {
      EXISchema schema = grammarCache.EXISchema;
      if (schemaId == null) {
        if (schema == null) {
          schemaId = SCHEMAID_NO_SCHEMA;
        }
        else if (schema == EmptySchema.EXISchema) {
          schemaId = SCHEMAID_EMPTY_SCHEMA;
        }
      }
      m_exiOptions.SchemaId = schemaId;
      m_exiOptions.GrammarOptions = grammarCache.grammarOptions;
      m_saxHandler.GrammarCache = grammarCache;
    }


    /// <summary>
    /// Set a datatype representation map (DTRM). The DTRM allows you to remap
    /// XMLSchema datatypes to EXI datatypes other than their default equivalents.
    /// The map is created using a sequence of Qualified Name pairs that identify
    /// a datatype definition in the XMLSchema namespace followed by the new 
    /// corresponding datatype mapping in the EXI namespace.
    /// <br /><br />
    /// For example, the following lines map the boolean datatype from XMLSchema 
    /// to the integer datatype in EXI.
    /// <pre>
    ///   QName q1 = new QName("xsd:boolean","http://www.w3.org/2001/XMLSchema");
    ///   QName q2 = new QName("exi:integer","http://www.w3.org/2009/exi");
    ///   QName[] dtrm = new QName[2];
    ///   dtrm = {q1, q2}; // Each mapping requires 2 qualified names.
    ///   transmogrifierInstance.setDatatypeRepresentationMap(dtrm, 1); // The array, and the number of pairs (1).
    /// </pre>
    /// </summary>
    /// <param name="dtrm"> a sequence of pairs of datatype QName and datatype representation QName </param>
    /// <param name="n_bindings"> the number of QName pairs </param>
    public void setDatatypeRepresentationMap(QName[] dtrm, int n_bindings) {
      if (!QName.isSame(m_exiOptions.DatatypeRepresentationMap, m_exiOptions.DatatypeRepresentationMapBindingsCount, dtrm, n_bindings)) {
        if (m_outputOptions != HeaderOptionsOutputType.none && m_exiOptions.PreserveLexicalValues && dtrm != null) {
          throw new EXIOptionsException("Preserve.lexicalValues option and datatypeRepresentationMap option cannot be specified together in EXI header options.");
        }
        m_exiOptions.setDatatypeRepresentationMap(dtrm, n_bindings);
        m_saxHandler.setDatatypeRepresentationMap(dtrm, n_bindings);
      }
    }
    /// <summary>
    /// Set an external SAX entity resolver. </summary>
    /// <param name="entityResolver"> <seealso cref="Org.System.Xml.Sax.EntityResolver"/> </param>
    public IEntityResolver EntityResolver {
      set {
        m_saxAdapter.EntityResolver = value;
      }
    }

    /// <summary>
    /// Tells the encoder whether to or not to start the stream by
    /// adding an EXI cookie. </summary>
    /// <param name="outputCookie"> <i>true</i> to include the EXI cookie </param>
    public bool OutputCookie {
      set {
        m_saxHandler.OutputCookie = value;
      }
    }
    /// <summary>
    /// Set the header output options. Choices are set using the 
    /// <seealso cref="Nagasena.Proc.HeaderOptionsOutputType"/> enumeration.
    /// Options are <i>all, lessSchemaID</i> (that is, all values
    /// except for the SchemaId), or <i>none.</i> </summary>
    /// <param name="outputOptions"> <seealso cref="Nagasena.Proc.HeaderOptionsOutputType"/> </param>
    /// <exception cref="EXIOptionsException"> </exception>
    public HeaderOptionsOutputType OutputOptions {
      set {
        if (m_outputOptions != value) {
          if (value != HeaderOptionsOutputType.none) {
            if (m_exiOptions.PreserveLexicalValues && m_exiOptions.DatatypeRepresentationMap != null) {
              throw new EXIOptionsException("Preserve.lexicalValues option and datatypeRepresentationMap option cannot be specified together in EXI header options.");
            }
          }
          m_outputOptions = value;
        }
      }
    }
    /// <summary>
    /// Set to true to preserve whitespace (for example, spaces, tabs, and
    /// line breaks) in the encoded EXI stream. By default, non-essential whitespace
    /// is removed from the encoded stream. </summary>
    /// <param name="preserveWhitespaces"> <i>true</i> to retain whitespace in the encoded EXI stream </param>
    public bool PreserveWhitespaces {
      set {
        m_saxHandler.PreserveWhitespaces = value;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Methods for controlling Deflater parameters
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Set ZLIB compression level. </summary>
    /// <param name="level"> the new compression level (0-9) </param>
    /// <seealso cref= java.util.zip.Deflator#setLevel(int level)
    /// Not for public use.
    /// @y.exclude </seealso>
    public int DeflateLevel {
      set {
        m_saxHandler.DeflateLevel = value;
      }
    }

    /// <summary>
    /// Set ZLIB compression strategy. </summary>
    /// <param name="strategy"> the new compression strategy </param>
    /// <seealso cref= java.util.zip.Deflator#setStrategy(int strategy)
    /// Not for public use.
    /// @y.exclude </seealso>
    public DeflateStrategy DeflateStrategy {
      set {
        m_saxHandler.DeflateStrategy = value;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Encode methods
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Parses XML input source and converts it to an EXI stream.</summary>
    /// <param name="inputSource">XML input source</param>
    public void encode(InputSource inputSource) {
      encode(inputSource, (XmlReader)null, (String)null);
    }

    internal void encode(XmlReader xmlReader, String URI) {
      encode((InputSource)null, xmlReader, URI);
    }

    private void encode(InputSource inputSource, XmlReader xmlReader, String URI) {
      reset();
      try {
        if (inputSource != null)
          m_saxAdapter.Parse(inputSource);
        else
          m_saxAdapter.Parse(xmlReader, URI);
      }
      catch (SaxException se) {
        Exception e;
        if ((e = se.InnerException) != null) {
          if (e is TransmogrifierException) {
            throw (TransmogrifierException)e;
          }
          else if (e is IOException) {
            throw (IOException)e;
          }
          else if (e is XmlException) {
            throw (XmlException)e;
          }
        }
        else {
          LocatorImpl locator = null;
          if (se is SaxParseException) {
            SaxParseException spe = (SaxParseException)se;
            ParseError parseError = spe.Error;
            if (parseError != null) {
              locator = new LocatorImpl();
              locator.SystemId = parseError.SystemId;
              locator.LineNumber = parseError.LineNumber;
              locator.ColumnNumber = parseError.ColumnNumber;
            }
          }
          //se.printStackTrace();
          throw new TransmogrifierException(TransmogrifierException.SAX_ERROR, new string[] { se.Message }, locator);
        }
      }
    }

    /// <summary>
    /// Returns the SAXTransmogrifier, which implements both the ContentHandler
    /// and LexicalHandler. SAX programmers can connect the SAXTransmogrifier to
    /// their favorite XML Parser to convert SAX events into an EXI stream.
    /// </summary>
    public SAXTransmogrifier SAXTransmogrifier {
      get {
        reset();
        return m_saxHandler;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// SAX-based Encoder
    ///////////////////////////////////////////////////////////////////////////

    internal sealed class SAXEventHandler : SAXTransmogrifier {
      private readonly Transmogrifier outerInstance;

      private const String W3C_2000_XMLNS_URI = "http://www.w3.org/2000/xmlns/";

      private XMLLocusItemEx[] m_locusStack;
      private int m_locusLastDepth;
      private bool m_inDTD;

      private bool[] m_xmlSpaceStack;
      private int m_xmlSpaceLastDepth;

      private const sbyte STAG = 0; // start tag
      private const sbyte CONT = 1; // non-whitespace content
      private const sbyte ETAG = 2; // end tag
      private sbyte m_contentState;

      private GrammarCache m_grammarCache;
      private EXISchema m_schema;
      private bool hasNS; // hasNS is subservient to m_grammarCache (derivative)

      private PrefixUriBindings m_prefixUriBindingsDefault;
      private PrefixUriBindings m_prefixUriBindings; // current bindings

      private char[] m_charBuf;
      private int m_charPos;

      private readonly SortedSet<ComparableAttribute> sortedAttributes;
      private ComparableAttribute[] m_comparableAttributes;
      private int m_n_comparableAttributes;

      private readonly NamespaceDeclarations m_decls;
      private readonly QName qname;

      // scriber is to be created based on alignment type as necessary
      private Scriber m_scriber;
      private ValueScriber m_stringValueScriber;

      private Stream m_outputStream;

      private ILocator m_locator;

      private bool m_outputCookie;

      private readonly EXIOptionsEncoder m_optionsEncoder;

      private readonly Scribble m_scribble;

      private int m_zlibLevel;
      private DeflateStrategy m_zlibStrategy;

      private bool m_preserveWhitespaces;

      private TransmogrifierException m_transmogrifierException;

      internal SAXEventHandler(Transmogrifier outerInstance) {
        this.outerInstance = outerInstance;
        m_schema = null;
        m_grammarCache = new GrammarCache((EXISchema)null);
        m_locusStack = new XMLLocusItemEx[32];
        for (int i = 0; i < 32; i++) {
          m_locusStack[i] = new XMLLocusItemEx();
        }
        m_locusLastDepth = -1;
        m_xmlSpaceStack = new bool[32];
        m_xmlSpaceLastDepth = -1;
        m_prefixUriBindingsDefault = new PrefixUriBindings();
        m_prefixUriBindings = null;
        m_charBuf = new char[128];
        sortedAttributes = new SortedSet<ComparableAttribute>();
        m_decls = new NamespaceDeclarations(outerInstance);
        qname = new QName();
        m_scriber = ScriberFactory.createScriber(AlignmentType.bitPacked);
        StringTable = Scriber.createStringTable(m_grammarCache);
        m_outputCookie = false;
        m_optionsEncoder = new EXIOptionsEncoder();
        m_scribble = new Scribble();
        // REVISIT: compression
        //m_zlibLevel = java.util.zip.Deflater.DEFAULT_COMPRESSION;
        //m_zlibStrategy = java.util.zip.Deflater.DEFAULT_STRATEGY;
        m_preserveWhitespaces = false;
        m_comparableAttributes = new ComparableAttribute[32];
        for (int i = 0; i < m_comparableAttributes.Length; i++) {
          m_comparableAttributes[i] = new ComparableAttribute();
        }
        m_n_comparableAttributes = 0;
      }

      internal void reset() {
        m_prefixUriBindings = null;
        m_locator = null;
        m_inDTD = false;
        m_contentState = ETAG;
        m_transmogrifierException = null;
      }

      public AlignmentType AlignmentType {
        set {
          if (m_scriber.AlignmentType != value) {
            m_scriber = ScriberFactory.createScriber(value);
            m_scriber.setSchema(m_schema, outerInstance.m_exiOptions.DatatypeRepresentationMap, outerInstance.m_exiOptions.DatatypeRepresentationMapBindingsCount);
            StringTable = Scriber.createStringTable(m_grammarCache);
            m_scriber.ValueMaxLength = outerInstance.m_exiOptions.ValueMaxLength;
            m_scriber.PreserveLexicalValues = outerInstance.m_exiOptions.PreserveLexicalValues;
          }
        }
      }

      public GrammarCache GrammarCache {
        set {
          if (m_grammarCache != value) {
            m_grammarCache = value;
            EXISchema schema;
            if ((schema = m_grammarCache.EXISchema) != m_schema) {
              m_schema = schema;
              m_scriber.setSchema(m_schema, outerInstance.m_exiOptions.DatatypeRepresentationMap, outerInstance.m_exiOptions.DatatypeRepresentationMapBindingsCount);
              StringTable = Scriber.createStringTable(m_grammarCache);
            }
          }
        }
        get {
          return m_grammarCache;
        }
      }


      public int ValueMaxLength {
        set {
          m_scriber.ValueMaxLength = value;
        }
      }

      public bool PreserveLexicalValues {
        set {
          m_scriber.PreserveLexicalValues = value;
        }
      }

      public void setDatatypeRepresentationMap(QName[] datatypeRepresentationMap, int n_bindings) {
        m_scriber.setSchema(m_schema, datatypeRepresentationMap, n_bindings);
      }

      /// <summary>
      /// Set an output stream to which encoded streams are written out. </summary>
      /// <param name="ostream"> output stream </param>
      internal Stream OutputStream {
        set {
          m_outputStream = value;
        }
      }

      internal PrefixUriBindings PrefixUriBindings {
        set {
          m_prefixUriBindingsDefault = value != null ? value : new PrefixUriBindings();
        }
      }

      internal bool OutputCookie {
        set {
          m_outputCookie = value;
        }
      }

      public int DeflateLevel {
        set {
          m_zlibLevel = value;
        }
      }

      public DeflateStrategy DeflateStrategy {
        set {
          m_zlibStrategy = value;
        }
      }

      public bool PreserveWhitespaces {
        set {
          m_preserveWhitespaces = value;
        }
      }

      //////////// SAX event handlers

      public void SetDocumentLocator(ILocator locator) {
        m_locator = locator;
      }

      public void StartDocument() {
        m_locusLastDepth = -1;
        m_xmlSpaceLastDepth = 0;
        m_xmlSpaceStack[m_xmlSpaceLastDepth] = false;
        m_decls.clear();
        m_charPos = 0;
        try {
          Scriber.writeHeaderPreamble(m_outputStream, m_outputCookie, outerInstance.m_outputOptions != HeaderOptionsOutputType.none);
          BitOutputStream bitOutputStream = null;
          if (outerInstance.m_outputOptions != HeaderOptionsOutputType.none) {
            BitOutputStream outputStream;
            outputStream = m_optionsEncoder.encode(outerInstance.m_exiOptions, outerInstance.m_outputOptions == HeaderOptionsOutputType.all, m_outputStream);
            if (outerInstance.m_exiOptions.AlignmentType == AlignmentType.bitPacked) {
              bitOutputStream = outputStream;
            }
          }
          m_scriber.reset();
          if (bitOutputStream != null) {
            ((BitPackedScriber)m_scriber).BitOutputStream = bitOutputStream;
          }
          else {
            m_scriber.OutputStream = m_outputStream;
          }
          hasNS = GrammarOptions.hasNS(m_grammarCache.grammarOptions);
          m_scriber.PreserveNS = hasNS;
          m_scriber.stringTable.ValuePartitionCapacity = outerInstance.m_exiOptions.ValuePartitionCapacity;
          m_scriber.BlockSize = outerInstance.m_exiOptions.BlockSize;
          m_scriber.setDeflateParams(m_zlibLevel, m_zlibStrategy);

          m_stringValueScriber = m_scriber.getValueScriberByID(Scriber.CODEC_STRING);

          m_grammarCache.retrieveRootGrammar(outerInstance.m_exiOptions.Fragment, m_scriber.eventTypesWorkSpace).init(m_scriber.currentState);
          m_prefixUriBindings = m_prefixUriBindingsDefault;

          EventTypeList eventTypes = m_scriber.NextEventTypes;
          EventType eventType;
          if ((eventType = eventTypes.SD) != null) {
            m_scriber.startDocument();
          }
          else {
            m_transmogrifierException = new TransmogrifierException(TransmogrifierException.UNEXPECTED_SD,
              (string[])null, new LocatorImpl(m_locator));
            throw new SaxException(m_transmogrifierException.Message, m_transmogrifierException);
          }
          m_scriber.writeEventType(eventType);
        }
        catch (IOException ioe) {
          throw new SaxException(ioe.Message, ioe);
        }
      }

      public void StartPrefixMapping(string prefix, string uri) {
        PrefixUriBindings bindings;
        if (prefix.Length != 0) {
          if (uri.Length != 0) {
            bindings = m_prefixUriBindings.bind(prefix, uri);
            m_decls.addDecl(prefix, uri, bindings == m_prefixUriBindings);
          }
          else {
            bindings = m_prefixUriBindings.unbind(prefix);
            m_decls.addDecl(prefix, "", bindings == m_prefixUriBindings);
          }
        }
        else {
          if (uri.Length != 0) {
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

      public void EndPrefixMapping(string prefix) {
      }

      public void StartElement(string uri, string localName, string qualifiedName, IAttributes attrs) {
        if (m_charPos > 0) {
          do_characters(false);
        }
        m_contentState = STAG;

        m_n_comparableAttributes = 0;

        string elementPrefix;
        string[] nsdecls;
        int n_nsdecls;
        if (hasNS) {
          nsdecls = m_decls.Decls;
          if ((n_nsdecls = m_decls.DeclsCount) != 0) {
            m_decls.clear();
          }
          int _pos;
          if ((_pos = qualifiedName.IndexOf(':')) != -1) {
            elementPrefix = qualifiedName.Substring(0, _pos);
          }
          else {
            elementPrefix = "";
          }
        }
        else {
          nsdecls = (string[])null;
          n_nsdecls = 0;
          elementPrefix = null;
        }
        if (++m_locusLastDepth == m_locusStack.Length) {
          int locusStackCapacity = m_locusLastDepth + 8;
          XMLLocusItemEx[] locusStack = new XMLLocusItemEx[locusStackCapacity];
          Array.Copy(m_locusStack, 0, locusStack, 0, m_locusLastDepth);
          for (int i = m_locusLastDepth; i < locusStackCapacity; i++) {
            locusStack[i] = new XMLLocusItemEx();
          }
          m_locusStack = locusStack;
        }
        XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
        if (++m_xmlSpaceLastDepth == m_xmlSpaceStack.Length) {
          int xmlSpaceStackCapacity = m_xmlSpaceLastDepth + 8;
          bool[] xmlSpaceStack = new bool[xmlSpaceStackCapacity];
          Array.Copy(m_xmlSpaceStack, 0, xmlSpaceStack, 0, m_xmlSpaceLastDepth);
          m_xmlSpaceStack = xmlSpaceStack;
        }
        // inherit parent's xml:space value
        m_xmlSpaceStack[m_xmlSpaceLastDepth] = m_xmlSpaceStack[m_xmlSpaceLastDepth - 1]; 
        locusItem.prefixUriBindings = m_prefixUriBindings;
        try {
          EventTypeList eventTypes = m_scriber.NextEventTypes;
          int i, j, i_len;
          EventType eventType = null;
          string eventTypeUri;
          sbyte itemType = -1;
          bool isWildcardElem = false;
          for (i = 0, i_len = eventTypes.Length; i < i_len; i++) {
            eventType = eventTypes.item(i);
            switch (itemType = eventType.itemType) {
              case EventType.ITEM_SE:
                if (localName.Equals(eventType.name)) {
                  eventTypeUri = eventType.uri;
                  if (!uri.Equals(eventTypeUri)) {
                    continue;
                  }
                  goto loopBreak;
                }
                break;
              case EventType.ITEM_SCHEMA_WC_ANY:
                isWildcardElem = true;
                goto loopBreak;
              case EventType.ITEM_SCHEMA_WC_NS:
                isWildcardElem = true;
                eventTypeUri = eventType.uri;
                if (!uri.Equals(eventTypeUri)) {
                  continue;
                }
                goto loopBreak;
              case EventType.ITEM_SE_WC:
                isWildcardElem = true;
                goto loopBreak;
              default:
                break;
            }
          }
          loopBreak:
          int epos = i;
          if (epos < i_len) {
            m_scriber.writeEventType(eventType);
            m_scriber.writeQName(qname.setValue(uri, localName, elementPrefix), eventType);
            locusItem.elementURI = qname.uriId;
            locusItem.elementLocalName = qname.localNameId;
            if (isWildcardElem) {
              m_scriber.startWildcardElement(epos, qname.uriId, qname.localNameId);
            }
            else {
              m_scriber.startElement(eventType);
            }
            sbyte grammarType = m_scriber.currentState.targetGrammar.grammarType;
            Debug.Assert(grammarType == Grammar.BUILTIN_GRAMMAR_ELEMENT || 
              grammarType == Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT || 
              grammarType == Grammar.SCHEMA_GRAMMAR_ELEMENT_AND_TYPE);
            if (hasNS) {
              if (n_nsdecls != 0) {
                eventTypes = m_scriber.NextEventTypes;
                for (i = 0, i_len = eventTypes.Length; i < i_len; i++) {
                  eventType = eventTypes.item(i);
                  if ((itemType = eventType.itemType) == EventType.ITEM_NS) {
                    int pos;
                    for (i = 0, pos = 0; i < n_nsdecls; i++, pos += 2) {
                      m_scriber.writeEventType(eventType);
                      string _prefix = nsdecls[pos];
                      m_scriber.writeNS(nsdecls[pos + 1], _prefix, elementPrefix.Equals(_prefix));
                    }
                    break;
                  }
                }
              }
            }
            else {
              Debug.Assert(elementPrefix == null);
            }

            bool isSchemaInformedGrammar = m_scriber.currentState.targetGrammar.SchemaInformed;
            bool useATStarForXsiType = outerInstance.m_divertBuiltinGrammarToAnyType && grammarType == Grammar.BUILTIN_GRAMMAR_ELEMENT;

            int positionOfNil = -1; // position of legitimate xsi:nil
            int positionOfType = -1; // position of legitimate xsi:type
            int n_attrs;
            if ((n_attrs = attrs.Length) != 0) {
              sortedAttributes.Clear();
              for (i = 0, i_len = n_attrs; i < i_len; i++) {
                String instanceUri = attrs.GetUri(i);
                String instanceQName = attrs.GetQName(i);
                if (W3C_2000_XMLNS_URI.Equals(instanceUri) ||
                    instanceQName.StartsWith("xmlns") && (instanceQName.Length == 5 || instanceQName[5] == ':')) { // i.e. "xmlns" or "xmlns:*"
                  --n_attrs;
                  continue;
                }
                else if (XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI.Equals(instanceUri)) {
                  string instanceName = attrs.GetLocalName(i);
                  if ("type".Equals(instanceName)) {
                    positionOfType = i;
                    --n_attrs;
                    continue;
                  }
                  else if ("nil".Equals(instanceName)) {
                    positionOfNil = i;
                    continue;
                  }
                }
                else if (XmlUriConst.W3C_XML_1998_URI.Equals(instanceUri)) {
                  if ("space".Equals(attrs.GetLocalName(i))) {
                    String attrValue = attrs.GetValue(i);
                    if ("preserve".Equals(attrValue)) {
                      m_xmlSpaceStack[m_xmlSpaceLastDepth] = true;
                    }
                    else if ("default".Equals(attrValue)) {
                      m_xmlSpaceStack[m_xmlSpaceLastDepth] = false;
                    }
                  }
                }
                ComparableAttribute comparableAttribute;
                string _prefix = hasNS ? getPrefixOfQualifiedName(instanceQName) : null;
                comparableAttribute = acquireComparableAttribute();
                comparableAttribute.init(instanceUri, attrs.GetLocalName(i), _prefix, i);
                sortedAttributes.Add(comparableAttribute);
              }
            }
            EXISchema corpus = m_grammarCache.EXISchema;
            if (useATStarForXsiType || positionOfType != -1) {
              EventType eventTypeForType;            
              if ((eventTypeForType = matchXsiType(m_scriber.NextEventTypes, useATStarForXsiType)) == null) { // xsi:type had no matching event type
                Debug.Assert(isSchemaInformedGrammar);
                m_transmogrifierException = new TransmogrifierException(TransmogrifierException.UNEXPECTED_ATTR,
                    new String[] { "type", XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, attrs.GetValue(positionOfType) }, new LocatorImpl(m_locator));
                throw new SaxException(m_transmogrifierException.Message, m_transmogrifierException);
              }
              m_scriber.writeEventType(eventTypeForType);
              String prefix = hasNS ? (positionOfType != -1 ? getPrefixOfQualifiedName(attrs.GetQName(positionOfType)) : "") : null;
              m_scriber.writeQName(qname.setValue(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "type", prefix), eventTypeForType);
              if (positionOfType != -1) {
                String xsiType = attrs.GetValue(positionOfType);
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
                m_scriber.wildcardAttribute(eventTypeForType.Index, XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID,
                    EXISchemaConst.XSI_LOCALNAME_TYPE_ID);
              }
              if (corpus != null) {
                int tp;
                if ((tp = corpus.getTypeOfSchema(qname.namespaceName, qname.localName)) != EXISchema.NIL_NODE) {
                  m_scriber.xsitp(tp);
                  isSchemaInformedGrammar = true;
                }
              }
            }
            if (positionOfNil != -1) {
              string nilval = attrs.GetValue(positionOfNil);
              bool? nilled = null;
              if (isSchemaInformedGrammar) {
                int length = nilval.Length;
                int limit = length - 1;
                for (; limit > 0; limit--) {
                  switch (nilval[limit]) {
                    case '\t':
                    case '\n':
                    case '\r':
                    case ' ':
                      break;
                    default:
                      goto skipTrailingWhiteSpacesBreak;
                  }
                }
                skipTrailingWhiteSpacesBreak:
                ++limit;
                int pos;
                for (pos = 0; pos < length; pos++) {
                  switch (nilval[pos]) {
                    case '\t':
                    case '\n':
                    case '\r':
                    case ' ':
                      break;
                    default:
                      goto skipWhiteSpacesBreak;
                  }
                }
                skipWhiteSpacesBreak:
                if (pos != 0 || limit != length) {
                  length = limit - pos;
                }
                eventTypes = m_scriber.NextEventTypes;
                bool nilAvailable = false;
                for (i = 0, i_len = eventTypes.Length; i < i_len; i++) {
                  eventType = eventTypes.item(i);
                  itemType = eventType.itemType;
                  if (itemType == EventType.ITEM_SCHEMA_NIL) {
                    nilAvailable = true;
                    if (length == 4) {
                      if (nilval[pos] == 't' && nilval[pos + 1] == 'r' && nilval[pos + 2] == 'u' && nilval[pos + 3] == 'e') {
                        nilled = true;
                        break;
                      }
                    }
                    else if (length == 5) {
                      if (nilval[pos] == 'f' && nilval[pos + 1] == 'a' && nilval[pos + 2] == 'l' && nilval[pos + 3] == 's' && nilval[pos + 4] == 'e') {
                        nilled = false;
                        break;
                      }
                    }
                    else if (length == 1) {
                      char c = nilval[pos];
                      if (c == '1') {
                        nilled = true;
                        break;
                      }
                      else if (c == '0') {
                        nilled = false;
                        break;
                      }
                    }
                  }
                  else if (itemType == EventType.ITEM_AT_WC_ANY_UNTYPED) {
                    Debug.Assert(nilAvailable);
                    break;
                  }
                }
                if (i == i_len) {
                  m_transmogrifierException = new TransmogrifierException(TransmogrifierException.UNEXPECTED_ATTR, 
                    new string[] { "nil", XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, nilval }, new LocatorImpl(m_locator));
                  throw new SaxException(m_transmogrifierException.Message, m_transmogrifierException);
                }
              }
              if (nilled.HasValue) {
                m_scriber.writeEventType(eventType);
                m_scriber.writeQName(qname.setValue(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "nil", 
                  hasNS ? getPrefixOfQualifiedName(attrs.GetQName(positionOfNil)) : null), eventType);
                m_scriber.writeXsiNilValue((bool)nilled, nilval);
                if ((bool)nilled) {
                  m_scriber.nillify(eventType.Index);
                }
                --n_attrs;
              }
              else {
                if (isSchemaInformedGrammar) {
                  Debug.Assert(itemType == EventType.ITEM_AT_WC_ANY_UNTYPED);
                  // process invalid xsi:nil value using AT(*) [untyped value]
                  m_scriber.writeEventType(eventType);
                  m_scriber.writeQName(qname.setValue(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, "nil", 
                    hasNS ? getPrefixOfQualifiedName(attrs.GetQName(positionOfNil)) : null), eventType);
                  m_stringValueScriber.scribe(nilval, m_scribble, EXISchemaConst.XSI_LOCALNAME_NIL_ID, 
                    XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID, EXISchema.NIL_NODE, m_scriber);
                  --n_attrs;
                }
                else {
                  // treat it as a vanilla attribute
                  ComparableAttribute comparableAttribute;
                  string _prefix = hasNS ? getPrefixOfQualifiedName(attrs.GetQName(positionOfNil)) : null;
                  comparableAttribute = acquireComparableAttribute();
                  comparableAttribute.init(attrs.GetUri(positionOfNil), attrs.GetLocalName(positionOfNil), _prefix, positionOfNil);
                  sortedAttributes.Add(comparableAttribute);
                }
              }
            }
            if (n_attrs != 0) {
              IEnumerator<ComparableAttribute> iter;
              for (i = 0, iter = sortedAttributes.GetEnumerator(); i < n_attrs; i++) {
                eventTypes = m_scriber.NextEventTypes;
                bool moveNextStatus = iter.MoveNext();
                Debug.Assert(moveNextStatus);
                ComparableAttribute attr = iter.Current;
                string instanceUri = attr.uri;
                string instanceName = attr.name;
                int tp = EXISchema.NIL_NODE;
                ValueScriber valueScriber = null;
                if (isSchemaInformedGrammar) {
                  eventType = eventTypes.getSchemaAttribute(instanceUri, instanceName);
                  if (eventType != null) {
                    EventTypeSchema eventTypeSchemaAttribute;
                    eventTypeSchemaAttribute = (EventTypeSchema)eventType;
                    if ((tp = eventTypeSchemaAttribute.nd) != EXISchema.NIL_NODE) {
                      valueScriber = m_scriber.getValueScriber(tp);
                      if (!valueScriber.process(attrs.GetValue(attr.index), tp, m_schema, m_scribble, m_scriber)) {
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
                      string prefix = attr.prefix;
                      if (hasNS) {
                        verifyPrefix(instanceUri, prefix);
                      }
                      m_scriber.writeQName(qname.setValue(instanceUri, instanceName, prefix), eventType);
                      valueScriber.scribe(attrs.GetValue(attr.index), m_scribble, qname.localNameId, qname.uriId, tp, m_scriber);
                      m_scriber.attribute(eventType);
                      continue;
                    }
                  }
                  if (eventType == null) {
                    if ((eventType = eventTypes.getSchemaAttributeWildcardNS(instanceUri)) == null) {
                      eventType = eventTypes.SchemaAttributeWildcardAny;
                    }
                    if (eventType != null) {
                      int _attr;
                      if ((_attr = corpus.getGlobalAttrOfSchema(instanceUri, instanceName)) != EXISchema.NIL_NODE) {
                        tp = corpus.getTypeOfAttr(_attr);
                        Debug.Assert(tp != EXISchema.NIL_NODE);
                        valueScriber = m_scriber.getValueScriber(tp);
                        if (!valueScriber.process(attrs.GetValue(attr.index), tp, m_schema, m_scribble, m_scriber)) {
                          tp = EXISchema.NIL_NODE; // revert variable tp back to NIL_NODE
                          eventType = null;
                        }
                      }
                      else {
                        valueScriber = m_stringValueScriber;
                      }
                    }
                    if (eventType == null && (eventType = eventTypes.AttributeWildcardAnyUntyped) != null) {
                      valueScriber = m_stringValueScriber;
                    }
                    if (eventType != null) {
                      m_scriber.writeEventType(eventType);
                      string prefix = attr.prefix;
                      if (hasNS) {
                        verifyPrefix(instanceUri, prefix);
                      }
                      m_scriber.writeQName(qname.setValue(instanceUri, instanceName, prefix), eventType);
                      valueScriber.scribe(attrs.GetValue(attr.index), m_scribble, qname.localNameId, qname.uriId, tp, m_scriber);
                      if (itemType == EventType.ITEM_AT_WC_ANY_UNTYPED) {
                        m_scriber.wildcardAttribute(eventType.Index, qname.uriId, qname.localNameId);
                      }
                      continue;
                    }
                  }
                }
                else { // built-in grammar
                  if ((eventType = eventTypes.getLearnedAttribute(instanceUri, instanceName)) != null || (eventType = eventTypes.AttributeWildcardAnyUntyped) != null) {
                    valueScriber = m_stringValueScriber;
                  }
                  if (eventType != null) {
                    m_scriber.writeEventType(eventType);
                    string prefix = attr.prefix;
                    if (hasNS) {
                      verifyPrefix(instanceUri, prefix);
                    }
                    m_scriber.writeQName(qname.setValue(instanceUri, instanceName, prefix), eventType);
                    valueScriber.scribe(attrs.GetValue(attr.index), m_scribble, qname.localNameId, qname.uriId, tp, m_scriber);
                    if (eventType.itemType == EventType.ITEM_AT_WC_ANY_UNTYPED) {
                      m_scriber.wildcardAttribute(eventType.Index, qname.uriId, qname.localNameId);
                    }
                    continue;
                  }
                }
                m_transmogrifierException = new TransmogrifierException(TransmogrifierException.UNEXPECTED_ATTR, 
                  new string[] { instanceName, instanceUri, attrs.GetValue(attr.index) }, new LocatorImpl(m_locator));
                throw new SaxException(m_transmogrifierException.Message, m_transmogrifierException);
              }
            }
          }
          else {
            m_transmogrifierException = new TransmogrifierException(TransmogrifierException.UNEXPECTED_ELEM, 
              new string[] { localName, uri }, new LocatorImpl(m_locator));
            throw new SaxException(m_transmogrifierException.Message, m_transmogrifierException);
          }
        }
        catch (IOException ioe) {
          throw new SaxException(ioe.Message, ioe);
        }
      }

      public void IgnorableWhitespace(char[] ch, int start, int len) {
        appendCharacters(ch, start, len);
      }

      public void SignificantWhitespace(char[] ch, int start, int len) {
        appendCharacters(ch, start, len);
      }

      public void Characters(char[] ch, int start, int len) {
        appendCharacters(ch, start, len);
      }

      public BinaryDataSink startBinaryData(long totalLength) {
        m_charPos = 0;
        try {
          EventTypeList eventTypes;
          eventTypes = m_scriber.NextEventTypes;
          EventType eventType;
          if ((eventType = eventTypes.SchemaCharacters) != null) {
            int tp = m_scriber.currentState.contentDatatype;
            Debug.Assert(tp != EXISchema.NIL_NODE);
            ValueScriber valueScriber = m_scriber.getValueScriber(tp);
            if (valueScriber is BinaryDataSink) {
              BinaryDataSink binaryDataSink = (BinaryDataSink)valueScriber;
              m_scriber.writeEventType(eventType);
              m_scriber.characters(eventType);
              try {
                binaryDataSink.startBinaryData(totalLength, m_scribble, m_scriber);
              }
              catch (ScriberRuntimeException se) {
                m_transmogrifierException = new TransmogrifierException(TransmogrifierException.SCRIBER_ERROR, 
                  new string[] { se.Message }, new LocatorImpl(m_locator));
                m_transmogrifierException.Exception = se;
                throw new SaxException(m_transmogrifierException.Message, m_transmogrifierException);
              }
              return binaryDataSink;
            }
          }
          m_transmogrifierException = new TransmogrifierException(TransmogrifierException.UNEXPECTED_BINARY_VALUE, 
            (string[])null, new LocatorImpl(m_locator));
          throw new SaxException(m_transmogrifierException.Message, m_transmogrifierException);
        }
        catch (IOException ioe) {
          throw new SaxException(ioe.Message, ioe);
        }
      }

      public void binaryData(byte[] octets, int offset, int length, BinaryDataSink binaryDataSink) {
        try {
          binaryDataSink.binaryData(octets, offset, length, m_scribble, m_scriber);
        }
        catch (IOException ioe) {
          throw new SaxException(ioe.Message, ioe);
        }
      }

      public void endBinaryData(BinaryDataSink binaryDataSink) {
        XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
        try {
          binaryDataSink.endBinaryData(m_scribble, locusItem.elementLocalName, locusItem.elementURI, m_scriber);
        }
        catch (ScriberRuntimeException se) {
          m_transmogrifierException = new TransmogrifierException(TransmogrifierException.SCRIBER_ERROR, 
            new string[] { se.Message }, new LocatorImpl(m_locator));
          m_transmogrifierException.Exception = se;
          throw new SaxException(m_transmogrifierException.Message, m_transmogrifierException);
        }
        catch (IOException ioe) {
          throw new SaxException(ioe.Message, ioe);
        }
      }

      /// <summary>
      /// Process characters. </summary>
      /// <param name="isContent"> true when the characters need to be treated as a content. </param>
      /// <exception cref="SAXException"> </exception>
      internal void do_characters(bool isContent) {
        isContent = isContent || m_contentState == CONT;
        try {
          EventTypeList eventTypes;
          eventTypes = m_scriber.NextEventTypes;
          int tp = EXISchema.NIL_NODE;
          EventType eventType;
          if ((eventType = eventTypes.SchemaCharacters) != null) {
            tp = m_scriber.currentState.contentDatatype;
            Debug.Assert(tp != EXISchema.NIL_NODE);
            ValueScriber valueScriber = m_scriber.getValueScriber(tp);
            // REVISIT: avoid converting to string.
            string stringValue = new string(m_charBuf, 0, m_charPos);
            if (valueScriber.process(stringValue, tp, m_schema, m_scribble, m_scriber)) {
              m_scriber.writeEventType(eventType);
              m_scriber.characters(eventType);
              XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
              valueScriber.scribe(stringValue, m_scribble, locusItem.elementLocalName, locusItem.elementURI, tp, m_scriber);
              m_charPos = 0;
              return;
            }
          }
          bool xmlSpacePreserve = m_xmlSpaceStack[m_xmlSpaceLastDepth];
          if ((eventType = eventTypes.Characters) != null) {
            bool preserveWhitespaces = m_preserveWhitespaces || xmlSpacePreserve;
            if (!preserveWhitespaces) {
              if (tp != EXISchema.NIL_NODE) {
                preserveWhitespaces = true;
              }
              else {
                if (m_scriber.currentState.targetGrammar.SchemaInformed) {
                  preserveWhitespaces = eventType.itemType == EventType.ITEM_SCHEMA_CH_MIXED;
                }
                else {
                  preserveWhitespaces = isContent;
                }
              }
            }
            if (preserveWhitespaces) {
              m_scriber.writeEventType(eventType);
              m_scriber.undeclaredCharacters(eventType.Index);
              XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
              m_stringValueScriber.scribe(new string(m_charBuf, 0, m_charPos), m_scribble, 
                locusItem.elementLocalName, locusItem.elementURI, EXISchema.NIL_NODE, m_scriber);
            }
            else {
              int _len = m_charPos;
              for (int i = 0; i < _len; i++) {
                switch (m_charBuf[i]) {
                  case '\t':
                  case '\n':
                  case '\r':
                  case ' ':
                    continue;
                  default:
                    m_scriber.writeEventType(eventType);
                    m_scriber.undeclaredCharacters(eventType.Index);
                    XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
                    m_stringValueScriber.scribe(new string(m_charBuf, 0, m_charPos), m_scribble, 
                      locusItem.elementLocalName, locusItem.elementURI, EXISchema.NIL_NODE, m_scriber);
                    goto iLoopBreak;
                }
              }
              iLoopBreak:;
            }
            m_charPos = 0;
            return;
          }
          int len = m_charPos;
          for (int i = 0; i < len; i++) {
            switch (m_charBuf[i]) {
              case '\t':
              case '\n':
              case '\r':
              case ' ':
                continue;
              default:
                m_transmogrifierException = new TransmogrifierException(TransmogrifierException.UNEXPECTED_CHARS, 
                  new string[] { new string(m_charBuf, 0, m_charPos) }, new LocatorImpl(m_locator));
                throw new SaxException(m_transmogrifierException.Message, m_transmogrifierException);
            }
          }
          if (xmlSpacePreserve && m_preserveWhitespaces) {
            m_transmogrifierException = new TransmogrifierException(TransmogrifierException.CANNOT_PRESERVE_WHITESPACES,
                (String[])null, new LocatorImpl(m_locator));
            throw new SaxException(m_transmogrifierException.Message, m_transmogrifierException);
          }
        }
        catch (IOException ioe) {
          throw new SaxException(ioe.Message, ioe);
        }
        m_charPos = 0;
      }

      public void EndElement(string uri, string localName, string qname) {
        if (m_charPos > 0) {
          do_characters(m_contentState == STAG);
        }
        m_contentState = ETAG;
        try {
          EventTypeList eventTypes;
          eventTypes = m_scriber.NextEventTypes;

          EventType eventType = null;
          if ((eventType = eventTypes.EE) != null) {
            m_scriber.writeEventType(eventType);
            m_scriber.endElement();
          }
          else {
            Debug.Assert(m_scriber.currentState.targetGrammar.SchemaInformed);
            if ((eventType = eventTypes.SchemaCharacters) != null) {
              int tp = m_scriber.currentState.contentDatatype;
              Debug.Assert(tp != EXISchema.NIL_NODE);
              ValueScriber valueScriber = m_scriber.getValueScriber(tp);
              if (valueScriber.process("", tp, m_schema, m_scribble, m_scriber)) {
                m_scriber.writeEventType(eventType);
                m_scriber.characters(eventType);
                XMLLocusItemEx locusItem = m_locusStack[m_locusLastDepth];
                valueScriber.scribe("", m_scribble, locusItem.elementLocalName, locusItem.elementURI, tp, m_scriber);
                EndElement(uri, localName, qname);
                return; // Good luck!
              }
            }
            m_transmogrifierException = new TransmogrifierException(
              TransmogrifierException.UNEXPECTED_END_ELEM, 
              new string[] { localName, uri }, new LocatorImpl(m_locator));
            throw new SaxException(m_transmogrifierException.Message, m_transmogrifierException);
          }
        }
        catch (IOException ioe) {
          throw new SaxException(ioe.Message, ioe);
        }
        --m_xmlSpaceLastDepth;
        m_prefixUriBindings = m_locusLastDepth-- != 0 ? m_locusStack[m_locusLastDepth].prefixUriBindings : m_prefixUriBindingsDefault;
      }

      public void EndDocument() {
        if (m_transmogrifierException != null)
          return;
        if (m_charPos > 0) {
          do_characters(false);
        }
        try {
          EventTypeList eventTypes = m_scriber.NextEventTypes;
          EventType eventType;
          if ((eventType = eventTypes.ED) != null) {
            m_scriber.endDocument();
          }
          else {
            m_transmogrifierException = new TransmogrifierException(
              TransmogrifierException.UNEXPECTED_ED, (string[])null, new LocatorImpl(m_locator));
            throw new SaxException(m_transmogrifierException.Message, m_transmogrifierException);
          }
          m_scriber.writeEventType(eventType);
          m_scriber.finish();
        }
        catch (IOException ioe) {
          throw new SaxException(ioe.Message, ioe);
        }
      }

      public void ProcessingInstruction(string target, string data) {
        if (outerInstance.m_exiOptions.PreservePIs) {
          if (m_charPos > 0) {
            do_characters(true);
          }
          EventTypeList eventTypes = m_scriber.NextEventTypes;
          int len = eventTypes.Length;
          for (int i = 0; i < len; i++) {
            EventType eventType = eventTypes.item(i);
            if (eventType.itemType == EventType.ITEM_PI) {
              m_scriber.miscContent(eventType.Index);
              try {
                m_scriber.writeEventType(eventType);
                m_scriber.writeName(target);
                m_scriber.writeText(data);
              }
              catch (IOException ioe) {
                throw new SaxException(ioe.Message, ioe);
              }
              m_contentState = CONT;
              break;
            }
          }
        }
      }

      public void SkippedEntity(string name) {
        if (outerInstance.m_exiOptions.PreserveDTD) {
          if (m_charPos > 0) {
            do_characters(true);
          }
          EventTypeList eventTypes = m_scriber.NextEventTypes;
          int len = eventTypes.Length;
          for (int i = 0; i < len; i++) {
            EventType eventType = eventTypes.item(i);
            if (eventType.itemType == EventType.ITEM_ER) {
              m_scriber.miscContent(eventType.Index);
              try {
                m_scriber.writeEventType(eventType);
                m_scriber.writeName(name);
              }
              catch (IOException ioe) {
                throw new SaxException(ioe.Message, ioe);
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

      public void Comment(char[] ch, int start, int length) {
        if (!m_inDTD && outerInstance.m_exiOptions.PreserveComments) {
          if (m_charPos > 0) {
            do_characters(true);
          }
          EventTypeList eventTypes = m_scriber.NextEventTypes;
          int len = eventTypes.Length;
          for (int i = 0; i < len; i++) {
            EventType eventType = eventTypes.item(i);
            if (eventType.itemType == EventType.ITEM_CM) {
              m_scriber.miscContent(eventType.Index);
              try {
                m_scriber.writeEventType(eventType);
                m_scriber.writeText(new string(ch, start, length));
              }
              catch (IOException ioe) {
                throw new SaxException(ioe.Message, ioe);
              }
              m_contentState = CONT;
              break;
            }
          }
        }
      }

      public void StartDtd(string name, string publicId, string systemId) {
        EventTypeList eventTypes = m_scriber.NextEventTypes;
        int i, len;
        EventType eventType = null;
        for (i = 0, len = eventTypes.Length; i < len; i++) {
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
            throw new SaxException(ioe.Message, ioe);
          }
        }
        m_inDTD = true;
      }

      public void EndDtd() {
        m_inDTD = false;
      }

      public void StartCData() {
      }

      public void EndCData() {
      }

      public void StartEntity(string name) {
      }

      public void EndEntity(string name) {
      }

      ///////////////////////////////////////////////////////////////////////////
      // Private convenience functions
      ///////////////////////////////////////////////////////////////////////////

      internal void appendCharacters(char[] ch, int start, int len) {
        while (m_charPos + len > m_charBuf.Length) {
          char[] charBuf = new char[2 * m_charBuf.Length];
          Array.Copy(m_charBuf, 0, charBuf, 0, m_charBuf.Length);
          m_charBuf = charBuf;
        }
        Array.Copy(ch, start, m_charBuf, m_charPos, len);
        m_charPos += len;
      }

      internal StringTable StringTable {
        set {
          m_scriber.StringTable = value;
        }
      }

      private ComparableAttribute acquireComparableAttribute() {
        if (m_n_comparableAttributes == m_comparableAttributes.Length) {
          ComparableAttribute[] comparableAttributes;
          int len = m_n_comparableAttributes + 32; // new array size
          comparableAttributes = new ComparableAttribute[len];
          Array.Copy(m_comparableAttributes, 0, comparableAttributes, 0, m_n_comparableAttributes);
          for (int i = m_n_comparableAttributes; i < len; i++) {
            comparableAttributes[i] = new ComparableAttribute();
          }
          m_comparableAttributes = comparableAttributes;
        }
        return m_comparableAttributes[m_n_comparableAttributes++];
      }

      internal void verifyPrefix(string uri, string prefix) {
        if (prefix.Length != 0) {
          string _uri = m_prefixUriBindings.getUri(prefix);
          if (_uri == null) {
            m_transmogrifierException = new TransmogrifierException(TransmogrifierException.PREFIX_NOT_BOUND, 
              new string[] { prefix }, new LocatorImpl(m_locator));
            throw new SaxException(m_transmogrifierException.Message, m_transmogrifierException);
          }
          else if (!uri.Equals(_uri)) {
            m_transmogrifierException = new TransmogrifierException(TransmogrifierException.PREFIX_BOUND_TO_ANOTHER_NAMESPACE, 
              new string[] { prefix, _uri }, new LocatorImpl(m_locator));
            throw new SaxException(m_transmogrifierException.Message, m_transmogrifierException);
          }
        }
      }

      private EventType matchXsiType(EventTypeList eventTypes, bool useWildcardAT) {
        Grammar targetGrammar = m_scriber.currentState.targetGrammar;
        bool isSchemaInformedGrammar = targetGrammar.SchemaInformed;
        for (int j = 0, j_len = eventTypes.Length; j < j_len; j++) {
          EventType eventType = eventTypes.item(j);
          switch (eventType.itemType) {
            case EventType.ITEM_SCHEMA_TYPE:
              Debug.Assert(targetGrammar.grammarType != Grammar.BUILTIN_GRAMMAR_ELEMENT);
              return eventType;
            case EventType.ITEM_AT_WC_ANY_UNTYPED:
              if (!isSchemaInformedGrammar)
                return eventType;
              break;
            case EventType.ITEM_AT:
              Debug.Assert(!isSchemaInformedGrammar);
              if (!useWildcardAT && "type".Equals(eventType.name) && 
                  XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI.Equals(eventType.uri)) {
                return eventType;
              }
              break;
          }
        }
        return null;
      }

    }

    private static string getPrefixOfQualifiedName(string qualifiedName) {
      int pos;
      if ((pos = qualifiedName.IndexOf(':')) != -1) {
        return qualifiedName.Substring(0, pos);
      }
      else {
        return "";
      }
    }

    private static QName setXsiTypeValue(QName qname, string qualifiedName, PrefixUriBindings namespacePrefixMap) {
      qname.qName = qualifiedName;
      int i = qualifiedName.IndexOf(':');
      if (i != -1) { // with prefix
        qname.prefix = qualifiedName.Substring(0, i);
        qname.namespaceName = namespacePrefixMap.getUri(qname.prefix);
        if (qname.namespaceName != null) {
          qname.localName = qualifiedName.Substring(i + 1);
        }
        else { //  prefix did not resolve into an uri
          qname.namespaceName = "";
          qname.localName = qualifiedName;
          qname.prefix = "";
        }
      }
      else { // no prefix
        qname.localName = qualifiedName;
        qname.namespaceName = namespacePrefixMap.DefaultUri;
        qname.prefix = "";
      }
      return qname;
    }

    private class ComparableAttribute : IComparable<ComparableAttribute> {
      internal sbyte priority; // 0 ... 2 ( 0 -> xsi:type, 1 -> xsi:nil, 2 -> other)
      internal string uri;
      internal string name;
      internal string prefix;
      internal int index;
      public virtual void init(string uri, string name, string prefix, int index) {
        this.uri = uri;
        this.name = name;
        this.index = index;
        this.prefix = prefix;
        sbyte priority = 2;
        if (uri.Equals(XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI)) {
          Debug.Assert(!"type".Equals(name));
          if ("nil".Equals(name)) {
            priority = 1;
          }
        }
        this.priority = priority;
      }
      public virtual int CompareTo(ComparableAttribute other) {
        int res;
        if ((res = priority - other.priority) != 0) {
          return res;
        }
        if ((res = name.CompareTo(other.name)) != 0) {
          return res;
        }
        else {
          return uri.CompareTo(other.uri);
        }
      }
    }

    private sealed class NamespaceDeclarations {
      private readonly Transmogrifier outerInstance;

      internal string[] decls; // new pairs of prefixes and namespaces
      internal int n_decls;

      public NamespaceDeclarations(Transmogrifier outerInstance) {
        this.outerInstance = outerInstance;
        decls = new string[16];
        n_decls = 0;
      }

      public string[] Decls {
        get {
          return decls;
        }
      }

      public int DeclsCount {
        get {
          return n_decls;
        }
      }

      internal void addDecl(string prefix, string uri, bool checkDuplicate) {
        int n_strings = n_decls << 1;
        if (checkDuplicate) {
          for (int i = 0; i < n_strings; i += 2) {
            if (decls[i].Equals(prefix)) {
              decls[i + 1] = uri;
              return;
            }
          }
        }
        string[] pairs;
        if (n_strings != decls.Length) {
          pairs = decls;
        }
        else {
          pairs = new string[2 * n_strings];
          Array.Copy(decls, 0, pairs, 0, n_strings);
        }
        pairs[n_strings] = prefix;
        pairs[n_strings + 1] = uri;

        decls = pairs;
        ++n_decls;
      }

      public void clear() {
        n_decls = 0;
      }
    }

    private class XMLLocusItemEx {
      internal int elementURI;
      internal int elementLocalName;
      internal PrefixUriBindings prefixUriBindings;
    }

  }

}