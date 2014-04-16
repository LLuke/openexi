namespace Nagasena.Proc.IO.Compression {

  internal abstract class ChannellingScanner : Scanner {

    public int BlockCount {
      get {
        return -1;
      }
    }

    internal ChannellingScanner()
      : base(false) {
    }

  }

}