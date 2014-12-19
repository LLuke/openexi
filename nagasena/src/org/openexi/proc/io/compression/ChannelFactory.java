package org.openexi.proc.io.compression;

import org.openexi.proc.common.Channel;

abstract class ChannelFactory {

  abstract Channel createChannel(int firstPos, int blockNum);
  
}
