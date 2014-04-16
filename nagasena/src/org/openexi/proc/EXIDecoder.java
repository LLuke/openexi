package org.openexi.proc;

import java.io.InputStream;
import java.io.IOException;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.SchemaId;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.HeaderOptionsInputStream;
import org.openexi.proc.io.BitPackedScanner;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.ScannerFactory;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EmptySchema;

/**
 * EXIDecoder provides methods to configure and 
 * instantiate a {@link org.openexi.proc.io.Scanner} object
 * you can use to parse the contents of an EXI stream. 
 */
public class EXIDecoder {

  private Scanner m_scanner; 
  
  private GrammarCache m_grammarCache;
  private EXISchema m_schema;
  private EXISchemaResolver m_schemaResolver;
  
  private InputStream m_inputStream;

  private final EXIOptions m_exiOptions;
  private final EXIOptions m_exiHeaderOptions;
  private final HeaderOptionsDecoder m_optionsDecoder;
  
  private static final int DEFAULT_INFLATOR_BUF_SIZE = 8192; 
  private final int m_inflatorBufSize;
  
  private final boolean m_useThreadedInflater;
  
  private boolean m_binaryDataEnabled;
  private int m_initialBinaryDataBufferSize;
  
  /**
   * Creates an instance of EXIDecoder with the default inflator 
   * buffer size of 8192 bytes.  Buffer size is only used when
   * the EXI stream is encoded with EXI compression.
   */
  public EXIDecoder() {
    this(DEFAULT_INFLATOR_BUF_SIZE, false);
  }

  /**
   * Creates an instance of EXIDecoder with the specified inflator buffer 
   * size. When dynamic memory is limited on the target device, reducing 
   * the buffer size can improve performance and avoid runtime errors. Buffer 
   * size is only used when the EXI stream is encoded with EXI compression.
   * @param inflatorBufSize size of the buffer, in bytes.
   * @param useThreadedInflater Inflater will be run in its own thread if true
   */
  public EXIDecoder(int inflatorBufSize, boolean useThreadedInflater) {
    m_inflatorBufSize = inflatorBufSize;
    m_exiOptions = new EXIOptions();
    m_exiHeaderOptions = new EXIOptions();
    m_optionsDecoder = new HeaderOptionsDecoder();
    m_grammarCache = null;
    m_schema = null;
    m_schemaResolver = null;
    m_useThreadedInflater = useThreadedInflater;
    m_scanner = ScannerFactory.createScanner(AlignmentType.bitPacked, m_inflatorBufSize, m_useThreadedInflater);
    m_scanner.setSchema(m_schema, (QName[])null, 0);
    m_scanner.setStringTable(Scanner.createStringTable(m_grammarCache));
    m_binaryDataEnabled = false;
    m_initialBinaryDataBufferSize = 8192;
  }

  /**
   * Set an input stream from which the encoded stream is read.
   * @param istream InputSream to be read.
   */
  public final void setInputStream(InputStream istream) {
    m_inputStream = istream;
  }
  
  /**
   * Set the bit alignment style of the stream to be decoded.
   * 
   * @param alignmentType {@link org.openexi.proc.common.AlignmentType} object
   * @throws EXIOptionsException
   */
  public final void setAlignmentType(AlignmentType alignmentType) throws EXIOptionsException {
    m_exiOptions.setAlignmentType(alignmentType);
    if (m_scanner.getAlignmentType() != alignmentType) {
      m_scanner = ScannerFactory.createScanner(alignmentType, m_inflatorBufSize, m_useThreadedInflater);
      m_scanner.setSchema(m_schema, m_exiOptions.getDatatypeRepresentationMap(), m_exiOptions.getDatatypeRepresentationMapBindingsCount());
      m_scanner.setStringTable(Scanner.createStringTable(m_grammarCache));
      m_scanner.setValueMaxLength(m_exiOptions.getValueMaxLength());
      m_scanner.setPreserveLexicalValues(m_exiOptions.getPreserveLexicalValues());
    }
  }
  
