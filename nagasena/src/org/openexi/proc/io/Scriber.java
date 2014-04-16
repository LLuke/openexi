package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.grammars.Apparatus;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.grammars.ValueApparatus;
import org.openexi.schema.Characters;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaConst;
import org.openexi.schema.EXISchemaLayout;

public abstract class Scriber extends Apparatus {

  static final BigInteger BIGINTEGER_0x007F  = BigInteger.valueOf(0x007F);

  private static final byte[] COOKIE = { 36, 69, 88, 73 }; // "$", "E", "X", "I"

  protected final DatatypeFactory m_datatypeFactory;

  protected boolean m_preserveNS;
  int valueMaxExclusiveLength;
  
  private final ValueScriber[] m_valueScribers;

  protected final ValueScriber[] m_valueScriberTable; // codec id -> valueScriber

  protected static final BinaryValueScriber m_base64BinaryValueScriberInherent;
  protected static final ValueScriber m_booleanValueScriberInherent;
  protected static final ValueScriber m_floatValueScriberInherent;
  protected static final ValueScriber m_integerValueScriberInherent;
  protected static final BinaryValueScriber m_hexBinaryValueScriberInherent;
  protected static final ValueScriber m_decimalValueScriberInherent;
  protected static final DateTimeValueScriber m_dateTimeValueScriberInherent;
  protected static final DateValueScriber m_dateValueScriberInherent;
  protected static final TimeValueScriber m_timeValueScriberInherent;
  protected static final GYearMonthValueScriber m_gYearMonthValueScriberInherent;
  protected static final GMonthDayValueScriber m_gMonthDayValueScriberInherent;
  protected static final GYearValueScriber m_gYearValueScriberInherent;
  protected static final GMonthValueScriber m_gMonthValueScriberInherent;
  protected static final GDayValueScriber m_gDayValueScriberInherent;
  static {
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

  protected final StringValueScriber m_stringValueScriberInherent;
  protected final ValueScriber m_enumerationValueScriberInherent;
  protected final ValueScriber m_listValueScriberInherent;
  
  protected final ValueScriber m_stringValueScriberLexical;
  protected final ValueScriber m_booleanValueScriberLexical;
  protected final ValueScriber m_enumerationValueScriberLexical;
  protected final ValueScriber m_listValueScriberLexical;
  protected final ValueScriber m_decimalValueScriberLexical;
  protected final ValueScriber m_dateTimeValueScriberLexical;
  protected final ValueScriber m_timeValueScriberLexical;
  protected final ValueScriber m_dateValueScriberLexical;
  protected final ValueScriber m_gDayValueScriberLexical;
  protected final ValueScriber m_gMonthValueScriberLexical;
  protected final ValueScriber m_gMonthDayValueScriberLexical;
  protected final ValueScriber m_gYearValueScriberLexical;
  protected final ValueScriber m_gYearMonthValueScriberLexical;
  protected final ValueScriber m_floatValueScriberLexical;
  protected final ValueScriber m_integerValueScriberLexical;
  protected final ValueScriber m_base64BinaryValueScriberLexical;
  protected final ValueScriber m_hexBinaryValueScriberLexical;

  protected CharacterBuffer m_characterBuffer;
  
  // Used by writeLiteralString method to temporarily store UCS characters
  private int[] m_ucsBuffer;
  // Used by some of the ValueScribers to temporarily store digits
  final StringBuilder stringBuilder1, stringBuilder2;
  final Scribble scribble1;
  
  protected OutputStream m_outputStream;

  /**
   * Creates a string table for use with a scriber. 
   * @param schema a schema that contains initial entries of the string table
   * @return a string table for use with a scriber
   */
  public static StringTable createStringTable(GrammarCache grammarCache) {
    return new StringTable(grammarCache, StringTable.Usage.encoding);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Constructor
  ///////////////////////////////////////////////////////////////////////////

  protected Scriber(boolean isForEXIOptions) {
  	super();
    m_preserveNS = false;
    valueMaxExclusiveLength = Integer.MAX_VALUE;
    m_preserveLexicalValues = false;
    DatatypeFactory datatypeFactory = null;
    try {
      datatypeFactory = DatatypeFactory.newInstance();
    }
    catch(DatatypeConfigurationException dce) {
      throw new RuntimeException(dce);
    }
    m_datatypeFactory = datatypeFactory;
    
    final ArrayList<ValueScriber> valueScribers = new ArrayList<ValueScriber>();
    valueScribers.add(m_stringValueScriberInherent = new StringValueScriber());
    valueScribers.add(m_booleanValueScriberInherent);
    valueScribers.add(m_decimalValueScriberInherent);
    valueScribers.add(m_floatValueScriberInherent);
    valueScribers.add(m_integerValueScriberInherent);
    valueScribers.add(m_base64BinaryValueScriberInherent);
    valueScribers.add(m_hexBinaryValueScriberInherent);
    if (!isForEXIOptions) {
      m_stringValueScriberLexical = new ValueScriberLexical(m_stringValueScriberInherent, m_stringValueScriberInherent);
      m_booleanValueScriberLexical = new ValueScriberLexical(m_booleanValueScriberInherent, m_stringValueScriberInherent);
      m_integerValueScriberLexical = new ValueScriberLexical(m_integerValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_enumerationValueScriberInherent = new EnumerationValueScriber(m_datatypeFactory));
      m_enumerationValueScriberLexical = new ValueScriberLexical(m_enumerationValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_listValueScriberInherent = new ListValueScriber());
      m_listValueScriberLexical = new ValueScriberLexical(m_listValueScriberInherent, m_stringValueScriberInherent);
      m_decimalValueScriberLexical = new ValueScriberLexical(m_decimalValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_dateTimeValueScriberInherent);
      m_dateTimeValueScriberLexical = new ValueScriberLexical(m_dateTimeValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_timeValueScriberInherent);
      m_timeValueScriberLexical = new ValueScriberLexical(m_timeValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_dateValueScriberInherent);
      m_dateValueScriberLexical = new ValueScriberLexical(m_dateValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_gDayValueScriberInherent);
      m_gDayValueScriberLexical = new ValueScriberLexical(m_gDayValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_gMonthValueScriberInherent);
      m_gMonthValueScriberLexical = new ValueScriberLexical(m_gMonthValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_gMonthDayValueScriberInherent);
      m_gMonthDayValueScriberLexical = new ValueScriberLexical(m_gMonthDayValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_gYearValueScriberInherent);
      m_gYearValueScriberLexical = new ValueScriberLexical(m_gYearValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_gYearMonthValueScriberInherent);
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
    
    m_valueScribers = new ValueScriber[valueScribers.size()];
    for (int i = 0; i < m_valueScribers.length; i++) {
      m_valueScribers[i] = valueScribers.get(i);
    }
    
    m_valueScriberTable = new ValueScriber[N_CODECS];
    
    m_characterBuffer = new CharacterBuffer(false);
    m_ucsBuffer = new int[1024];
    stringBuilder1 = new StringBuilder();
    stringBuilder2 = new StringBuilder();
    scribble1 = new Scribble();
    
    m_outputStream = null;
  }
  
  protected final CharacterBuffer ensureCharacters(final int length) {
    CharacterBuffer characterBuffer = m_characterBuffer;
    final int availability;
    if ((availability = m_characterBuffer.availability()) < length) {
      final int bufSize = length > CharacterBuffer.BUFSIZE_DEFAULT ? length : CharacterBuffer.BUFSIZE_DEFAULT;
      characterBuffer = new CharacterBuffer(bufSize, false);
    }
    if (characterBuffer != m_characterBuffer) {
      final int _availability = characterBuffer.availability();
      if (_availability != 0 && availability < _availability) {
        m_characterBuffer = characterBuffer;
      }
    }
    return characterBuffer;
  }

  public static void writeHeaderPreamble(OutputStream ostream, boolean outputCookie, boolean outputOptions) throws IOException {
    if (outputCookie)
      ostream.write(COOKIE);
    // write 10 1 00000 if outputOptions is true, otherwise write 10 0 00000 
    ostream.write(outputOptions ? 160 : 128);
  }

  @Override
  protected final ValueApparatus[] getValueApparatuses() {
    return m_valueScribers;
  }

  /**
   * Set an output stream to which encoded streams are written out.
   * @param dataStream output stream
   */
  public abstract void setOutputStream(OutputStream dataStream);
  
  public final void setValueMaxLength(int valueMaxLength) {
    valueMaxExclusiveLength = valueMaxLength == EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED ? 
        Integer.MAX_VALUE : valueMaxLength + 1;
  }

  public final void setPreserveNS(boolean preserveNS) {
    m_preserveNS = preserveNS;
  }
  
  public abstract void setBlockSize(int blockSize);
  
  ///////////////////////////////////////////////////////////////////////////
  /// Methods for controlling Deflater parameters
  ///////////////////////////////////////////////////////////////////////////

  public void setDeflateParams(int level, int strategy) {
    // Do nothing.
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Value Scriber Functions
  ///////////////////////////////////////////////////////////////////////////

  public final ValueScriber getValueScriberByID(short valueScriberID) {
    return m_valueScriberTable[valueScriberID];
  }
  
  public final ValueScriber getValueScriber(int stype) {
    assert stype != EXISchema.NIL_NODE;
    final int serial = schema.getSerialOfType(stype);
    return m_valueScriberTable[m_codecTable[serial]];
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Structure Scriber Functions
  ///////////////////////////////////////////////////////////////////////////
  
  public abstract void writeEventType(EventType eventType) throws IOException;

  public abstract void writeNS(String uri, String prefix, boolean localElementNs) throws IOException; 

  public abstract void writeQName(QName qName, EventType eventType) throws IOException;
  
  /**
   * Write a name content item.
   * Name content items are used in PI, DT, ER. 
   */
  public final void writeName(String name) throws IOException {
    writeLiteralString(name, 0, m_outputStream);
  }

  /**
   * Write a "public" content item.
   * "Public" content items are used in DT. 
   */
  public final void writePublic(String publicId) throws IOException {
    writeLiteralString(publicId, 0, m_outputStream);
  }

  /**
   * Write a "system" content item.
   * "System" content items are used in DT. 
   */
  public final void writeSystem(String systemId) throws IOException {
    writeLiteralString(systemId, 0, m_outputStream);
  }

  /**
   * Write a text content item. 
   * Text content items are used in CM, PI, DT.
   */
  public final void writeText(String text) throws IOException {
    writeLiteralString(text, 0, m_outputStream);
  }
  
  /**
   * Write xsi:type attribute value
   */
  public final void writeXsiTypeValue(QName qName) throws IOException {
    final OutputStream outputStream = m_outputStream;
    if (m_preserveLexicalValues) {
      m_valueScriberTable[CODEC_STRING].scribe(qName.qName, (Scribble)null, EXISchemaConst.XSI_LOCALNAME_TYPE_ID,  
          XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID, EXISchema.NIL_NODE, outputStream, this);
    }
    else {
      String typeNamespaceName = qName.namespaceName;
      final boolean isResolved;
      if (!(isResolved = typeNamespaceName != null))
        typeNamespaceName = "";
      final StringTable.LocalNamePartition localNamePartition;
      final int uriId = writeURI(typeNamespaceName, outputStream);
      localNamePartition = stringTable.getLocalNamePartition(uriId);
      writeLocalName(isResolved ? qName.localName : qName.qName, localNamePartition, outputStream);
      if (m_preserveNS) {
        writePrefixOfQName(qName.prefix, uriId, outputStream);
      }
    }
  }

  /**
   * Write a value of xsi:nil attribute that matched AT(xsi:nil) event type  
   */
  public final void writeXsiNilValue(boolean val, String stringValue) throws IOException {
    final OutputStream outputStream = m_outputStream;
    if (m_preserveLexicalValues) {
      m_valueScriberTable[CODEC_STRING].scribe(stringValue, (Scribble)null, EXISchemaConst.XSI_LOCALNAME_NIL_ID,  
          XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI_ID,  
          EXISchema.NIL_NODE, outputStream, this);
    }
    else
      writeBoolean(val, outputStream);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Other Functions
  ///////////////////////////////////////////////////////////////////////////

  public abstract void finish() throws IOException;
  
  protected abstract void writeUnsignedInteger32(int uint, OutputStream ostream) throws IOException;
  protected abstract void writeUnsignedInteger64(long ulong, OutputStream ostream) throws IOException;
  protected abstract void writeUnsignedInteger(BigInteger uint, OutputStream ostream) throws IOException;

  protected final void writeLiteralCharacters(Characters str, final int length, int lengthOffset, 
    int simpleType, OutputStream ostream) throws IOException {
    final int n_chars, escapeIndex, startIndex, width;
    final int[] rcs;  
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
          assert false;
          width = -1;
          rcs = null;
          break;
      }
      escapeIndex = n_chars = rcs.length;
    }
    else { // simpleType == EXISchema.NIL_NODE
      n_chars = startIndex = width = escapeIndex = -1;
      rcs = null;
    }
    final int n_ucsCount = str.ucsCount;
    writeUnsignedInteger32(lengthOffset + n_ucsCount, ostream);
    final char[] characters = str.characters;
    final int charactersIndex = str.startIndex;
    iloop:
    for (int i = 0; i < length; i++) {
      final int c = characters[charactersIndex + i];
      if (width > 0) {
        int min = startIndex;
        int max = escapeIndex - 1;
        do {
          final int watershed = (min + max) / 2;
          final int watershedValue = rcs[watershed];
          if (c == watershedValue) {
            writeNBitUnsigned(watershed - startIndex, width, ostream);
            continue iloop;
          }
          if (c < watershedValue)
            max = watershed - 1;
          else // watershedValue < c
            min = watershed + 1;
        }
        while (min <= max);
        // the character did not match any of the RCS chars.
        writeNBitUnsigned(escapeIndex - startIndex, width, ostream);
      }
      final int ucs;
      if ((c & 0xFC00) != 0xD800) 
        ucs = c;
      else { // high surrogate
        final char c2 = characters[charactersIndex + ++i];
        if ((c2 & 0xFC00) == 0xDC00) { // low surrogate
          ucs = (((c & 0x3FF) << 10) | (c2 & 0x3FF)) + 0x10000; 
        }
        else {
          --i;
          ucs = c;
        }
      }
      writeUnsignedInteger32(ucs, ostream);
    }
  }

  protected final int writeURI(String uri, OutputStream structureChannelStream) throws IOException {
    final int n_uris, width, uriId;
    n_uris = stringTable.n_uris;
    width = stringTable.uriForwardedWidth;
    if ((uriId = stringTable.internURI(uri)) < n_uris)
      writeNBitUnsigned(uriId + 1, width, structureChannelStream);
    else {
      writeNBitUnsigned(0, width, structureChannelStream);
      writeLiteralString(uri, 0, structureChannelStream);
    }
    return uriId;
  }

  /**
   * Write out a local name.
   * @return localName ID
   */
  protected final int writeLocalName(String localName, StringTable.LocalNamePartition partition, OutputStream structureChannelStream) throws IOException {
    final int n_names, width, id;
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
  
  private void writeLiteralString(String str, int lengthOffset, OutputStream structureChannelStream) throws IOException {
    final int length = str.length();
    if (length > m_ucsBuffer.length) {
      m_ucsBuffer = new int[length + 256];
    }
    int ucsCount = 0;
    for (int i = 0; i < length; ++ucsCount) {
      final char c = str.charAt(i++);
      int ucs = c;
      if ((c & 0xFC00) == 0xD800) { // high surrogate
        if (i < length) {
          final char c2 = str.charAt(i);
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
  
  protected final void writePrefixOfQName(String prefix, int uriId, OutputStream structureChannelStream) throws IOException {
    assert m_preserveNS;
    if (prefix != null) {
      final StringTable.PrefixPartition prefixPartition;
      prefixPartition = stringTable.getPrefixPartition(uriId);
      int id;
      if ((id = prefixPartition.getCompactId(prefix)) == -1)
        id = 0;
      writeNBitUnsigned(id, prefixPartition.width, structureChannelStream);
    }
    else
      throw new ScriberRuntimeException(ScriberRuntimeException.PREFIX_IS_NULL);
  }

  protected abstract void writeNBitUnsigned(int val, int width, OutputStream ostream) throws IOException;
  
  protected abstract void writeBoolean(boolean val, OutputStream ostream) throws IOException;

}
