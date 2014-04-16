package org.openexi.proc.io.compression;

import java.util.ArrayList;

class ScriberChannel extends Channel {

  final ArrayList<ScriberValueHolder> values;
  
  ScriberChannel(String name, String uri, int firstPos, ChannelKeeper channelKeeper) {
    super(name, uri, firstPos, channelKeeper);
    values = new ArrayList();
  }

}
