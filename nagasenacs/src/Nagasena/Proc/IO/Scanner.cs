using System;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Numerics;
using System.Collections.Generic;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EXIOptions = Nagasena.Proc.Common.EXIOptions;
using EventType = Nagasena.Proc.Common.EventType;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using IGrammar = Nagasena.Proc.Common.IGrammar;
using QName = Nagasena.Proc.Common.QName;
using StringTable = Nagasena.Proc.Common.StringTable;
using PrefixPartition = Nagasena.Proc.Common.StringTable.PrefixPartition;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using EXIEventNS = Nagasena.Proc.Events.EXIEventNS;
using EXIEventSchemaNil = Nagasena.Proc.Events.EXIEventSchemaNil;
using EXIEventSchemaType = Nagasena.Proc.Events.EXIEventSchemaType;
using Apparatus = Nagasena.Proc.Grammars.Apparatus;
using Grammar = Nagasena.Proc.Grammars.Grammar;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using GrammarState = Nagasena.Proc.Grammars.GrammarState;
using ValueApparatus = Nagasena.Proc.Grammars.ValueApparatus;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using Characters = Nagasena.Schema.Characters;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using EXISchemaLayout = Nagasena.Schema.EXISchemaLayout;

namespace Nagasena.Proc.IO {

  /// 
  /// <summary>
  /// The Scanner class provides methods for scanning events 
  /// in the body of an EXI stream.
  /// </summary>
  public abstract class Scanner : Apparatus {

    // m_grammarOptions and m_preserveNS need to change together.
    private short m_grammarOptions;

    /// <summary>
    /// @y.exclude </summary>
    protected internal bool m_preserveNS;

    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly QName qname;

    /// <summary>
    /// Pairs of (elementURI, elementLocalName)
    /// @y.exclude 
    /// 
    /// </summary>
    protected internal readonly int[] m_nameLocusStack;
    /// <summary>
    /// @y.exclude </summary>
    protected internal int m_nameLocusLastDepth;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly PrefixUriBindings[] m_prefixUriBindingsLocusStack;
    /// <summary>
    /// @y.exclude </summary>
    protected internal int m_prefixUriBindingsLocusLastDepth;

    private PrefixUriBindings m_prefixUriBindingsDefault;

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    protected internal PrefixUriBindings m_prefixUriBindings; // current bindings

    private readonly ValueApparatus[] m_valueScanners;

    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner[] m_valueScannerTable; // codec id -> valueScanner
    /// <summary>
    /// @y.exclude </summary>
    internal readonly StringValueScanner m_stringValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_booleanValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_enumerationValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_listValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_decimalValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_dateTimeValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_timeValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_dateValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_gYearMonthValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_gYearValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_gMonthDayValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_gDayValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_gMonthValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_floatValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    private readonly IntegerValueScanner m_integerValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_base64BinaryValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_hexBinaryValueScannerInherent;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_stringValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_booleanValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_enumerationValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_listValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_decimalValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_dateTimeValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_timeValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_dateValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_gYearMonthValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_gYearValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_gMonthDayValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_gDayValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    private readonly ValueScanner m_gMonthValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_floatValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_integerValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_base64BinaryValueScannerLexical;
    /// <summary>
    /// @y.exclude </summary>
    protected internal readonly ValueScanner m_hexBinaryValueScannerLexical;

    private EXIOptions m_exiHeaderOptions;

    /// <summary>
    /// @y.exclude </summary>
    protected internal bool m_binaryDataEnabled;
    /// <summary>
    /// @y.exclude </summary>
    protected internal int m_binaryChunkSize;

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    protected internal Stream m_inputStream;

    private static readonly Characters TRUE; // "true" (4)
    private static readonly Characters FALSE; // "false" (5)
    private static readonly Characters ZERO; // "0" (1)
    private static readonly Characters ONE; // "1" (1)
    static Scanner() {
      TRUE = new Characters("true".ToCharArray(), 0, "true".Length, false);
      FALSE = new Characters("false".ToCharArray(), 0, "false".Length, false);
      ZERO = new Characters("0".ToCharArray(), 0, "0".Length, false);
      ONE = new Characters("1".ToCharArray(), 0, "1".Length, false);
    }

    protected internal readonly CharacterBuffer m_characterBuffer;
    protected internal readonly OctetBuffer octetBuffer;

