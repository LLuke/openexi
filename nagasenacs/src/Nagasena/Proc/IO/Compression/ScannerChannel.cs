using System.Collections.Generic;

using Channel = Nagasena.Proc.Common.Channel;

namespace Nagasena.Proc.IO.Compression {

  internal class ScannerChannel : Channel {

    internal readonly List<EXIEventValueReference> values;

    internal ScannerChannel(int firstPos, int blockNum) : base(firstPos, blockNum) {
      values = new List<EXIEventValueReference>();
    }

    public override void reset(int firstPos, int blockNum) {
      base.reset(firstPos, blockNum);
      values.Clear();
    }

  }

}