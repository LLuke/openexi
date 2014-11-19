package com.sumerogi.proc.io.compression;

import com.sumerogi.proc.common.Channel;

abstract class ChannelFactory {

  abstract Channel createChannel(int firstPos, int blockNum);
  
}
