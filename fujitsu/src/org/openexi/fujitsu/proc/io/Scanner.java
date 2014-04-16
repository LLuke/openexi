package org.openexi.fujitsu.proc.io;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.openexi.fujitsu.proc.common.AlignmentType;
import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EXIEvent;
import org.openexi.fujitsu.proc.common.EXIOptions;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.GrammarOptions;
import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.proc.events.EXIEventNS;
import org.openexi.fujitsu.proc.events.EXIEventSchemaNil;
import org.openexi.fujitsu.proc.events.EXIEventSchemaType;
import org.openexi.fujitsu.proc.grammars.EventCodeTuple;
import org.openexi.fujitsu.proc.grammars.Grammar;
import org.openexi.fujitsu.proc.grammars.GrammarState;
import org.openexi.fujitsu.proc.grammars.DocumentGrammarState;
import org.openexi.fujitsu.proc.util.Base64;
import org.openexi.fujitsu.proc.util.HexBin;
import org.openexi.fujitsu.proc.util.URIConst;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaConst;
import org.openexi.fujitsu.schema.XSDateTime;

public abstract class Scanner extends Apparatus {

  private StringTable m_stringTable;

  // m_grammarOptions and m_preserveNS need to change together.
  private short m_grammarOptions;
  protected boolean m_preserveNS;
  
  protected boolean m_preserveLexicalValues;

  protected DocumentGrammarState m_documentGrammarState;
  protected final QName qname;

  protected XMLLocusItem[] m_locusStack;
  protected int m_locusLastDepth;

  private PrefixUriBindings m_prefixUriBindingsDefault;
  protected PrefixUriBindings m_prefixUriBindings; // current bindings

  private final ValueApparatus[] m_valueScanners;
                 
  protected final ValueScanner[] m_valueScannerTable; // codec id -> valueScanner

  protected final StringValueScanner m_stringValueScannerInherent;
  protected final ValueScanner m_booleanValueScannerInherent;
  protected final ValueScanner m_enumerationValueScannerInherent;
  protected final ValueScanner m_listValueScannerInherent;
  protected final ValueScanner m_decimalValueScannerInherent;
  protected final ValueScanner m_dateTimeValueScannerInherent;
  protected final ValueScanner m_floatValueScannerInherent;
  protected final ValueScanner m_integerValueScannerInherent;
  protected final ValueScanner m_base64BinaryValueScannerInherent;
  protected final ValueScanner m_hexBinaryValueScannerInherent;

  protected final ValueScanner m_stringValueScannerLexical;
  protected final ValueScanner m_booleanValueScannerLexical;
  protected final ValueScanner m_enumerationValueScannerLexical;
  protected final ValueScanner m_listValueScannerLexical;
  protected final ValueScanner m_decimalValueScannerLexical;
  protected final ValueScanner m_dateTimeValueScannerLexical;
  protected final ValueScanner m_floatValueScannerLexical;
  protected final ValueScanner m_integerValueScannerLexical;
  protected final ValueScanner m_base64BinaryValueScannerLexical;
  protected final ValueScanner m_hexBinaryValueScannerLexical;

  private EXIOptions m_exiHeaderOptions;
  
  protected InputStream m_inputStream;
  
  private static final CharacterSequence TRUE; // "true" (4)
  private static final CharacterSequence FALSE; // "false" (5)
  private static final CharacterSequence ZERO; // "0" (1)
  private static final CharacterSequence ONE; // "1" (1)
  static {
    final CharacterBuffer characterBuffer = new CharacterBuffer(11);
    TRUE = characterBuffer.addChars(new char[] { 't', 'r', 'u', 'e' });
    FALSE = characterBuffer.addChars(new char[] { 'f', 'a', 'l', 's', 'e' });
    ZERO = characterBuffer.addChars(new char[] { '0' });
    ONE = characterBuffer.addChars(new char[] { '1' });
  }
  
