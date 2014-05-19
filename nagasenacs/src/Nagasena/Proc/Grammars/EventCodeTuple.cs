using EventCode = Nagasena.Proc.Common.EventCode;

namespace Nagasena.Proc.Grammars {

  /// <exclude/>
  public abstract class EventCodeTuple : EventCode {

    public readonly bool reversed;
    // items are in reverse order if reversed is true.
    public EventCode[] eventCodes;

    public int width;
    public int itemsCount;

    // m_eventCodes[0]
    public EventCode headItem;

    protected internal EventCodeTuple(sbyte itemType, bool reversed) : base(itemType) {
      width = 0;
      itemsCount = 0;
      this.reversed = reversed;
    }

    public abstract EventCode getItem(int i);

  }

}