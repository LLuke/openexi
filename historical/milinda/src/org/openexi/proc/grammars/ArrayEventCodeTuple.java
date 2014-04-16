package org.openexi.proc.grammars;

import org.openexi.proc.common.EventCode;

class ArrayEventCodeTuple extends EventCodeTuple {

  private EventCode[] m_items;
  
  /**
   * Constructor
   */
  final static ArrayEventCodeTuple createTuple() {
    return new ArrayEventCodeTuple();
  }

  protected ArrayEventCodeTuple() {
    super(EventCode.ITEM_TUPLE, false);
    m_items = null;
  }

  final void setItems(EventCode[] items) {
    assert items != null && items.length > 0;
    m_items = items;
    itemsCount = m_items.length;
    int n, _width;
    for (_width = 0, n = m_items.length - 1; n > 0; _width++) {
      n >>= 1;
    }
    width = _width;
    for (int i = 0; i < items.length; i++) {
      EventCode ith = items[i];
      ith.setParentalContext(i, this);
      if (ith.itemType != EventCode.ITEM_TUPLE) {
        ((AbstractEventType)ith).computeItemPath();
      }
    }
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventCodeTuple interface
  ///////////////////////////////////////////////////////////////////////////
  
  @Override
  public final EventCode getItem(int i) {
    return m_items[i];
  }

}