  protected Scanner(boolean isForEXIOptions) {
    m_grammarOptions = GrammarOptions.OPTIONS_UNUSED;
    m_preserveNS = false;
    m_preserveLexicalValues = false;
    qname = new QName();
    m_locusStack = new XMLLocusItem[32];
    for (int i = 0; i < 32; i++) {
      m_locusStack[i] = new XMLLocusItem();
    }
    m_locusLastDepth = -1;
    m_prefixUriBindingsDefault = isForEXIOptions ? null : new PrefixUriBindings();
    m_prefixUriBindings = null;
    
    final ArrayList<ValueScanner> valueScanners = new ArrayList<ValueScanner>();
    valueScanners.add(m_stringValueScannerInherent = new StringValueScanner1());
    valueScanners.add(m_booleanValueScannerInherent = new BooleanValueScanner());
    valueScanners.add(m_integerValueScannerInherent = new IntegerValueScanner());
    if (!isForEXIOptions) {
      m_stringValueScannerLexical = new ValueScannerLexical(m_stringValueScannerInherent, m_stringValueScannerInherent);
      m_booleanValueScannerLexical = new ValueScannerLexical(m_booleanValueScannerInherent, m_stringValueScannerInherent);
      m_integerValueScannerLexical = new ValueScannerLexical(m_integerValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_dateTimeValueScannerInherent = new DateTimeValueScanner());
      m_dateTimeValueScannerLexical = new ValueScannerLexical(m_dateTimeValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_base64BinaryValueScannerInherent = new Base64BinaryValueScanner());
      m_base64BinaryValueScannerLexical = new ValueScannerLexical(m_base64BinaryValueScannerInherent, m_stringValueScannerInherent);
      valueScanners.add(m_hexBinaryValueScannerInherent = new HexBinaryValueScanner());
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
      m_stringValueScannerLexical = null;
      m_booleanValueScannerLexical = null;
      m_integerValueScannerLexical = null;
      m_dateTimeValueScannerInherent = m_dateTimeValueScannerLexical = null;
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
    // REVISIT: use distinct value scanner for each datetime-ish type.
    m_valueScannerTable[CODEC_DATETIME] = m_dateTimeValueScannerInherent; 
    m_valueScannerTable[CODEC_TIME] = m_dateTimeValueScannerInherent;
    m_valueScannerTable[CODEC_DATE] = m_dateTimeValueScannerInherent;
    m_valueScannerTable[CODEC_GYEARMONTH] = m_dateTimeValueScannerInherent;
    m_valueScannerTable[CODEC_GYEAR] = m_dateTimeValueScannerInherent;
    m_valueScannerTable[CODEC_GMONTHDAY] = m_dateTimeValueScannerInherent;
    m_valueScannerTable[CODEC_GDAY] = m_dateTimeValueScannerInherent;
    m_valueScannerTable[CODEC_GMONTH] = m_dateTimeValueScannerInherent;
    m_valueScannerTable[CODEC_DECIMAL] = m_decimalValueScannerInherent;
    m_valueScannerTable[CODEC_DOUBLE] = m_floatValueScannerInherent;
    m_valueScannerTable[CODEC_INTEGER] = m_integerValueScannerInherent;
    m_valueScannerTable[CODEC_STRING] = m_stringValueScannerInherent;
    m_valueScannerTable[CODEC_LIST] = m_listValueScannerInherent;
    m_valueScannerTable[CODEC_ENUMERATION] = m_enumerationValueScannerInherent;
  }
  
  public void init(DocumentGrammarState documentGrammarState, int inflatorBufSize) {
    m_documentGrammarState = documentGrammarState;
  }
  
  private void initValueScanners(InputStream istream) {
    for (int i = 0; i < N_CODECS; i++) {
      final ValueScanner valueScanner;
      if ((valueScanner = m_valueScannerTable[i]) != null)
        valueScanner.setInputStream(istream);
    }
  }

  @Override
  public void reset() {
    super.reset();
    m_stringTable.clear();
    m_locusLastDepth = -1;
    m_prefixUriBindings = null;
    m_inputStream = null;
  }

  public abstract EXIEvent nextEvent() throws IOException;

  public abstract AlignmentType getAlignmentType();

  @Override
  final ValueApparatus[] getValueApparatuses() {
    return m_valueScanners;
  }

  public void prepare() throws IOException {
    if (m_preserveNS) {
      m_prefixUriBindings = m_prefixUriBindingsDefault;
    }
  }
  
  public void setInputStream(InputStream istream) {
    m_inputStream = istream;
    initValueScanners(istream);
  }
  
  public final void setGrammar(Grammar grammar, short grammarOptions) {
    grammar.init(m_documentGrammarState);
    m_grammarOptions = grammarOptions;
    m_preserveNS = GrammarOptions.hasNS(m_grammarOptions);
  }

  public final StringTable getStringTable() {
    return m_stringTable;
  }

  public final void setStringTable(StringTable stringTable) {
    m_stringTable = stringTable;
  }
  
  public final void setPreserveLexicalValues(boolean preserveLexicalValues) {
    final boolean prevPreserveLexicalValues = m_preserveLexicalValues;
    m_preserveLexicalValues = preserveLexicalValues;
    if (prevPreserveLexicalValues != preserveLexicalValues) {
      if (preserveLexicalValues) {
        m_valueScannerTable[CODEC_BASE64BINARY] = m_base64BinaryValueScannerLexical;
        m_valueScannerTable[CODEC_HEXBINARY] = m_hexBinaryValueScannerLexical;
        m_valueScannerTable[CODEC_BOOLEAN] = m_booleanValueScannerLexical;
        // REVISIT: use distinct value scanner for each datetime-ish type.
        m_valueScannerTable[CODEC_DATETIME] = m_dateTimeValueScannerLexical;
        m_valueScannerTable[CODEC_TIME] = m_dateTimeValueScannerLexical;
        m_valueScannerTable[CODEC_DATE] = m_dateTimeValueScannerLexical;
        m_valueScannerTable[CODEC_GYEARMONTH] = m_dateTimeValueScannerLexical;
        m_valueScannerTable[CODEC_GYEAR] = m_dateTimeValueScannerLexical;
        m_valueScannerTable[CODEC_GMONTHDAY] = m_dateTimeValueScannerLexical;
        m_valueScannerTable[CODEC_GDAY] = m_dateTimeValueScannerLexical;
        m_valueScannerTable[CODEC_GMONTH] = m_dateTimeValueScannerLexical;
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
        // REVISIT: use distinct value scanner for each datetime-ish type.
        m_valueScannerTable[CODEC_DATETIME] = m_dateTimeValueScannerInherent;
        m_valueScannerTable[CODEC_TIME] = m_dateTimeValueScannerInherent;
        m_valueScannerTable[CODEC_DATE] = m_dateTimeValueScannerInherent;
        m_valueScannerTable[CODEC_GYEARMONTH] = m_dateTimeValueScannerInherent;
        m_valueScannerTable[CODEC_GYEAR] = m_dateTimeValueScannerInherent;
        m_valueScannerTable[CODEC_GMONTHDAY] = m_dateTimeValueScannerInherent;
        m_valueScannerTable[CODEC_GDAY] = m_dateTimeValueScannerInherent;
        m_valueScannerTable[CODEC_GMONTH] = m_dateTimeValueScannerInherent;
        m_valueScannerTable[CODEC_DECIMAL] = m_decimalValueScannerInherent;
        m_valueScannerTable[CODEC_DOUBLE] = m_floatValueScannerInherent;
        m_valueScannerTable[CODEC_INTEGER] = m_integerValueScannerInherent;
        m_valueScannerTable[CODEC_STRING] = m_stringValueScannerInherent;
        m_valueScannerTable[CODEC_LIST] = m_listValueScannerInherent;
        m_valueScannerTable[CODEC_ENUMERATION] = m_enumerationValueScannerInherent;
      }
    }
  }

  public abstract void setBlockSize(int blockSize);
  
  public final void setValueMaxLength(int valueMaxLength) {
    m_stringValueScannerInherent.setValueMaxLength(
        valueMaxLength != EXIOptions.VALUE_MAX_LENGTH_UNBOUNDED ? valueMaxLength : Integer.MAX_VALUE);
  }
  
  public final void setHeaderOptions(EXIOptions headerOptions) {
    m_exiHeaderOptions = headerOptions;
  }

  public final EXIOptions getHeaderOptions() {
    return m_exiHeaderOptions;
  }

  /**
   * Returns the current grammar state if the alignment type is bit-packed or byte-alignment. 
   * @return current grammar state
   */
  public final GrammarState getGrammarState() {
    switch (getAlignmentType()) {
      case bitPacked:
      case byteAligned:
        return m_documentGrammarState.currentState;
      default:
        return null;
    }
  }

  public final boolean getPreserveNS() {
    return m_preserveNS;
  }
  
  public final boolean getPreserveLexicalValues() {
    return m_preserveLexicalValues;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Convenience Functions
  ///////////////////////////////////////////////////////////////////////////

  protected final void pushLocusItem(final String uri, final String localName) {
    if (++m_locusLastDepth == m_locusStack.length) {
      final int locusStackCapacity = m_locusLastDepth + 8;
      final XMLLocusItem[] locusStack = new XMLLocusItem[locusStackCapacity];
      System.arraycopy(m_locusStack, 0, locusStack, 0, m_locusLastDepth);
      for (int i = m_locusLastDepth; i < locusStackCapacity; i++) {
        locusStack[i] = new XMLLocusItem();
      }
      m_locusStack = locusStack;
    }
    final XMLLocusItem locusItem;
    locusItem = m_locusStack[m_locusLastDepth];
    locusItem.elementURI = uri;
    locusItem.elementLocalName = localName;
    if (m_preserveNS) {
      locusItem.prefixUriBindings = m_prefixUriBindings;
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Accessors
  ///////////////////////////////////////////////////////////////////////////

  protected final ValueScanner getValueScannerByID(short valueScannerID) {
    return m_valueScannerTable[valueScannerID];
  }
  
  public final ValueScanner getValueScanner(int stype) {
    if (stype != EXISchema.NIL_NODE) {
      final int serial = m_schema.getSerialOfType(stype);
      return m_valueScannerTable[m_codecTable[serial]];
    }
    return m_valueScannerTable[CODEC_STRING];
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Structure Scanner Functions
  ///////////////////////////////////////////////////////////////////////////

  public final EventType readEventType(EventCode eventCodeItem) throws IOException {
    while (eventCodeItem.itemType == EventCode.ITEM_TUPLE) {
      EventCodeTuple eventCodeTuple;
      eventCodeTuple = (EventCodeTuple)eventCodeItem;
      final int width;
      if ((width = eventCodeTuple.width) != 0) {
        eventCodeItem = eventCodeTuple.getItem(readNBitUnsigned(width, m_inputStream));
      }
      else {
        eventCodeItem = eventCodeTuple.getItem(0);
      }
    }
    return (EventType)eventCodeItem;
  }

  protected final EXIEventNS readNS(EventType eventType) throws IOException {
    final String uri = readURI();
    final String prefix = readPrefixOfNS(uri);
    final boolean localElementNs = readBoolean(m_inputStream);
    if (m_preserveNS) {
      m_locusStack[m_locusLastDepth].prefixUriBindings = m_prefixUriBindings =
        prefix.length() != 0 ? m_prefixUriBindings.bind(prefix, uri) : m_prefixUriBindings.bindDefault(uri);
    }
    return new EXIEventNS(prefix, uri, localElementNs, eventType);
  }
  
  public final void readQName(final QName qName, final EventType eventType) throws IOException {
    final String uri, localName;
    switch (eventType.itemType) {
      case EventCode.ITEM_SCHEMA_WC_ANY:
      case EventCode.ITEM_AT_WC_ANY_UNTYPED:
      case EventCode.ITEM_SE_WC:
      case EventCode.ITEM_SCHEMA_AT_WC_ANY:
        uri = readURI();
        localName = readLocalName(uri);
        break;
      case EventCode.ITEM_SCHEMA_WC_NS:
      case EventCode.ITEM_SCHEMA_AT_WC_NS:
        uri = eventType.getURI();
        localName = readLocalName(uri);
        break;
      case EventCode.ITEM_SCHEMA_SE:
      case EventCode.ITEM_SCHEMA_AT:
      case EventCode.ITEM_AT:        
      case EventCode.ITEM_SE:
      case EventCode.ITEM_SCHEMA_TYPE:        
      case EventCode.ITEM_SCHEMA_NIL:     
      case EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE:
        uri = eventType.getURI();
        localName = eventType.getName();
        break;
      default:
        uri = localName = null;
        assert false;
    }
    qName.namespaceName = uri;
    qName.localName = localName;
    qName.prefix = m_preserveNS ? readPrefixOfQName(uri) : null;
    qName.qName = null;
  }
  
  /**
   * Read a text (or a name) content item. 
   */
  public final CharacterSequence readText() throws IOException {
    final int len = readUnsignedInteger(m_inputStream);
    return readLiteralString(len, EXISchema.NIL_NODE, m_inputStream);
  }

  /**
   * Read xsi:nil value.
   */
  public final EXIEventSchemaNil readXsiNilValue(String prefix, EventType eventType) throws IOException {
    if (m_preserveLexicalValues) {
      final CharacterSequence characterSequence;
      characterSequence = m_valueScannerTable[CODEC_STRING].scan("nil", URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, 
          EXISchema.NIL_NODE, m_inputStream);
      int length = characterSequence.length();
      final int startIndex = characterSequence.getStartIndex();
      final int fullLimit = startIndex + length - 1; 
      final char[] characters = characterSequence.getCharacters();
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
      return new EXIEventSchemaNil(nilled, (CharacterSequence)null, prefix, eventType);
    }
  }

  /**
   * Read xsi:type attribute value as QName
   */
  protected final EXIEventSchemaType readXsiTypeValue(String prefix, EventType eventType) throws IOException {
    final CharacterSequence characterSequence;
    final String typeUri, typeName, typePrefix;
    if (m_preserveLexicalValues) {
      characterSequence = m_valueScannerTable[CODEC_STRING].scan("type", URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI, 
          EXISchema.NIL_NODE, m_inputStream);
      final int i;
      if ((i = characterSequence.indexOf(':')) != -1) { // with prefix
        final int startIndex = characterSequence.getStartIndex();
        final char[] characters = characterSequence.getCharacters();
        int pos = startIndex + characterSequence.length() - 1;
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
      typeUri = readURI();
      typeName = readLocalName(typeUri);
      typePrefix = m_preserveNS ? readPrefixOfQName(typeUri) : null;
      characterSequence = null;
    }
    final int tp, ns;
    if (typeUri != null && m_schema != null && (ns = m_schema.getNamespaceOfSchema(typeUri)) != EXISchema.NIL_NODE) {
      if ((tp = m_schema.getTypeOfNamespace(ns, typeName)) != EXISchema.NIL_NODE) {
        m_documentGrammarState.xsitp(tp);
      }
    }
    else
      tp = EXISchema.NIL_NODE;
    return new EXIEventSchemaType(tp, typeUri, typeName, typePrefix, characterSequence, prefix, eventType);
  }

  protected final String readPrefixOfNS(final String uri) throws IOException {
    final StringTable.PrefixPartition partition;
    partition = m_stringTable.getPrefixPartition(uri);
    final int width, id;
    width = partition.forwardedWidth;
    final String name;
    id = readNBitUnsigned(width, m_inputStream);
    if (id != 0)
      name = partition.getString(id - 1);
    else {
      final int length = readUnsignedInteger(m_inputStream);
      name = readLiteralString(length, EXISchema.NIL_NODE, m_inputStream).makeString();
      partition.addString(name);
    }
    return name;
  }

  protected final String readURI() throws IOException {
    final StringTable.URIPartition uriPartition = m_stringTable.getURIPartition();
    final int width, id;
    width = uriPartition.forwardedWidth;
    final String uri;
    id = readNBitUnsigned(width, m_inputStream);
    if (id != 0)
      uri = uriPartition.getString(id - 1);
    else {
      final int length = readUnsignedInteger(m_inputStream);
      uri = readLiteralString(length, EXISchema.NIL_NODE, m_inputStream).makeString();
      uriPartition.addString(uri);
    }
    return uri;
  }
  
  protected final String readLocalName(final String uri) throws IOException {
    final StringTable.LocalNamePartition partition;
    partition = m_stringTable.getLocalNamePartition(uri);
    final String name;
    int length = readUnsignedInteger(m_inputStream);
    if (length != 0) {
      name = readLiteralString(length - 1, EXISchema.NIL_NODE, m_inputStream).makeString();
      partition.addString(name);
    }
    else {
      final int id = readNBitUnsigned(partition.width, m_inputStream);
      name = partition.getString(id);
    }
    return name;
  }

  protected String readPrefixOfQName(String uri) throws IOException {
    final StringTable.PrefixPartition prefixPartition;
    prefixPartition = m_stringTable.getPrefixPartition(uri);
    final int width, id;
    width = prefixPartition.width;
    return (id = readNBitUnsigned(width, m_inputStream)) < prefixPartition.n_strings ?  
        prefixPartition.getString(id) : null; 
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Crude bits Reader functions
  ///////////////////////////////////////////////////////////////////////////

  protected abstract boolean readBoolean(InputStream istream) throws IOException;
  
  protected final int readUnsignedInteger(InputStream istream) throws IOException {
    int shift = 0;
    int uint = 0;
    boolean continued = true;
    do {
      int nextByte = readNBitUnsigned(8, istream);
      if ((nextByte & 0x0080) != 0) // check continuation flag
        nextByte &= 0x007F;
      else
        continued = false;
      uint += (nextByte << shift);
      shift += 7;
    }
    while (continued);
    return uint;
  }

  /**
   */
  protected final String readUnsignedIntegerString(InputStream istream, boolean addOne) throws IOException {
    int shift = 0;
    int uint = 0;
    boolean continued = true;
    do {
      int nextByte = readNBitUnsigned(8, istream);
      if ((nextByte & 0x0080) != 0) // check continuation flag
        nextByte &= 0x007F;
      else
        continued = false;
      uint += (nextByte << shift);
      if (!continued) {
        if (addOne)
          ++uint;
        return Integer.toString(uint);
      }
      shift += 7;
    }
    while (shift != 28);
    
    final int shiftLimit = addOne ? 56 : 63;
    long ulong = uint;
    do {
      long nextByte = readNBitUnsigned(8, istream);
      if ((nextByte & 0x0080) != 0) // check continuation flag
        nextByte &= 0x007F;
      else
        continued = false;
      ulong += (nextByte << shift);
      if (!continued) {
        if (addOne)
          ++ulong;
        return Long.toString(ulong);
      }
      shift += 7;
    }
    while (shift != shiftLimit);
    
    BigInteger uinteger = BigInteger.valueOf(ulong);
    do {
      int nextByte = readNBitUnsigned(8, istream);
      if ((nextByte & 0x0080) != 0) // check continuation flag
        nextByte &= 0x007F;
      else
        continued = false;
      uinteger = uinteger.add(BigInteger.valueOf(nextByte).shiftLeft(shift));
      shift += 7;
    }
    while (continued);

    if (addOne)
      uinteger = uinteger.add(BigInteger.ONE);
    return uinteger.toString();
  }
  
  protected final CharacterSequence readLiteralString(int ucsCount, int tp, InputStream istream) throws IOException {
    final int n_chars, startIndex, width;
    final int[] rcs;  
    if (tp > 0) {
      final int primType = m_schema.getPrimitiveTypeOfAtomicSimpleType(tp);
      n_chars = primType != EXISchema.NIL_NODE && m_schema.getSerialOfType(primType) == EXISchemaConst.STRING_TYPE ?  
        m_schema.getRestrictedCharacterCountOfSimpleType(tp) : 0;
      if (n_chars != 0) {
        rcs = m_schema.getNodes();
        startIndex = m_schema.getRestrictedCharacterOfSimpleType(tp);
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
    final CharacterBuffer characterBuffer = ensureCharacters(ucsCount);
    char[] characters = characterBuffer.characters;
    int charactersIndex = characterBuffer.allocCharacters(ucsCount);
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
          characterBuffer.redeemCharacters(_ucsCount);
          foundNonBMP = true;
        }
        characters[length++] = (char)(((c - 0x10000) >> 10) | 0xD800);
        characters[length++] = (char)(((c - 0x10000) & 0x3FF) | 0xDC00);
        continue;
      }
      characters[charactersIndex + length++] = (char)c;
    }
    return new Characters(characters, charactersIndex, length);
  }
  
  protected abstract int readNBitUnsigned(int width, InputStream istream) throws IOException;

  ///////////////////////////////////////////////////////////////////////////
  /// Value Scanners
  ///////////////////////////////////////////////////////////////////////////

  public final class StringValueScanner1 extends StringValueScanner {
    @Override
    public short getCodecID() {
      return CODEC_STRING;
    }    
    private int m_valueMaxExclusiveLength;
    public void setValueMaxLength(int valueMaxLength) {
      assert valueMaxLength >= 0;
      m_valueMaxExclusiveLength = valueMaxLength != Integer.MAX_VALUE ? valueMaxLength + 1 : Integer.MAX_VALUE;
    }
    public StringValueScanner1() {
      m_valueMaxExclusiveLength = Integer.MAX_VALUE;
    }
    @Override
    public CharacterSequence scan(String localName, String uri, int tp, InputStream istream) throws IOException {
      if (istream == null)
        istream = m_istream;
      final StringTable.GlobalPartition globalPartition = m_stringTable.getGlobalPartition();
      int ucsCount = readUnsignedInteger(istream);
      if ((ucsCount & 0xFFFFFFFE) != 0) { // i.e. length > 1 
        ucsCount -= 2;
        final CharacterSequence value = readLiteralString(ucsCount, tp, istream);
        if (ucsCount != 0 && ucsCount < m_valueMaxExclusiveLength) {
          globalPartition.addString(value, localName, uri);
        }
        return value;
      }
      else {
        final int id;
        if (ucsCount == 0) {
          final StringTable.LocalPartition localPartition;
          localPartition = globalPartition.getLocalPartition(localName, uri);
          id = readNBitUnsigned(localPartition.width, istream);
          return localPartition.getString(id).value;
        }
        else { // length == 1
          id = readNBitUnsigned(globalPartition.width, istream);
          return globalPartition.getString(id);
        }
      }
    }
  }
  
  public final class BooleanValueScanner extends ValueScannerBase {
    BooleanValueScanner() {
      super(new QName("exi:boolean", URIConst.W3C_2009_EXI_URI));
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
    public CharacterSequence scan(String localName, String uri, int tp, InputStream istream) throws IOException {
      if (istream == null)
        istream = m_istream;
      if (m_schema.getRestrictedCharacterCountOfSimpleType(tp) != 0) {
        switch (readNBitUnsigned(2, istream)) {
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
        final boolean val = readBoolean(istream);
        return val ? TRUE : FALSE;
      }
    }
  }

  public final class IntegerValueScanner extends ValueScannerBase {
    IntegerValueScanner() {
      super(new QName("exi:integer", URIConst.W3C_2009_EXI_URI));
    }
    @Override
    public short getCodecID() {
      return CODEC_INTEGER;
    }
    @Override
    public int getBuiltinRCS(int simpleType) {
      return BuiltinRCS.RCS_ID_INTEGER;
    }
    @Override
    public CharacterSequence scan(String localName, String uri, int tp, InputStream istream) throws IOException {
      if (istream == null)
        istream = m_istream;
      int intValue;
      final int width;
      CharacterBuffer characterBuffer;
      String stringValue;
      final int length;
      if (m_schema.isIntegralSimpleType(tp)) {
        switch (width = m_schema.getWidthOfIntegralSimpleType(tp)) {
          case EXISchema.INTEGER_CODEC_DEFAULT:
            boolean isNegative = readBoolean(istream);
            stringValue = readUnsignedIntegerString(istream, isNegative);
            stringValue = isNegative ?  "-" + stringValue : stringValue;
            break;
          case EXISchema.INTEGER_CODEC_NONNEGATIVE:
            stringValue = readUnsignedIntegerString(istream, false);
            break;
          default:
            intValue = readNBitUnsigned(width, istream);
            final int minInclusiveFacet = m_schema.getMinInclusiveFacetOfSimpleType(tp);
            final int variantType;
            switch ((variantType = m_schema.getTypeOfVariant(minInclusiveFacet))) {
              case EXISchema.VARIANT_INT:
                final int minInclusiveIntValue = m_schema.getIntValueOfVariant(minInclusiveFacet);
                stringValue = Integer.toString(minInclusiveIntValue + intValue); 
                break;
              case EXISchema.VARIANT_LONG:
                final long minInclusiveLongValue = m_schema.getLongValueOfVariant(minInclusiveFacet); 
                stringValue = Long.toString(minInclusiveLongValue + intValue);
                break;
              default:
                assert variantType ==  EXISchema.VARIANT_INTEGER;
                final BigInteger minInclusiveIntegerValue = m_schema.getIntegerValueOfVariant(minInclusiveFacet);
                stringValue = minInclusiveIntegerValue.add(BigInteger.valueOf(intValue)).toString();
                break;
            }
        }
      }
      else {
        boolean isNegative = readBoolean(istream);
        stringValue = readUnsignedIntegerString(istream, isNegative);
        stringValue = isNegative  ?  "-" + stringValue : stringValue;
      }
      length = stringValue.length();
      characterBuffer = ensureCharacters(length);
      return characterBuffer.addString(stringValue, length);
    }
  }

  public final class EnumerationValueScanner extends ValueScannerBase {
    EnumerationValueScanner() {
      super((QName)null);
    }
    @Override
    public short getCodecID() {
      return CODEC_ENUMERATION;
    }
    @Override
    public int getBuiltinRCS(int simpleType) {
      final int baseType = m_schema.getBaseTypeOfType(simpleType);
      return getValueScanner(baseType).getBuiltinRCS(baseType);
    }
    @Override
    public CharacterSequence scan(String localName, String uri, int tp, InputStream istream) throws IOException {
      if (istream == null)
        istream = m_istream;
      int n_enums = m_schema.getEnumerationFacetCountOfSimpleType(tp);
      assert n_enums > 0;
      int width, n;
      for (width = 0, n = n_enums - 1; n != 0; n >>= 1, ++width);
      
      int index = readNBitUnsigned(width, istream);
      assert index >= 0;

      final int primTypeId = m_schema.getSerialOfType(m_schema.getPrimitiveTypeOfAtomicSimpleType(tp));
      int facet = m_schema.getEnumerationFacetOfSimpleType(tp, index);
      final String stringValue;
      final byte[] binaryValue;
      switch (primTypeId) {
        case EXISchemaConst.G_YEAR_TYPE:
        case EXISchemaConst.G_YEARMONTH_TYPE:
        case EXISchemaConst.G_MONTHDAY_TYPE:
        case EXISchemaConst.G_MONTH_TYPE:
        case EXISchemaConst.G_DAY_TYPE:
        case EXISchemaConst.TIME_TYPE:
        case EXISchemaConst.DATE_TYPE:
        case EXISchemaConst.DATETIME_TYPE:
          final XSDateTime dateTime = m_schema.getDateTimeValueOfVariant(facet);
          stringValue = dateTime.getXMLGregorianCalendar().toXMLFormat();
          break;
        case EXISchemaConst.DECIMAL_TYPE:
          BigDecimal decimal = m_schema.getDecimalValueOfVariant(facet);
          stringValue = decimal.toPlainString();
          break;
        case EXISchemaConst.FLOAT_TYPE:
          stringValue = Float.toString(m_schema.getFloatValueOfVariant(facet));
          break;
        case EXISchemaConst.DOUBLE_TYPE:
          stringValue = Double.toString(m_schema.getDoubleValueOfVariant(facet));
          break;
        case EXISchemaConst.DURATION_TYPE:
          stringValue = m_schema.getDurationValueOfVariant(facet).toString();
          break;
        case EXISchemaConst.BASE64BINARY_TYPE:
          binaryValue = m_schema.getBinaryValueOfVariant(facet);
          int maxChars = (binaryValue.length / 3) << 2;
          if (binaryValue.length % 3 != 0)
            maxChars += 4;
          maxChars += maxChars / 76;
          final CharacterBuffer characterBuffer = ensureCharacters(maxChars);
          final char[] characters = characterBuffer.characters;
          final int startIndex = characterBuffer.allocCharacters(maxChars);
          final int n_chars = Base64.encode(binaryValue, binaryValue.length, characters, startIndex);
          return new Characters(characters, startIndex, n_chars);
        case EXISchemaConst.HEXBINARY_TYPE:
          binaryValue = m_schema.getBinaryValueOfVariant(facet);
          final StringBuffer stringBuffer = new StringBuffer();
          HexBin.encode(binaryValue, stringBuffer);
          stringValue = stringBuffer.toString();
          break;
        case EXISchemaConst.BOOLEAN_TYPE:
          assert false;
          stringValue = "";
          break;
        default:
          stringValue = m_schema.getStringValueOfVariant(facet);
          break;
      }
      final int length = stringValue.length();
      final CharacterBuffer characterBuffer = ensureCharacters(length);
      return characterBuffer.addString(stringValue, length);
    }
  }

  public final class ListValueScanner extends ValueScannerBase {
    private final StringBuffer m_stringBuffer; 
    public ListValueScanner() {
      super((QName)null);
      m_stringBuffer = new StringBuffer();
    }
    @Override
    public short getCodecID() {
      return CODEC_LIST;
    }
    @Override
    public int getBuiltinRCS(int simpleType) {
      final EXISchema schema = m_schema;
      assert schema.getVarietyOfSimpleType(simpleType) == EXISchema.LIST_SIMPLE_TYPE;
      
      final int itemType = schema.getItemTypeOfListSimpleType(simpleType);
      final short codecID = m_codecTable[schema.getSerialOfType(itemType)];
      final ValueScanner itemValueScanner = m_valueScannerTable[codecID]; 
      
      return itemValueScanner.getBuiltinRCS(itemType);
    }
    @Override
    public CharacterSequence scan(String localName, String uri, int tp, InputStream istream) throws IOException {
      assert m_schema.getVarietyOfSimpleType(tp) == EXISchema.LIST_SIMPLE_TYPE;
      if (istream == null)
        istream = m_istream;
      
      final int itemType = m_schema.getItemTypeOfListSimpleType(tp);
      final short codecID = m_codecTable[m_schema.getSerialOfType(itemType)];
      final ValueScanner itemValueScanner = m_valueScannerTable[codecID];

      final int n_items = readUnsignedInteger(istream);
      m_stringBuffer.setLength(0);
      for (int i = 0; i < n_items; i++) {
        if (i != 0) { 
          m_stringBuffer.append(" ");
        }
        final String itemStringValue;
        itemStringValue = itemValueScanner.scan(localName, uri, itemType, istream).makeString();
        m_stringBuffer.append(itemStringValue);
      }
      final int length = m_stringBuffer.length();
      final CharacterBuffer characterBuffer = ensureCharacters(length);
      return characterBuffer.addString(m_stringBuffer.toString(), length);
    }
  }
  
  public final class DecimalValueScanner extends ValueScannerBase {
    DecimalValueScanner() {
      super(new QName("exi:decimal", URIConst.W3C_2009_EXI_URI));
    }
    @Override
    public short getCodecID() {
      return CODEC_DECIMAL;
    }
    @Override
    public int getBuiltinRCS(int simpleType) {
      return BuiltinRCS.RCS_ID_DECIMAL;
    }
    @Override
    public CharacterSequence scan(String localName, String uri, int tp, InputStream istream) throws IOException {
      if (istream == null)
        istream = m_istream;
      final boolean isNegative = readBoolean(istream);
      final String integralDigitsString = readUnsignedIntegerString(istream, false);
      final String fractionalDigitsString = new StringBuffer(readUnsignedIntegerString(istream, false)).reverse().toString();
      final String stringValue = isNegative ? "-" + integralDigitsString + "." + fractionalDigitsString :
        integralDigitsString + "." + fractionalDigitsString;
      final int length = stringValue.length();
      final CharacterBuffer characterBuffer = ensureCharacters(length);
      return characterBuffer.addString(stringValue, length);
    }
  }

  public final class DateTimeValueScanner extends ValueScannerBase {
    private final StringBuffer m_stringBuffer1, m_stringBuffer2;
    public DateTimeValueScanner() {
      // REVISIT: separate DateTimeValueScanner into distinct classes
      super(new QName("exi:dateTime", URIConst.W3C_2009_EXI_URI));
      m_stringBuffer1 = new StringBuffer();
      m_stringBuffer2 = new StringBuffer();
    }
    @Override
    public short getCodecID() {
      // REVISIT: separate DateTimeValueScanner into distinct classes
      return CODEC_DATETIME;
    }
    @Override
    public int getBuiltinRCS(int simpleType) {
      return BuiltinRCS.RCS_ID_DATETIME;
    }
    @Override
    public CharacterSequence scan(String localName, String uri, int tp, InputStream istream) throws IOException {
      if (istream == null)
        istream = m_istream;
      final int builtinType = m_schema.getBuiltinTypeOfAtomicSimpleType(tp);
      final int builtinId = m_schema.getSerialOfType(builtinType);
      m_stringBuffer1.setLength(0);
      String stringValue;
      final int length;
      CharacterBuffer characterBuffer;
      switch (builtinId) {
        case EXISchemaConst.DATETIME_TYPE:
          readYear(m_stringBuffer1, istream);
          readMonthDay(m_stringBuffer1, istream);
          m_stringBuffer1.append('T');
          readTime(m_stringBuffer1, istream);
          readTimeZone(m_stringBuffer1, istream);
          stringValue = m_stringBuffer1.toString(); 
          break;
        case EXISchemaConst.DATE_TYPE:
          readYear(m_stringBuffer1, istream);
          readMonthDay(m_stringBuffer1, istream);
          readTimeZone(m_stringBuffer1, istream);
          stringValue = m_stringBuffer1.toString(); 
          break;
        case EXISchemaConst.TIME_TYPE:
          readTime(m_stringBuffer1, istream);
          readTimeZone(m_stringBuffer1, istream);
          stringValue = m_stringBuffer1.toString(); 
          break;
        case EXISchemaConst.G_DAY_TYPE:
          readGDay(m_stringBuffer1, istream);
          readTimeZone(m_stringBuffer1, istream);
          stringValue = m_stringBuffer1.toString(); 
          break;
        case EXISchemaConst.G_MONTH_TYPE:
          readGMonth(m_stringBuffer1, istream);
          readTimeZone(m_stringBuffer1, istream);
          stringValue = m_stringBuffer1.toString(); 
          break;
        case EXISchemaConst.G_MONTHDAY_TYPE:
          readGMonthDay(m_stringBuffer1, istream);
          readTimeZone(m_stringBuffer1, istream);
          stringValue = m_stringBuffer1.toString(); 
          break;
        case EXISchemaConst.G_YEAR_TYPE:
          readYear(m_stringBuffer1, istream);
          readTimeZone(m_stringBuffer1, istream);
          stringValue = m_stringBuffer1.toString(); 
          break;
        case EXISchemaConst.G_YEARMONTH_TYPE:
          readGYearMonth(m_stringBuffer1, istream);
          readTimeZone(m_stringBuffer1, istream);
          stringValue = m_stringBuffer1.toString(); 
          break;
        default:
          assert false;
          return null;
      }
      length = stringValue.length();
      characterBuffer = ensureCharacters(length);
      return characterBuffer.addString(stringValue, length);
    }
    private void readYear(StringBuffer stringBuffer, InputStream istream) throws IOException {
      final boolean isNegative = readBoolean(istream);
      int year = readUnsignedInteger(istream);
      year = isNegative ? 1999 - year : year + 2000; 
      if (year < 0) {
        stringBuffer.append('-');
        year = 0 - year;
      }
      if (year < 10) {
        stringBuffer.append("000");
        stringBuffer.append(year);
      }
      else {
        if (year < 100) {
          stringBuffer.append("00");
          stringBuffer.append(year);
        }
        else {
          if (year < 1000) {
            stringBuffer.append("0");
            stringBuffer.append(year);
          }
          else {
            stringBuffer.append(year);
          }
        }
      }
    }
    private void readGDay(StringBuffer stringBuffer, InputStream istream) throws IOException {
      final int day = readNBitUnsigned(9, istream);
      stringBuffer.append("---");
      stringBuffer.append(day < 10 ? "0" + day : day);
    }
    private void readGMonth(StringBuffer stringBuffer, InputStream istream) throws IOException {
      final int intValue = readNBitUnsigned(9, istream);
      int month = intValue >>> 5;
      assert intValue % 32 == 0;
      stringBuffer.append("--");
      stringBuffer.append(month < 10 ? "0" + month : month);
    }
    private void readGMonthDay(StringBuffer stringBuffer, InputStream istream) throws IOException {
      final int intValue = readNBitUnsigned(9, istream);
      final int month = intValue >>> 5;
      final int day = intValue & 0x001F; 
      stringBuffer.append("--");
      stringBuffer.append(month < 10 ? "0" + month : month);
      stringBuffer.append('-');
      stringBuffer.append(day < 10 ? "0" + day : day);
    }
    private void readGYearMonth(StringBuffer stringBuffer, InputStream istream) throws IOException {
      readYear(stringBuffer, istream);
      stringBuffer.append('-');
      final int intValue = readNBitUnsigned(9, istream);
      final int month = intValue >>> 5;
      assert intValue % 32 == 0;
      stringBuffer.append(month < 10 ? "0" + month : month);
    }
    private void readMonthDay(StringBuffer stringBuffer, InputStream istream) throws IOException {
      final int intValue = readNBitUnsigned(9, istream);
      final int month = intValue >>> 5;
      final int day = intValue & 0x001F; 
      stringBuffer.append('-');
      stringBuffer.append(month < 10 ? "0" + month : month);
      stringBuffer.append('-');
      stringBuffer.append(day < 10 ? "0" + day : day);
    }
    private void readTime(StringBuffer stringBuffer, InputStream istream) throws IOException {
      int intValue = readNBitUnsigned(17, istream);
      int hours = intValue / 4096;
      intValue %= 4096;
      int minutes = intValue / 64;
      int seconds = intValue % 64;
      stringBuffer.append(hours < 10 ? "0" + hours : hours);
      stringBuffer.append(minutes < 10 ? ":0" : ':');
      stringBuffer.append(minutes);
      stringBuffer.append(seconds < 10 ? ":0" : ':');
      stringBuffer.append(seconds);
      if (readBoolean(istream)) {
        intValue = readUnsignedInteger(istream);
        stringBuffer.append('.');
        m_stringBuffer2.setLength(0);
        stringBuffer.append(m_stringBuffer2.append(Integer.toString(intValue)).reverse());
      }
    }
    private void readTimeZone(StringBuffer stringBuffer, InputStream istream) throws IOException {
      if (readBoolean(istream)) {
        int intValue = readNBitUnsigned(11, istream);
        if ((intValue -= 64 * 14) != 0) {
          if (intValue < 0) {
            stringBuffer.append('-');
            intValue = 0 - intValue;
          }
          else
            stringBuffer.append('+');
          final int hours = intValue / 64;
          final int minutes = intValue % 64;
          stringBuffer.append(hours < 10 ? "0" + hours : hours);
          stringBuffer.append(minutes < 10 ? ":0" : ':');
          stringBuffer.append(minutes);
        }
        else
          m_stringBuffer1.append('Z');
      }
    }
    
  }
  
  public final class FloatValueScanner extends ValueScannerBase {
    FloatValueScanner() {
      super(new QName("exi:double", URIConst.W3C_2009_EXI_URI));
    }
    @Override
    public short getCodecID() {
      return CODEC_DOUBLE;
    }
    @Override
    public int getBuiltinRCS(int simpleType) {
      return BuiltinRCS.RCS_ID_DOUBLE;
    }
    @Override
    public CharacterSequence scan(String localName, String uri, int tp, InputStream istream) throws IOException {
      if (istream == null)
        istream = m_istream;
      final boolean isNegative = readBoolean(istream);
      long longValue = readUnsignedInteger63(istream);
      if (isNegative)
        longValue = -longValue - 1;
      final String mantissaDigitsString = Long.toString(longValue); 
      final boolean isNegativeExponent = readBoolean(istream);
      int intValue = readUnsignedInteger(istream);
      if (isNegativeExponent)
        ++intValue;
      final String stringValue;
      if (16384 != intValue) {
        stringValue = mantissaDigitsString + 'E' + (isNegativeExponent ? "-" : "") +  Integer.toString(intValue); 
      }
      else {
        stringValue = longValue == 1 ? "INF" : longValue == -1 ? "-INF" : "NaN";    
      }
      CharacterBuffer characterBuffer;
      final int length = stringValue.length();
      characterBuffer = ensureCharacters(length);
      return characterBuffer.addString(stringValue, length);
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
        long nextByte = readNBitUnsigned(8, istream);
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

  public final class Base64BinaryValueScanner extends ValueScannerBase {
    protected byte[] m_octets;
    public Base64BinaryValueScanner() {
      super(new QName("exi:base64Binary", URIConst.W3C_2009_EXI_URI));
      m_octets = new byte[8192];
    }
    @Override
    public short getCodecID() {
      return CODEC_BASE64BINARY;
    }
    @Override
    public int getBuiltinRCS(int simpleType) {
      return BuiltinRCS.RCS_ID_BASE64BINARY;
    }
    @Override
    public CharacterSequence scan(String localName, String uri, int tp, InputStream istream) throws IOException {
      if (istream == null)
        istream = m_istream;
      final int len = readUnsignedInteger(istream);
      if (m_octets.length < len) {
        int _length;
        for (_length = m_octets.length << 1; _length < len; _length <<= 1);
        m_octets = new byte[_length];
      }
      for (int i = 0; i < len; i++) {
        m_octets[i] = (byte)readNBitUnsigned(8, istream);
      }
      int maxChars = (len / 3) << 2;
      if (len % 3 != 0)
        maxChars += 4;
      maxChars += maxChars / 76;
      final CharacterBuffer characterBuffer = ensureCharacters(maxChars);
      final char[] characters = characterBuffer.characters;
      final int startIndex = characterBuffer.allocCharacters(maxChars);
      int n_chars = Base64.encode(m_octets, len, characters, startIndex);
      return new Characters(characters, startIndex, n_chars);
    }
  }

  public final class HexBinaryValueScanner extends ValueScannerBase {
    private final StringBuffer m_stringBuffer;
    public HexBinaryValueScanner() {
      super(new QName("exi:hexBinary", URIConst.W3C_2009_EXI_URI));
      m_stringBuffer = new StringBuffer();
    }
    @Override
    public short getCodecID() {
      return CODEC_HEXBINARY;
    }
    @Override
    public int getBuiltinRCS(int simpleType) {
      return BuiltinRCS.RCS_ID_HEXBINARY;
    }
    @Override
    public CharacterSequence scan(String localName, String uri, int tp, InputStream istream) throws IOException {
      if (istream == null)
        istream = m_istream;
      final int len = readUnsignedInteger(istream);
      final byte[] bts = new byte[len];
      for (int i = 0; i < len; i++) {
        bts[i] = (byte)readNBitUnsigned(8, istream);
      }
      m_stringBuffer.setLength(0);
      HexBin.encode(bts, m_stringBuffer);
      final String stringValue = m_stringBuffer.toString();
      final int length = stringValue.length();
      final CharacterBuffer characterBuffer = ensureCharacters(length);
      return characterBuffer.addString(stringValue, length);
    }
  }
  
}
