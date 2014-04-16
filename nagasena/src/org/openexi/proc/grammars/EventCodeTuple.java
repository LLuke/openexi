package org.openexi.proc.grammars;

import org.openexi.proc.common.EventCode;

public abstract class EventCodeTuple extends EventCode {

  public final boolean reversed;
  // items are in reverse order if reversed is true.
  public EventCode[] eventCodes;

  public int width;
  public int itemsCount;
  
  // m_eventCodes[0]
  public EventCode headItem;
  
  protected EventCodeTuple(byte itemType, boolean reversed) {
    super(itemType);
    width = 0;
    itemsCount = 0;
    this.reversed = reversed;
  }

  public abstract EventCode getItem(int i);

}
