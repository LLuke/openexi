using System.Collections.Generic;

using Channel = Nagasena.Proc.Common.Channel;

namespace Nagasena.Proc.IO.Compression {

  internal class ScriberChannel : Channel {

    internal readonly List<ScriberValueHolder> values;

    internal ScriberChannel(int firstPos, int blockNum) : base(firstPos, blockNum) {
      values = new List<ScriberValueHolder>();
    }

    public override void reset(int firstPos, int blockNum) {
      base.reset(firstPos, blockNum);
      values.Clear();
    }

  }

}