using Channel = Nagasena.Proc.Common.Channel;

namespace Nagasena.Proc.IO.Compression {

  internal abstract class ChannelFactory {

    internal abstract Channel createChannel(int firstPos, int blockNum);

  }

}