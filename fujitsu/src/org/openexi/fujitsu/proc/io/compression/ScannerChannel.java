package org.openexi.fujitsu.proc.io.compression;

import java.util.ArrayList;

class ScannerChannel extends Channel {
  
  final ArrayList<ScannerValueHolder> values;

  ScannerChannel(String name, String uri, int firstPos, ChannelKeeper channelKeeper) {
    super(name, uri, firstPos, channelKeeper);
    values = new ArrayList();
  }

}
