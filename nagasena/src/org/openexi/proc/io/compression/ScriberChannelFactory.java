package org.openexi.proc.io.compression;

import org.openexi.proc.common.Channel;

final class ScriberChannelFactory extends ChannelFactory {

  @Override
  public Channel createChannel(int firstPos, int blockNum) {
    return new ScriberChannel(firstPos, blockNum);
  }

}
