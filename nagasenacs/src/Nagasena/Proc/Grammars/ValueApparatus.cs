using QName = Nagasena.Proc.Common.QName;

namespace Nagasena.Proc.Grammars {

  /// <exclude/>
  public abstract class ValueApparatus {

    public abstract QName Name { get; }
    public abstract short CodecID { get; }

  }

}