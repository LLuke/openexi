package org.openexi.fujitsu.proc.grammars;

import org.openexi.fujitsu.proc.common.EventCode;

final class ReverseEventCodeTuple extends EventCodeTuple {

  private EventCode[] m_items; // items in reverse order.
  
  public ReverseEventCodeTuple() {
    super(EventCode.ITEM_TUPLE, true);
    m_items = new EventCode[16];
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventCodeTuple interface
  ///////////////////////////////////////////////////////////////////////////
  
  @Override
  public EventCode getItem(int i) {
    return m_items[itemsCount - (i + 1)];
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Methods
  ///////////////////////////////////////////////////////////////////////////

  void setInitialSoloTuple(EventCodeTuple tuple) {
    assert itemsCount == 0 && tuple.isPositionReversed();
    if (itemsCount == m_items.length) {
      final EventCode[] items = new EventCode[itemsCount + 16];
      System.arraycopy(m_items, 0, items, 0, itemsCount);
      m_items = items;
    }
    m_items[itemsCount] = tuple;
    tuple.setParentalContext(itemsCount++, this);
    updateWidth();
  }
  
  void setInitialItems(EventTypeNonSchema eventTypeEndElement, EventCode tuple) {
    assert itemsCount == 0 && eventTypeEndElement.itemType == EventCode.ITEM_EE && 
      eventTypeEndElement.isPositionReversed() && tuple.isPositionReversed();
    if (itemsCount > m_items.length - 2) {
      final EventCode[] items = new EventCode[itemsCount + 16];
      System.arraycopy(m_items, 0, items, 0, itemsCount);
      m_items = items;
    }
    m_items[itemsCount] = tuple;
    m_items[itemsCount + 1] = eventTypeEndElement;
    // set position in reverse order
    tuple.setParentalContext(itemsCount++, this);
    eventTypeEndElement.setParentalContext(itemsCount++, this);
    updateWidth();
    eventTypeEndElement.computeItemPath();
  }

  void setInitialItems(EventTypeNonSchema elementWildcard, EventTypeNonSchema endDocument, EventCode tuple) {
    assert itemsCount == 0 && (tuple == null || tuple.isPositionReversed()) &&
      elementWildcard.itemType == EventCode.ITEM_SE_WC && elementWildcard.isPositionReversed() &&
      endDocument.itemType == EventCode.ITEM_ED && endDocument.isPositionReversed();
    if (tuple != null) {
      if (itemsCount == m_items.length) {
        final EventCode[] items = new EventCode[itemsCount + 16];
        System.arraycopy(m_items, 0, items, 0, itemsCount);
        m_items = items;
      }
      m_items[itemsCount] = tuple;
      tuple.setParentalContext(itemsCount++, this);
    }
    if (itemsCount > m_items.length - 2) {
      final EventCode[] items = new EventCode[itemsCount + 16];
      System.arraycopy(m_items, 0, items, 0, itemsCount);
      m_items = items;
    }
    m_items[itemsCount] = endDocument;
    m_items[itemsCount + 1] = elementWildcard;
    // set position in reverse order
    endDocument.setParentalContext(itemsCount++, this);
    elementWildcard.setParentalContext(itemsCount++, this);
    updateWidth();
    endDocument.computeItemPath();
    elementWildcard.computeItemPath();
  }

  void addItem(EventTypeNonSchema eventType) {
    if (itemsCount == m_items.length) {
      final EventCode[] items = new EventCode[itemsCount + 16];
      System.arraycopy(m_items, 0, items, 0, itemsCount);
      m_items = items;
    }
    m_items[itemsCount] = eventType;
    eventType.setParentalContext(itemsCount++, this);
    updateWidth();
    eventType.computeItemPath();
  }

  ///////////////////////////////////////////////////////////////////////////
  // Private methods
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Update width.
   */
  private void updateWidth() {
    switch (itemsCount) {
      case 1:
        width = 0;
        break;
      case 2:
        width = 1;
        break;
      case 3:
      case 4:
        width = 2;
        break;
      case 5:
      case 6:
      case 7:
      case 8:
        width = 3;
        break;
      case 9:
      case 10:
      case 11:
      case 12:
      case 13:
      case 14:
      case 15:
      case 16:
        width = 4;
        break;
      case 17:
      case 18:
      case 19:
      case 20:
      case 21:
      case 22:
      case 23:
      case 24:
      case 25:
      case 26:
      case 27:
      case 28:
      case 29:
      case 30:
      case 31:
      case 32:
        width = 5;
        break;
      default:
        int n, _width;
        for (_width = 0, n = itemsCount - 1; n != 0; _width++) {
          n >>= 1;
        }
        width = _width;
        break;
    }
  }

}