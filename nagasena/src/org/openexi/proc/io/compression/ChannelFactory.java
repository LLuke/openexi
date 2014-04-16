package org.openexi.proc.io.compression;

import org.openexi.proc.common.Channel;

abstract class ChannelFactory {

  public abstract Channel createChannel(int firstPos, int blockNum);
  
}
