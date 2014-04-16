package org.openexi.proc.io;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.schema.EXISchema;

public abstract class Scriber extends Apparatus {

  static final BigInteger BIGINTEGER_0x007F  = BigInteger.valueOf(0x007F);

  private static final byte[] COOKIE = { 36, 69, 88, 73 }; // "$", "E", "X", "I"

  protected final DatatypeFactory m_datatypeFactory;

  protected StringTable m_stringTable;
  
  protected boolean m_preserveNS;
  protected boolean m_preserveLexicalValues;
  
  private final ValueScriber[] m_valueScribers;

  protected final ValueScriber[] m_valueScriberTable; // codec id -> valueScriber

  protected final StringValueScriber m_stringValueScriberInherent;
  protected final ValueScriber m_booleanValueScriberInherent;
  protected final ValueScriber m_enumerationValueScriberInherent;
  protected final ValueScriber m_listValueScriberInherent;
  protected final ValueScriber m_decimalValueScriberInherent;
  protected final ValueScriber m_dateTimeValueScriberInherent;
  protected final ValueScriber m_timeValueScriberInherent;
  protected final ValueScriber m_dateValueScriberInherent;
  protected final ValueScriber m_gDayValueScriberInherent;
  protected final ValueScriber m_gMonthValueScriberInherent;
  protected final ValueScriber m_gMonthDayValueScriberInherent;
  protected final ValueScriber m_gYearValueScriberInherent;
  protected final ValueScriber m_gYearMonthValueScriberInherent;
  protected final ValueScriber m_floatValueScriberInherent;
  protected final ValueScriber m_integerValueScriberInherent;
  protected final ValueScriber m_base64BinaryValueScriberInherent;
  protected final ValueScriber m_hexBinaryValueScriberInherent;
  
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

  protected Scriber(boolean isForEXIOptions) {
    m_preserveNS = false;
    m_preserveLexicalValues = false;
    DatatypeFactory datatypeFactory = null;
    try {
      datatypeFactory = DatatypeFactory.newInstance();
    }
    catch(DatatypeConfigurationException dce) {
      throw new RuntimeException(dce);
    }
    m_datatypeFactory = datatypeFactory;
    
    final ArrayList<ValueScriber> valueScribers = new ArrayList();
    valueScribers.add(m_stringValueScriberInherent = new StringValueScriber(this));
    valueScribers.add(m_booleanValueScriberInherent = new BooleanValueScriber(this));
    valueScribers.add(m_integerValueScriberInherent = new IntegerValueScriber(this));
    if (!isForEXIOptions) {
      m_stringValueScriberLexical = new ValueScriberLexical(m_stringValueScriberInherent, m_stringValueScriberInherent);
      m_booleanValueScriberLexical = new ValueScriberLexical(m_booleanValueScriberInherent, m_stringValueScriberInherent);
      m_integerValueScriberLexical = new ValueScriberLexical(m_integerValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_enumerationValueScriberInherent = new EnumerationValueScriber(this, m_datatypeFactory));
      m_enumerationValueScriberLexical = new ValueScriberLexical(m_enumerationValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_listValueScriberInherent = new ListValueScriber(this));
      m_listValueScriberLexical = new ValueScriberLexical(m_listValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_decimalValueScriberInherent = new DecimalValueScriber(this));
      m_decimalValueScriberLexical = new ValueScriberLexical(m_decimalValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_dateTimeValueScriberInherent = new DateTimeValueScriber(this, m_datatypeFactory));
      m_dateTimeValueScriberLexical = new ValueScriberLexical(m_dateTimeValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_timeValueScriberInherent = new TimeValueScriber(this, m_datatypeFactory));
      m_timeValueScriberLexical = new ValueScriberLexical(m_timeValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_dateValueScriberInherent = new DateValueScriber(this, m_datatypeFactory));
      m_dateValueScriberLexical = new ValueScriberLexical(m_dateValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_gDayValueScriberInherent = new GDayValueScriber(this, m_datatypeFactory));
      m_gDayValueScriberLexical = new ValueScriberLexical(m_gDayValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_gMonthValueScriberInherent = new GMonthValueScriber(this, m_datatypeFactory));
      m_gMonthValueScriberLexical = new ValueScriberLexical(m_gMonthValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_gMonthDayValueScriberInherent = new GMonthDayValueScriber(this, m_datatypeFactory));
      m_gMonthDayValueScriberLexical = new ValueScriberLexical(m_gMonthDayValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_gYearValueScriberInherent = new GYearValueScriber(this, m_datatypeFactory));
      m_gYearValueScriberLexical = new ValueScriberLexical(m_gYearValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_gYearMonthValueScriberInherent = new GYearMonthValueScriber(this, m_datatypeFactory));
      m_gYearMonthValueScriberLexical = new ValueScriberLexical(m_gYearMonthValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_floatValueScriberInherent = new FloatValueScriber(this));
      m_floatValueScriberLexical = new ValueScriberLexical(m_floatValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_base64BinaryValueScriberInherent = new Base64BinaryValueScriber(this));
      m_base64BinaryValueScriberLexical = new ValueScriberLexical(m_base64BinaryValueScriberInherent, m_stringValueScriberInherent);
      valueScribers.add(m_hexBinaryValueScriberInherent = new HexBinaryValueScriber(this));
      m_hexBinaryValueScriberLexical = new ValueScriberLexical(m_hexBinaryValueScriberInherent, m_stringValueScriberInherent);
    }
    else {
      m_stringValueScriberLexical = null;
      m_booleanValueScriberLexical = null;
      m_integerValueScriberLexical = null;
      m_enumerationValueScriberInherent = m_enumerationValueScriberLexical = null;
      m_listValueScriberInherent = m_listValueScriberLexical = null;
      m_decimalValueScriberInherent = m_decimalValueScriberLexical = null;
      m_dateTimeValueScriberInherent = m_dateTimeValueScriberLexical = null;
      m_timeValueScriberInherent = m_timeValueScriberLexical = null;
      m_dateValueScriberInherent = m_dateValueScriberLexical = null;
      m_gDayValueScriberInherent = m_gDayValueScriberLexical = null;
      m_gMonthValueScriberInherent = m_gMonthValueScriberLexical = null;
      m_gMonthDayValueScriberInherent = m_gMonthDayValueScriberLexical = null;
      m_gYearValueScriberInherent = m_gYearValueScriberLexical = null;
      m_gYearMonthValueScriberInherent = m_gYearMonthValueScriberLexical = null;
      m_floatValueScriberInherent = m_floatValueScriberLexical = null;
      m_base64BinaryValueScriberInherent = m_base64BinaryValueScriberLexical = null;
      m_hexBinaryValueScriberInherent = m_hexBinaryValueScriberLexical = null;
    }
    
    m_valueScribers = new ValueScriber[valueScribers.size()];
    for (int i = 0; i < m_valueScribers.length; i++) {
      m_valueScribers[i] = valueScribers.get(i);
    }
    
    m_valueScriberTable = new ValueScriber[N_CODECS];
  }