    /// <summary>
    /// Creates a string table for use with a scanner. </summary>
    /// <param name="schema"> a schema that contains initial entries of the string table </param>
    /// <returns> a string table for use with a scanner
    /// Not for public use.
    /// @y.exclude </returns>
    public static StringTable createStringTable(GrammarCache grammarCache) {
      return new StringTable(grammarCache, StringTable.Usage.decoding);
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Constructor
    ///////////////////////////////////////////////////////////////////////////

    protected internal Scanner(bool isForEXIOptions) : base() {
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

      List<ValueScanner> valueScanners = new List<ValueScanner>();
      valueScanners.Add(m_stringValueScannerInherent = new StringValueScanner(this));
      valueScanners.Add(m_booleanValueScannerInherent = new BooleanValueScanner(this));
      valueScanners.Add(m_integerValueScannerInherent = new IntegerValueScanner(this));
      if (!isForEXIOptions) {
        octetBuffer = new OctetBuffer();
        m_stringValueScannerLexical = new ValueScannerLexical(m_stringValueScannerInherent, m_stringValueScannerInherent);
        m_booleanValueScannerLexical = new ValueScannerLexical(m_booleanValueScannerInherent, m_stringValueScannerInherent);
        m_integerValueScannerLexical = new ValueScannerLexical(m_integerValueScannerInherent, m_stringValueScannerInherent);
        valueScanners.Add(m_dateTimeValueScannerInherent = new DateTimeValueScanner(this));
        m_dateTimeValueScannerLexical = new ValueScannerLexical(m_dateTimeValueScannerInherent, m_stringValueScannerInherent);
        valueScanners.Add(m_timeValueScannerInherent = new TimeValueScanner(this));
        m_timeValueScannerLexical = new ValueScannerLexical(m_timeValueScannerInherent, m_stringValueScannerInherent);
        valueScanners.Add(m_dateValueScannerInherent = new DateValueScanner(this));
        m_dateValueScannerLexical = new ValueScannerLexical(m_dateValueScannerInherent, m_stringValueScannerInherent);
        valueScanners.Add(m_gYearMonthValueScannerInherent = new GYearMonthValueScanner(this));
        m_gYearMonthValueScannerLexical = new ValueScannerLexical(m_gYearMonthValueScannerInherent, m_stringValueScannerInherent);
        valueScanners.Add(m_gYearValueScannerInherent = new GYearValueScanner(this));
        m_gYearValueScannerLexical = new ValueScannerLexical(m_gYearValueScannerInherent, m_stringValueScannerInherent);
        valueScanners.Add(m_gMonthDayValueScannerInherent = new GMonthDayValueScanner(this));
        m_gMonthDayValueScannerLexical = new ValueScannerLexical(m_gMonthDayValueScannerInherent, m_stringValueScannerInherent);
        valueScanners.Add(m_gDayValueScannerInherent = new GDayValueScanner(this));
        m_gDayValueScannerLexical = new ValueScannerLexical(m_gDayValueScannerInherent, m_stringValueScannerInherent);
        valueScanners.Add(m_gMonthValueScannerInherent = new GMonthValueScanner(this));
        m_gMonthValueScannerLexical = new ValueScannerLexical(m_gMonthValueScannerInherent, m_stringValueScannerInherent);
        valueScanners.Add(m_base64BinaryValueScannerInherent = new Base64BinaryValueScanner(this));
        m_base64BinaryValueScannerLexical = new ValueScannerLexical(m_base64BinaryValueScannerInherent, m_stringValueScannerInherent);
        valueScanners.Add(m_hexBinaryValueScannerInherent = new HexBinaryValueScanner(this));
        m_hexBinaryValueScannerLexical = new ValueScannerLexical(m_hexBinaryValueScannerInherent, m_stringValueScannerInherent);
        valueScanners.Add(m_floatValueScannerInherent = new FloatValueScanner(this));
        m_floatValueScannerLexical = new ValueScannerLexical(m_floatValueScannerInherent, m_stringValueScannerInherent);
        valueScanners.Add(m_listValueScannerInherent = new ListValueScanner(this));
        m_listValueScannerLexical = new ValueScannerLexical(m_listValueScannerInherent, m_stringValueScannerInherent);
        valueScanners.Add(m_decimalValueScannerInherent = new DecimalValueScanner(this));
        m_decimalValueScannerLexical = new ValueScannerLexical(m_decimalValueScannerInherent, m_stringValueScannerInherent);
        valueScanners.Add(m_enumerationValueScannerInherent = new EnumerationValueScanner(this));
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
      m_valueScanners = new ValueScanner[valueScanners.Count];
      for (int i = 0; i < m_valueScanners.Length; i++) {
        m_valueScanners[i] = valueScanners[i];
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

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    protected internal virtual void init(int inflatorBufSize) {
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    private void initValueScanners(Stream istream) {
      for (int i = 0; i < N_CODECS; i++) {
        ValueScanner valueScanner;
        if ((valueScanner = m_valueScannerTable[i]) != null) {
          valueScanner.InputStream = istream;
        }
      }
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public override void reset() {
      base.reset();
      m_nameLocusLastDepth = -2;
      m_prefixUriBindingsLocusLastDepth = -1;
      m_prefixUriBindings = null;
    }

    public override void setSchema(EXISchema schema, QName[] dtrm, int n_bindings) {
      base.setSchema(schema, dtrm, n_bindings);
      m_integerValueScannerInherent.Schema = schema;
    }

    /// <summary>
    /// Gets the next event from the EXI stream. </summary>
    /// <returns> EXIEvent </returns>
    /// <exception cref="IOException"> </exception>
    public abstract EventDescription nextEvent();

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>

    protected internal override ValueApparatus[] ValueApparatuses {
      get {
        return m_valueScanners;
      }
    }

    /// <summary>
    /// Prepares the scanner ready for getting nextEvent() called. </summary>
    /// <exception cref="IOException">
    /// Not for public use.
    /// @y.exclude </exception>
    public virtual void prepare() {
      if (m_preserveNS) {
        m_prefixUriBindings = m_prefixUriBindingsDefault;
      }
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public virtual Stream InputStream {
      set {
        m_inputStream = value;
        initValueScanners(value);
      }
    }

    /// <summary>
    /// Close the input stream.
    /// </summary>
    public virtual void closeInputStream() {
      m_inputStream.Close();
    }

    /// <summary>
    /// Set one of FragmentGrammar, BuiltinFragmentGrammar or DocumentGrammar.
    /// @y.exclude
    /// </summary>
    public void setGrammar(Grammar grammar, short grammarOptions) {
      grammar.init(currentState);
      m_grammarOptions = grammarOptions;
      m_preserveNS = GrammarOptions.hasNS(m_grammarOptions);
    }

    /// <summary>
    /// @y.exclude </summary>
    public override StringTable StringTable {
      set {
        base.StringTable = value;
        m_stringValueScannerInherent.StringTable = value;
      }
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public override bool PreserveLexicalValues {
      set {
        bool prevPreserveLexicalValues = m_preserveLexicalValues;
        base.PreserveLexicalValues = value;
        if (prevPreserveLexicalValues != value) {
          if (value) {
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
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public abstract int BlockSize { set; }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public int ValueMaxLength {
      set {
        m_stringValueScannerInherent.ValueMaxLength = value != EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED ? value : int.MaxValue;
      }
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public EXIOptions HeaderOptions {
      set {
        m_exiHeaderOptions = value;
      }
      get {
        return m_exiHeaderOptions;
      }
    }

    /// <summary>
    /// Returns the current grammar state if the alignment type is bit-packed or byte-alignment. </summary>
    /// <returns> current grammar state
    /// @y.exclude </returns>
    public GrammarState GrammarState {
      get {
        switch (AlignmentType) {
          case AlignmentType.bitPacked:
          case AlignmentType.byteAligned:
            return currentState;
          default:
            Debug.Assert(false);
            return null;
        }
      }
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public bool PreserveNS {
      get {
        return m_preserveNS;
      }
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public void setEnableBinaryData(bool enable, int initialBufferSize) {
      if (m_binaryDataEnabled = enable) {
        octetBuffer.init(initialBufferSize);
      }
    }

    /// <summary>
    /// Binary values are read in chunks of the specified size when the
    /// use of binary data is enabled. </summary>
    /// <param name="chunkSize"> </param>
    public abstract int BinaryChunkSize { set; }

    ///////////////////////////////////////////////////////////////////////////
    /// Accessors
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public ValueScanner getValueScanner(int stype) {
      if (stype != EXISchema.NIL_NODE) {
        int serial = m_types[stype + EXISchemaLayout.TYPE_NUMBER];
        return m_valueScannerTable[m_codecTable[serial]];
      }
      return m_valueScannerTable[CODEC_STRING];
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Structure Scanner Functions
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    protected internal EXIEventNS readNS(EventType eventType) {
      int uriId = readURI();
      string uri = stringTable.getURI(uriId);
      string prefix = readPrefixOfNS(uriId);
      bool localElementNs = readBoolean(m_inputStream);
      if (m_preserveNS) {
        m_prefixUriBindingsLocusStack[m_prefixUriBindingsLocusLastDepth] = 
          m_prefixUriBindings = prefix.Length != 0 ? m_prefixUriBindings.bind(prefix, uri) : m_prefixUriBindings.bindDefault(uri);
      }
      return new EXIEventNS(prefix, uri, localElementNs, eventType);
    }

    /// <summary>
    /// Read a text (or a name) content item. 
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    protected internal Characters readText() {
      int len = readUnsignedInteger(m_inputStream);
      return readLiteralString(len, EXISchema.NIL_NODE, m_inputStream);
    }

    /// <summary>
    /// Read xsi:nil value.
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    protected internal EXIEventSchemaNil readXsiNilValue(string prefix, EventType eventType) {
      if (m_preserveLexicalValues) {
        Characters characterSequence;
        characterSequence = m_valueScannerTable[CODEC_STRING].scan(EXISchemaConst.XSI_LOCALNAME_NIL_ID, XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID, EXISchema.NIL_NODE);
        int length = characterSequence.length;
        int startIndex = characterSequence.startIndex;
        int fullLimit = startIndex + length - 1;
        char[] characters = characterSequence.characters;
        int limit;
        for (limit = fullLimit; limit > 0; limit--) {
          switch (characters[limit]) {
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
        for (pos = startIndex; pos < length; pos++) {
          switch (characters[pos]) {
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
        if (pos != startIndex || limit != fullLimit) {
          length = limit - pos;
        }
        bool nilled;
        switch (length) {
          case 4: // "true"
            Debug.Assert(characters[pos] == 't' && characters[pos + 1] == 'r' && characters[pos + 2] == 'u' && characters[pos + 3] == 'e');
            nilled = true;
            break;
          case 5: // "false"
            Debug.Assert(characters[pos] == 'f' && characters[pos + 1] == 'a' && characters[pos + 2] == 'l' && characters[pos + 3] == 's' && characters[pos + 4] == 'e');
            nilled = false;
            break;
          case 1: // "1" or "0"
            Debug.Assert(characters[pos] == '0' || characters[pos] == '1');
            nilled = characters[pos] == '1';
            break;
          default:
            Debug.Assert(false);
            nilled = false;
            break;
        }
        return new EXIEventSchemaNil(nilled, characterSequence, prefix, eventType);
      }
      else {
        bool nilled = readBoolean(m_inputStream);
        return new EXIEventSchemaNil(nilled, (Characters)null, prefix, eventType);
      }
    }

    /// <summary>
    /// Read xsi:type attribute value as QName
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    protected internal EXIEventSchemaType readXsiTypeValue(string prefix, EventType eventType) {
      Characters characterSequence;
      string typeUri, typeName, typePrefix;
      if (m_preserveLexicalValues) {
        characterSequence = m_valueScannerTable[CODEC_STRING].scan(EXISchemaConst.XSI_LOCALNAME_TYPE_ID, XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID, EXISchema.NIL_NODE);
        int i;
        if ((i = characterSequence.IndexOf(':')) != -1) { // with prefix
          int startIndex = characterSequence.startIndex;
          char[] characters = characterSequence.characters;
          int pos = startIndex + characterSequence.length - 1;
          for (; pos > 0; pos--) {
            switch (characters[pos]) {
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
          typeName = characterSequence.Substring(i + 1, pos + 1);
          for (pos = startIndex; pos < i; pos++) {
            switch (characters[pos]) {
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
          typePrefix = characterSequence.Substring(pos, i);
          typeUri = m_preserveNS ? m_prefixUriBindings.getUri(typePrefix) : null;
        }
        else { // no prefix
          typeName = characterSequence.makeString();
          typePrefix = "";
          typeUri = m_preserveNS ? m_prefixUriBindings.DefaultUri : null;
        }
      }
      else {
        int uriId = readURI();
        typeUri = stringTable.getURI(uriId);
        StringTable.LocalNamePartition partition = stringTable.getLocalNamePartition(uriId);
        int localNameId = readLocalName(partition);
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
    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    protected internal string readPrefixOfNS(int uriId) {
      StringTable.PrefixPartition partition;
      partition = stringTable.getPrefixPartition(uriId);
      int width, id;
      width = partition.forwardedWidth;
      string name;
      id = readNBitUnsigned(width, m_inputStream);
      if (id != 0) {
        name = partition.prefixEntries[id - 1].value;
      }
      else {
        int length = readUnsignedInteger(m_inputStream);
        name = readLiteralString(length, EXISchema.NIL_NODE, m_inputStream).makeString();
        partition.addPrefix(name);
      }
      return name;
    }
    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    protected internal int readURI() {
      int width, id;
      width = stringTable.uriForwardedWidth;
      int uriId;
      id = readNBitUnsigned(width, m_inputStream);
      if (id != 0) {
        uriId = id - 1;
      }
      else {
        int length = readUnsignedInteger(m_inputStream);
        string uri = readLiteralString(length, EXISchema.NIL_NODE, m_inputStream).makeString();
        uriId = stringTable.addURI(uri, (StringTable.LocalNamePartition)null, (StringTable.PrefixPartition)null);
      }
      return uriId;
    }
    /// <summary>
    /// Read a localName. </summary>
    /// <returns> localName ID
    /// @y.exclude </returns>
    protected internal int readLocalName(StringTable.LocalNamePartition partition) {
      int length = readUnsignedInteger(m_inputStream);
      if (length != 0) {
        string name = readLiteralString(length - 1, EXISchema.NIL_NODE, m_inputStream).makeString();
        return partition.addName(name, (IGrammar)null);
      }
      else {
        return readNBitUnsigned(partition.width, m_inputStream);
      }
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    protected internal string readPrefixOfQName(int uriId) {
      StringTable.PrefixPartition prefixPartition;
      prefixPartition = stringTable.getPrefixPartition(uriId);
      int width, id;
      width = prefixPartition.width;
      return (id = readNBitUnsigned(width, m_inputStream)) < prefixPartition.n_strings ? prefixPartition.prefixEntries[id].value : null;
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Crude bits Reader functions
    ///////////////////////////////////////////////////////////////////////////
    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    protected internal abstract bool readBoolean(Stream istream);

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    protected internal int readUnsignedInteger(Stream istream) {
      int shift = 0;
      int @uint = 0;
      do {
        int nextByte;
        if (((nextByte = readEightBitsUnsigned(istream)) & 0x0080) != 0) { // check continuation flag
          @uint |= ((nextByte & 0x007F) << shift);
          shift += 7;
        }
        else {
          return @uint | (nextByte << shift);
        }
      }
      while (true);
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    protected internal long readUnsignedIntegerAsLong(Stream istream) {
      int shift = 0;
      long @uint = 0;
      do {
        int nextByte;
        if (((nextByte = readEightBitsUnsigned(istream)) & 0x0080) != 0) { // check continuation flag
          @uint |= (((long)nextByte & 0x007F) << shift);
          shift += 7;
        }
        else {
          return @uint | ((long)nextByte << shift);
        }
      }
      while (true);
    }

    /// <summary>
    /// Digits are stored into the character array in reverse order. 
    /// @y.exclude
    /// </summary>
    protected internal int readUnsignedIntegerChars(Stream istream, bool addOne, char[] resultChars) {
      int shift = 0;
      int @uint = addOne ? 1 : 0;
      int pos = 0;
      bool continued = true;
      do {
        int nextByte = readEightBitsUnsigned(istream);
        if ((nextByte & 0x0080) != 0) { // check continuation flag
          nextByte &= 0x007F;
        }
        else {
          continued = false;
        }
        @uint += (nextByte << shift);
        if (!continued) {
          do {
            resultChars[pos++] = (char)(48 + @uint % 10);
            @uint /= 10;
          }
          while (@uint != 0);
          return pos;
        }
        shift += 7;
      }
      while (shift != 28);
      
      int shiftLimit = addOne ? 56 : 63;
      long @ulong = @uint;
      do {
        long nextByte = readEightBitsUnsigned(istream);
        if ((nextByte & 0x0080) != 0) { // check continuation flag
          nextByte &= 0x007F;
        }
        else {
          continued = false;
        }
        @ulong += (nextByte << shift);
        if (!continued) {
          while (@ulong != 0) {
            resultChars[pos++] = (char)(48 + (int)(@ulong % 10L));
            @ulong /= 10L;
          }
          return pos;
        }
        shift += 7;
      }
      while (shift != shiftLimit);
      
      BigInteger uinteger = new BigInteger(@ulong);
      do {
        int nextByte = readEightBitsUnsigned(istream);
        if ((nextByte & 0x0080) != 0) { // check continuation flag
          nextByte &= 0x007F;
        }
        else {
          continued = false;
        }
        uinteger = uinteger + (new BigInteger(nextByte) << shift);
        shift += 7;
      }
      while (continued);

      // NOTE: Let BigInteger to the job of the conversion. It's just faster that way.
      string digitsString = uinteger.ToString(NumberFormatInfo.InvariantInfo);
      int n_digits = digitsString.Length;
      int i, ind;
      for (i = 0, ind = n_digits; i < n_digits; i++) {
        resultChars[pos++] = digitsString[--ind];
      }
      return pos;
    }

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    protected internal Characters readLiteralString(int ucsCount, int tp, Stream istream) {
      int n_chars, startIndex, width;
      int[] rcs;
      if (tp >= 0) {
        int serial = m_types[tp + EXISchemaLayout.TYPE_NUMBER];
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
            Debug.Assert(false);
            width = -1;
            rcs = null;
            break;
        }
        n_chars = rcs.Length;
      }
      else { // tp == EXISchema.NIL_NODE
        n_chars = startIndex = width = -1;
        rcs = null;
      }
      m_characterBuffer.ensureCharacters(ucsCount);
      char[] characters = m_characterBuffer.characters;
      int charactersIndex = m_characterBuffer.allocCharacters(ucsCount);
      int _ucsCount = ucsCount;
      Debug.Assert(charactersIndex != -1);
      int length = 0;
      for (bool foundNonBMP = false; ucsCount != 0; --ucsCount) {
        int c, ind;
        if (width > 0 && (ind = readNBitUnsigned(width, istream)) < n_chars) {
          c = rcs[startIndex + ind];
        }
        else if (((c = readUnsignedInteger(istream)) & 0xFFFF0000) != 0) { // non-BMP character
          if (!foundNonBMP) {
            char[] _characters = new char[2 * _ucsCount];
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

    /// <summary>
    /// @y.exclude </summary>
    protected internal abstract int readNBitUnsigned(int width, Stream istream);

    /// <summary>
    /// @y.exclude </summary>
    protected internal abstract int readEightBitsUnsigned(Stream istream);

    ///////////////////////////////////////////////////////////////////////////
    /// Value Scanners
    ///////////////////////////////////////////////////////////////////////////

    private sealed class BooleanValueScanner : ValueScannerBase {
      private readonly Scanner outerInstance;

      internal BooleanValueScanner(Scanner outerInstance) : base(new QName("exi:boolean", ExiUriConst.W3C_2009_EXI_URI)) {
        this.outerInstance = outerInstance;
      }
      public override short CodecID {
        get {
          return CODEC_BOOLEAN;
        }
      }
      public override int getBuiltinRCS(int simpleType) {
        return BuiltinRCS.RCS_ID_BOOLEAN;
      }
      public override Characters scan(int localNameId, int uriId, int tp) {
        if (outerInstance.schema.isPatternedBooleanSimpleType(tp)) {
          switch (outerInstance.readNBitUnsigned(2, m_istream)) {
            case 0:
              return FALSE;
            case 1:
              return ZERO;
            case 2:
              return TRUE;
            case 3:
              return ONE;
            default:
              Debug.Assert(false);
              return null;
          }
        }
        else {
          bool val = outerInstance.readBoolean(m_istream);
          return val ? TRUE : FALSE;
        }
      }
    }

    private sealed class IntegerValueScanner : ValueScannerBase {
      private readonly Scanner outerInstance;

      internal int[] m_ints, m_variants;
      internal long[] m_longs;
      internal readonly char[] m_digitsBuffer;
      internal IntegerValueScanner(Scanner outerInstance) : base(new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI)) {
        this.outerInstance = outerInstance;
        m_ints = m_variants = null;
        m_longs = null;
        m_digitsBuffer = new char[128];
      }
      public EXISchema Schema {
        set {
          if (value != null) {
            m_ints = value.Ints;
            m_variants = value.Variants;
            m_longs = value.Longs;
          }
          else {
            m_ints = m_variants = null;
            m_longs = null;
          }
        }
      }
      /// <summary>
      /// Not for public use.
      /// @y.exclude
      /// </summary>
      public override short CodecID {
        get {
          return CODEC_INTEGER;
        }
      }
      /// <summary>
      /// Not for public use.
      /// @y.exclude
      /// </summary>
      public override int getBuiltinRCS(int simpleType) {
        return BuiltinRCS.RCS_ID_INTEGER;
      }
      /// <summary>
      /// Not for public use.
      /// @y.exclude
      /// </summary>
      public override Characters scan(int localName, int uri, int tp) {
        int pos;
        bool isNegative = false;
        if (outerInstance.schema.ancestryIds[outerInstance.m_types[tp + EXISchemaLayout.TYPE_NUMBER]] == EXISchemaConst.INTEGER_TYPE) {
          int intValue;
          int width;
          switch (width = EXISchema._getWidthOfIntegralSimpleType(tp, outerInstance.m_types)) {
            case EXISchema.INTEGER_CODEC_DEFAULT:
              isNegative = outerInstance.readBoolean(m_istream);
              goto case EXISchema.INTEGER_CODEC_NONNEGATIVE;
            case EXISchema.INTEGER_CODEC_NONNEGATIVE:
              pos = outerInstance.readUnsignedIntegerChars(m_istream, isNegative, m_digitsBuffer);
              if (isNegative) {
                m_digitsBuffer[pos++] = '-';
              }
              outerInstance.m_characterBuffer.ensureCharacters(pos);
              return outerInstance.m_characterBuffer.addCharsReverse(m_digitsBuffer, pos);
            default:
              pos = 0;
              intValue = outerInstance.readNBitUnsigned(width, m_istream);
              int minInclusiveFacet = outerInstance.schema.getMinInclusiveFacetOfIntegerSimpleType(tp);
              int variantType;
              switch ((variantType = outerInstance.schema.getTypeOfVariant(minInclusiveFacet))) {
                case EXISchema.VARIANT_INT:
                  int minInclusiveIntValue = m_ints[m_variants[minInclusiveFacet]];
                  if (isNegative = (intValue += minInclusiveIntValue) < 0) {
                    intValue = -intValue;
                  }
                  do {
                    m_digitsBuffer[pos++] = (char)(48 + (intValue % 10));
                    intValue /= 10;
                  }
                  while (intValue != 0);
                  if (isNegative) {
                    m_digitsBuffer[pos++] = '-';
                  }
                  outerInstance.m_characterBuffer.ensureCharacters(pos);
                  return outerInstance.m_characterBuffer.addCharsReverse(m_digitsBuffer, pos);
                case EXISchema.VARIANT_LONG:
                  long minInclusiveLongValue = m_longs[m_variants[minInclusiveFacet]];
                  long longValue = minInclusiveLongValue + intValue;
                  if (isNegative = longValue < 0) {
                    longValue = -longValue;
                  }
                  do {
                    m_digitsBuffer[pos++] = (char)(48 + (longValue % 10));
                    longValue /= 10;
                  }
                  while (longValue != 0);
                  if (isNegative) {
                    m_digitsBuffer[pos++] = '-';
                  }
                  outerInstance.m_characterBuffer.ensureCharacters(pos);
                  return outerInstance.m_characterBuffer.addCharsReverse(m_digitsBuffer, pos);
                default:
                  Debug.Assert(variantType == EXISchema.VARIANT_INTEGER);
                  BigInteger minInclusiveIntegerValue = outerInstance.schema.getIntegerValueOfVariant(minInclusiveFacet);
                  string stringValue = (minInclusiveIntegerValue + intValue).ToString(NumberFormatInfo.InvariantInfo);
                  int length = stringValue.Length;
                  outerInstance.m_characterBuffer.ensureCharacters(length);
                  return outerInstance.m_characterBuffer.addString(stringValue, length);
              }
          }
        }
        else {
          isNegative = outerInstance.readBoolean(m_istream);
          pos = outerInstance.readUnsignedIntegerChars(m_istream, isNegative, m_digitsBuffer);
          if (isNegative) {
            m_digitsBuffer[pos++] = '-';
          }
          outerInstance.m_characterBuffer.ensureCharacters(pos);
          return outerInstance.m_characterBuffer.addCharsReverse(m_digitsBuffer, pos);
        }
      }
    }

    private sealed class EnumerationValueScanner : ValueScannerBase {
      private readonly Scanner outerInstance;

      internal EnumerationValueScanner(Scanner outerInstance) : base((QName)null) {
        this.outerInstance = outerInstance;
      }
      /// <summary>
      /// Not for public use.
      /// @y.exclude
      /// </summary>
      public override short CodecID {
        get {
          return CODEC_ENUMERATION;
        }
      }
      /// <summary>
      /// Not for public use.
      /// @y.exclude
      /// </summary>
      public override int getBuiltinRCS(int simpleType) {
        int baseType = outerInstance.schema.getBaseTypeOfSimpleType(simpleType);
        return outerInstance.getValueScanner(baseType).getBuiltinRCS(baseType);
      }
      /// <summary>
      /// Not for public use.
      /// @y.exclude
      /// </summary>
      public override Characters scan(int localNameId, int uriId, int tp) {
        int n_enums = outerInstance.schema.getEnumerationFacetCountOfAtomicSimpleType(tp);
        Debug.Assert(n_enums > 0);
        int width, n;
        for (width = 0, n = n_enums - 1; n != 0; n >>= 1, ++width) {
          ;
        }

        int index = outerInstance.readNBitUnsigned(width, m_istream);
        Debug.Assert(index >= 0);

        int facet = outerInstance.schema.getEnumerationFacetOfAtomicSimpleType(tp, index);
        return outerInstance.schema.getVariantCharacters(facet);
      }
    }

    private sealed class ListValueScanner : ValueScannerBase {
      private readonly Scanner outerInstance;

      internal char[] m_listChars;
      public ListValueScanner(Scanner outerInstance) : base((QName)null) {
        this.outerInstance = outerInstance;
        m_listChars = new char[512];
      }
      /// <summary>
      /// @y.exclude </summary>
      public override short CodecID {
        get {
          return CODEC_LIST;
        }
      }
      /// <summary>
      /// @y.exclude </summary>
      public override int getBuiltinRCS(int simpleType) {
        Debug.Assert(outerInstance.schema.getVarietyOfSimpleType(simpleType) == EXISchema.LIST_SIMPLE_TYPE);
        int itemType = outerInstance.schema.getItemTypeOfListSimpleType(simpleType);
        short codecID = outerInstance.m_codecTable[outerInstance.schema.getSerialOfType(itemType)];
        ValueScanner itemValueScanner = outerInstance.m_valueScannerTable[codecID];
        return itemValueScanner.getBuiltinRCS(itemType);
      }
      /// <summary>
      /// @y.exclude </summary>
      public override Characters scan(int localName, int uri, int tp) {
        Debug.Assert(outerInstance.schema.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE);

        int itemType = outerInstance.schema.getItemTypeOfListSimpleType(tp);
        short codecID = outerInstance.m_codecTable[outerInstance.schema.getSerialOfType(itemType)];
        ValueScanner itemValueScanner = outerInstance.m_valueScannerTable[codecID];

        int n_items = outerInstance.readUnsignedInteger(m_istream);
        int n_listChars = 0;
        for (int i = 0; i < n_items; i++) {
          if (i != 0) {
            if (n_listChars == m_listChars.Length) {
              expandCharArray();
            }
            m_listChars[n_listChars++] = ' ';
          }
          Characters itemValue = itemValueScanner.scan(localName, uri, itemType);
          int n_characters = itemValue.length;
          if (n_listChars + n_characters > m_listChars.Length) {
            expandCharArray();
          }
          Array.Copy(itemValue.characters, itemValue.startIndex, m_listChars, n_listChars, n_characters);
          n_listChars += n_characters;
        }
        outerInstance.m_characterBuffer.ensureCharacters(n_listChars);
        return outerInstance.m_characterBuffer.addChars(m_listChars, n_listChars);
      }
      internal void expandCharArray() {
        int clen = m_listChars.Length;
        int nlen = clen + (clen >> 1);
        char[] listChars = new char[nlen];
        Array.Copy(m_listChars, 0, listChars, 0, clen);
        m_listChars = listChars;
      }
    }

    private sealed class DecimalValueScanner : ValueScannerBase {
      private readonly Scanner outerInstance;

      internal readonly char[] m_integralDigitsChars;
      internal readonly char[] m_fractionDigitsChars;
      internal DecimalValueScanner(Scanner outerInstance) : base(new QName("exi:decimal", ExiUriConst.W3C_2009_EXI_URI)) {
        this.outerInstance = outerInstance;
        m_integralDigitsChars = new char[128];
        m_fractionDigitsChars = new char[128];
      }
      /// <summary>
      /// @y.exclude </summary>
      public override short CodecID {
        get {
          return CODEC_DECIMAL;
        }
      }
      /// <summary>
      /// @y.exclude </summary>
      public override int getBuiltinRCS(int simpleType) {
        return BuiltinRCS.RCS_ID_DECIMAL;
      }
      /// <summary>
      /// @y.exclude </summary>
      public override Characters scan(int localName, int uri, int tp) {
        bool isNegative = outerInstance.readBoolean(m_istream);
        int n_integralDigits = outerInstance.readUnsignedIntegerChars(m_istream, false, m_integralDigitsChars);
        if (isNegative) {
          m_integralDigitsChars[n_integralDigits++] = '-';
        }
        int n_fractionDigits = outerInstance.readUnsignedIntegerChars(m_istream, false, m_fractionDigitsChars);
        int totalLength = n_integralDigits + 1 + n_fractionDigits;
        outerInstance.m_characterBuffer.ensureCharacters(totalLength);
        return outerInstance.m_characterBuffer.addDecimalChars(m_integralDigitsChars, n_integralDigits, m_fractionDigitsChars, n_fractionDigits, totalLength);
      }
    }

    private sealed class DateTimeValueScanner : DateTimeValueScannerBase {
      private readonly Scanner outerInstance;

      internal DateTimeValueScanner(Scanner outerInstance) : base(new QName("exi:dateTime", ExiUriConst.W3C_2009_EXI_URI), outerInstance) {
        this.outerInstance = outerInstance;
      }
      public override short CodecID {
        get {
          return CODEC_DATETIME;
        }
      }
      public override Characters scan(int localNameId, int uriId, int tp) {
        m_n_dateTimeCharacters = 0;
        readYear(m_istream);
        readMonthDay(m_istream);
        m_dateTimeCharacters[m_n_dateTimeCharacters++] = 'T';
        readTime(m_istream);
        readTimeZone(m_istream);
        outerInstance.m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
        return outerInstance.m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
      }
    }

    private sealed class TimeValueScanner : DateTimeValueScannerBase {
      private readonly Scanner outerInstance;

      internal TimeValueScanner(Scanner outerInstance) : base(new QName("exi:time", ExiUriConst.W3C_2009_EXI_URI), outerInstance) {
        this.outerInstance = outerInstance;
      }
      public override short CodecID {
        get {
          return CODEC_TIME;
        }
      }
      public override Characters scan(int localNameId, int uriId, int tp) {
        m_n_dateTimeCharacters = 0;
        readTime(m_istream);
        readTimeZone(m_istream);
        outerInstance.m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
        return outerInstance.m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
      }
    }

    private sealed class DateValueScanner : DateTimeValueScannerBase {
      private readonly Scanner outerInstance;

      internal DateValueScanner(Scanner outerInstance) : base(new QName("exi:date", ExiUriConst.W3C_2009_EXI_URI), outerInstance) {
        this.outerInstance = outerInstance;
      }
      public override short CodecID {
        get {
          return CODEC_DATE;
        }
      }
      public override Characters scan(int localNameId, int uriId, int tp) {
        m_n_dateTimeCharacters = 0;
        readYear(m_istream);
        readMonthDay(m_istream);
        readTimeZone(m_istream);
        outerInstance.m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
        return outerInstance.m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
      }
    }

    private sealed class GYearMonthValueScanner : DateTimeValueScannerBase {
      private readonly Scanner outerInstance;

      internal GYearMonthValueScanner(Scanner outerInstance) : base(new QName("exi:gYearMonth", ExiUriConst.W3C_2009_EXI_URI), outerInstance) {
        this.outerInstance = outerInstance;
      }
      public override short CodecID {
        get {
          return CODEC_GYEARMONTH;
        }
      }
      public override Characters scan(int localNameId, int uriId, int tp) {
        m_n_dateTimeCharacters = 0;
        readGYearMonth(m_istream);
        readTimeZone(m_istream);
        outerInstance.m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
        return outerInstance.m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
      }
    }

    private sealed class GYearValueScanner : DateTimeValueScannerBase {
      private readonly Scanner outerInstance;

      internal GYearValueScanner(Scanner outerInstance) : base(new QName("exi:gYear", ExiUriConst.W3C_2009_EXI_URI), outerInstance) {
        this.outerInstance = outerInstance;
      }
      public override short CodecID {
        get {
          return CODEC_GYEAR;
        }
      }
      public override Characters scan(int localNameId, int uriId, int tp) {
        m_n_dateTimeCharacters = 0;
        readYear(m_istream);
        readTimeZone(m_istream);
        outerInstance.m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
        return outerInstance.m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
      }
    }

    private sealed class GMonthDayValueScanner : DateTimeValueScannerBase {
      private readonly Scanner outerInstance;

      internal GMonthDayValueScanner(Scanner outerInstance) : base(new QName("exi:gMonthDay", ExiUriConst.W3C_2009_EXI_URI), outerInstance) {
        this.outerInstance = outerInstance;
      }
      public override short CodecID {
        get {
          return CODEC_GMONTHDAY;
        }
      }
      public override Characters scan(int localNameId, int uriId, int tp) {
        m_n_dateTimeCharacters = 0;
        readGMonthDay(m_istream);
        readTimeZone(m_istream);
        outerInstance.m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
        return outerInstance.m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
      }
    }

    private sealed class GDayValueScanner : DateTimeValueScannerBase {
      private readonly Scanner outerInstance;

      internal GDayValueScanner(Scanner outerInstance) : base(new QName("exi:gDay", ExiUriConst.W3C_2009_EXI_URI), outerInstance) {
        this.outerInstance = outerInstance;
      }
      public override short CodecID {
        get {
          return CODEC_GDAY;
        }
      }
      public override Characters scan(int localNameId, int uriId, int tp) {
        m_n_dateTimeCharacters = 0;
        readGDay(m_istream);
        readTimeZone(m_istream);
        outerInstance.m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
        return outerInstance.m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
      }
    }

    private sealed class GMonthValueScanner : DateTimeValueScannerBase {
      private readonly Scanner outerInstance;

      internal GMonthValueScanner(Scanner outerInstance) : base(new QName("exi:gMonth", ExiUriConst.W3C_2009_EXI_URI), outerInstance) {
        this.outerInstance = outerInstance;
      }
      public override short CodecID {
        get {
          return CODEC_GMONTH;
        }
      }
      public override Characters scan(int localNameId, int uriId, int tp) {
        m_n_dateTimeCharacters = 0;
        readGMonth(m_istream);
        readTimeZone(m_istream);
        outerInstance.m_characterBuffer.ensureCharacters(m_n_dateTimeCharacters);
        return outerInstance.m_characterBuffer.addChars(m_dateTimeCharacters, m_n_dateTimeCharacters);
      }
    }

    private sealed class FloatValueScanner : ValueScannerBase {
      private readonly Scanner outerInstance;

      internal FloatValueScanner(Scanner outerInstance) : base(new QName("exi:double", ExiUriConst.W3C_2009_EXI_URI)) {
        this.outerInstance = outerInstance;
      }
      /// <summary>
      /// Not for public use.
      /// @y.exclude
      /// </summary>
      public override short CodecID {
        get {
          return CODEC_DOUBLE;
        }
      }
      /// <summary>
      /// Not for public use.
      /// @y.exclude
      /// </summary>
      public override int getBuiltinRCS(int simpleType) {
        return BuiltinRCS.RCS_ID_DOUBLE;
      }
      /// <summary>
      /// Not for public use.
      /// @y.exclude
      /// </summary>
      public override Characters scan(int localNameId, int uriId, int tp) {
        bool isNegative = outerInstance.readBoolean(m_istream);
        long longValue = readUnsignedInteger63(m_istream);
        if (isNegative) {
          longValue = -longValue - 1;
        }
        string mantissaDigitsString = Convert.ToString(longValue, NumberFormatInfo.InvariantInfo);
        bool isNegativeExponent = outerInstance.readBoolean(m_istream);
        int intValue = outerInstance.readUnsignedInteger(m_istream);
        if (isNegativeExponent) {
          ++intValue;
        }
        string stringValue;
        if (16384 != intValue) {
          stringValue = mantissaDigitsString + 'E' + (isNegativeExponent ? "-" : "") + Convert.ToString(intValue, NumberFormatInfo.InvariantInfo);
        }
        else {
          stringValue = longValue == 1 ? "INF" : longValue == -1 ? "-INF" : "NaN";
        }
        int length = stringValue.Length;
        outerInstance.m_characterBuffer.ensureCharacters(length);
        return outerInstance.m_characterBuffer.addString(stringValue, length);
      }
      /// <summary>
      /// Read an unsigned integer value of range [0 ... 2^63 - 1].
      /// Possible effective number of bits 7, 14, 21, 28, 35, 42, 49, 56, 63.
      /// </summary>
      internal long readUnsignedInteger63(Stream istream) {
        int shift = 0;
        bool continued = true;
        long @ulong = 0;
        do {
          long nextByte = outerInstance.readEightBitsUnsigned(istream);
          if ((nextByte & 0x0080) != 0) { // check continuation flag
            nextByte &= 0x007F;
          }
          else {
            continued = false;
          }
          @ulong += (nextByte << shift);
          if (!continued) {
            return @ulong;
          }
          shift += 7;
        }
        while (shift != 63);
        Debug.Assert(!continued);
        return @ulong;
      }
    }

  }

}