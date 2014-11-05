package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.EventCode;
import com.sumerogi.proc.common.EventType;

final class ArrayEventCodeTuple extends EventCodeTuple {

  /**
   * Constructor
   */
  ArrayEventCodeTuple() {
    super(EventCode.ITEM_TUPLE, false);
    eventCodes = null;
  }

  final void setItems(EventCode[] items) {
    assert items != null && items.length > 0;
    eventCodes = items;
    itemsCount = eventCodes.length;
    int n, _width;
    for (_width = 0, n = eventCodes.length - 1; n > 0; _width++) {
      n >>= 1;
    }
    width = _width;
    for (int i = 0; i < items.length; i++) {
      final EventCode ith = items[i];
      // Productions of ITEM_SCHEMA_NIL do not participate in TypeEmpty grammars,
      // which renders an item null.
      if (ith != null) {
        ith.setParentalContext(i, this);
        if (ith.itemType != EventCode.ITEM_TUPLE) {
          ((EventType)ith).computeItemPath();
        }
      }
    }
    headItem = items[0];
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventCodeTuple interface
  ///////////////////////////////////////////////////////////////////////////
  
  @Override
  public final EventCode getItem(int i) {
    return eventCodes[i];
  }

}