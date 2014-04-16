package org.openexi.proc.common;

public abstract class Channel implements Comparable<Channel> {
  
  private int firstPos;
  public int blockNum;
  
  public int valueCount;
  
  protected Channel(int firstPos, int blockNum) {
    _reset(firstPos, blockNum);
  }
  
  private void _reset(int firstPos, int blockNum) {
    this.firstPos = firstPos;
    this.blockNum = blockNum;
    valueCount = 0;
  }
  
  public void reset(int firstPos, int blockNum) {
    _reset(firstPos, blockNum);
  }

  public final int compareTo(Channel obj) {
    return firstPos - obj.firstPos;
  }
  
}
