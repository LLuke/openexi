package org.openexi.proc.io;

import java.math.BigInteger;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.IGrammar;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.common.StringTable.PrefixPartition;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.events.EXIEventNS;
import org.openexi.proc.events.EXIEventSchemaNil;
import org.openexi.proc.events.EXIEventSchemaType;
import org.openexi.proc.grammars.Apparatus;
import org.openexi.proc.grammars.Grammar;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.grammars.GrammarState;
import org.openexi.proc.grammars.ValueApparatus;
import org.openexi.proc.util.ExiUriConst;
import org.openexi.schema.Characters;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.EXISchemaLayout;

/**
 * 
 * The Scanner class provides methods for scanning events 
 * in the body of an EXI stream.
 */
public abstract class Scanner extends Apparatus {

  // m_grammarOptions and m_preserveNS need to change together.
  private short m_grammarOptions;
  
  /** @y.exclude */
  protected boolean m_preserveNS;
  
  /** @y.exclude */
  protected final QName qname;

  /**
   * Pairs of (elementURI, elementLocalName)
   * @y.exclude 
   **/
  protected final int[] m_nameLocusStack;
  /** @y.exclude */
  protected int m_nameLocusLastDepth;
  /** @y.exclude */
  protected final PrefixUriBindings[] m_prefixUriBindingsLocusStack;
  /** @y.exclude */
  protected int m_prefixUriBindingsLocusLastDepth;
  
  private PrefixUriBindings m_prefixUriBindingsDefault;
  
  /**
   * Not for public use.
   * @y.exclude
   */
  protected PrefixUriBindings m_prefixUriBindings; // current bindings

  private final ValueApparatus[] m_valueScanners;
                 
  /** @y.exclude */
  protected final ValueScanner[] m_valueScannerTable; // codec id -> valueScanner
  /** @y.exclude */
  protected final StringValueScanner m_stringValueScannerInherent;
  /** @y.exclude */
  protected final ValueScanner m_booleanValueScannerInherent;
  /** @y.exclude */
  protected final ValueScanner m_enumerationValueScannerInherent;
  /** @y.exclude */
  protected final ValueScanner m_listValueScannerInherent;
  /** @y.exclude */
  protected final ValueScanner m_decimalValueScannerInherent;
  /** @y.exclude */
  private final ValueScanner m_dateTimeValueScannerInherent;
  /** @y.exclude */
  private final ValueScanner m_timeValueScannerInherent;
  /** @y.exclude */
  private final ValueScanner m_dateValueScannerInherent;
  /** @y.exclude */
  private final ValueScanner m_gYearMonthValueScannerInherent;
  /** @y.exclude */
  private final ValueScanner m_gYearValueScannerInherent;
  /** @y.exclude */
  private final ValueScanner m_gMonthDayValueScannerInherent;
  /** @y.exclude */
  private final ValueScanner m_gDayValueScannerInherent;
  /** @y.exclude */
  private final ValueScanner m_gMonthValueScannerInherent;
  /** @y.exclude */
  protected final ValueScanner m_floatValueScannerInherent;
  /** @y.exclude */
  private final IntegerValueScanner m_integerValueScannerInherent;
  /** @y.exclude */
  protected final ValueScanner m_base64BinaryValueScannerInherent;
  /** @y.exclude */
  protected final ValueScanner m_hexBinaryValueScannerInherent;
  /** @y.exclude */
  protected final ValueScanner m_stringValueScannerLexical;
  /** @y.exclude */
  protected final ValueScanner m_booleanValueScannerLexical;
  /** @y.exclude */
  protected final ValueScanner m_enumerationValueScannerLexical;
  /** @y.exclude */
  protected final ValueScanner m_listValueScannerLexical;
  /** @y.exclude */
  protected final ValueScanner m_decimalValueScannerLexical;
  /** @y.exclude */
  private final ValueScanner m_dateTimeValueScannerLexical;
  /** @y.exclude */
  private final ValueScanner m_timeValueScannerLexical;
  /** @y.exclude */
  private final ValueScanner m_dateValueScannerLexical;
  /** @y.exclude */
  private final ValueScanner m_gYearMonthValueScannerLexical;
  /** @y.exclude */
  private final ValueScanner m_gYearValueScannerLexical;
  /** @y.exclude */
  private final ValueScanner m_gMonthDayValueScannerLexical;
  /** @y.exclude */
  private final ValueScanner m_gDayValueScannerLexical;
  /** @y.exclude */
  private final ValueScanner m_gMonthValueScannerLexical;
  /** @y.exclude */
  protected final ValueScanner m_floatValueScannerLexical;
  /** @y.exclude */
  protected final ValueScanner m_integerValueScannerLexical;
  /** @y.exclude */
  protected final ValueScanner m_base64BinaryValueScannerLexical;
  /** @y.exclude */
  protected final ValueScanner m_hexBinaryValueScannerLexical;

  private EXIOptions m_exiHeaderOptions;

  /** @y.exclude */
  protected boolean m_binaryDataEnabled;
  /** @y.exclude */
  protected int m_binaryChunkSize;
  
  /**
   * Not for public use.
   * @y.exclude
   */
  protected InputStream m_inputStream;
  
  private static final Characters TRUE; // "true" (4)
  private static final Characters FALSE; // "false" (5)
  private static final Characters ZERO; // "0" (1)
  private static final Characters ONE; // "1" (1)
  static {
    TRUE = new Characters("true".toCharArray(), 0, "true".length(), false);
    FALSE = new Characters("false".toCharArray(), 0, "false".length(), false);
    ZERO = new Characters("0".toCharArray(), 0, "0".length(), false);
    ONE = new Characters("1".toCharArray(), 0, "1".length(), false);
  }
  
  protected final CharacterBuffer m_characterBuffer;
  protected final OctetBuffer octetBuffer;
  
