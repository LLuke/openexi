package com.sumerogi.proc.io.compression;

import java.util.ArrayList;

import com.sumerogi.proc.common.Channel;

class ScannerChannel extends Channel {
  
  final ArrayList<EXIEventValueReference> values;

  ScannerChannel(int firstPos, int blockNum) {
    super(firstPos, blockNum);
    values = new ArrayList<EXIEventValueReference>();
  }

  @Override
  public void reset(int firstPos, int blockNum) {
    super.reset(firstPos, blockNum);
    values.clear();
  }

}
