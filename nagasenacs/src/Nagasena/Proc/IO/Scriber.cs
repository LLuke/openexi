using System;
using System.Diagnostics;
using System.Collections.Generic;
using System.IO;
using System.Numerics;
using System.Text;

using DeflateStrategy = ICSharpCode.SharpZipLib.Zip.Compression.DeflateStrategy;

using EXIOptions = Nagasena.Proc.Common.EXIOptions;
using EventType = Nagasena.Proc.Common.EventType;
using QName = Nagasena.Proc.Common.QName;
using StringTable = Nagasena.Proc.Common.StringTable;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using Apparatus = Nagasena.Proc.Grammars.Apparatus;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using ValueApparatus = Nagasena.Proc.Grammars.ValueApparatus;
using Characters = Nagasena.Schema.Characters;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using EXISchemaLayout = Nagasena.Schema.EXISchemaLayout;

namespace Nagasena.Proc.IO {

  /// <exclude/>
  public abstract class Scriber : Apparatus {

    internal static readonly BigInteger BIGINTEGER_0x007F = new BigInteger(0x007F);

    private static readonly byte[] COOKIE = new byte[] { 36, 69, 88, 73 }; // "$", "E", "X", "I"

    protected internal bool m_preserveNS;
    internal int valueMaxExclusiveLength;

    private readonly ValueScriber[] m_valueScribers;

    protected internal readonly ValueScriber[] m_valueScriberTable; // codec id -> valueScriber

    internal static readonly BinaryValueScriber m_base64BinaryValueScriberInherent;
    protected internal static readonly ValueScriber m_booleanValueScriberInherent;
    protected internal static readonly ValueScriber m_floatValueScriberInherent;
    protected internal static readonly ValueScriber m_integerValueScriberInherent;
    internal static readonly BinaryValueScriber m_hexBinaryValueScriberInherent;
    protected internal static readonly ValueScriber m_decimalValueScriberInherent;
    internal static readonly DateTimeValueScriber m_dateTimeValueScriberInherent;
    internal static readonly DateValueScriber m_dateValueScriberInherent;
    internal static readonly TimeValueScriber m_timeValueScriberInherent;
    internal static readonly GYearMonthValueScriber m_gYearMonthValueScriberInherent;
    internal static readonly GMonthDayValueScriber m_gMonthDayValueScriberInherent;
    internal static readonly GYearValueScriber m_gYearValueScriberInherent;
    internal static readonly GMonthValueScriber m_gMonthValueScriberInherent;
    internal static readonly GDayValueScriber m_gDayValueScriberInherent;

    static Scriber() {
      m_base64BinaryValueScriberInherent = Base64BinaryValueScriber.instance;
      m_booleanValueScriberInherent = BooleanValueScriber.instance;
      m_decimalValueScriberInherent = DecimalValueScriber.instance;
      m_hexBinaryValueScriberInherent = HexBinaryValueScriber.instance;
      m_integerValueScriberInherent = IntegerValueScriber.instance;
      m_floatValueScriberInherent = FloatValueScriber.instance;
      m_dateTimeValueScriberInherent = DateTimeValueScriber.instance;
      m_dateValueScriberInherent = DateValueScriber.instance;
      m_timeValueScriberInherent = TimeValueScriber.instance;
      m_gYearMonthValueScriberInherent = GYearMonthValueScriber.instance;
      m_gMonthDayValueScriberInherent = GMonthDayValueScriber.instance;
      m_gYearValueScriberInherent = GYearValueScriber.instance;
      m_gMonthValueScriberInherent = GMonthValueScriber.instance;
      m_gDayValueScriberInherent = GDayValueScriber.instance;
    }

    internal readonly StringValueScriber m_stringValueScriberInherent;
    protected internal readonly ValueScriber m_enumerationValueScriberInherent;
    protected internal readonly ValueScriber m_listValueScriberInherent;

