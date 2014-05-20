using System;
using System.Collections.Generic;
using System.Linq;

using Channel = Nagasena.Proc.Common.Channel;
using EXIOptions = Nagasena.Proc.Common.EXIOptions;
using StringTable = Nagasena.Proc.Common.StringTable;
using LocalNamePartition = Nagasena.Proc.Common.StringTable.LocalNamePartition;

namespace Nagasena.Proc.IO.Compression {

  internal sealed class ChannelKeeper {

    private int m_totalValueCount;

    private readonly LinkedList<Channel> m_smallChannelList;
    private readonly List<Channel> m_largeChannelList;

    private readonly ChannelFactory m_channelFactory;

    private int m_blockSize;

    private int m_blockNum;

    internal ChannelKeeper(ChannelFactory channelFactory) {
      m_totalValueCount = 0;
      m_smallChannelList = new LinkedList<Channel>();
      m_largeChannelList = new List<Channel>();
      m_channelFactory = channelFactory;
      m_blockSize = EXIOptions.BLOCKSIZE_DEFAULT;
      m_blockNum = -1;
    }

    public void punctuate() {
      m_totalValueCount = 0;
      m_smallChannelList.Clear();
      m_largeChannelList.Clear();
      ++m_blockNum;
    }

    public void reset() {
      m_blockNum = -1;
      punctuate();
    }

    public void finish() {
      m_largeChannelList.Sort();
    }

    public int BlockSize {
      get {
        return m_blockSize;
      }
      set {
        m_blockSize = value;
      }
    }

    internal IOrderedEnumerable<Channel> SmallChannels {
      get {
        return Enumerable.OrderBy<Channel,int>(m_smallChannelList, delegate(Channel c) { return c.firstPos; });
      }
    }

    internal IList<Channel> LargeChannels {
      get {
        return m_largeChannelList;
      }
    }

    internal int TotalValueCount {
      get {
        return m_totalValueCount;
      }
    }

    internal Channel getChannel(int name, int uri, StringTable stringTable) {
      StringTable.LocalNamePartition localNamePartition = stringTable.getLocalNamePartition(uri);
      Channel channel;
      if ((channel = localNamePartition.localNameEntries[name].channel) != null) {
        if (channel.blockNum != m_blockNum) {
          channel.reset(m_totalValueCount, m_blockNum); // reuse
        }
        else {
          return channel;
        }
      }
      else {
        channel = m_channelFactory.createChannel(m_totalValueCount, m_blockNum);
        localNamePartition.setChannel(name, channel);
      }
      m_smallChannelList.AddLast(channel);
      return channel;
    }

    internal bool incrementValueCount(Channel channel) {
      if (++channel.valueCount == 101) {
        m_smallChannelList.Remove(channel);
        m_largeChannelList.Add(channel);
      }
      return ++m_totalValueCount == m_blockSize;
    }

  }

}