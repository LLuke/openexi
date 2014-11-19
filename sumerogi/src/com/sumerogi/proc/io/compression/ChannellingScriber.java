package com.sumerogi.proc.io.compression;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;

import com.sumerogi.proc.common.AlignmentType;
import com.sumerogi.proc.common.Channel;
import com.sumerogi.proc.common.EventCode;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.grammars.EventCodeTuple;
import com.sumerogi.proc.io.ByteAlignedCommons;
import com.sumerogi.proc.io.Scriber;
import com.sumerogi.proc.io.ValueScriber;

public final class ChannellingScriber extends Scriber {

  private final boolean m_compressed;

  private OutputStream m_baseDataStream;

  private final ValueScriber m_stringValueScriberInherentProxy;
  private final ValueScriber m_booleanValueScriberInherentProxy;
  private final ValueScriber m_numberValueScriberInherentProxy;
  
  private final ChannelKeeper m_channelKeeper;
  private final Deflater m_deflator;

  public ChannellingScriber(boolean compressed) {
    m_compressed = compressed;
    m_channelKeeper = new ChannelKeeper(new ScriberChannelFactory());
    m_deflator = compressed ? new Deflater(Deflater.DEFAULT_COMPRESSION, true) : null;
    
    m_stringValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, stringValueScriber);
    m_booleanValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, booleanValueScriber);
    m_numberValueScriberInherentProxy = new ChannellingValueScriberProxy(m_channelKeeper, numberValueScriber);
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

  public final ValueScriber getStringValueScriber() {
    return m_stringValueScriberInherentProxy;
  }
  
  public final ValueScriber getBooleanValueScriber() {
    return m_booleanValueScriberInherentProxy;
  }
  
  public final ValueScriber getNumberValueScriber() {
    return m_numberValueScriberInherentProxy;
  }
  
  @Override
  public void writeHeaderPreamble() throws IOException {
    m_outputStream.write(m_compressed ? AlignmentType.compress.headerValue : AlignmentType.preCompress.headerValue);
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

//  @Override
//  public void writeNS(String uri, String prefix, boolean localElementNs) throws IOException {
//    assert m_preserveNS;
//    assert m_outputStream != null;
//    final int uriId = writeURI(uri, m_outputStream);
//    writePrefixOfNS(prefix, uriId);
//    writeBoolean(localElementNs, m_outputStream);
//  }

//  public abstract int writeName(String name, EventType eventType) throws IOException;

  @Override
  public int writeName(String name, EventType eventType) throws IOException {
    final byte itemType = eventType.itemType;
    final int localNameId;
    switch (itemType) {
      case EventType.ITEM_STRING_VALUE_WILDCARD:
      case EventType.ITEM_NUMBER_VALUE_WILDCARD:
      case EventType.ITEM_BOOLEAN_VALUE_WILDCARD:
      case EventType.ITEM_NULL_WILDCARD:
      case EventType.ITEM_START_ARRAY_WILDCARD:
      case EventType.ITEM_START_OBJECT_WILDCARD:
        localNameId = writeLocalName(name, stringTable, m_outputStream);
        break;
      case EventType.ITEM_STRING_VALUE_NAMED:
      case EventType.ITEM_NUMBER_VALUE_NAMED:
      case EventType.ITEM_BOOLEAN_VALUE_NAMED:
      case EventType.ITEM_NULL_NAMED:
      case EventType.ITEM_START_OBJECT_NAMED:
      case EventType.ITEM_START_ARRAY_NAMED:
        localNameId = eventType.getNameId();
        break;
      default:
        localNameId = -1;
        assert false;
    }
    return localNameId;
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
