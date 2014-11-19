package com.sumerogi.proc.io.compression;

import com.sumerogi.proc.common.Channel;

final class ScannerChannelFactory extends ChannelFactory {

  @Override
  public Channel createChannel(int firstPos, int blockNum) {
    return new ScannerChannel(firstPos, blockNum);
  }

}
