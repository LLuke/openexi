using Channel = Nagasena.Proc.Common.Channel;

namespace Nagasena.Proc.IO.Compression {

  internal sealed class ScriberChannelFactory : ChannelFactory {

    internal override Channel createChannel(int firstPos, int blockNum) {
      return new ScriberChannel(firstPos, blockNum);
    }

  }

}