    protected internal readonly ValueScriber m_stringValueScriberLexical;
    protected internal readonly ValueScriber m_booleanValueScriberLexical;
    protected internal readonly ValueScriber m_enumerationValueScriberLexical;
    protected internal readonly ValueScriber m_listValueScriberLexical;
    protected internal readonly ValueScriber m_decimalValueScriberLexical;
    protected internal readonly ValueScriber m_dateTimeValueScriberLexical;
    protected internal readonly ValueScriber m_timeValueScriberLexical;
    protected internal readonly ValueScriber m_dateValueScriberLexical;
    protected internal readonly ValueScriber m_gDayValueScriberLexical;
    protected internal readonly ValueScriber m_gMonthValueScriberLexical;
    protected internal readonly ValueScriber m_gMonthDayValueScriberLexical;
    protected internal readonly ValueScriber m_gYearValueScriberLexical;
    protected internal readonly ValueScriber m_gYearMonthValueScriberLexical;
    protected internal readonly ValueScriber m_floatValueScriberLexical;
    protected internal readonly ValueScriber m_integerValueScriberLexical;
    protected internal readonly ValueScriber m_base64BinaryValueScriberLexical;
    protected internal readonly ValueScriber m_hexBinaryValueScriberLexical;

    protected internal CharacterBuffer m_characterBuffer;

    // Used by writeLiteralString method to temporarily store UCS characters
    private int[] m_ucsBuffer;
    // Used by some of the ValueScribers to temporarily store digits
    internal readonly StringBuilder stringBuilder1, stringBuilder2;
    internal readonly Scribble scribble1;

    protected Stream m_outputStream;