  /**
   * Set whether the document is a fragment. Fragments are nonstandard
   * XML documents with multiple root elements. Default is false.
   * @param isFragment true if the stream is an XML fragment
   */
  public final void setFragment(boolean isFragment) {
    m_exiOptions.setFragment(isFragment);
  }
  
  /**
   * Set the GrammarCache used in decoding EXI streams. 
   * @param grammarCache {@link org.openexi.proc.grammars.GrammarCache}
   * @throws EXIOptionsException
   */
  public final void setGrammarCache(GrammarCache grammarCache) throws EXIOptionsException {
    m_exiOptions.setGrammarOptions(grammarCache.grammarOptions);
    if (m_grammarCache != grammarCache) {
      m_grammarCache = grammarCache;
      final EXISchema schema;
      if ((schema = m_grammarCache.getEXISchema()) != m_schema) {
        m_schema = schema;
        m_scanner.setSchema(m_schema, m_exiOptions.getDatatypeRepresentationMap(), m_exiOptions.getDatatypeRepresentationMapBindingsCount());
        m_scanner.setStringTable(Scanner.createStringTable(m_grammarCache));
      }
    }
  }
  
  /**
   * Not for public use.
   * @param schemaResolver
   * @y.exclude
   */
  public final void setEXISchemaResolver(EXISchemaResolver schemaResolver) {
    m_schemaResolver = schemaResolver;
  }
  
