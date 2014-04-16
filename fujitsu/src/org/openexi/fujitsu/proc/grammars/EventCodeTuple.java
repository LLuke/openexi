package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventCode;

public abstract class EventCodeTuple extends EventCode {
  
  public int width;
  public int itemsCount;
  public final boolean reversed;
  
  protected EventCodeTuple(byte itemType, boolean reversed) {
    super(itemType);
    width = 0;
    itemsCount = 0;
    this.reversed = reversed;
  }

  public abstract EventCode getItem(int i);

}
