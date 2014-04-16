package org.openexi.proc.io.compression;

import org.openexi.proc.common.Channel;

final class ScannerChannelFactory extends ChannelFactory {

  @Override
  public Channel createChannel(int firstPos, int blockNum) {
    return new ScannerChannel(firstPos, blockNum);
  }

}
