using System.Diagnostics;
using System.IO;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EXIOptions = Nagasena.Proc.Common.EXIOptions;
using EXIOptionsException = Nagasena.Proc.Common.EXIOptionsException;
using QName = Nagasena.Proc.Common.QName;
using SchemaId = Nagasena.Proc.Common.SchemaId;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using HeaderOptionsInputStream = Nagasena.Proc.IO.HeaderOptionsInputStream;
using BitPackedScanner = Nagasena.Proc.IO.BitPackedScanner;
using Scanner = Nagasena.Proc.IO.Scanner;
using ScannerFactory = Nagasena.Proc.IO.ScannerFactory;
using EXISchema = Nagasena.Schema.EXISchema;
using EmptySchema = Nagasena.Schema.EmptySchema;

namespace Nagasena.Proc {

  /// <summary>
  /// EXIDecoder provides methods to configure and 
  /// instantiate a <seealso cref="Nagasena.Proc.io.Scanner"/> object
  /// you can use to parse the contents of an EXI stream. 
  /// </summary>
  public class EXIDecoder {

    private Scanner m_scanner;

    private GrammarCache m_grammarCache;
    private EXISchema m_schema;
    private EXISchemaResolver m_schemaResolver;

    private Stream m_inputStream;

    private readonly EXIOptions m_exiOptions;
    private readonly EXIOptions m_exiHeaderOptions;
    private readonly HeaderOptionsDecoder m_optionsDecoder;

    private const int DEFAULT_INFLATOR_BUF_SIZE = 8192;
    private readonly int m_inflatorBufSize;

    private bool m_binaryDataEnabled;
    private int m_initialBinaryDataBufferSize;

    /// <summary>
    /// Creates an instance of EXIDecoder with the default inflator 
    /// buffer size of 8192 bytes.  Buffer size is only used when
    /// the EXI stream is encoded with EXI compression.
    /// </summary>
    public EXIDecoder() : this(DEFAULT_INFLATOR_BUF_SIZE) {
    }

    /// <summary>
    /// Creates an instance of EXIDecoder with the specified inflator buffer 
    /// size. When dynamic memory is limited on the target device, reducing 
    /// the buffer size can improve performance and avoid runtime errors. Buffer 
    /// size is only used when the EXI stream is encoded with EXI compression. </summary>
    /// <param name="inflatorBufSize"> size of the buffer, in bytes. </param>
    public EXIDecoder(int inflatorBufSize) {
      m_inflatorBufSize = inflatorBufSize;
      m_exiOptions = new EXIOptions();
      m_exiHeaderOptions = new EXIOptions();
      m_optionsDecoder = new HeaderOptionsDecoder();
      m_grammarCache = null;
      m_schema = null;
      m_schemaResolver = null;
      m_scanner = ScannerFactory.createScanner(AlignmentType.bitPacked, m_inflatorBufSize);
      m_scanner.setSchema(m_schema, (QName[])null, 0);
      m_scanner.StringTable = Scanner.createStringTable(m_grammarCache);
      m_binaryDataEnabled = false;
      m_initialBinaryDataBufferSize = 8192;
    }

    /// <summary>
    /// Set an input stream from which the encoded stream is read. </summary>
    /// <param name="istream"> InputSream to be read. </param>
    public Stream InputStream {
      set {
        m_inputStream = value;
      }
    }

    /// <summary>
    /// Set the bit alignment style of the stream to be decoded.
    /// </summary>
    /// <param name="alignmentType"> <seealso cref="Nagasena.Proc.common.AlignmentType"/> object </param>
    /// <exception cref="EXIOptionsException"> </exception>
    public AlignmentType AlignmentType {
      set {
        m_exiOptions.AlignmentType = value;
        if (m_scanner.AlignmentType != value) {
          m_scanner = ScannerFactory.createScanner(value, m_inflatorBufSize);
          m_scanner.setSchema(m_schema, m_exiOptions.DatatypeRepresentationMap, m_exiOptions.DatatypeRepresentationMapBindingsCount);
          m_scanner.StringTable = Scanner.createStringTable(m_grammarCache);
          m_scanner.ValueMaxLength = m_exiOptions.ValueMaxLength;
          m_scanner.PreserveLexicalValues = m_exiOptions.PreserveLexicalValues;
        }
      }
    }

