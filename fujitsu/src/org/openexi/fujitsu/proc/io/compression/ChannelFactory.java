package org.openexi.fujitsu.proc.io.compression;

abstract class ChannelFactory {

  public abstract Channel createChannel(String name, String uri, int firstPos, ChannelKeeper channelKeeper);
  
}