  /**
   * Creates a string table for use with a scanner. 
   * @param schema a schema that contains initial entries of the string table
   * @return a string table for use with a scanner
   * Not for public use.
   * @y.exclude
   */
  public static StringTable createStringTable(GrammarCache grammarCache) {
    return new StringTable(grammarCache, StringTable.Usage.decoding);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Constructor
  ///////////////////////////////////////////////////////////////////////////

  protected Scanner(boolean isForEXIOptions) {
  	super();
    m_grammarOptions = GrammarOptions.OPTIONS_UNUSED;
    m_preserveNS = false;
    m_preserveLexicalValues = false;
    qname = new QName();
    m_nameLocusStack = new int[128];
    m_nameLocusLastDepth = -2;
    m_prefixUriBindingsLocusStack = new PrefixUriBindings[64];
    m_prefixUriBindingsLocusLastDepth = -1;
    
    m_prefixUriBindingsDefault = isForEXIOptions ? null : new PrefixUriBindings();
    m_prefixUriBindings = null;

    m_characterBuffer = new CharacterBuffer(true);
    
    m_binaryDataEnabled = false;
    m_binaryChunkSize = -1;

    final ArrayList<ValueScanner> valueScanners = new ArrayList<ValueScanner>();
    valueScanners.add(m_stringValueScannerInherent = new StringValueScanner(this));
    valueScanners.add(m_booleanValueScannerInherent = new BooleanValueScanner());
    valueScanners.add(m_integerValueScannerInherent = new IntegerValueScanner());
    if (!isForEXIOptions) {
      octetBuffer = new OctetBuffer();
      m_stringValueScannerLexical = new ValueScannerLexical(m_stringValueScannerInherent, m_stringValueScannerInherent);
      m_booleanValueScannerLexical = new ValueScannerLexical(m_booleanValueScannerInherent, m_stringValueScannerInherent);
      m_integerValueScannerLexical = new ValueScannerLexical(m_integerValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_dateTimeValueScannerInherent = new DateTimeValueScanner());
      m_dateTimeValueScannerLexical = new ValueScannerLexical(m_dateTimeValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_timeValueScannerInherent = new TimeValueScanner());
      m_timeValueScannerLexical = new ValueScannerLexical(m_timeValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_dateValueScannerInherent = new DateValueScanner());
      m_dateValueScannerLexical = new ValueScannerLexical(m_dateValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_gYearMonthValueScannerInherent = new GYearMonthValueScanner());
      m_gYearMonthValueScannerLexical = new ValueScannerLexical(m_gYearMonthValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_gYearValueScannerInherent = new GYearValueScanner());
      m_gYearValueScannerLexical = new ValueScannerLexical(m_gYearValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_gMonthDayValueScannerInherent = new GMonthDayValueScanner());
      m_gMonthDayValueScannerLexical = new ValueScannerLexical(m_gMonthDayValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_gDayValueScannerInherent = new GDayValueScanner());
      m_gDayValueScannerLexical = new ValueScannerLexical(m_gDayValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_gMonthValueScannerInherent = new GMonthValueScanner());
      m_gMonthValueScannerLexical = new ValueScannerLexical(m_gMonthValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_base64BinaryValueScannerInherent = new Base64BinaryValueScanner(this));
      m_base64BinaryValueScannerLexical = new ValueScannerLexical(m_base64BinaryValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_hexBinaryValueScannerInherent = new HexBinaryValueScanner(this));
      m_hexBinaryValueScannerLexical = new ValueScannerLexical(m_hexBinaryValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_floatValueScannerInherent = new FloatValueScanner());
      m_floatValueScannerLexical = new ValueScannerLexical(m_floatValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_listValueScannerInherent =  new ListValueScanner());
      m_listValueScannerLexical = new ValueScannerLexical(m_listValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_decimalValueScannerInherent = new DecimalValueScanner());
      m_decimalValueScannerLexical = new ValueScannerLexical(m_decimalValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_enumerationValueScannerInherent = new EnumerationValueScanner());
      m_enumerationValueScannerLexical = new ValueScannerLexical(m_enumerationValueScannerInherent, m_stringValueScannerInherent);
    }
    else {
      octetBuffer = null;
      m_stringValueScannerLexical = null;
      m_booleanValueScannerLexical = null;
      m_integerValueScannerLexical = null;
      m_dateTimeValueScannerInherent = m_dateTimeValueScannerLexical = null;
      m_timeValueScannerInherent = m_timeValueScannerLexical = null;
      m_dateValueScannerInherent = m_dateValueScannerLexical = null;
      m_gYearMonthValueScannerInherent = m_gYearMonthValueScannerLexical = null;
      m_gYearValueScannerInherent = m_gYearValueScannerLexical = null;
      m_gMonthDayValueScannerInherent = m_gMonthDayValueScannerLexical = null;
      m_gDayValueScannerInherent = m_gDayValueScannerLexical = null;
      m_gMonthValueScannerInherent = m_gMonthValueScannerLexical = null;
      m_base64BinaryValueScannerInherent = m_base64BinaryValueScannerLexical = null;
      m_hexBinaryValueScannerInherent = m_hexBinaryValueScannerLexical = null;
      m_floatValueScannerInherent = m_floatValueScannerLexical = null;
      m_listValueScannerInherent = m_listValueScannerLexical = null;
      m_decimalValueScannerInherent = m_decimalValueScannerLexical = null;
      m_enumerationValueScannerInherent = m_enumerationValueScannerLexical = null;
    }
    m_valueScanners = new ValueScanner[valueScanners.size()];
    for (int i = 0; i < m_valueScanners.length; i++) {
      m_valueScanners[i] = valueScanners.get(i);
    }

    m_valueScannerTable = new ValueScanner[N_CODECS];
    m_valueScannerTable[CODEC_BASE64BINARY] = m_base64BinaryValueScannerInherent;
    m_valueScannerTable[CODEC_HEXBINARY] = m_hexBinaryValueScannerInherent;
    m_valueScannerTable[CODEC_BOOLEAN] = m_booleanValueScannerInherent;
    m_valueScannerTable[CODEC_DATETIME] = m_dateTimeValueScannerInherent; 
    m_valueScannerTable[CODEC_TIME] = m_timeValueScannerInherent;
    m_valueScannerTable[CODEC_DATE] = m_dateValueScannerInherent;
    m_valueScannerTable[CODEC_GYEARMONTH] = m_gYearMonthValueScannerInherent;
    m_valueScannerTable[CODEC_GYEAR] = m_gYearValueScannerInherent;
    m_valueScannerTable[CODEC_GMONTHDAY] = m_gMonthDayValueScannerInherent;
    m_valueScannerTable[CODEC_GDAY] = m_gDayValueScannerInherent;
    m_valueScannerTable[CODEC_GMONTH] = m_gMonthValueScannerInherent;
    m_valueScannerTable[CODEC_DECIMAL] = m_decimalValueScannerInherent;
    m_valueScannerTable[CODEC_DOUBLE] = m_floatValueScannerInherent;
    m_valueScannerTable[CODEC_INTEGER] = m_integerValueScannerInherent;
    m_valueScannerTable[CODEC_STRING] = m_stringValueScannerInherent;
    m_valueScannerTable[CODEC_LIST] = m_listValueScannerInherent;
    m_valueScannerTable[CODEC_ENUMERATION] = m_enumerationValueScannerInherent;
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */ 
  protected void init(int inflatorBufSize) {
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */  
  private void initValueScanners(InputStream istream) {
    for (int i = 0; i < N_CODECS; i++) {
      final ValueScanner valueScanner;
      if ((valueScanner = m_valueScannerTable[i]) != null)
        valueScanner.setInputStream(istream);
    }
  }

  /**
   * Not for public use.
   * @y.exclude
   */
  @Override
  public void reset() {
    super.reset();
    m_nameLocusLastDepth = -2;
    m_prefixUriBindingsLocusLastDepth = -1;
    m_prefixUriBindings = null;
  }
  
  @Override
  public void setSchema(EXISchema schema, QName[] dtrm, int n_bindings) {
    super.setSchema(schema, dtrm, n_bindings);
    m_integerValueScannerInherent.setSchema(schema);    
  }

  /**
   * Gets the next event from the EXI stream.
   * @return EXIEvent
   * @throws IOException
   */
  public abstract EventDescription nextEvent() throws IOException;
  
  /**
   * Not for public use.
   * @y.exclude
   */

  @Override
  protected ValueApparatus[] getValueApparatuses() {
    return m_valueScanners;
  }

  /**
   * Prepares the scanner ready for getting nextEvent() called.
   * @throws IOException
   * Not for public use.
   * @y.exclude
   */
  public void prepare() throws IOException {
    if (m_preserveNS) {
      m_prefixUriBindings = m_prefixUriBindingsDefault;
    }
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */
  public void setInputStream(InputStream istream) {
    m_inputStream = istream;
    initValueScanners(istream);
  }
  
  /**
   * Close the input stream.
   */
  public void closeInputStream() throws IOException {
    m_inputStream.close();
  }
  
  /**
   * Set one of FragmentGrammar, BuiltinFragmentGrammar or DocumentGrammar.
   * @y.exclude
   */
  public final void setGrammar(Grammar grammar, short grammarOptions) {
    grammar.init(currentState);
    m_grammarOptions = grammarOptions;
    m_preserveNS = GrammarOptions.hasNS(m_grammarOptions);
  }

  /** @y.exclude */
  @Override
  public final void setStringTable(StringTable stringTable) {
    super.setStringTable(stringTable);
    m_stringValueScannerInherent.setStringTable(stringTable);
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */
  @Override
  public final void setPreserveLexicalValues(boolean preserveLexicalValues) {
    final boolean prevPreserveLexicalValues = m_preserveLexicalValues;
    super.setPreserveLexicalValues(preserveLexicalValues);
    if (prevPreserveLexicalValues != preserveLexicalValues) {
      if (preserveLexicalValues) {
        m_valueScannerTable[CODEC_BASE64BINARY] = m_base64BinaryValueScannerLexical;
        m_valueScannerTable[CODEC_HEXBINARY] = m_hexBinaryValueScannerLexical;
        m_valueScannerTable[CODEC_BOOLEAN] = m_booleanValueScannerLexical;
        m_valueScannerTable[CODEC_DATETIME] = m_dateTimeValueScannerLexical;
        m_valueScannerTable[CODEC_TIME] = m_timeValueScannerLexical;
        m_valueScannerTable[CODEC_DATE] = m_dateValueScannerLexical;
        m_valueScannerTable[CODEC_GYEARMONTH] = m_gYearMonthValueScannerLexical;
        m_valueScannerTable[CODEC_GYEAR] = m_gYearValueScannerLexical;
        m_valueScannerTable[CODEC_GMONTHDAY] = m_gMonthDayValueScannerLexical;
        m_valueScannerTable[CODEC_GDAY] = m_gDayValueScannerLexical;
        m_valueScannerTable[CODEC_GMONTH] = m_gMonthValueScannerLexical;
        m_valueScannerTable[CODEC_DECIMAL] = m_decimalValueScannerLexical;
        m_valueScannerTable[CODEC_DOUBLE] = m_floatValueScannerLexical;
        m_valueScannerTable[CODEC_INTEGER] = m_integerValueScannerLexical;
        m_valueScannerTable[CODEC_STRING] = m_stringValueScannerLexical;
        m_valueScannerTable[CODEC_LIST] = m_listValueScannerLexical;
        m_valueScannerTable[CODEC_ENUMERATION] = m_enumerationValueScannerLexical;
      }
      else {
        m_valueScannerTable[CODEC_BASE64BINARY] = m_base64BinaryValueScannerInherent;
        m_valueScannerTable[CODEC_HEXBINARY] = m_hexBinaryValueScannerInherent;
        m_valueScannerTable[CODEC_BOOLEAN] = m_booleanValueScannerInherent;
        m_valueScannerTable[CODEC_DATETIME] = m_dateTimeValueScannerInherent;
        m_valueScannerTable[CODEC_TIME] = m_timeValueScannerInherent;
        m_valueScannerTable[CODEC_DATE] = m_dateValueScannerInherent;
        m_valueScannerTable[CODEC_GYEARMONTH] = m_gYearMonthValueScannerInherent;
        m_valueScannerTable[CODEC_GYEAR] = m_gYearValueScannerInherent;
        m_valueScannerTable[CODEC_GMONTHDAY] = m_gMonthDayValueScannerInherent;
        m_valueScannerTable[CODEC_GDAY] = m_gDayValueScannerInherent;
        m_valueScannerTable[CODEC_GMONTH] = m_gMonthValueScannerInherent;
        m_valueScannerTable[CODEC_DECIMAL] = m_decimalValueScannerInherent;
        m_valueScannerTable[CODEC_DOUBLE] = m_floatValueScannerInherent;
        m_valueScannerTable[CODEC_INTEGER] = m_integerValueScannerInherent;
        m_valueScannerTable[CODEC_STRING] = m_stringValueScannerInherent;
        m_valueScannerTable[CODEC_LIST] = m_listValueScannerInherent;
        m_valueScannerTable[CODEC_ENUMERATION] = m_enumerationValueScannerInherent;
      }
    }
  }

  /**
   * Not for public use.
   * @y.exclude
   */
  public abstract void setBlockSize(int blockSize);
  
  /**
   * Not for public use.
   * @y.exclude
   */
  public final void setValueMaxLength(int valueMaxLength) {
    m_stringValueScannerInherent.setValueMaxLength(
        valueMaxLength != EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED ? valueMaxLength : Integer.MAX_VALUE);
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */
  public final void setHeaderOptions(EXIOptions headerOptions) {
    m_exiHeaderOptions = headerOptions;
  }
  /**
   * Returns the EXI Header options from the header of the
   * EXI stream, if present. Otherwise, returns null.
   * @return EXIOptions or <i>null</i> if no header options are set.
   */
  public final EXIOptions getHeaderOptions() {
    return m_exiHeaderOptions;
  }

  /**
   * Returns the current grammar state if the alignment type is bit-packed or byte-alignment. 
   * @return current grammar state
   * @y.exclude
   */
  public final GrammarState getGrammarState() {
    switch (getAlignmentType()) {
      case bitPacked:
      case byteAligned:
        return currentState;
      default:
        assert false;
        return null;
    }
  }
  /**
   * Not for public use.
   * @y.exclude
   */

  public final boolean getPreserveNS() {
    return m_preserveNS;
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */
  public final void setEnableBinaryData(boolean enable, int initialBufferSize) {
    if (m_binaryDataEnabled = enable) {
      octetBuffer.init(initialBufferSize);
    }
  }

  /**
   * Binary values are read in chunks of the specified size when the
   * use of binary data is enabled.
   * @param chunkSize
   */
  public abstract void setBinaryChunkSize(int chunkSize);

  ///////////////////////////////////////////////////////////////////////////
  /// Accessors
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Not for public use.
   * @y.exclude
   */
  public final ValueScanner getValueScanner(int stype) {
    if (stype != EXISchema.NIL_NODE) {
      final int serial = m_types[stype + EXISchemaLayout.TYPE_NUMBER];
      return m_valueScannerTable[m_codecTable[serial]];
    }
    return m_valueScannerTable[CODEC_STRING];
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Structure Scanner Functions
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Not for public use.
   * @y.exclude
   */
  protected final EXIEventNS readNS(EventType eventType) throws IOException {
    final int uriId = readURI();
    final String uri = stringTable.getURI(uriId);
    final String prefix = readPrefixOfNS(uriId);
    final boolean localElementNs = readBoolean(m_inputStream);
    if (m_preserveNS) {
      m_prefixUriBindingsLocusStack[m_prefixUriBindingsLocusLastDepth] = m_prefixUriBindings =
        prefix.length() != 0 ? m_prefixUriBindings.bind(prefix, uri) : m_prefixUriBindings.bindDefault(uri);
    }
    return new EXIEventNS(prefix, uri, localElementNs, eventType);
  }
  
  /**
   * Read a text (or a name) content item. 
   * Not for public use.
   * @y.exclude
   */
  protected final Characters readText() throws IOException {
    final int len = readUnsignedInteger(m_inputStream);
    return readLiteralString(len, EXISchema.NIL_NODE, m_inputStream);
  }

  /**
   * Read xsi:nil value.
   * Not for public use.
   * @y.exclude
   */
  protected final EXIEventSchemaNil readXsiNilValue(String prefix, EventType eventType) throws IOException {
    if (m_preserveLexicalValues) {
      final Characters characterSequence;
      characterSequence = m_valueScannerTable[CODEC_STRING].scan(EXISchemaConst.XSI_LOCALNAME_NIL_ID,  
          XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID, EXISchema.NIL_NODE);
      int length = characterSequence.length;
      final int startIndex = characterSequence.startIndex;
      final int fullLimit = startIndex + length - 1; 
      final char[] characters = characterSequence.characters;
      int limit;
      skipTrailingWhiteSpaces:
      for (limit = fullLimit; limit > 0; limit--) {
        switch (characters[limit]) {
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
      for (pos = startIndex; pos < length; pos++) {
        switch (characters[pos]) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            break;
          default:
            break skipWhiteSpaces;
        }
      }
      if (pos != startIndex || limit != fullLimit) {
        length = limit - pos;
      }
      final boolean nilled;
      switch (length) {
        case 4: // "true"
          assert characters[pos] == 't' && characters[pos + 1] == 'r' &&  characters[pos + 2] == 'u' && characters[pos + 3] == 'e';
          nilled = true;
          break;
        case 5: // "false"
          assert characters[pos] == 'f' && characters[pos + 1] == 'a' &&  characters[pos + 2] == 'l' && characters[pos + 3] == 's' && characters[pos + 4] == 'e';
          nilled = false;
          break;
        case 1: // "1" or "0"
          assert characters[pos] == '0' || characters[pos] == '1';
          nilled = characters[pos] == '1';
          break;
        default:
          assert false;
          nilled = false;
          break;
      }
      return new EXIEventSchemaNil(nilled, characterSequence, prefix, eventType);
    }
    else {
      final boolean nilled = readBoolean(m_inputStream);
      return new EXIEventSchemaNil(nilled, (Characters)null, prefix, eventType);
    }
  }

  /**
   * Read xsi:type attribute value as QName
   * Not for public use.
   * @y.exclude
   */
  protected final EXIEventSchemaType readXsiTypeValue(String prefix, EventType eventType) throws IOException {
    final Characters characterSequence;
    final String typeUri, typeName, typePrefix;
    if (m_preserveLexicalValues) {
      characterSequence = m_valueScannerTable[CODEC_STRING].scan(EXISchemaConst.XSI_LOCALNAME_TYPE_ID,  
          XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID, EXISchema.NIL_NODE);
      final int i;
      if ((i = characterSequence.indexOf(':')) != -1) { // with prefix
        final int startIndex = characterSequence.startIndex;
        final char[] characters = characterSequence.characters;
        int pos = startIndex + characterSequence.length - 1;
        skipTrailingWhiteSpaces:
        for (; pos > 0; pos--) {
          switch (characters[pos]) {
            case '\t':
            case '\n':
            case '\r':
            case ' ':
              break;
            default:
              break skipTrailingWhiteSpaces;
          }
        }
        typeName = characterSequence.substring(i + 1, pos + 1);
        skipWhiteSpaces:
        for (pos = startIndex; pos < i; pos++) {
          switch (characters[pos]) {
            case '\t':
            case '\n':
            case '\r':
            case ' ':
              break;
            default:
              break skipWhiteSpaces;
          }
        }
        typePrefix = characterSequence.substring(pos, i);
        typeUri = m_preserveNS ? m_prefixUriBindings.getUri(typePrefix) : null; 
      }
      else { // no prefix
        typeName = characterSequence.makeString();
        typePrefix = "";
        typeUri = m_preserveNS ? m_prefixUriBindings.getDefaultUri() : null;
      }
    }
    else {
      final int uriId = readURI();
      typeUri = stringTable.getURI(uriId);
      StringTable.LocalNamePartition partition = stringTable.getLocalNamePartition(uriId);
      final int localNameId = readLocalName(partition); 
      typeName = partition.localNameEntries[localNameId].localName;
      typePrefix = m_preserveNS ? readPrefixOfQName(uriId) : null;
      characterSequence = null;
    }
    int tp = EXISchema.NIL_NODE;
    if (typeUri != null && schema != null && (tp = schema.getTypeOfSchema(typeUri, typeName)) != EXISchema.NIL_NODE) {
      xsitp(tp);
    }
    return new EXIEventSchemaType(tp, typeUri, typeName, typePrefix, characterSequence, prefix, eventType);
  }
  /**
   * Not for public use.
   * @y.exclude
   */
  protected final String readPrefixOfNS(final int uriId) throws IOException {
    final StringTable.PrefixPartition partition;
    partition = stringTable.getPrefixPartition(uriId);
    final int width, id;
    width = partition.forwardedWidth;
    final String name;
    id = readNBitUnsigned(width, m_inputStream);
    if (id != 0)
      name = partition.prefixEntries[id - 1].value;
    else {
      final int length = readUnsignedInteger(m_inputStream);
      name = readLiteralString(length, EXISchema.NIL_NODE, m_inputStream).makeString();
      partition.addPrefix(name);
    }
    return name;
  }
  /**
   * Not for public use.
   * @y.exclude
   */
  protected final int readURI() throws IOException {
    final int width, id;
    width = stringTable.uriWidth;
    final int uriId;
    id = readNBitUnsigned(width, m_inputStream);
    if (id != 0)
      uriId = id - 1;
    else {
      final int length = readUnsignedInteger(m_inputStream);
      final String uri = readLiteralString(length, EXISchema.NIL_NODE, m_inputStream).makeString();
      uriId = stringTable.addURI(uri, (StringTable.LocalNamePartition)null, (StringTable.PrefixPartition)null);
    }
    return uriId;
  }
  /**
   * Read a localName.
   * @return localName ID
   * @y.exclude
   */
  protected final int readLocalName(StringTable.LocalNamePartition partition) throws IOException {
    final int length = readUnsignedInteger(m_inputStream);
    if (length != 0) {
      final String name = readLiteralString(length - 1, EXISchema.NIL_NODE, m_inputStream).makeString();
      return partition.addName(name, (IGrammar)null);
    }
    else {
      return readNBitUnsigned(partition.width, m_inputStream);
    }
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */
  protected final String readPrefixOfQName(int uriId) throws IOException {
    final PrefixPartition prefixPartition;
    prefixPartition = stringTable.getPrefixPartition(uriId);
    final int width, id;
    width = prefixPartition.width;
    return (id = readNBitUnsigned(width, m_inputStream)) < prefixPartition.n_strings ?  
        prefixPartition.prefixEntries[id].value : null;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Crude bits Reader functions
  ///////////////////////////////////////////////////////////////////////////
  /**
   * Not for public use.
   * @y.exclude
   */
  protected abstract boolean readBoolean(InputStream istream) throws IOException;
  
  /**
   * Not for public use.
   * @y.exclude
   */
  protected final int readUnsignedInteger(InputStream istream) throws IOException {
    int shift = 0;
    int uint = 0;
    do {
      final int nextByte;
      if (((nextByte = readEightBitsUnsigned(istream)) & 0x0080) != 0) { // check continuation flag
        uint |= ((nextByte & 0x007F) << shift);
        shift += 7;
      }
      else {
        return uint | (nextByte << shift);
      }
    }
    while (true);
  }

  /**
   * Not for public use.
   * @y.exclude
   */
  protected final long readUnsignedIntegerAsLong(InputStream istream) throws IOException {
    int shift = 0;
    long uint = 0;
    do {
      final int nextByte;
      if (((nextByte = readEightBitsUnsigned(istream)) & 0x0080) != 0) { // check continuation flag
        uint |= (((long)nextByte & 0x007F) << shift);
        shift += 7;
      }
      else {
        return uint | ((long)nextByte << shift);
      }
    }
    while (true);
  }

  /**
   * Digits are stored into the character array in reverse order. 
   * @y.exclude
   */
  protected final int readUnsignedIntegerChars(InputStream istream, boolean addOne, char[] resultChars) throws IOException {
    int shift = 0;
    int uint = addOne ? 1 : 0;
    int pos = 0;
    boolean continued = true;
    do {
      int nextByte = readEightBitsUnsigned(istream);
      if ((nextByte & 0x0080) != 0) // check continuation flag
        nextByte &= 0x007F;
      else
        continued = false;
      uint += (nextByte << shift);
      if (!continued) {
        do {
          resultChars[pos++] = (char)(48 + uint % 10);
          uint /= 10;
        }
        while (uint != 0);
        return pos;
      }
      shift += 7;
    }
    while (shift != 28);
    
    final int shiftLimit = addOne ? 56 : 63;
    long ulong = uint;
    do {
      long nextByte = readEightBitsUnsigned(istream);
      if ((nextByte & 0x0080) != 0) // check continuation flag
        nextByte &= 0x007F;
      else
        continued = false;
      ulong += (nextByte << shift);
      if (!continued) {
        while (ulong != 0) {
          resultChars[pos++] = (char)(48 + (int)(ulong % 10L));
          ulong /= 10L;
        }
        return pos;
      }
      shift += 7;
    }
    while (shift != shiftLimit);
    
    BigInteger uinteger = BigInteger.valueOf(ulong);
    do {
      int nextByte = readEightBitsUnsigned(istream);
      if ((nextByte & 0x0080) != 0) // check continuation flag
        nextByte &= 0x007F;
      else
        continued = false;
      uinteger = uinteger.add(BigInteger.valueOf(nextByte).shiftLeft(shift));
      shift += 7;
    }
    while (continued);

    // NOTE: Let BigInteger to the job of the conversion. It's just faster that way.
    final String digitsString = uinteger.toString();
    final int n_digits = digitsString.length();
    int i, ind;
    for (i = 0, ind = n_digits; i < n_digits; i++)
      resultChars[pos++] = digitsString.charAt(--ind);
    return pos;
  }
  
  /**
   * Not for public use.
   * @y.exclude
   */  
  protected final Characters readLiteralString(int ucsCount, int tp, InputStream istream) throws IOException {
    final int n_chars, startIndex, width;
    final int[] rcs;  
    if (tp >= 0) {
      final int serial = m_types[tp + EXISchemaLayout.TYPE_NUMBER];
      n_chars = schema.ancestryIds[serial] == EXISchemaConst.STRING_TYPE ? m_restrictedCharacterCountTable[serial] : 0;
      if (n_chars != 0) {
        rcs = m_types;
        startIndex = schema.getRestrictedCharacterOfSimpleType(tp);
        width = BuiltinRCS.WIDTHS[n_chars];
      }
      else {
        startIndex = width = -1;
        rcs = null;
      }
    }
    else if (tp != EXISchema.NIL_NODE) {
      startIndex = 0;
      switch (tp) { 
        case BuiltinRCS.RCS_ID_BASE64BINARY:
          rcs = BuiltinRCS.RCS_BASE64BINARY;
          width = BuiltinRCS.RCS_BASE64BINARY_WIDTH;
          break;
        case BuiltinRCS.RCS_ID_HEXBINARY:
          rcs = BuiltinRCS.RCS_HEXBINARY;
          width = BuiltinRCS.RCS_HEXBINARY_WIDTH;
          break;
        case BuiltinRCS.RCS_ID_BOOLEAN:
          rcs = BuiltinRCS.RCS_BOOLEAN;
          width = BuiltinRCS.RCS_BOOLEAN_WIDTH;
          break;
        case BuiltinRCS.RCS_ID_DATETIME:
          rcs = BuiltinRCS.RCS_DATETIME;
          width = BuiltinRCS.RCS_DATETIME_WIDTH;
          break;
        case BuiltinRCS.RCS_ID_DECIMAL:
          rcs = BuiltinRCS.RCS_DECIMAL;
          width = BuiltinRCS.RCS_DECIMAL_WIDTH;
          break;
        case BuiltinRCS.RCS_ID_DOUBLE:
          rcs = BuiltinRCS.RCS_DOUBLE;
          width = BuiltinRCS.RCS_DOUBLE_WIDTH;
          break;
        case BuiltinRCS.RCS_ID_INTEGER:
          rcs = BuiltinRCS.RCS_INTEGER;
          width = BuiltinRCS.RCS_INTEGER_WIDTH;
          break;
        default:
          assert false;
          width = -1;
          rcs = null;
          break;
      }
      n_chars = rcs.length;
    }
    else { // tp == EXISchema.NIL_NODE
      n_chars = startIndex = width = -1;
      rcs = null;
    }
    m_characterBuffer.ensureCharacters(ucsCount);
    char[] characters = m_characterBuffer.characters;
    int charactersIndex = m_characterBuffer.allocCharacters(ucsCount);
    final int _ucsCount = ucsCount;
    assert charactersIndex != -1;
    int length = 0;;
    for (boolean foundNonBMP = false; ucsCount != 0; --ucsCount) {
      final int c, ind;
      if (width > 0 && (ind = readNBitUnsigned(width, istream)) < n_chars)
        c = rcs[startIndex + ind];
      else if (((c = readUnsignedInteger(istream)) & 0xFFFF0000) != 0) { // non-BMP character
        if (!foundNonBMP) {
          final char[] _characters = new char[2 * _ucsCount];
          for (int i = 0; i < length; i++) {
            _characters[i] = characters[charactersIndex + i];
          }
          charactersIndex = 0;
          characters = _characters;
          m_characterBuffer.redeemCharacters(_ucsCount);
          foundNonBMP = true;
        }
        characters[length++] = (char)(((c - 0x10000) >> 10) | 0xD800);
        characters[length++] = (char)(((c - 0x10000) & 0x3FF) | 0xDC00);
        continue;
      }
      characters[charactersIndex + length++] = (char)c;
    }
    return new Characters(characters, charactersIndex, length, m_characterBuffer.isVolatile);
  }

  /** @y.exclude */
  protected abstract int readNBitUnsigned(int width, InputStream istream) throws IOException;

  /** @y.exclude */
  protected abstract int readEightBitsUnsigned(InputStream istream) throws IOException;
  
  ///////////////////////////////////////////////////////////////////////////
  /// Value Scanners
  ///////////////////////////////////////////////////////////////////////////
  
  private final class BooleanValueScanner extends ValueScannerBase {
    BooleanValueScanner() {
      super(new QName("exi:boolean", ExiUriConst.W3C_2009_EXI_URI));
    }
    @Override
    public short getCodecID() {
      return CODEC_BOOLEAN;
    }
    @Override
    public int getBuiltinRCS(int simpleType) {
      return BuiltinRCS.RCS_ID_BOOLEAN;
    }
    @Override
    public Characters scan(int localNameId, int uriId, int tp) throws IOException {
      if (schema.isPatternedBooleanSimpleType(tp)) {
        switch (readNBitUnsigned(2, m_istream)) {
          case 0:
            return FALSE;
          case 1:
            return ZERO;
          case 2:
            return TRUE; 
          case 3:
            return ONE; 
          default:
            assert false;
            return null;
        }
      }
      else {
        final boolean val = readBoolean(m_istream);
        return val ? TRUE : FALSE;
      }
    }
  }

  private final class IntegerValueScanner extends ValueScannerBase {
    private int[] m_ints, m_variants;
    private long[] m_longs;
    private final char[] m_digitsBuffer;
    IntegerValueScanner() {
      super(new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI));
      m_ints = m_variants = null;
      m_longs = null;
      m_digitsBuffer  = new char[128];
    }
    public void setSchema(EXISchema schema) {
      if (schema != null) {
        m_ints = schema.getInts();
        m_variants = schema.getVariants();
        m_longs = schema.getLongs();
      }
      else {
        m_ints = m_variants = null;
        m_longs = null;
      }
    }
    /**
     * Not for public use.
     * @y.exclude
     */
    @Override
    public short getCodecID() {
      return CODEC_INTEGER;
    }
    /**
     * Not for public use.
     * @y.exclude
     */
    @Override
    public int getBuiltinRCS(int simpleType) {
      return BuiltinRCS.RCS_ID_INTEGER;
    }
    /**
     * Not for public use.
     * @y.exclude
     */
    @Override
    public Characters scan(int localName, int uri, int tp) throws IOException {
      int pos;
      boolean isNegative = false;
      if (schema.ancestryIds[m_types[tp + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.INTEGER_TYPE) {
        int intValue;
        final int width;
        switch (width = EXISchema._getWidthOfIntegralSimpleType(tp, m_types)) {
          case EXISchema.INTEGER_CODEC_DEFAULT:
            isNegative = readBoolean(m_istream);
          case EXISchema.INTEGER_CODEC_NONNEGATIVE:
            pos = readUnsignedIntegerChars(m_istream, isNegative, m_digitsBuffer);
            if (isNegative)
              m_digitsBuffer[pos++] = '-';
            m_characterBuffer.ensureCharacters(pos);
            return m_characterBuffer.addCharsReverse(m_digitsBuffer, pos);
          default:
            pos = 0;
            intValue = readNBitUnsigned(width, m_istream);
            final int minInclusiveFacet = schema.getMinInclusiveFacetOfIntegerSimpleType(tp);
            final int variantType;
            switch ((variantType = schema.getTypeOfVariant(minInclusiveFacet))) {
              case EXISchema.VARIANT_INT:
                final int minInclusiveIntValue = m_ints[m_variants[minInclusiveFacet]]; 
                if (isNegative = (intValue += minInclusiveIntValue) < 0)
                  intValue = -intValue;
                do {
                  m_digitsBuffer[pos++] = (char)(48 + (intValue % 10));
                  intValue /= 10;
                }
                while (intValue != 0);
                if (isNegative)
                  m_digitsBuffer[pos++] = '-';
                m_characterBuffer.ensureCharacters(pos);
                return m_characterBuffer.addCharsReverse(m_digitsBuffer, pos);
              case EXISchema.VARIANT_LONG:
                final long minInclusiveLongValue = m_longs[m_variants[minInclusiveFacet]]; 
                long longValue = minInclusiveLongValue + intValue;
                if (isNegative = longValue < 0)
                  longValue = -longValue;
                do {
                  m_digitsBuffer[pos++] = (char)(48 + (longValue % 10));
                  longValue /= 10;
                }
                while (longValue != 0);
                if (isNegative)
                  m_digitsBuffer[pos++] = '-';
                m_characterBuffer.ensureCharacters(pos);
                return m_characterBuffer.addCharsReverse(m_digitsBuffer, pos);
              default:
                assert variantType ==  EXISchema.VARIANT_INTEGER;
                final BigInteger minInclusiveIntegerValue = schema.getIntegerValueOfVariant(minInclusiveFacet);
                final String stringValue = minInclusiveIntegerValue.add(BigInteger.valueOf(intValue)).toString();
                final int length = stringValue.length();
                m_characterBuffer.ensureCharacters(length);
                return m_characterBuffer.addString(stringValue, length);
            }
        }
      }
      else {
        isNegative = readBoolean(m_istream);
        pos = readUnsignedIntegerChars(m_istream, isNegative, m_digitsBuffer);
        if (isNegative)
          m_digitsBuffer[pos++] = '-';
        m_characterBuffer.ensureCharacters(pos);
        return m_characterBuffer.addCharsReverse(m_digitsBuffer, pos);
      }
    }
  }

  private final class EnumerationValueScanner extends ValueScannerBase {
    EnumerationValueScanner() {
      super((QName)null);
    }
    /**
     * Not for public use.
     * @y.exclude
     */
    @Override
    public short getCodecID() {
      return CODEC_ENUMERATION;
    }
    /**
     * Not for public use.
     * @y.exclude
     */
    @Override
    public int getBuiltinRCS(int simpleType) {
      final int baseType = schema.getBaseTypeOfSimpleType(simpleType);
      return getValueScanner(baseType).getBuiltinRCS(baseType);
    }
    /**
     * Not for public use.
     * @y.exclude
     */
    @Override
    public Characters scan(int localNameId, int uriId, int tp) throws IOException {
      int n_enums = schema.getEnumerationFacetCountOfAtomicSimpleType(tp);
      assert n_enums > 0;
      int width, n;
      for (width = 0, n = n_enums - 1; n != 0; n >>= 1, ++width);
      
      int index = readNBitUnsigned(width, m_istream);
      assert index >= 0;

      final int facet = schema.getEnumerationFacetOfAtomicSimpleType(tp, index);
      return schema.getVariantCharacters(facet);
    }
  }

  private final class ListValueScanner extends ValueScannerBase {
    private char[] m_listChars;
    public ListValueScanner() {
      super((QName)null);
      m_listChars = new char[512];
    }
    /** @y.exclude */
    @Override
    public short getCodecID() {
      return CODEC_LIST;
    }
    /** @y.exclude */
    @Override
    public int getBuiltinRCS(int simpleType) {
      assert schema.getVarietyOfSimpleType(simpleType) == EXISchema.LIST_SIMPLE_TYPE;
      final int itemType = schema.getItemTypeOfListSimpleType(simpleType);
      final short codecID = m_codecTable[schema.getSerialOfType(itemType)];
      final ValueScanner itemValueScanner = m_valueScannerTable[codecID]; 
      return itemValueScanner.getBuiltinRCS(itemType);
    }
    /** @y.exclude */
    @Override
    public Characters scan(int localName, int uri, int tp) throws IOException {
      assert schema.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
      
      final int itemType = schema.getItemTypeOfListSimpleType(tp);
      final short codecID = m_codecTable[schema.getSerialOfType(itemType)];
      final ValueScanner itemValueScanner = m_valueScannerTable[codecID];

      final int n_items = readUnsignedInteger(m_istream);
      int n_listChars = 0;
      for (int i = 0; i < n_items; i++) {
        if (i != 0) { 
          if (n_listChars == m_listChars.length)
            expandCharArray();
          m_listChars[n_listChars++] = ' ';
        }
        final Characters itemValue = itemValueScanner.scan(localName, uri, itemType);
        final int n_characters = itemValue.length;
        if (n_listChars + n_characters > m_listChars.length)
          expandCharArray();
        System.arraycopy(itemValue.characters, itemValue.startIndex, m_listChars, n_listChars, n_characters);
        n_listChars += n_characters;
      }
      m_characterBuffer.ensureCharacters(n_listChars);
      return m_characterBuffer.addChars(m_listChars, n_listChars);
    }
    private void expandCharArray() {
      final int clen = m_listChars.length;
      final int nlen = clen + (clen >> 1);
      final char[] listChars = new char[nlen];
      System.arraycopy(m_listChars, 0, listChars, 0, clen);
      m_listChars = listChars;
    }
  }
  
  private final class DecimalValueScanner extends ValueScannerBase {
    private final char[] m_integralDigitsChars;
    private final char[] m_fractionDigitsChars;
    DecimalValueScanner() {
      super(new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI));
      m_integralDigitsChars = new char[128];
      m_fractionDigitsChars = new char[128];
    }
    /** @y.exclude */
    @Override
    public short getCodecID() {
      return CODEC_DECIMAL;
    }
    /** @y.exclude */
    @Override
    public int getBuiltinRCS(int simpleType) {
      return BuiltinRCS.RCS_ID_DECIMAL;
    }
    /** @y.exclude */
    @Override
    public Characters scan(int localName, int uri, int tp) throws IOException {
      final boolean isNegative = readBoolean(m_istream);
      int n_integralDigits = readUnsignedIntegerChars(m_istream, false, m_integralDigitsChars);
      if (isNegative)
        m_integralDigitsChars[n_integralDigits++] = '-';
      final int n_fractionDigits = readUnsignedIntegerChars(m_istream, false, m_fractionDigitsChars);
      final int totalLength = n_integralDigits + 1 + n_fractionDigits;
      m_characterBuffer.ensureCharacters(totalLength);
      return m_characterBuffer.addDecimalChars(m_integralDigitsChars, n_integralDigits, m_fractionDigitsChars, n_fractionDigits, totalLength);
    }
  }

  private final class DateTimeValueScanner extends DateTimeValueScannerBase {
    DateTimeValueScanner() {
      super(new QName("exi:dateTime", ExiUriConst.W3C_2009_EXI_URI), Scanner.this);
    }
    @Override
    public short getCodecID() {
      return CODEC_DATETIME;
    }
    @Override
    public Characters scan(int localNameId, int uriId, int tp) throws IOException {
      m_n_dateTimeCharacters = 0;
      readYear(m_istream);
      readMonthDay(m_istream);
      m_dateTimeCharacters[m_n_dateTimeCharacters++] = 'T';
      readTime(m_istream);
      readTimeZone(m_istream);
      m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
      return m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
    }
  }
  
  private final class TimeValueScanner extends DateTimeValueScannerBase {
    TimeValueScanner() {
      super(new QName("exi:time", ExiUriConst.W3C_2009_EXI_URI), Scanner.this);
    }
    @Override
    public short getCodecID() {
      return CODEC_TIME;
    }
    @Override
    public Characters scan(int localNameId, int uriId, int tp) throws IOException {
      m_n_dateTimeCharacters = 0;
      readTime(m_istream);
      readTimeZone(m_istream);
      m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
      return m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
    }
  }

  private final class DateValueScanner extends DateTimeValueScannerBase {
    DateValueScanner() {
      super(new QName("exi:date", ExiUriConst.W3C_2009_EXI_URI), Scanner.this);
    }
    @Override
    public short getCodecID() {
      return CODEC_DATE;
    }
    @Override
    public Characters scan(int localNameId, int uriId, int tp) throws IOException {
      m_n_dateTimeCharacters = 0;
      readYear(m_istream);
      readMonthDay(m_istream);
      readTimeZone(m_istream);
      m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
      return m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
    }
  }

  private final class GYearMonthValueScanner extends DateTimeValueScannerBase {
    GYearMonthValueScanner() {
      super(new QName("exi:gYearMonth", ExiUriConst.W3C_2009_EXI_URI), Scanner.this);
    }
    @Override
    public short getCodecID() {
      return CODEC_GYEARMONTH;
    }
    @Override
    public Characters scan(int localNameId, int uriId, int tp) throws IOException {
      m_n_dateTimeCharacters = 0;
      readGYearMonth(m_istream);
      readTimeZone(m_istream);
      m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
      return m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
    }
  }

  private final class GYearValueScanner extends DateTimeValueScannerBase {
    GYearValueScanner() {
      super(new QName("exi:gYear", ExiUriConst.W3C_2009_EXI_URI), Scanner.this);
    }
    @Override
    public short getCodecID() {
      return CODEC_GYEAR;
    }
    @Override
    public Characters scan(int localNameId, int uriId, int tp) throws IOException {
      m_n_dateTimeCharacters = 0;
      readYear(m_istream);
      readTimeZone(m_istream);
      m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
      return m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
    }
  }

  private final class GMonthDayValueScanner extends DateTimeValueScannerBase {
    GMonthDayValueScanner() {
      super(new QName("exi:gMonthDay", ExiUriConst.W3C_2009_EXI_URI), Scanner.this);
    }
    @Override
    public short getCodecID() {
      return CODEC_GMONTHDAY;
    }
    @Override
    public Characters scan(int localNameId, int uriId, int tp) throws IOException {
      m_n_dateTimeCharacters = 0;
      readGMonthDay(m_istream);
      readTimeZone(m_istream);
      m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
      return m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
    }
  }

  private final class GDayValueScanner extends DateTimeValueScannerBase {
    GDayValueScanner() {
      super(new QName("exi:gDay", ExiUriConst.W3C_2009_EXI_URI), Scanner.this);
    }
    @Override
    public short getCodecID() {
      return CODEC_GDAY;
    }
    @Override
    public Characters scan(int localNameId, int uriId, int tp) throws IOException {
      m_n_dateTimeCharacters = 0;
      readGDay(m_istream);
      readTimeZone(m_istream);
      m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
      return m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
    }
  }
  
  private final class GMonthValueScanner extends DateTimeValueScannerBase {
    GMonthValueScanner() {
      super(new QName("exi:gMonth", ExiUriConst.W3C_2009_EXI_URI), Scanner.this);
    }
    @Override
    public short getCodecID() {
      return CODEC_GMONTH;
    }
    @Override
    public Characters scan(int localNameId, int uriId, int tp) throws IOException {
      m_n_dateTimeCharacters = 0;
      readGMonth(m_istream);
      readTimeZone(m_istream);
      m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
      return m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
    }
  }
  
  private final class FloatValueScanner extends ValueScannerBase {
    FloatValueScanner() {
      super(new QName("exi:double", ExiUriConst.W3C_2009_EXI_URI));
    }
    /**
     * Not for public use.
     * @y.exclude
     */
    @Override
    public short getCodecID() {
      return CODEC_DOUBLE;
    }
    /**
     * Not for public use.
     * @y.exclude
     */
    @Override
    public int getBuiltinRCS(int simpleType) {
      return BuiltinRCS.RCS_ID_DOUBLE;
    }
    /**
     * Not for public use.
     * @y.exclude
     */
    @Override
    public Characters scan(int localNameId, int uriId, int tp) throws IOException {
      final boolean isNegative = readBoolean(m_istream);
      long longValue = readUnsignedInteger63(m_istream);
      if (isNegative)
        longValue = -longValue - 1;
      final String mantissaDigitsString = Long.toString(longValue); 
      final boolean isNegativeExponent = readBoolean(m_istream);
      int intValue = readUnsignedInteger(m_istream);
      if (isNegativeExponent)
        ++intValue;
      final String stringValue;
      if (16384 != intValue) {
        stringValue = mantissaDigitsString + 'E' + (isNegativeExponent ? "-" : "") +  Integer.toString(intValue); 
      }
      else {
        stringValue = longValue == 1 ? "INF" : longValue == -1 ? "-INF" : "NaN";    
      }
      final int length = stringValue.length();
      m_characterBuffer.ensureCharacters(length);
      return m_characterBuffer.addString(stringValue, length);
    }
    /**
     * Read an unsigned integer value of range [0 ... 2^63 - 1].
     * Possible effective number of bits 7, 14, 21, 28, 35, 42, 49, 56, 63.
     */
    private final long readUnsignedInteger63(InputStream istream) throws IOException {
      int shift = 0;
      boolean continued = true;
      long ulong = 0;
      do {
        long nextByte = readEightBitsUnsigned(istream);
        if ((nextByte & 0x0080) != 0) // check continuation flag
          nextByte &= 0x007F;
        else
          continued = false;
        ulong += (nextByte << shift);
        if (!continued)
          return ulong;
        shift += 7;
      }
      while (shift != 63);
      assert !continued;
      return ulong;
    }
  }

}