    /// <summary>
    /// Set whether the document is a fragment. Fragments are nonstandard
    /// XML documents with multiple root elements. Default is false. </summary>
    /// <param name="isFragment"> true if the stream is an XML fragment </param>
    public bool Fragment {
      set {
        m_exiOptions.Fragment = value;
      }
    }

    /// <summary>
    /// Set the GrammarCache used in decoding EXI streams. </summary>
    /// <param name="grammarCache"> <seealso cref="Nagasena.Proc.grammars.GrammarCache"/> </param>
    /// <exception cref="EXIOptionsException"> </exception>
    public GrammarCache GrammarCache {
      set {
        m_exiOptions.GrammarOptions = value.grammarOptions;
        if (m_grammarCache != value) {
          m_grammarCache = value;
          EXISchema schema;
          if ((schema = m_grammarCache.EXISchema) != m_schema) {
            m_schema = schema;
            m_scanner.setSchema(m_schema, m_exiOptions.DatatypeRepresentationMap, m_exiOptions.DatatypeRepresentationMapBindingsCount);
            m_scanner.StringTable = Scanner.createStringTable(m_grammarCache);
          }
        }
      }
    }

    /// <summary>
    /// Not for public use. </summary>
    /// <param name="schemaResolver">
    /// @y.exclude </param>
    public EXISchemaResolver EXISchemaResolver {
      set {
        m_schemaResolver = value;
      }
    }

    /// <summary>
    /// Set the size, in number of values, of the information that will be 
    /// processed as a chunk of the entire EXI stream. Reducing the block size 
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
        m_scanner.ValueMaxLength = value;
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