    /// <summary>
    /// Creates a string table for use with a scriber. </summary>
    /// <param name="schema"> a schema that contains initial entries of the string table </param>
    /// <returns> a string table for use with a scriber </returns>
    public static StringTable createStringTable(GrammarCache grammarCache) {
      return new StringTable(grammarCache, StringTable.Usage.encoding);
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Constructor
    ///////////////////////////////////////////////////////////////////////////

    protected internal Scriber(bool isForEXIOptions) : base() {
      m_preserveNS = false;
      valueMaxExclusiveLength = int.MaxValue;
      m_preserveLexicalValues = false;

      List<ValueScriber> valueScribers = new List<ValueScriber>();
      valueScribers.Add(m_stringValueScriberInherent = new StringValueScriber());
      valueScribers.Add(m_booleanValueScriberInherent);
      valueScribers.Add(m_decimalValueScriberInherent);
      valueScribers.Add(m_floatValueScriberInherent);
      valueScribers.Add(m_integerValueScriberInherent);
      valueScribers.Add(m_base64BinaryValueScriberInherent);
      valueScribers.Add(m_hexBinaryValueScriberInherent);
      if (!isForEXIOptions) {
        m_stringValueScriberLexical = new ValueScriberLexical(m_stringValueScriberInherent, m_stringValueScriberInherent);
        m_booleanValueScriberLexical = new ValueScriberLexical(m_booleanValueScriberInherent, m_stringValueScriberInherent);
        m_integerValueScriberLexical = new ValueScriberLexical(m_integerValueScriberInherent, m_stringValueScriberInherent);
        valueScribers.Add(m_enumerationValueScriberInherent = new EnumerationValueScriber());
        m_enumerationValueScriberLexical = new ValueScriberLexical(m_enumerationValueScriberInherent, m_stringValueScriberInherent);
        valueScribers.Add(m_listValueScriberInherent = new ListValueScriber());
        m_listValueScriberLexical = new ValueScriberLexical(m_listValueScriberInherent, m_stringValueScriberInherent);
        m_decimalValueScriberLexical = new ValueScriberLexical(m_decimalValueScriberInherent, m_stringValueScriberInherent);
        valueScribers.Add(m_dateTimeValueScriberInherent);
        m_dateTimeValueScriberLexical = new ValueScriberLexical(m_dateTimeValueScriberInherent, m_stringValueScriberInherent);
        valueScribers.Add(m_timeValueScriberInherent);
        m_timeValueScriberLexical = new ValueScriberLexical(m_timeValueScriberInherent, m_stringValueScriberInherent);
        valueScribers.Add(m_dateValueScriberInherent);
        m_dateValueScriberLexical = new ValueScriberLexical(m_dateValueScriberInherent, m_stringValueScriberInherent);
        valueScribers.Add(m_gDayValueScriberInherent);
        m_gDayValueScriberLexical = new ValueScriberLexical(m_gDayValueScriberInherent, m_stringValueScriberInherent);
        valueScribers.Add(m_gMonthValueScriberInherent);
        m_gMonthValueScriberLexical = new ValueScriberLexical(m_gMonthValueScriberInherent, m_stringValueScriberInherent);
        valueScribers.Add(m_gMonthDayValueScriberInherent);
        m_gMonthDayValueScriberLexical = new ValueScriberLexical(m_gMonthDayValueScriberInherent, m_stringValueScriberInherent);
        valueScribers.Add(m_gYearValueScriberInherent);
        m_gYearValueScriberLexical = new ValueScriberLexical(m_gYearValueScriberInherent, m_stringValueScriberInherent);
        valueScribers.Add(m_gYearMonthValueScriberInherent);
        m_gYearMonthValueScriberLexical = new ValueScriberLexical(m_gYearMonthValueScriberInherent, m_stringValueScriberInherent);
        m_floatValueScriberLexical = new ValueScriberLexical(m_floatValueScriberInherent, m_stringValueScriberInherent);
        m_base64BinaryValueScriberLexical = new ValueScriberLexical(m_base64BinaryValueScriberInherent, m_stringValueScriberInherent);
        m_hexBinaryValueScriberLexical = new ValueScriberLexical(m_hexBinaryValueScriberInherent, m_stringValueScriberInherent);
      }
      else {
        m_stringValueScriberLexical = null;
        m_booleanValueScriberLexical = null;
        m_integerValueScriberLexical = null;
        m_decimalValueScriberLexical = null;
        m_floatValueScriberLexical = null;
        m_base64BinaryValueScriberLexical = null;
        m_hexBinaryValueScriberLexical = null;
        m_dateTimeValueScriberLexical = null;
        m_dateValueScriberLexical = null;
        m_timeValueScriberLexical = null;
        m_gYearMonthValueScriberLexical = null;
        m_gMonthDayValueScriberLexical = null;
        m_gYearValueScriberLexical = null;
        m_gMonthValueScriberLexical = null;
        m_gDayValueScriberLexical = null;
        m_enumerationValueScriberInherent = m_enumerationValueScriberLexical = null;
        m_listValueScriberInherent = m_listValueScriberLexical = null;
      }

      m_valueScribers = new ValueScriber[valueScribers.Count];
      for (int i = 0; i < m_valueScribers.Length; i++) {
        m_valueScribers[i] = valueScribers[i];
      }

      m_valueScriberTable = new ValueScriber[N_CODECS];

      m_characterBuffer = new CharacterBuffer(false);
      m_ucsBuffer = new int[1024];
      stringBuilder1 = new StringBuilder();
      stringBuilder2 = new StringBuilder();
      scribble1 = new Scribble();

      m_outputStream = null;
    }

    protected internal CharacterBuffer ensureCharacters(int length) {
      CharacterBuffer characterBuffer = m_characterBuffer;
      int availability;
      if ((availability = m_characterBuffer.availability()) < length) {
        int bufSize = length > CharacterBuffer.BUFSIZE_DEFAULT ? length : CharacterBuffer.BUFSIZE_DEFAULT;
        characterBuffer = new CharacterBuffer(bufSize, false);
      }
      if (characterBuffer != m_characterBuffer) {
        int _availability = characterBuffer.availability();
        if (_availability != 0 && availability < _availability) {
          m_characterBuffer = characterBuffer;
        }
      }
      return characterBuffer;
    }

    public static void writeHeaderPreamble(Stream ostream, bool outputCookie, bool outputOptions) {
      if (outputCookie) {
        ostream.Write(COOKIE, 0, COOKIE.Length);
      }
      // write 10 1 00000 if outputOptions is true, otherwise write 10 0 00000 
      ostream.WriteByte((byte)(outputOptions ? 160 : 128));
    }

    protected internal override ValueApparatus[] ValueApparatuses {
      get {
        return m_valueScribers;
      }
    }

    /// <summary>
    /// Set an output stream to which encoded streams are written out. </summary>
    /// <param name="dataStream"> output stream </param>
    public abstract Stream OutputStream { set; }

    public int ValueMaxLength {
      set {
        valueMaxExclusiveLength = value == EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED ? int.MaxValue : value + 1;
      }
    }

    public bool PreserveNS {
      set {
        m_preserveNS = value;
      }
    }

    public abstract int BlockSize { set; }

    ///////////////////////////////////////////////////////////////////////////
    /// Methods for controlling Deflater parameters
    ///////////////////////////////////////////////////////////////////////////

    public virtual void setDeflateParams(int level, DeflateStrategy strategy) {
      // Do nothing.
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Value Scriber Functions
    ///////////////////////////////////////////////////////////////////////////

    public ValueScriber getValueScriberByID(short valueScriberID) {
      return m_valueScriberTable[valueScriberID];
    }

    public ValueScriber getValueScriber(int stype) {
      Debug.Assert(stype != EXISchema.NIL_NODE);
      int serial = schema.getSerialOfType(stype);
      return m_valueScriberTable[m_codecTable[serial]];
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Structure Scriber Functions
    ///////////////////////////////////////////////////////////////////////////

    public abstract void writeEventType(EventType eventType);

    public abstract void writeNS(string uri, string prefix, bool localElementNs);

    public abstract void writeQName(QName qName, EventType eventType);

    /// <summary>
    /// Write a name content item.
    /// Name content items are used in PI, DT, ER. 
    /// </summary>
    public void writeName(string name) {
      writeLiteralString(name, 0, m_outputStream);
    }

    /// <summary>
    /// Write a "public" content item.
    /// "Public" content items are used in DT. 
    /// </summary>
    public void writePublic(string publicId) {
      writeLiteralString(publicId, 0, m_outputStream);
    }

    /// <summary>
    /// Write a "system" content item.
    /// "System" content items are used in DT. 
    /// </summary>
    public void writeSystem(string systemId) {
      writeLiteralString(systemId, 0, m_outputStream);
    }

    /// <summary>
    /// Write a text content item. 
    /// Text content items are used in CM, PI, DT.
    /// </summary>
    public void writeText(string text) {
      writeLiteralString(text, 0, m_outputStream);
    }

    /// <summary>
    /// Write xsi:type attribute value
    /// </summary>
    public void writeXsiTypeValue(QName qName) {
      Stream outputStream = m_outputStream;
      if (m_preserveLexicalValues) {
        m_valueScriberTable[CODEC_STRING].scribe(qName.qName, (Scribble)null, EXISchemaConst.XSI_LOCALNAME_TYPE_ID, 
          XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID, EXISchema.NIL_NODE, outputStream, this);
      }
      else {
        string typeNamespaceName = qName.namespaceName;
        bool isResolved;
        if (!(isResolved = typeNamespaceName != null)) {
          typeNamespaceName = "";
        }
        StringTable.LocalNamePartition localNamePartition;
        int uriId = writeURI(typeNamespaceName, outputStream);
        localNamePartition = stringTable.getLocalNamePartition(uriId);
        writeLocalName(isResolved ? qName.localName : qName.qName, localNamePartition, outputStream);
        if (m_preserveNS) {
          writePrefixOfQName(qName.prefix, uriId, outputStream);
        }
      }
    }

    /// <summary>
    /// Write a value of xsi:nil attribute that matched AT(xsi:nil) event type  
    /// </summary>
    public void writeXsiNilValue(bool val, string stringValue) {
      Stream outputStream = m_outputStream;
      if (m_preserveLexicalValues) {
        m_valueScriberTable[CODEC_STRING].scribe(stringValue, (Scribble)null, EXISchemaConst.XSI_LOCALNAME_NIL_ID, 
          XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID, EXISchema.NIL_NODE, outputStream, this);
      }
      else {
        writeBoolean(val, outputStream);
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Other Functions
    ///////////////////////////////////////////////////////////////////////////


    public abstract void finish();

    protected internal abstract void writeUnsignedInteger32(int @uint, Stream ostream);
    protected internal abstract void writeUnsignedInteger64(long @ulong, Stream ostream);
    protected internal abstract void writeUnsignedInteger(BigInteger @uint, Stream ostream);

    protected internal void writeLiteralCharacters(Characters str, int length, int lengthOffset, int simpleType, Stream ostream) {
      int n_chars, escapeIndex, startIndex, width;
      int[] rcs;
      if (simpleType >= 0) {
        n_chars = m_restrictedCharacterCountTable[m_types[simpleType + EXISchemaLayout.TYPE_NUMBER]];
        if (n_chars != 0) {
          rcs = m_types;
          startIndex = schema.getRestrictedCharacterOfSimpleType(simpleType);
          width = BuiltinRCS.WIDTHS[n_chars];
          escapeIndex = startIndex + n_chars;
        }
        else {
          startIndex = width = escapeIndex = -1;
          rcs = null;
        }
      }
      else if (simpleType != EXISchema.NIL_NODE) {
        startIndex = 0;
        switch (simpleType) {
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
        escapeIndex = n_chars = rcs.Length;
      }
      else { // simpleType == EXISchema.NIL_NODE
        n_chars = startIndex = width = escapeIndex = -1;
        rcs = null;
      }
      int n_ucsCount = str.ucsCount;
      writeUnsignedInteger32(lengthOffset + n_ucsCount, ostream);
      char[] characters = str.characters;
      int charactersIndex = str.startIndex;
      for (int i = 0; i < length; i++) {
        int c = characters[charactersIndex + i];
        if (width > 0) {
          int min = startIndex;
          int max = escapeIndex - 1;
          do {
            int watershed = (min + max) / 2;
            int watershedValue = rcs[watershed];
            if (c == watershedValue) {
              writeNBitUnsigned(watershed - startIndex, width, ostream);
              goto iloopContinue;
            }
            if (c < watershedValue) {
              max = watershed - 1;
            }
            else { // watershedValue < c
              min = watershed + 1;
            }
          }
          while (min <= max);
          // the character did not match any of the RCS chars.
          writeNBitUnsigned(escapeIndex - startIndex, width, ostream);
        }
        int ucs;
        if ((c & 0xFC00) != 0xD800) {
          ucs = c;
        }
        else { // high surrogate
          char c2 = characters[charactersIndex + ++i];
          if ((c2 & 0xFC00) == 0xDC00) { // low surrogate
            ucs = (((c & 0x3FF) << 10) | (c2 & 0x3FF)) + 0x10000;
          }
          else {
            --i;
            ucs = c;
          }
        }
        writeUnsignedInteger32(ucs, ostream);
        iloopContinue:;
      }
    }

    protected internal int writeURI(string uri, Stream structureChannelStream) {
      int n_uris, width, uriId;
      n_uris = stringTable.n_uris;
      width = stringTable.uriForwardedWidth;
      if ((uriId = stringTable.internURI(uri)) < n_uris) {
        writeNBitUnsigned(uriId + 1, width, structureChannelStream);
      }
      else {
        writeNBitUnsigned(0, width, structureChannelStream);
        writeLiteralString(uri, 0, structureChannelStream);
      }
      return uriId;
    }

    /// <summary>
    /// Write out a local name. </summary>
    /// <returns> localName ID </returns>
    protected internal int writeLocalName(string localName, StringTable.LocalNamePartition partition, Stream structureChannelStream) {
      int n_names, width, id;
      n_names = partition.n_strings;
      width = partition.width;
      if ((id = partition.internName(localName)) < n_names) {
        writeUnsignedInteger32(0, structureChannelStream);
        writeNBitUnsigned(id, width, structureChannelStream);
      }
      else {
        writeLiteralString(localName, 1, structureChannelStream);
      }
      return id;
    }

    private void writeLiteralString(string str, int lengthOffset, Stream structureChannelStream) {
      int length = str.Length;
      if (length > m_ucsBuffer.Length) {
        m_ucsBuffer = new int[length + 256];
      }
      int ucsCount = 0;
      for (int i = 0; i < length; ++ucsCount) {
        char c = str[i++];
        int ucs = c;
        if ((c & 0xFC00) == 0xD800) { // high surrogate
          if (i < length) {
            char c2 = str[i];
            if ((c2 & 0xFC00) == 0xDC00) { // low surrogate
              ucs = (((c & 0x3FF) << 10) | (c2 & 0x3FF)) + 0x10000;
              ++i;
            }
          }
        }
        m_ucsBuffer[ucsCount] = ucs;
      }
      writeUnsignedInteger32(lengthOffset + ucsCount, structureChannelStream);
      for (int i = 0; i < ucsCount; i++) {
        writeUnsignedInteger32(m_ucsBuffer[i], structureChannelStream);
      }
    }

    protected internal void writePrefixOfQName(string prefix, int uriId, Stream structureChannelStream) {
      Debug.Assert(m_preserveNS);
      if (prefix != null) {
        StringTable.PrefixPartition prefixPartition;
        prefixPartition = stringTable.getPrefixPartition(uriId);
        int id;
        if ((id = prefixPartition.getCompactId(prefix)) == -1)
          id = 0;
        writeNBitUnsigned(id, prefixPartition.width, structureChannelStream);
      }
      else
        throw new ScriberRuntimeException(ScriberRuntimeException.PREFIX_IS_NULL);
    }

    protected internal abstract void writeNBitUnsigned(int val, int width, Stream ostream);

    protected internal abstract void writeBoolean(bool val, Stream ostream);

  }

}