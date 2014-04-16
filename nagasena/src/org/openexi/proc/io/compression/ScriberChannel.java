package org.openexi.proc.io.compression;

import java.util.ArrayList;

import org.openexi.proc.common.Channel;

class ScriberChannel extends Channel {

  final ArrayList<ScriberValueHolder> values;
  
  ScriberChannel(int firstPos, int blockNum) {
    super(firstPos, blockNum);
    values = new ArrayList<ScriberValueHolder>();
  }

  @Override
  public void reset(int firstPos, int blockNum) {
    super.reset(firstPos, blockNum);
    values.clear();
  }
  
}