  /**
   * Set the size, in number of values, of the information that will be 
   * processed as a chunk of the entire EXI stream. Reducing the block size 
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
    m_scanner.setValueMaxLength(valueMaxLength);
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
  
  /**
   * Set to <i>true</i> to preserve the original string values from the EXI
   * stream. For example, a date string might be converted to a different
   * format when interpreted by the EXIDecoder. Preserving the lexical values
   * ensures that the identical strings are restored, and not just their 
   * logical values.
   * 
   * @param preserveLexicalValues true to keep original strings intact
   * @throws EXIOptionsException
   */
  public final void setPreserveLexicalValues(boolean preserveLexicalValues) throws EXIOptionsException {
    m_exiOptions.setPreserveLexicalValues(preserveLexicalValues);
    m_scanner.setPreserveLexicalValues(preserveLexicalValues);
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
   *   decoderInstance.setDatatypeRepresentationMap(dtrm, 1); // The array, and the number of pairs (1).
   * </pre>
   * 
   * @param dtrm a sequence of pairs of datatype QName and datatype representation QName
   * @param n_bindings the number of QName pairs
   */
  public final void setDatatypeRepresentationMap(QName[] dtrm, int n_bindings) throws EXIOptionsException {
    if (!QName.isSame(m_exiOptions.getDatatypeRepresentationMap(), m_exiOptions.getDatatypeRepresentationMapBindingsCount(), dtrm, n_bindings)) {
      m_exiOptions.setDatatypeRepresentationMap(dtrm, n_bindings);
      m_scanner.setSchema(m_schema, dtrm, n_bindings);
    }
  }
  
  /**
   * Each binary value will be returned as in a EventDescription of EVENT_BLOB 
   * instead of EVENT_CH when enabled.
   * @param enable
   */
  public final void setEnableBinaryData(boolean enable) {
    m_binaryDataEnabled = enable;
  }

  public final void setInitialBinaryDataBufferSize(int initialSize) {
    m_initialBinaryDataBufferSize = initialSize;
  }

  /**
   * This method reads and configures any header options present
   * in the EXI stream, then returns a {@link org.openexi.proc.io.Scanner} 
   * object you can use to parse the values from the EXI stream.  
   * @return Scanner parsable object with header options applied.
   * @throws IOException
   * @throws EXIOptionsException
   */
  public Scanner processHeader() throws IOException, EXIOptionsException {
    int val = m_inputStream.read();
    if (val == 36) {
      m_inputStream.read(); // 69
      m_inputStream.read(); // 88
      m_inputStream.read(); // 73
      val = m_inputStream.read();
    }
    final Scanner scanner;
    final GrammarCache grammarCache;
    final boolean isFragment; 
    HeaderOptionsInputStream bitInputStream = null;
    if ((val & 0x20) != 0) {
      final AlignmentType alignmentType;
      final EXISchema schema;
      m_exiHeaderOptions.init();
      HeaderOptionsInputStream inputStream;
      inputStream = m_optionsDecoder.decode(m_exiHeaderOptions, m_inputStream);
      final short grammarOptions = m_exiHeaderOptions.toGrammarOptions();
      final SchemaId schemaId;
      if ((schemaId = m_exiHeaderOptions.getSchemaId()) != null) {
        final String schemaIdValue;
        if ((schemaIdValue = schemaId.getValue()) == null) {
          schema = null;
          grammarCache = new GrammarCache(grammarOptions);
        }
        else if (schemaIdValue.length() == 0) {
          // REVISIT: test
          assert false;
          schema = EmptySchema.getEXISchema();
          grammarCache = new GrammarCache(schema, grammarOptions);
        }
        else {
          GrammarCache specifiedGrammarCache = null;
          if (m_schemaResolver != null && (specifiedGrammarCache = m_schemaResolver.resolveSchema(schemaIdValue, grammarOptions)) != null) {
            grammarCache = specifiedGrammarCache;
            schema = grammarCache.getEXISchema();
          }
          else { // Failed to resolve schemaIdValue into a grammar cache
            // REVISIT: check the ID associated with m_schema (if any has been provided).
            schema = m_schema;
            if (m_grammarCache.grammarOptions != grammarOptions)
              grammarCache = new GrammarCache(schema, grammarOptions);
            else
              grammarCache = m_grammarCache;
          }
        }
      }
      else {
        schema = m_schema;
        if (m_grammarCache.grammarOptions != grammarOptions)
          grammarCache = new GrammarCache(schema, grammarOptions);
        else
          grammarCache = m_grammarCache;
      }
      if ((alignmentType = m_exiHeaderOptions.getAlignmentType()) == AlignmentType.bitPacked) {
        bitInputStream = inputStream;
      }
      scanner = ScannerFactory.createScanner(alignmentType, m_inflatorBufSize, m_useThreadedInflater);
      scanner.setSchema(schema, m_exiHeaderOptions.getDatatypeRepresentationMap(), m_exiHeaderOptions.getDatatypeRepresentationMapBindingsCount());
      scanner.setStringTable(Scanner.createStringTable(grammarCache));
      scanner.setValueMaxLength(m_exiHeaderOptions.getValueMaxLength());
      scanner.setPreserveLexicalValues(m_exiHeaderOptions.getPreserveLexicalValues());
      scanner.setValueMaxLength(m_exiHeaderOptions.getValueMaxLength());

      scanner.setBlockSize(m_exiHeaderOptions.getBlockSize());
      scanner.stringTable.setValuePartitionCapacity(m_exiHeaderOptions.getValuePartitionCapacity());
      isFragment = m_exiHeaderOptions.isFragment();
      scanner.setHeaderOptions(m_exiHeaderOptions);
    }
    else {
      scanner = m_scanner;
      scanner.setBlockSize(m_exiOptions.getBlockSize());
      scanner.stringTable.setValuePartitionCapacity(m_exiOptions.getValuePartitionCapacity());
      isFragment = m_exiOptions.isFragment();
      scanner.setHeaderOptions(null);
      grammarCache = m_grammarCache;
    }
    scanner.reset();
    scanner.setEnableBinaryData(m_binaryDataEnabled, m_initialBinaryDataBufferSize);
  
    if (bitInputStream != null)
      ((BitPackedScanner)scanner).takeover(bitInputStream);
    else
      scanner.setInputStream(m_inputStream);
  
    scanner.setGrammar(grammarCache.retrieveRootGrammar(isFragment, scanner.eventTypesWorkSpace), grammarCache.grammarOptions);
    scanner.prepare();
    
    return scanner;
  }
  
}
