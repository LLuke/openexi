package org.openexi.proc.io.compression;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.CharacterSequence;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.QName;
import org.openexi.proc.grammars.EventCodeTuple;
import org.openexi.proc.io.ByteAlignedCommons;
import org.openexi.proc.io.CharacterBuffer;
import org.openexi.proc.io.Scriber;
import org.openexi.proc.io.StringTable;
import org.openexi.proc.io.ValueScriber;
import org.openexi.schema.EXISchema;

public final class ChannellingScriber extends Scriber {

  private final boolean m_compressed;

  private OutputStream m_baseDataStream;
  private OutputStream m_structureDataStream;

  private final ValueScriber m_stringValueScriberInherentProxy;
  private final ValueScriber m_booleanValueScriberInherentProxy;
  private final ValueScriber m_enumerationValueScriberInherentProxy;
  private final ValueScriber m_listValueScriberInherentProxy;
  private final ValueScriber m_decimalValueScriberInherentProxy;
  private final ValueScriber m_dateTimeValueScriberInherentProxy;
  private final ValueScriber m_timeValueScriberInherentProxy;
  private final ValueScriber m_dateValueScriberInherentProxy;
  private final ValueScriber m_gDayValueScriberInherentProxy;
  private final ValueScriber m_gMonthValueScriberInherentProxy;
  private final ValueScriber m_gMonthDayValueScriberInherentProxy;
  private final ValueScriber m_gYearValueScriberInherentProxy;
  private final ValueScriber m_gYearMonthValueScriberInherentProxy;
  private final ValueScriber m_floatValueScriberInherentProxy;
  private final ValueScriber m_integerValueScriberInherentProxy;
  private final ValueScriber m_base64BinaryValueScriberInherentProxy;
  private final ValueScriber m_hexBinaryValueScriberInherentProxy;

  private final ValueScriber m_stringValueScriberLexicalProxy;
  private final ValueScriber m_booleanValueScriberLexicalProxy;
  private final ValueScriber m_enumerationValueScriberLexicalProxy;
  private final ValueScriber m_listValueScriberLexicalProxy;
  private final ValueScriber m_decimalValueScriberLexicalProxy;
  private final ValueScriber m_dateTimeValueScriberLexicalProxy;
  private final ValueScriber m_timeValueScriberLexicalProxy;
  private final ValueScriber m_dateValueScriberLexicalProxy;
  private final ValueScriber m_gDayValueScriberLexicalProxy;
  private final ValueScriber m_gMonthValueScriberLexicalProxy;
  private final ValueScriber m_gMonthDayValueScriberLexicalProxy;
  private final ValueScriber m_gYearValueScriberLexicalProxy;
  private final ValueScriber m_gYearMonthValueScriberLexicalProxy;
  private final ValueScriber m_floatValueScriberLexicalProxy;
  private final ValueScriber m_integerValueScriberLexicalProxy;
  private final ValueScriber m_base64BinaryValueScriberLexicalProxy;
  private final ValueScriber m_hexBinaryValueScriberLexicalProxy;

  private final ChannelKeeper m_channelKeeper;
  private final Deflater m_deflator;

