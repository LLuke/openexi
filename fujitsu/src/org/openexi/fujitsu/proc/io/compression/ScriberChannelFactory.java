package org.openexi.fujitsu.proc.io.compression;

final class ScriberChannelFactory extends ChannelFactory {

  @Override
  public Channel createChannel(String name, String uri, int firstPos, ChannelKeeper channelKeeper) {
    return new ScriberChannel(name, uri, firstPos, channelKeeper);
  }

}
