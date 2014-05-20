using System;

namespace Nagasena.Proc.Common {

  /// <exclude/>
  public abstract class Channel : IComparable<Channel> {

    internal int firstPos;
    internal int blockNum;

    public int valueCount;

    protected internal Channel(int firstPos, int blockNum) {
      _reset(firstPos, blockNum);
    }

    private void _reset(int firstPos, int blockNum) {
      this.firstPos = firstPos;
      this.blockNum = blockNum;
      valueCount = 0;
    }

    public virtual void reset(int firstPos, int blockNum) {
      _reset(firstPos, blockNum);
    }

    public int CompareTo(Channel obj) {
      return firstPos - obj.firstPos;
    }

  }

}