package org.openexi.proc.io.compression;

abstract class Channel implements Comparable {
  
  final String name; 
  final String uri; 
  final int firstPos;
  
  int valueCount;
  protected final ChannelKeeper m_channelKeeper;
  
  Channel(String name, String uri, int firstPos, ChannelKeeper channelKeeper) {
    this.name = name;
    this.uri = uri;
    this.firstPos = firstPos;
    valueCount = 0;
    m_channelKeeper = channelKeeper;
  }

  public int compareTo(Object obj) {
    return firstPos - ((Channel)obj).firstPos;
  }
  
}
