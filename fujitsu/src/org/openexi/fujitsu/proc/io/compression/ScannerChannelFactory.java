package org.openexi.fujitsu.proc.io.compression;

final class ScannerChannelFactory extends ChannelFactory {

  @Override
  public Channel createChannel(String name, String uri, int firstPos, ChannelKeeper channelKeeper) {
    return new ScannerChannel(name, uri, firstPos, channelKeeper);
  }

}
