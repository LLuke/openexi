package com.sumerogi.proc.io.compression;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

import com.sumerogi.proc.common.Channel;
import com.sumerogi.proc.common.EXIOptions;
import com.sumerogi.proc.common.StringTable;

final class ChannelKeeper {
  
  private int m_totalValueCount;
  
  private final LinkedList<Channel> m_smallChannelList;
  private final ArrayList<Channel> m_largeChannelList;
  
  private final ChannelFactory m_channelFactory;
  
  private int m_blockSize;
  
  private int m_blockNum;
  
  ChannelKeeper(ChannelFactory channelFactory) {
    m_totalValueCount = 0;
    m_smallChannelList = new LinkedList<Channel>();
    m_largeChannelList = new ArrayList<Channel>();
    m_channelFactory = channelFactory;
    m_blockSize = EXIOptions.BLOCKSIZE_DEFAULT;
    m_blockNum = -1; 
  }
  
  public void punctuate() {
    m_totalValueCount = 0;
    m_smallChannelList.clear();
    m_largeChannelList.clear();
    ++m_blockNum;
  }

  public void reset() {
    m_blockNum = -1;
    punctuate();
  }

  public void finish() {
    Collections.sort(m_smallChannelList);
    Collections.sort(m_largeChannelList);
  }
  
  public int getBlockSize() {
    return m_blockSize;
  }
  
  void setBlockSize(int blockSize) {
    m_blockSize = blockSize;
  }
  
  List<Channel> getSmallChannels() {
    return m_smallChannelList;
  }
  
  List<Channel> getLargeChannels() {
    return m_largeChannelList;
  }
  
  int getTotalValueCount() {
    return m_totalValueCount;
  }
  
  Channel getChannel(int name, StringTable stringTable) {
    Channel channel;
    if ((channel = stringTable.localNameEntries[name].channel) != null) {
      if (channel.blockNum != m_blockNum)
        channel.reset(m_totalValueCount, m_blockNum); // reuse
      else
        return channel;
    }
    else {
      channel = m_channelFactory.createChannel(m_totalValueCount, m_blockNum);
      stringTable.setChannel(name, channel);
    }
    m_smallChannelList.add(channel);
    return channel;
  }

  boolean incrementValueCount(Channel channel) {
    if (++channel.valueCount == 101) {
      m_smallChannelList.remove(channel);
      m_largeChannelList.add(channel);
    }
    return ++m_totalValueCount == m_blockSize;
  }
  
}