  @Override
  public void reset() {
    super.reset();
    m_stringTable.clear();
  }

  public static void writeHeaderPreamble(OutputStream ostream, boolean outputCookie, boolean outputOptions) throws IOException {
    if (outputCookie)
      ostream.write(COOKIE);
    // write 10 1 00000 if outputOptions is true, otherwise write 10 0 00000 
    ostream.write(outputOptions ? 160 : 128);
  }

  public abstract AlignmentType getAlignmentType();
  
  @Override
  final ValueApparatus[] getValueApparatuses() {
    return m_valueScribers;
  }

  public final StringTable getStringTable() {
    return m_stringTable;
  }
  
  /**
   * Set an output stream to which encoded streams are written out.
   * @param dataStream output stream
   */
  public abstract void setOutputStream(OutputStream dataStream);
  
  @Override
  public final void setSchema(EXISchema schema, QName[] dtrm, int n_dtrmItems) {
    super.setSchema(schema, dtrm, n_dtrmItems);
    final int n_valueScribers = m_valueScribers.length; 
    for (int i = 0; i < n_valueScribers; i++) {
      m_valueScribers[i].setEXISchema(schema);
    }
  }

  public final void setStringTable(StringTable stringTable) {
    m_stringTable = stringTable;
    final int len = m_valueScribers.length; 
    for (int i = 0; i < len; i++) {
      final ValueScriber valueScriber = m_valueScribers[i]; 
      valueScriber.setStringTable(stringTable);
    }
  }
  
  public final void setValueMaxLength(int valueMaxLength) {
    if (valueMaxLength == EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED)
      valueMaxLength = Integer.MAX_VALUE;
    final int len = m_valueScribers.length; 
    for (int i = 0; i < len; i++) {
      final ValueScriber valueScriber = m_valueScribers[i]; 
      valueScriber.setValueMaxLength(valueMaxLength);
    }
  }

  public final void setPreserveNS(boolean preserveNS) {
    m_preserveNS = preserveNS;
  }
  
  public void setPreserveLexicalValues(boolean preserveLexicalValues) {
    m_preserveLexicalValues = preserveLexicalValues;
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
    final int serial = m_schema.getSerialOfType(stype);
    return m_valueScriberTable[m_codecTable[serial]];
  }
  
  public abstract OutputStream getStructureChannelStream();

  ///////////////////////////////////////////////////////////////////////////
  /// Structure Scriber Functions
  ///////////////////////////////////////////////////////////////////////////
  
  public abstract void writeEventType(EventType eventType) throws IOException;

  public abstract void writeNS(String uri, String prefix, boolean localElementNs) throws IOException; 

  public abstract void writeQName(QName qName, byte itemType) throws IOException;
  
  /**
   * Write a text (or a name) content item. 
   */
  public final void writeText(String str) throws IOException {
    final int length = str.length();
    final CharacterBuffer characterBuffer = ensureCharacters(length);
    final CharacterSequence characterSequence = characterBuffer.addString(str, length);
    writeLiteralString(characterSequence, length, 0, EXISchema.NIL_NODE, getStructureChannelStream());
  }

