package org.openexi.proc.common;

/**
 * Event codes are byte values used to identify discrete events in an 
 * EXI stream. Several events have multiple definitions that  
 * distinguish events defined in a schema, events defined in a namespace,
 * and undefined events that are captured when the document is encoded.
 */
public abstract class EventCode {

  /**
   * Not for public use.
   * @y.exclude
   */
  public static final byte ITEM_TUPLE = -1;

  /**
   * Not for public use.
   * @y.exclude
   */
  public static final byte EVENT_CODE_DEPTH_ONE   = 1;
  /**
   * Not for public use.
   * @y.exclude
   */
  public static final byte EVENT_CODE_DEPTH_TWO   = 2;
  /**
   * Not for public use.
   * @y.exclude
   */
  public static final byte EVENT_CODE_DEPTH_THREE = 3;

  /**
   * A tuple that is the parent of this item. 
   * Null if this event code item is the root.
   * @y.exclude
   */
  public EventCode parent;
  
  /**
   * Not for public use.
   * @y.exclude
   */
  public int position;
  
  protected EventCode(byte itemType) {
    parent = null;
    position = -1;
    this.itemType = itemType;
  }
  
  /**
   * Byte value that identifies the item type from the list of 25 defined constants.   
   */
  public final byte itemType;
 
  ///////////////////////////////////////////////////////////////////////////
  /// 
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Not for public use.
   * @y.exclude
   */
  public final void setParentalContext(int position, EventCode parent) {
    this.position = position;
    this.parent = parent;
  }
  
}