    /// <summary>
    /// Set to <i>true</i> to preserve the original string values from the EXI
    /// stream. For example, a date string might be converted to a different
    /// format when interpreted by the EXIDecoder. Preserving the lexical values
    /// ensures that the identical strings are restored, and not just their 
    /// logical values.
    /// </summary>
    /// <param name="preserveLexicalValues"> true to keep original strings intact </param>
    /// <exception cref="EXIOptionsException"> </exception>
    public bool PreserveLexicalValues {
      set {
        m_exiOptions.PreserveLexicalValues = value;
        m_scanner.PreserveLexicalValues = value;
      }
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
    ///   decoderInstance.setDatatypeRepresentationMap(dtrm, 1); // The array, and the number of pairs (1).
    /// </pre>
    /// </summary>
    /// <param name="dtrm"> a sequence of pairs of datatype QName and datatype representation QName </param>
    /// <param name="n_bindings"> the number of QName pairs </param>
    public void setDatatypeRepresentationMap(QName[] dtrm, int n_bindings) {
      if (!QName.isSame(m_exiOptions.DatatypeRepresentationMap, m_exiOptions.DatatypeRepresentationMapBindingsCount, dtrm, n_bindings)) {
        m_exiOptions.setDatatypeRepresentationMap(dtrm, n_bindings);
        m_scanner.setSchema(m_schema, dtrm, n_bindings);
      }
    }

    /// <summary>
    /// Each binary value will be returned as in a EventDescription of EVENT_BLOB 
    /// instead of EVENT_CH when enabled. </summary>
    /// <param name="enable"> </param>
    public bool EnableBinaryData {
      set {
        m_binaryDataEnabled = value;
      }
    }

    public int InitialBinaryDataBufferSize {
      set {
        m_initialBinaryDataBufferSize = value;
      }
    }

    /// <summary>
    /// This method reads and configures any header options present
    /// in the EXI stream, then returns a <seealso cref="Nagasena.Proc.io.Scanner"/> 
    /// object you can use to parse the values from the EXI stream. </summary>
    /// <returns> Scanner parsable object with header options applied. </returns>
    /// <exception cref="IOException"> </exception>
    /// <exception cref="EXIOptionsException"> </exception>
    public virtual Scanner processHeader() {
      int val = m_inputStream.ReadByte();
      if (val == 36) {
        m_inputStream.ReadByte(); // 69
        m_inputStream.ReadByte(); // 88
        m_inputStream.ReadByte(); // 73
        val = m_inputStream.ReadByte();
      }
      Scanner scanner;
      GrammarCache grammarCache;
      bool isFragment;
      HeaderOptionsInputStream bitInputStream = null;
      if ((val & 0x20) != 0) {
        AlignmentType alignmentType;
        EXISchema schema;
        m_exiHeaderOptions.init();
        HeaderOptionsInputStream inputStream;
        inputStream = m_optionsDecoder.decode(m_exiHeaderOptions, m_inputStream);
        short grammarOptions = m_exiHeaderOptions.toGrammarOptions();
        SchemaId schemaId;
        if ((schemaId = m_exiHeaderOptions.SchemaId) != null) {
          string schemaIdValue;
          if ((schemaIdValue = schemaId.Value) == null) {
            schema = null;
            grammarCache = new GrammarCache(grammarOptions);
          }
          else if (schemaIdValue.Length == 0) {
            // REVISIT: test
            Debug.Assert(false);
            schema = EmptySchema.EXISchema;
            grammarCache = new GrammarCache(schema, grammarOptions);
          }
          else {
            GrammarCache specifiedGrammarCache = null;
            if (m_schemaResolver != null && (specifiedGrammarCache = m_schemaResolver.resolveSchema(schemaIdValue, grammarOptions)) != null) {
              grammarCache = specifiedGrammarCache;
              schema = grammarCache.EXISchema;
            }
            else { // Failed to resolve schemaIdValue into a grammar cache
              // REVISIT: check the ID associated with m_schema (if any has been provided).
              schema = m_schema;
              if (m_grammarCache.grammarOptions != grammarOptions) {
                grammarCache = new GrammarCache(schema, grammarOptions);
              }
              else {
                grammarCache = m_grammarCache;
              }
            }
          }
        }
        else {
          schema = m_schema;
          if (m_grammarCache.grammarOptions != grammarOptions) {
            grammarCache = new GrammarCache(schema, grammarOptions);
          }
          else {
            grammarCache = m_grammarCache;
          }
        }
        if ((alignmentType = m_exiHeaderOptions.AlignmentType) == AlignmentType.bitPacked) {
          bitInputStream = inputStream;
        }
        scanner = ScannerFactory.createScanner(alignmentType, m_inflatorBufSize);
        scanner.setSchema(schema, m_exiHeaderOptions.DatatypeRepresentationMap, m_exiHeaderOptions.DatatypeRepresentationMapBindingsCount);
        scanner.StringTable = Scanner.createStringTable(grammarCache);
        scanner.ValueMaxLength = m_exiHeaderOptions.ValueMaxLength;
        scanner.PreserveLexicalValues = m_exiHeaderOptions.PreserveLexicalValues;
        scanner.ValueMaxLength = m_exiHeaderOptions.ValueMaxLength;

        scanner.BlockSize = m_exiHeaderOptions.BlockSize;
        scanner.stringTable.ValuePartitionCapacity = m_exiHeaderOptions.ValuePartitionCapacity;
        isFragment = m_exiHeaderOptions.Fragment;
        scanner.HeaderOptions = m_exiHeaderOptions;
      }
      else {
        scanner = m_scanner;
        scanner.BlockSize = m_exiOptions.BlockSize;
        scanner.stringTable.ValuePartitionCapacity = m_exiOptions.ValuePartitionCapacity;
        isFragment = m_exiOptions.Fragment;
        scanner.HeaderOptions = null;
        grammarCache = m_grammarCache;
      }
      scanner.reset();
      scanner.setEnableBinaryData(m_binaryDataEnabled, m_initialBinaryDataBufferSize);

      if (bitInputStream != null) {
        ((BitPackedScanner)scanner).takeover(bitInputStream);
      }
      else {
        scanner.InputStream = m_inputStream;
      }

      scanner.setGrammar(grammarCache.retrieveRootGrammar(isFragment, scanner.eventTypesWorkSpace), grammarCache.grammarOptions);
      scanner.prepare();

      return scanner;
    }

  }

}