  public ChannellingScriber(boolean compressed) {
    super(false);
    m_compressed = compressed;
    m_channelKeeper = new ChannelKeeper(new ScriberChannelFactory());
    m_deflator = compressed ? new Deflater(Deflater.DEFAULT_COMPRESSION, true) : null;
    
    m_stringValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_stringValueScriberInherent, this);
    m_booleanValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_booleanValueScriberInherent, this);
    m_enumerationValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_enumerationValueScriberInherent, this);
    m_listValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_listValueScriberInherent, this);
    m_decimalValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_decimalValueScriberInherent, this);
    m_dateTimeValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_dateTimeValueScriberInherent, this);
    m_timeValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_timeValueScriberInherent, this);
    m_dateValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_dateValueScriberInherent, this);
    m_gDayValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gDayValueScriberInherent, this);
    m_gMonthValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gMonthValueScriberInherent, this);
    m_gMonthDayValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gMonthDayValueScriberInherent, this);
    m_gYearValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gYearValueScriberInherent, this);
    m_gYearMonthValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gYearMonthValueScriberInherent, this);
    m_floatValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_floatValueScriberInherent, this); 
    m_integerValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_integerValueScriberInherent, this);
    m_base64BinaryValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_base64BinaryValueScriberInherent, this);
    m_hexBinaryValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_hexBinaryValueScriberInherent, this);
    
    m_stringValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_stringValueScriberLexical, this);
    m_booleanValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_booleanValueScriberLexical, this);
    m_enumerationValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_enumerationValueScriberLexical, this);
    m_listValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_listValueScriberLexical, this);
    m_decimalValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_decimalValueScriberLexical, this);
    m_dateTimeValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_dateTimeValueScriberLexical, this);
    m_timeValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_timeValueScriberLexical, this);
    m_dateValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_dateValueScriberLexical, this);
    m_gDayValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gDayValueScriberLexical, this);
    m_gMonthValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gMonthValueScriberLexical, this);
    m_gMonthDayValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gMonthDayValueScriberLexical, this);
    m_gYearValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gYearValueScriberLexical, this);
    m_gYearMonthValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gYearMonthValueScriberLexical, this);
    m_floatValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_floatValueScriberLexical, this); 
    m_integerValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_integerValueScriberLexical, this);
    m_base64BinaryValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_base64BinaryValueScriberLexical, this);
    m_hexBinaryValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_hexBinaryValueScriberLexical, this);
    
    m_valueScriberTable[CODEC_STRING] = m_stringValueScriberInherentProxy;
    m_valueScriberTable[CODEC_BOOLEAN] = m_booleanValueScriberInherentProxy;
    m_valueScriberTable[CODEC_ENUMERATION] = m_enumerationValueScriberInherentProxy;
    m_valueScriberTable[CODEC_LIST] = m_listValueScriberInherentProxy;
    m_valueScriberTable[CODEC_DECIMAL] = m_decimalValueScriberInherentProxy;
    m_valueScriberTable[CODEC_DATETIME] = m_dateTimeValueScriberInherentProxy;
    m_valueScriberTable[CODEC_TIME] = m_timeValueScriberInherentProxy;
    m_valueScriberTable[CODEC_DATE] = m_dateValueScriberInherentProxy;
    m_valueScriberTable[CODEC_GDAY] =  m_gDayValueScriberInherentProxy;
    m_valueScriberTable[CODEC_GMONTH] =  m_gMonthValueScriberInherentProxy;
    m_valueScriberTable[CODEC_GMONTHDAY] =  m_gMonthDayValueScriberInherentProxy;
    m_valueScriberTable[CODEC_GYEAR] =  m_gYearValueScriberInherentProxy;
    m_valueScriberTable[CODEC_GYEARMONTH] =  m_gYearMonthValueScriberInherentProxy;
    m_valueScriberTable[CODEC_DOUBLE] = m_floatValueScriberInherentProxy;
    m_valueScriberTable[CODEC_INTEGER] = m_integerValueScriberInherentProxy;
    m_valueScriberTable[CODEC_BASE64BINARY] = m_base64BinaryValueScriberInherentProxy;
    m_valueScriberTable[CODEC_HEXBINARY] = m_hexBinaryValueScriberInherentProxy;
  }

  @Override
  public void reset() {
    super.reset();
    m_channelKeeper.reset();
    if (m_compressed)
      m_deflator.reset();
  }

  /**
   * Set ZLIB compression level and strategy.
   * @param level the new compression level (0-9)
   * @param strategy the new compression strategy
   * @see java.util.zip.Deflator#setLevel(int level)
   * @see java.util.zip.Deflator#setStrategy(int strategy)
   */
  @Override
  public void setDeflateParams(int level, int strategy) {
    if (m_compressed) {
      m_deflator.setLevel(level);
      m_deflator.setStrategy(strategy);
    }
  }

  public AlignmentType getAlignmentType() {
    return m_compressed ? AlignmentType.compress : AlignmentType.preCompress;
  }
  
  @Override
  public void setOutputStream(OutputStream dataStream) {
    m_baseDataStream = dataStream;
    m_structureDataStream = m_compressed ? new DeflaterOutputStream(m_baseDataStream, m_deflator) : m_baseDataStream;
  }

  @Override
  protected OutputStream getOutputStream() {
    return m_baseDataStream;
  }

  @Override
  public OutputStream getStructureChannelStream() {
    return m_structureDataStream;
  }

  @Override
  public void setBlockSize(int blockSize) {
    m_channelKeeper.setBlockSize(blockSize);
  }
  
  @Override
  public final void setPreserveLexicalValues(boolean preserveLexicalValues) {
    final boolean prevPreserveLexicalValues = m_preserveLexicalValues;
    if (prevPreserveLexicalValues != preserveLexicalValues) {
      super.setPreserveLexicalValues(preserveLexicalValues);
      if (preserveLexicalValues) {
        m_valueScriberTable[CODEC_STRING] = m_stringValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_BOOLEAN] = m_booleanValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_ENUMERATION] = m_enumerationValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_LIST] = m_listValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_DECIMAL] = m_decimalValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_DATETIME] = m_dateTimeValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_TIME] = m_timeValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_DATE] = m_dateValueScriberLexicalProxy; 
        m_valueScriberTable[CODEC_GDAY] = m_gDayValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_GMONTH] = m_gMonthValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_GMONTHDAY] = m_gMonthDayValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_GYEAR] = m_gYearValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_GYEARMONTH] = m_gYearMonthValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_DOUBLE] = m_floatValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_INTEGER] = m_integerValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_BASE64BINARY] = m_base64BinaryValueScriberLexicalProxy;
        m_valueScriberTable[CODEC_HEXBINARY] = m_hexBinaryValueScriberLexicalProxy;
      }
      else {
        m_valueScriberTable[CODEC_STRING] = m_booleanValueScriberInherentProxy;
        m_valueScriberTable[CODEC_BOOLEAN] = m_booleanValueScriberInherentProxy;
        m_valueScriberTable[CODEC_ENUMERATION] = m_enumerationValueScriberInherentProxy;
        m_valueScriberTable[CODEC_LIST] = m_listValueScriberInherentProxy;
        m_valueScriberTable[CODEC_DECIMAL] = m_decimalValueScriberInherentProxy;
        m_valueScriberTable[CODEC_DATETIME] = m_dateTimeValueScriberInherentProxy;
        m_valueScriberTable[CODEC_TIME] = m_timeValueScriberInherentProxy;
        m_valueScriberTable[CODEC_DATE] = m_dateValueScriberInherentProxy; 
        m_valueScriberTable[CODEC_GDAY] = m_gDayValueScriberInherentProxy;
        m_valueScriberTable[CODEC_GMONTH] = m_gMonthValueScriberInherentProxy;
        m_valueScriberTable[CODEC_GMONTHDAY] = m_gMonthDayValueScriberInherentProxy;
        m_valueScriberTable[CODEC_GYEAR] = m_gYearValueScriberInherentProxy;
        m_valueScriberTable[CODEC_GYEARMONTH] = m_gYearMonthValueScriberInherentProxy;
        m_valueScriberTable[CODEC_DOUBLE] = m_floatValueScriberInherentProxy;
        m_valueScriberTable[CODEC_INTEGER] = m_integerValueScriberInherentProxy;
        m_valueScriberTable[CODEC_BASE64BINARY] = m_base64BinaryValueScriberInherentProxy;
        m_valueScriberTable[CODEC_HEXBINARY] = m_hexBinaryValueScriberInherentProxy;
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Structure Scriber Functions
  ///////////////////////////////////////////////////////////////////////////
  
  @Override
  public final void writeEventType(EventType eventType) throws IOException {
    EventCode[] path;
    path = eventType.getItemPath();

    int i, len;
    EventCode item = path[0].parent;
    for (i = 0, len = path.length; i < len; i++) {
      EventCodeTuple parent = (EventCodeTuple)item;
      item = path[i];
      final int width;
      if ((width = parent.width) != 0) {
        if (m_compressed)
          ensureStructureDataStream();
        writeNBitUnsigned(parent.reversed ? parent.itemsCount - 1 - item.m_position : item.m_position, width, m_structureDataStream);
      }
    }
  }

  @Override
  public void writeNS(String uri, String prefix, boolean localElementNs) throws IOException {
    assert m_preserveNS;
    assert m_structureDataStream != null;
    writeURI(uri, m_structureDataStream);
    writePrefixOfNS(prefix, uri);
    writeBoolean(localElementNs, m_structureDataStream);
  }

  @Override
  public void writeQName(QName qName, byte itemType) throws IOException {
    switch (itemType) {
      case EventCode.ITEM_SCHEMA_WC_ANY:
      case EventCode.ITEM_AT_WC_ANY_UNTYPED:
      case EventCode.ITEM_SE_WC:
      case EventCode.ITEM_SCHEMA_AT_WC_ANY:
        if (m_compressed)
          ensureStructureDataStream();
        writeURI(qName.namespaceName, m_structureDataStream);
      case EventCode.ITEM_SCHEMA_WC_NS:
      case EventCode.ITEM_SCHEMA_AT_WC_NS:
        if (m_compressed)
          ensureStructureDataStream();
        writeLocalName(qName.localName, qName.namespaceName, m_structureDataStream);
        break;
      case EventCode.ITEM_SCHEMA_SE:
      case EventCode.ITEM_SCHEMA_AT:
      case EventCode.ITEM_AT:        
      case EventCode.ITEM_SE:
      case EventCode.ITEM_SCHEMA_TYPE:        
      case EventCode.ITEM_SCHEMA_NIL:     
      case EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE:
        break;
      default:
        assert false;
    }
    if (m_preserveNS) {
      if (m_compressed)
        ensureStructureDataStream();
      writePrefixOfQName(qName.prefix, qName.namespaceName, m_structureDataStream);
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Other functions
  ///////////////////////////////////////////////////////////////////////////

  private void writePrefixOfNS(String prefix, String uri) throws IOException {
    final StringTable.PrefixPartition partition;
    partition = m_stringTable.getPrefixPartition(uri);
    final int n_names, width, id;
    n_names = partition.n_strings;
    width = partition.forwardedWidth;
    if ((id = partition.internString(prefix)) < n_names)
      writeNBitUnsigned(id + 1, width, m_structureDataStream);
    else {
      writeNBitUnsigned(0, width, m_structureDataStream);
      final int length = prefix.length();
      final CharacterBuffer characterBuffer = ensureCharacters(length);
      final CharacterSequence characterSequence = characterBuffer.addString(prefix, length);
      writeLiteralString(characterSequence, length, 0, EXISchema.NIL_NODE, m_structureDataStream);
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Value Scriber Functions
  ///////////////////////////////////////////////////////////////////////////

  @Override
  protected void writeBoolean(boolean val, OutputStream ostream) throws IOException {
    ostream.write(val ? 1 : 0);
  }

  @Override
  protected void writeNBitUnsigned(int val, int width, OutputStream ostream) throws IOException {
    ByteAlignedCommons.writeNBitUnsigned(val, width, ostream);
  }

  @Override
  protected void writeUnsignedInteger32(int uint, OutputStream ostream) throws IOException {
    ByteAlignedCommons.writeUnsignedInteger32(uint, ostream);
  }
  
  @Override
  protected void writeUnsignedInteger64(long uint, OutputStream ostream) throws IOException {
    ByteAlignedCommons.writeUnsignedInteger64(uint, ostream);
  }
  
  @Override
  protected void writeUnsignedInteger(BigInteger uint, OutputStream ostream) throws IOException {
    ByteAlignedCommons.writeUnsignedInteger(uint, ostream);
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Other IO Functions
  ///////////////////////////////////////////////////////////////////////////

  public void finishBlock() throws IOException {
    final int n_values = m_channelKeeper.getTotalValueCount();
    DeflaterOutputStream deflaterStream = null;
    if (m_compressed && n_values == 0) {
      if ((deflaterStream = (DeflaterOutputStream)m_structureDataStream) != null)
        deflaterStream.finish();
      return;
    }
    if (m_compressed)
        ensureStructureDataStream();
    boolean moreValues = false;
    if (m_compressed){
      if (moreValues = n_values > 100 || n_values == 0) {
        ((DeflaterOutputStream)m_structureDataStream).finish();
      }
    }
    m_channelKeeper.finish();
    final List<Channel> smallChannels, largeChannels;
    final int n_smallChannels, n_largeChannels;
    int i = 0;
    ScriberChannel channel;
    smallChannels = m_channelKeeper.getSmallChannels();
    OutputStream ostream = m_baseDataStream;
    if ((n_smallChannels = smallChannels.size()) != 0) {
      if (m_compressed) {
        if (moreValues) {
          m_deflator.reset();
          deflaterStream = new DeflaterOutputStream(m_baseDataStream, m_deflator); 
        }
        else
          deflaterStream = (DeflaterOutputStream)m_structureDataStream;
          
        ostream = deflaterStream;
      }
      do {
        channel = (ScriberChannel)smallChannels.get(i);
        ArrayList<ScriberValueHolder> textProviderList = channel.values;
        final int len = textProviderList.size();
        for (int j = 0; j < len; j++) {
          textProviderList.get(j).scribeValue(ostream);
        }
      } while (++i < n_smallChannels);
    }
    largeChannels = m_channelKeeper.getLargeChannels();
    for (i = 0, n_largeChannels = largeChannels.size(); i < n_largeChannels; i++) {
      if (m_compressed) {
        if (deflaterStream != null)
          deflaterStream.finish();
        m_deflator.reset();
        deflaterStream = new DeflaterOutputStream(m_baseDataStream, m_deflator); 
        ostream = deflaterStream;
      }
      channel = (ScriberChannel)largeChannels.get(i);
      ArrayList<ScriberValueHolder> textProviderList = channel.values;
      final int len = textProviderList.size();
      for (int j = 0; j < len; j++) {
        textProviderList.get(j).scribeValue(ostream);
      }
    }
    if (m_compressed) {
      if (deflaterStream != null)
        deflaterStream.finish();
      m_structureDataStream = null;
    }
    m_channelKeeper.reset();
  }
  
  @Override
  public void finish() throws IOException {
    finishBlock();
    m_baseDataStream.flush();
  }

  private void ensureStructureDataStream() {
    if (m_structureDataStream == null) {
      m_deflator.reset();
      m_structureDataStream = new DeflaterOutputStream(m_baseDataStream, m_deflator);
    }
  }
  
}
