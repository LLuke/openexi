package org.openexi.proc.common;

public abstract class EventCode {

  // DO NOT CHANGE CODE ASSIGNMENT of ITEM_PI ... ITEM_EE 
  public static final byte ITEM_PI                                        = 0;
  public static final byte ITEM_CM                                        = 1;
  public static final byte ITEM_ER                                        = 2;
  public static final byte ITEM_CH                                        = 3;
  public static final byte ITEM_ED                                        = 4;
  public static final byte ITEM_SE_WC                                     = 5;
  public static final byte ITEM_SC                                        = 6;
  public static final byte ITEM_NS                                        = 7;
  public static final byte ITEM_AT_WC_ANY_UNTYPED                         = 8;
  public static final byte ITEM_EE                                        = 9;

  public static final byte ITEM_TUPLE                                     = 10;
  
  public static final byte ITEM_DTD                                       = 11;
  public static final byte ITEM_SE                                        = 12;
  public static final byte ITEM_AT                                        = 13;
  public static final byte ITEM_SD                                        = 14;
  
  public static final byte ITEM_SCHEMA_SE                                 = 15;
  public static final byte ITEM_SCHEMA_WC_ANY                             = 16;
  public static final byte ITEM_SCHEMA_WC_NS                              = 17;
  public static final byte ITEM_SCHEMA_AT                                 = 18;
  public static final byte ITEM_SCHEMA_AT_WC_ANY                          = 19;
  public static final byte ITEM_SCHEMA_AT_WC_NS                           = 20;
  public static final byte ITEM_SCHEMA_CH                                 = 21;
  public static final byte ITEM_SCHEMA_CH_MIXED                           = 22;
  public static final byte ITEM_SCHEMA_EE                                 = 23;
  public static final byte ITEM_SCHEMA_NIL                                = 24;
  public static final byte ITEM_SCHEMA_TYPE                               = 25;
  public static final byte ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE        = 26;

  public static final byte EVENT_CODE_DEPTH_ONE   = 1;
  public static final byte EVENT_CODE_DEPTH_TWO   = 2;
  public static final byte EVENT_CODE_DEPTH_THREE = 3;

  // N_NONSCHEMA_ITEMS must be ITEM_EE plus 1
  public static final short N_NONSCHEMA_ITEMS = 10;

  /**
   * A tuple that is the parent of this item. 
   * Null if this event code item is the root.
   */
  public EventCode parent;
  
  public int m_position;
  
  protected EventCode(byte itemType) {
    parent = null;
    m_position = -1;
    this.itemType = itemType;
  }
  
  public final byte itemType;
 
  ///////////////////////////////////////////////////////////////////////////
  /// 
  ///////////////////////////////////////////////////////////////////////////

  public final void setParentalContext(int pos, EventCode parent) {
    m_position = pos;
    this.parent = parent;
  }

  public boolean isPositionReversed() {
    return false;
  }
  
}