  /**
   * Write xsi:type attribute value
   */
  public final void writeXsiTypeValue(QName qName) throws IOException {
    final OutputStream structureChannelStream = getStructureChannelStream();
    if (m_preserveLexicalValues) {
      m_valueScriberTable[CODEC_STRING].scribe(qName.qName, (Scribble)null, "type", XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, EXISchema.NIL_NODE, getStructureChannelStream());
    }
    else {
      String typeNamespaceName = qName.namespaceName;
      final boolean isResolved;
      if (!(isResolved = typeNamespaceName != null))
        typeNamespaceName = "";
      writeURI(typeNamespaceName, structureChannelStream);
      if (isResolved)
        writeLocalName(qName.localName, typeNamespaceName, structureChannelStream);
      else
        writeLocalName(qName.qName, "", structureChannelStream);
      if (m_preserveNS) {
        writePrefixOfQName(qName.prefix, typeNamespaceName, structureChannelStream);
      }
    }
  }

  /**
   * Write a value of xsi:nil attribute that matched AT(xsi:nil) event type  
   */
  public final void writeXsiNilValue(boolean val, String stringValue) throws IOException {
    final OutputStream structureChannelStream = getStructureChannelStream();
    if (m_preserveLexicalValues) {
      m_valueScriberTable[CODEC_STRING].scribe(stringValue, (Scribble)null, "nil", XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, 
          EXISchema.NIL_NODE, getStructureChannelStream());
    }
    else
      writeBoolean(val, structureChannelStream);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Other Functions
  ///////////////////////////////////////////////////////////////////////////

  protected abstract OutputStream getOutputStream();
  
  public abstract void finish() throws IOException;
  
  protected abstract void writeUnsignedInteger32(int uint, OutputStream ostream) throws IOException;
  protected abstract void writeUnsignedInteger64(long ulong, OutputStream ostream) throws IOException;
  protected abstract void writeUnsignedInteger(BigInteger uint, OutputStream ostream) throws IOException;
  
  protected final void writeLiteralString(CharacterSequence str, final int length, int lengthOffset, 
    int simpleType, OutputStream ostream) throws IOException {
    final int n_chars, escapeIndex, startIndex, width;
    final int[] rcs;  
    if (simpleType > 0) {
      n_chars = m_schema.getRestrictedCharacterCountOfSimpleType(simpleType);
      if (n_chars != 0) {
        rcs = m_schema.getNodes();
        startIndex = m_schema.getRestrictedCharacterOfSimpleType(simpleType);
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
    final int n_ucsCount = str.getUCSCount();
    writeUnsignedInteger32(lengthOffset + n_ucsCount, ostream);
    final char[] characters = str.getCharacters();
    final int charactersIndex = str.getStartIndex();
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

  protected final void writeURI(String uri, OutputStream structureChannelStream) throws IOException {
    final StringTable.URIPartition uriPartition = m_stringTable.getURIPartition();
    final int n_uris, width, id;
    n_uris = uriPartition.n_strings;
    width = uriPartition.forwardedWidth;
    if ((id = uriPartition.internString(uri)) < n_uris)
      writeNBitUnsigned(id + 1, width, structureChannelStream);
    else {
      writeNBitUnsigned(0, width, structureChannelStream);
      final int length = uri.length();
      final CharacterBuffer characterBuffer = ensureCharacters(length);
      final CharacterSequence characterSequence = characterBuffer.addString(uri, length);
      writeLiteralString(characterSequence, length, 0, EXISchema.NIL_NODE, structureChannelStream);
    }
  }

  protected final void writeLocalName(String localName, String uri, OutputStream structureChannelStream) throws IOException {
    final StringTable.LocalNamePartition partition;
    partition = m_stringTable.getLocalNamePartition(uri);
    final int n_names, width, id;
    n_names = partition.n_strings;
    width = partition.width;
    if ((id = partition.internString(localName)) < n_names) {
      writeUnsignedInteger32(0, structureChannelStream);
      writeNBitUnsigned(id, width, structureChannelStream);
    }
    else {
      final int length = localName.length();
      final CharacterBuffer characterBuffer = ensureCharacters(length);
      final CharacterSequence characterSequence = characterBuffer.addString(localName, length);
      writeLiteralString(characterSequence, length, 1, EXISchema.NIL_NODE, structureChannelStream);
    }
  }
  

  protected final void writePrefixOfQName(String prefix, String uri, OutputStream structureChannelStream) throws IOException {
    assert m_preserveNS;
    StringTable.PrefixPartition prefixPartition;
    prefixPartition = m_stringTable.getPrefixPartition(uri);
    final int width, id;
    width = prefixPartition.width;
    id = prefixPartition.getCompactId(prefix);
    writeNBitUnsigned(id != -1 ? id : 0, width, structureChannelStream);
  }

  protected abstract void writeNBitUnsigned(int val, int width, OutputStream ostream) throws IOException;
  
  protected abstract void writeBoolean(boolean val, OutputStream ostream) throws IOException;

}
