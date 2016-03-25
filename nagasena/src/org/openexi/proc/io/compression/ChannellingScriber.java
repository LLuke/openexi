package org.openexi.proc.io.compression;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.Channel;
import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.StringTable;
import org.openexi.proc.grammars.EventCodeTuple;
import org.openexi.proc.io.ByteAlignedCommons;
import org.openexi.proc.io.CharacterBuffer;
import org.openexi.proc.io.Scriber;
import org.openexi.proc.io.ValueScriber;
import org.openexi.schema.Characters;
import org.openexi.schema.EXISchema;

public final class ChannellingScriber extends Scriber {

  private final boolean m_compressed;

  private OutputStream m_baseDataStream;

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
    
    m_stringValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_stringValueScriberInherent);
    m_booleanValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_booleanValueScriberInherent);
    m_enumerationValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_enumerationValueScriberInherent);
    m_listValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_listValueScriberInherent);
    m_decimalValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_decimalValueScriberInherent);
    m_dateTimeValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_dateTimeValueScriberInherent);
    m_timeValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_timeValueScriberInherent);
    m_dateValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_dateValueScriberInherent);
    m_gDayValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gDayValueScriberInherent);
    m_gMonthValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gMonthValueScriberInherent);
    m_gMonthDayValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gMonthDayValueScriberInherent);
    m_gYearValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gYearValueScriberInherent);
    m_gYearMonthValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gYearMonthValueScriberInherent);
    m_floatValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_floatValueScriberInherent); 
    m_integerValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_integerValueScriberInherent);
    m_base64BinaryValueScriberInherentProxy = new ChannellingBinaryValueScriberProxy(m_channelKeeper, m_base64BinaryValueScriberInherent);
    m_hexBinaryValueScriberInherentProxy = new ChannellingBinaryValueScriberProxy(m_channelKeeper, m_hexBinaryValueScriberInherent);
    
    m_stringValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_stringValueScriberLexical);
    m_booleanValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_booleanValueScriberLexical);
    m_enumerationValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_enumerationValueScriberLexical);
    m_listValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_listValueScriberLexical);
    m_decimalValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_decimalValueScriberLexical);
    m_dateTimeValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_dateTimeValueScriberLexical);
    m_timeValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_timeValueScriberLexical);
    m_dateValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_dateValueScriberLexical);
    m_gDayValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gDayValueScriberLexical);
    m_gMonthValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gMonthValueScriberLexical);
    m_gMonthDayValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gMonthDayValueScriberLexical);
    m_gYearValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gYearValueScriberLexical);
    m_gYearMonthValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_gYearMonthValueScriberLexical);
    m_floatValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_floatValueScriberLexical); 
    m_integerValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_integerValueScriberLexical);
    m_base64BinaryValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_base64BinaryValueScriberLexical);
    m_hexBinaryValueScriberLexicalProxy = new ChannellingValueScriberProxy(m_channelKeeper, m_hexBinaryValueScriberLexical);
    
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
    m_baseDataStream = null;
    m_outputStream = null;
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

  @Override
  public AlignmentType getAlignmentType() {
    return m_compressed ? AlignmentType.compress : AlignmentType.preCompress;
  }
  
  @Override
  public void setOutputStream(OutputStream dataStream) {
    m_baseDataStream = dataStream;
    m_outputStream = m_compressed ? new EXIDeflaterOutputStream(m_baseDataStream, m_deflator) : m_baseDataStream;
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
        m_valueScriberTable[CODEC_STRING] = m_stringValueScriberInherentProxy;
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
      if ((width = parent.width) != 0)
        writeNBitUnsigned(parent.reversed ? parent.itemsCount - 1 - item.position : item.position, width, m_outputStream);
    }
  }

  @Override
  public void writeNS(String uri, String prefix, boolean localElementNs) throws IOException {
    assert m_preserveNS;
    assert m_outputStream != null;
    final int uriId = writeURI(uri, m_outputStream);
    writePrefixOfNS(prefix, uriId);
    writeBoolean(localElementNs, m_outputStream);
  }

  @Override
  public void writeQName(QName qName, EventType eventType) throws IOException {
    final byte itemType = eventType.itemType;
    StringTable.LocalNamePartition localNamePartition;
    final int uriId, localNameId;
    switch (itemType) {
      case EventType.ITEM_SCHEMA_WC_ANY:
      case EventType.ITEM_AT_WC_ANY_UNTYPED:
      case EventType.ITEM_SE_WC:
      case EventType.ITEM_SCHEMA_AT_WC_ANY:
        uriId = writeURI(qName.namespaceName, m_outputStream);
        localNamePartition = stringTable.getLocalNamePartition(uriId);
        localNameId = writeLocalName(qName.localName, localNamePartition, m_outputStream);
        break;
      case EventType.ITEM_SCHEMA_WC_NS:
      case EventType.ITEM_SCHEMA_AT_WC_NS:
        uriId = eventType.getURIId();
        localNamePartition = stringTable.getLocalNamePartition(uriId);
        localNameId = writeLocalName(qName.localName, localNamePartition, m_outputStream);
        break;
      case EventType.ITEM_SCHEMA_AT:
      case EventType.ITEM_AT:        
      case EventType.ITEM_SE:
      case EventType.ITEM_SCHEMA_TYPE:        
      case EventType.ITEM_SCHEMA_NIL:     
      case EventType.ITEM_SCHEMA_AT_INVALID_VALUE:
        uriId = eventType.getURIId();
        localNameId = eventType.getNameId();
        break;
      default:
        uriId = localNameId = -1;
        assert false;
    }
    qName.uriId = uriId;
    qName.localNameId = localNameId;
    if (m_preserveNS)
      writePrefixOfQName(qName.prefix, uriId, m_outputStream);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Other functions
  ///////////////////////////////////////////////////////////////////////////

  private void writePrefixOfNS(String prefix, int uriId) throws IOException {
    final StringTable.PrefixPartition partition;
    partition = stringTable.getPrefixPartition(uriId);
    final int n_names, width, id;
    n_names = partition.n_strings;
    width = partition.forwardedWidth;
    if ((id = partition.internPrefix(prefix)) < n_names)
      writeNBitUnsigned(id + 1, width, m_outputStream);
    else {
      writeNBitUnsigned(0, width, m_outputStream);
      final int length = prefix.length();
      final CharacterBuffer characterBuffer = ensureCharacters(length);
      final Characters characterSequence = characterBuffer.addString(prefix, length);
      writeLiteralCharacters(characterSequence, length, 0, EXISchema.NIL_NODE, m_outputStream);
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
    @SuppressWarnings("resource")
	final EXIDeflaterOutputStream deflaterStream = m_compressed ? 
        (EXIDeflaterOutputStream)m_outputStream : null;
    boolean moreValues = false;
    if (m_compressed) {
      final int n_values;
      if ((n_values = m_channelKeeper.getTotalValueCount()) == 0) {
        deflaterStream.resetDeflater();
        m_channelKeeper.punctuate();
        return;
      }
      if (moreValues = n_values > 100)
        deflaterStream.resetDeflater();
    }
    m_channelKeeper.finish();
    final List<Channel> smallChannels, largeChannels;
    final int n_smallChannels, n_largeChannels;
    ScriberChannel channel;
    smallChannels = m_channelKeeper.getSmallChannels();
    if ((n_smallChannels = smallChannels.size()) != 0) {
      int i = 0;
      do {
        channel = (ScriberChannel)smallChannels.get(i);
        ArrayList<ScriberValueHolder> textProviderList = channel.values;
        final int len = textProviderList.size();
        for (int j = 0; j < len; j++) {
          textProviderList.get(j).scribeValue(m_outputStream, this);
        }
      } while (++i < n_smallChannels);
      if (m_compressed && moreValues)
        deflaterStream.resetDeflater();
    }
    largeChannels = m_channelKeeper.getLargeChannels();
    n_largeChannels = largeChannels.size();
    for (int i = 0; i < n_largeChannels; i++) {
      channel = (ScriberChannel)largeChannels.get(i);
      ArrayList<ScriberValueHolder> textProviderList = channel.values;
      final int len = textProviderList.size();
      for (int j = 0; j < len; j++) {
        textProviderList.get(j).scribeValue(m_outputStream, this);
      }
      if (m_compressed)
        deflaterStream.resetDeflater();
    }
    if (m_compressed && !moreValues)
      deflaterStream.resetDeflater();
    m_channelKeeper.punctuate();
  }
  
  @Override
  public void finish() throws IOException {
    finishBlock();
    m_baseDataStream.flush();
  }

}
