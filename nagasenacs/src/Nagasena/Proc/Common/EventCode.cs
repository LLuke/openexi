namespace Nagasena.Proc.Common {

  /// <summary>
  /// Event codes are byte values used to identify discrete events in an 
  /// EXI stream. Several events have multiple definitions that  
  /// distinguish events defined in a schema, events defined in a namespace,
  /// and undefined events that are captured when the document is encoded.
  /// </summary>
  public abstract class EventCode {

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public const sbyte ITEM_TUPLE = -1;

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public const sbyte EVENT_CODE_DEPTH_ONE = 1;
    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public const sbyte EVENT_CODE_DEPTH_TWO = 2;
    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public const sbyte EVENT_CODE_DEPTH_THREE = 3;

    /// <summary>
    /// A tuple that is the parent of this item. 
    /// Null if this event code item is the root.
    /// @y.exclude
    /// </summary>
    public EventCode parent;

    /// <summary>
    /// Not for public use.
    /// @y.exclude
    /// </summary>
    public int position;

    protected internal EventCode(sbyte itemType) {
      parent = null;
      position = -1;
      this.itemType = itemType;
    }

    /// <summary>
    /// Byte value that identifies the item type from the list of 25 defined constants.   
    /// </summary>
    public readonly sbyte itemType;

    ///////////////////////////////////////////////////////////////////////////
    /// 
    ///////////////////////////////////////////////////////////////////////////

    internal void setParentalContext(int position, EventCode parent) {
      this.position = position;
      this.parent = parent;
    }

  }

}