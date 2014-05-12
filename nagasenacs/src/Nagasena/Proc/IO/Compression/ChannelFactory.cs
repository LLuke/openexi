using Channel = Nagasena.Proc.Common.Channel;

namespace Nagasena.Proc.IO.Compression {

  internal abstract class ChannelFactory {

    public abstract Channel createChannel(int firstPos, int blockNum);

  }

}